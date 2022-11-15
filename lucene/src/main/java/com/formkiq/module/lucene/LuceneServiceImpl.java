/**
 * MIT License
 * 
 * Copyright (c) 2018 - 2020 FormKiQ
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.formkiq.module.lucene;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexNotFoundException;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

/**
 * 
 * {@link LuceneService} implementation.
 */
public class LuceneServiceImpl implements LuceneService {

  /** Index Base Path. */
  private String indexBasePath;

  /**
   * constructor.
   * 
   * @param basePath {@link String}
   */
  public LuceneServiceImpl(final String basePath) {
    this.indexBasePath = basePath;
    if (this.indexBasePath == null) {
      throw new IllegalArgumentException("'basePath' is required");
    }
  }

  @Override
  public void addDocument(final String siteId, final Document document) throws IOException {
    addDocuments(siteId, Arrays.asList(document));
  }

  @Override
  public void addDocuments(final String siteId, final Collection<Document> documents)
      throws IOException {

    Path indexPath = getIndexPath(siteId);

    try (StandardAnalyzer analyzer = new StandardAnalyzer()) {

      IndexWriterConfig indexWriterConfig = new IndexWriterConfig(analyzer);

      try (Directory indexDirectory = FSDirectory.open(indexPath);
          IndexWriter indexWriter = new IndexWriter(indexDirectory, indexWriterConfig)) {

        for (Document doc : documents) {
          indexWriter.addDocument(doc);
        }
      }
    }
  }

  @Override
  public void deleteDocuments(final String siteId, final Term term) throws IOException {

    Path indexPath = getIndexPath(siteId);

    try (StandardAnalyzer analyzer = new StandardAnalyzer()) {

      IndexWriterConfig indexWriterConfig = new IndexWriterConfig(analyzer);

      try (Directory indexDirectory = FSDirectory.open(indexPath);
          IndexWriter indexWriter = new IndexWriter(indexDirectory, indexWriterConfig)) {

        indexWriter.deleteDocuments(term);
      }
    }
  }

  private List<Document> find(final String siteId, final Query query) throws IOException {
    try {
      Path indexPath = getIndexPath(siteId);

      try (Directory indexDirectory = FSDirectory.open(indexPath);
          IndexReader indexReader = DirectoryReader.open(indexDirectory);) {
        IndexSearcher indexSearcher = new IndexSearcher(indexReader);

        TopDocs docs = indexSearcher.search(query, 1);

        List<Document> documents = toDocuments(indexSearcher, docs);
        return documents;
      }
    } catch (IndexNotFoundException e) {
      return Collections.emptyList();
    }
  }

  @Override
  public List<Document> findByTag(final String siteId, final String tagKey, final String tagValue)
      throws IOException {
    Term term = new Term("tags#" + tagKey, tagValue);
    Query query = new TermQuery(term);
    return find(siteId, query);
  }

  @Override
  public List<Document> findByTerms(final String siteId, final String key, final String value)
      throws IOException {
    Term term = new Term(key, value);
    Query query = new TermQuery(term);
    return find(siteId, query);
  }

  @Override
  public Document findDocument(final String siteId, final String documentId) throws IOException {
    Term term = new Term("documentId", documentId);
    Query query = new TermQuery(term);
    List<Document> documents = find(siteId, query);
    return !documents.isEmpty() ? documents.get(0) : null;
  }

  private Path getIndexPath(final String siteId) {
    String indexName = siteId != null ? siteId : "default";

    Path indexPath = Paths.get(this.indexBasePath, indexName);
    return indexPath;
  }

  @Override
  public Document mergeDocument(final Document doc1, final Document doc2) {
    Document doc = new Document();

    doc1.getFields().forEach(f -> {
      if (doc.get(f.name()) == null) {
        doc.add(new StringField(f.name(), f.stringValue(), Field.Store.YES));
      }
    });

    doc2.getFields().forEach(f -> {
      if (doc.get(f.name()) == null) {
        doc.add(new StringField(f.name(), f.stringValue(), Field.Store.YES));
      }
    });

    return doc;
  }

  @Override
  public List<Document> searchFulltext(final String siteId, final String text, final int maxResults)
      throws IOException {
    try (StandardAnalyzer analyzer = new StandardAnalyzer()) {
      Query query = new QueryParser("text", analyzer).parse(text);
      return find(siteId, query);
    } catch (ParseException e) {
      throw new IOException(e);
    }
  }

  @Override
  public List<String> searchFulltextForDocumentId(final String siteId, final String text,
      final int maxResults) throws IOException {

    List<Document> documents = searchFulltext(siteId, text, maxResults);

    List<String> documentIds = documents.stream().map(d -> d.get("documentId"))
        .filter(d -> d != null).collect(Collectors.toList());

    return documentIds;
  }

  /**
   * Convert {@link IndexSearcher} {@link TopDocs} to {@link Document}.
   * 
   * @param indexSearcher {@link IndexSearcher}
   * @param docs {@link TopDocs}
   * @return {@link List} {@link Document}
   * @throws IOException IOException
   */
  private List<Document> toDocuments(final IndexSearcher indexSearcher, final TopDocs docs)
      throws IOException {
    List<Document> documents = new ArrayList<>();
    for (ScoreDoc scoreDoc : docs.scoreDocs) {
      Document doc = indexSearcher.doc(scoreDoc.doc);
      documents.add(doc);
    }
    return documents;
  }

  @Override
  public void updateDocument(final String siteId, final Term term, final Document document)
      throws IOException {

    Path indexPath = getIndexPath(siteId);

    try (StandardAnalyzer analyzer = new StandardAnalyzer()) {

      IndexWriterConfig indexWriterConfig = new IndexWriterConfig(analyzer);

      try (Directory indexDirectory = FSDirectory.open(indexPath);
          IndexWriter indexWriter = new IndexWriter(indexDirectory, indexWriterConfig)) {

        indexWriter.updateDocument(term, document);
      }
    }
  }
}
