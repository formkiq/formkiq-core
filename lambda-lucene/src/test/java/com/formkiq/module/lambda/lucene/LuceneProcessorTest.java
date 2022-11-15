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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.apache.lucene.document.Document;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import com.amazonaws.services.lambda.runtime.Context;
import com.formkiq.testutils.aws.LambdaContextRecorder;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import software.amazon.awssdk.utils.IoUtils;

/**
 * 
 * Unit Tests {@link LuceneProcessor}.
 *
 */
class LuceneProcessorTest {

  /** {@link Gson}. */
  private static final Gson GSON = new GsonBuilder().create();
  /** Max results. */
  private static final int MAX = 10;
  /** {@link LuceneProcessor}. */
  private static LuceneProcessor processor;
  /** {@link LuceneService}. */
  private static LuceneService service;

  @BeforeAll
  public static void beforeAll() {
    String tmpdir = System.getProperty("java.io.tmpdir") + UUID.randomUUID();
    processor = new LuceneProcessor(Map.of("LUCENE_BASE_PATH", tmpdir));
    service = new LuceneServiceImpl(tmpdir);
  }

  /** {@link Context}. */
  private Context context = new LambdaContextRecorder();

  private void expectDoc1(final Document doc) {
    final int expected = 5;
    assertNotNull(doc);
    assertEquals(expected, doc.getFields().size());
    assertEquals("/somewhere/else/test.pdf", doc.get("path"));
    assertEquals("acd4be1b-9466-4dcd-b8b8-e5b19135b460", doc.get("documentId"));
    assertEquals("testadminuser@formkiq.com", doc.get("userId"));
    assertEquals("true", doc.get("tags#untagged"));
    assertEquals("this is some content karate", doc.get("md#content"));
  }

  private void expectDoc2(final Document doc) {
    final int expected = 3;
    assertNotNull(doc);
    assertEquals(expected, doc.getFields().size());
    assertEquals("bleh/some.pdf", doc.get("path"));
    assertEquals("717a3cee-888d-47e0-83a3-a7487a588954", doc.get("documentId"));
    assertEquals("arn:aws:iam::111111111:user/mike", doc.get("userId"));
  }

  /**
   * Load Request File.
   * 
   * @param name {@link String}
   * @return {@link Map}
   * @throws IOException IOException
   */
  @SuppressWarnings("unchecked")
  private Map<String, Object> loadRequest(final String name) throws IOException {

    try (InputStream is = getClass().getResourceAsStream(name)) {
      String s = IoUtils.toUtf8String(is);
      return GSON.fromJson(s, Map.class);
    }
  }

  /**
   * Insert 2 records.
   * 
   * @throws Exception Exception
   */
  @Test
  void testHandleRequest01() throws Exception {
    // given
    String siteId = null;
    Map<String, Object> map = loadRequest("/insert.json");

    // when
    processor.handleRequest(map, this.context);

    // then
    String documentId = "acd4be1b-9466-4dcd-b8b8-e5b19135b460";
    Document document = service.findDocument(siteId, documentId);
    expectDoc1(document);

    List<Document> documents = service.searchFulltext(siteId, "karate", MAX);
    assertEquals(1, documents.size());
    Document doc = documents.get(0);
    expectDoc1(doc);

    List<Document> findByTerms = service.findByTerms(siteId, "userId", "testadminuser@formkiq.com");
    assertEquals(1, findByTerms.size());
    expectDoc1(findByTerms.get(0));

    List<Document> findByTag = service.findByTag(siteId, "untagged", "true");
    assertEquals(1, findByTag.size());
    expectDoc1(findByTag.get(0));

    documents = service.searchFulltext(siteId, "test.pdf", MAX);
    assertEquals(1, documents.size());
    expectDoc1(documents.get(0));

    documents = service.searchFulltext(siteId, "bleh.pdf", MAX);
    assertEquals(0, documents.size());
  }

  /**
   * Modify records.
   * 
   * @throws Exception Exception
   */
  @Test
  void testHandleRequest02() throws Exception {
    // given
    String siteId = null;
    String documentId = "717a3cee-888d-47e0-83a3-a7487a588954";
    Map<String, Object> map = loadRequest("/modify.json");

    // when
    processor.handleRequest(map, this.context);

    // then
    Document document = service.findDocument(siteId, documentId);
    expectDoc2(document);

    List<Document> documents = service.searchFulltext(siteId, "some.pdf", MAX);
    assertEquals(1, documents.size());
    Document doc = documents.get(0);
    expectDoc2(doc);
  }
}
