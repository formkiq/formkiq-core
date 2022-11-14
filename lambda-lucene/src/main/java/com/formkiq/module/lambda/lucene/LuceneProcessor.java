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
package com.formkiq.module.lambda.lucene;

import static com.formkiq.aws.dynamodb.DbKeys.GSI1_PK;
import static com.formkiq.aws.dynamodb.DbKeys.GSI1_SK;
import static com.formkiq.aws.dynamodb.DbKeys.GSI2_PK;
import static com.formkiq.aws.dynamodb.DbKeys.GSI2_SK;
import static com.formkiq.aws.dynamodb.DbKeys.PK;
import static com.formkiq.aws.dynamodb.DbKeys.SK;
import static com.formkiq.aws.dynamodb.DbKeys.TAG_DELIMINATOR;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.Term;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.formkiq.aws.dynamodb.SiteIdKeyGenerator;
import com.formkiq.graalvm.annotations.Reflectable;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/** {@link RequestHandler} for handling DynamoDb to Lucene Processor. */
@Reflectable
public class LuceneProcessor implements RequestHandler<Map<String, Object>, Void> {

  /** {@link Gson}. */
  private Gson gson = new GsonBuilder().create();

  /** {@link LuceneService}. */
  private LuceneService luceneService;
  /** {@link DocumentToFulltextDocument}. */
  private DocumentToFulltextDocument fulltext = new DocumentToFulltextDocument();

  /**
   * constructor.
   * 
   */
  public LuceneProcessor() {
    this(System.getenv());
  }

  /**
   * constructor.
   *
   * @param map {@link Map}
   */
  protected LuceneProcessor(final Map<String, String> map) {
    this.luceneService = new LuceneServiceImpl(map.get("LUCENE_BASE_PATH"));
  }

  @SuppressWarnings("unchecked")
  @Override
  public Void handleRequest(final Map<String, Object> map, final Context context) {

    String json = null;

    try {

      LambdaLogger logger = context.getLogger();

      if ("true".equals(System.getenv("DEBUG"))) {
        json = this.gson.toJson(map);
        logger.log(json);
      }

      List<Map<String, Object>> records = (List<Map<String, Object>>) map.get("Records");
      processRecords(logger, records);

    } catch (Exception e) {
      e.printStackTrace();
    }

    return null;
  }

  /**
   * Process Record.
   * 
   * @param record {@link Map}
   */
  private void processRecord(final Map<String, Object> record) {

    String eventName = record.get("eventName").toString();
    Map<String, Object> dynamodb = toMap(record.get("dynamodb"));

    if ("INSERT".equalsIgnoreCase(eventName)) {

      Map<String, Object> newImage = toMap(dynamodb.get("NewImage"));

      String siteId = SiteIdKeyGenerator.getSiteId(newImage.get(PK).toString());
      String documentId = newImage.get("documentId").toString();

      try {

        writeToIndex(siteId, documentId, newImage);

      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }

  /**
   * Process Records.
   * 
   * @param logger {@link LambdaLogger}
   * @param records {@link List} {@link Map}
   */
  private void processRecords(final LambdaLogger logger, final List<Map<String, Object>> records) {
    for (Map<String, Object> record : records) {

      if (record.containsKey("eventName")) {
        processRecord(record);
      }
    }
  }

  /**
   * Remove DynamoDb Keys.
   * 
   * @param map {@link Map}
   */
  private void removeDynamodbKeys(final Map<String, Object> map) {
    map.remove(PK);
    map.remove(SK);
    map.remove(GSI1_PK);
    map.remove(GSI1_SK);
    map.remove(GSI2_PK);
    map.remove(GSI2_SK);
  }

  @SuppressWarnings("unchecked")
  private Map<String, Object> toMap(final Object object) {
    return (Map<String, Object>) object;
  }

  /**
   * Write Data to Lucene Index.
   * 
   * @param siteId {@link String}
   * @param documentId {@link String}
   * @param data {@link Map}
   * @throws IOException IOException
   */
  private void writeToIndex(final String siteId, final String documentId,
      final Map<String, Object> data) throws IOException {

    boolean isDocument = data.get(SK).toString().contains("document");
    boolean isTag = data.get(SK).toString().contains("tags" + TAG_DELIMINATOR);

    removeDynamodbKeys(data);

    if (isDocument) {
      Document document = new DocumentMapToDocument().apply(data);
      document = this.fulltext.apply(document);
      addOrUpdate(siteId, document);
    }

    if (isTag) {
      Document document = new DocumentTagMapToDocument().apply(data);
      addOrUpdate(siteId, document);
    }
  }

  /**
   * Add or Update Document.
   * 
   * @param siteId {@link String}
   * @param document {@link Document}
   * @throws IOException IOException
   */
  private void addOrUpdate(final String siteId, final Document document) throws IOException {
    String documentId = document.get("documentId");

    Document existingDocument = this.luceneService.findDocument(siteId, documentId);

    if (existingDocument != null) {

      existingDocument.getFields().forEach(f -> {
        if (document.get(f.name()) == null) {
          document.add(new StringField(f.name(), f.stringValue(), Field.Store.YES));
        }
      });

      Term term = new Term("documentId", documentId);
      this.luceneService.updateDocument(siteId, term, this.fulltext.apply(document));
    } else {
      this.luceneService.addDocument(siteId, document);
    }
  }
}
