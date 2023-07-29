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
package com.formkiq.stacks.api.awstest;

import static com.formkiq.testutils.aws.FkqDocumentService.waitForDocumentContent;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.http.HttpResponse;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import com.formkiq.stacks.client.FormKiqClientV1;
import com.formkiq.stacks.client.models.AddDocument;
import com.formkiq.stacks.client.models.AddDocumentResponse;
import com.formkiq.stacks.client.models.AddDocumentTag;
import com.formkiq.stacks.client.models.DocumentTags;
import com.formkiq.stacks.client.requests.AddDocumentRequest;
import com.formkiq.stacks.client.requests.AddDocumentTagRequest;
import com.formkiq.stacks.client.requests.DeleteDocumentTagRequest;
import com.formkiq.stacks.client.requests.GetDocumentTagsKeyRequest;
import com.formkiq.stacks.client.requests.GetDocumentTagsRequest;
import com.formkiq.stacks.client.requests.OptionsDocumentTagsKeyRequest;
import com.formkiq.stacks.client.requests.OptionsDocumentTagsRequest;
import com.formkiq.stacks.client.requests.SetDocumentTagKeyRequest;
import com.formkiq.stacks.client.requests.SetDocumentTagsRequest;
import com.formkiq.stacks.client.requests.UpdateDocumentTagsRequest;
import joptsimple.internal.Strings;

/**
 * GET, OPTIONS, POST /documents/{documentId}/tags tests.
 *
 */
@Execution(ExecutionMode.CONCURRENT)
public class DocumentsDocumentIdTagsRequestTest extends AbstractApiTest {

  /** JUnit Test Timeout. */
  private static final int TEST_TIMEOUT = 30000;

  /**
   * Delete Document Tag.
   * 
   * @param client {@link FormKiqClientV1}
   * @param documentId {@link String}
   * @throws IOException IOException
   * @throws URISyntaxException URISyntaxException
   * @throws InterruptedException InterruptedException
   */
  private void deleteDocumentTag(final FormKiqClientV1 client, final String documentId)
      throws IOException, URISyntaxException, InterruptedException {
    // given
    DeleteDocumentTagRequest delRequest =
        new DeleteDocumentTagRequest().documentId(documentId).tagKey("category");
    GetDocumentTagsKeyRequest req =
        new GetDocumentTagsKeyRequest().documentId(documentId).tagKey("category");

    // when
    HttpResponse<String> response = client.deleteDocumentTagAsHttpResponse(delRequest);

    // then
    assertEquals("200", String.valueOf(response.statusCode()));
    HttpResponse<String> response2 = client.getDocumentTagAsHttpResponse(req);
    assertEquals("404", String.valueOf(response2.statusCode()));
  }

  /**
   * Test /documents/{documentId}/tags with tagValue.
   * 
   * @throws Exception Exception
   */
  @SuppressWarnings("unchecked")
  @Test
  @Timeout(unit = TimeUnit.SECONDS, value = TEST_TIMEOUT)
  public void testDocumentsTags01() throws Exception {

    for (FormKiqClientV1 client : getFormKiqDefaultClients()) {
      // given
      String documentId = addDocumentWithoutFile(client, null, null);
      AddDocumentTagRequest request =
          new AddDocumentTagRequest().documentId(documentId).tagKey("test").tagValue("somevalue");
      OptionsDocumentTagsRequest optionReq =
          new OptionsDocumentTagsRequest().documentId(documentId);

      try {
        // when
        HttpResponse<String> response = client.addDocumentTagAsHttpResponse(request);

        // then
        assertEquals("201", String.valueOf(response.statusCode()));
        assertRequestCorsHeaders(response.headers());

        HttpResponse<String> options = client.optionsDocumentTags(optionReq);
        assertPreflightedCorsHeaders(options.headers());

        // given
        GetDocumentTagsRequest req = new GetDocumentTagsRequest().documentId(documentId);
        // when
        response = client.getDocumentTagsAsHttpResponse(req);

        // then
        assertEquals("200", String.valueOf(response.statusCode()));
        assertRequestCorsHeaders(response.headers());

        Map<String, Object> map = toMap(response);
        List<Map<String, Object>> list = (List<Map<String, Object>>) map.get("tags");
        assertEquals(1, list.size());
        map = list.get(0);
        assertEquals("test", map.get("key"));
        assertEquals("somevalue", map.get("value"));
        verifyUserId(map);
        assertNotNull(map.get("insertedDate"));

      } finally {
        deleteDocument(client, documentId);
      }
    }
  }

  /**
   * Test /documents/{documentId}/tags with tagValues.
   * 
   * @throws Exception Exception
   */
  @SuppressWarnings("unchecked")
  @Test
  @Timeout(unit = TimeUnit.SECONDS, value = TEST_TIMEOUT)
  public void testDocumentsTags02() throws Exception {

    for (FormKiqClientV1 client : getFormKiqDefaultClients()) {

      // given
      String documentId = addDocumentWithoutFile(client, null, null);
      AddDocumentTagRequest request = new AddDocumentTagRequest().documentId(documentId)
          .tagKey("test").tagValues(Arrays.asList("somevalue0", "somevalue1"));
      OptionsDocumentTagsRequest optionReq =
          new OptionsDocumentTagsRequest().documentId(documentId);

      try {
        // when
        HttpResponse<String> response = client.addDocumentTagAsHttpResponse(request);

        // then
        assertEquals("201", String.valueOf(response.statusCode()));
        assertRequestCorsHeaders(response.headers());

        HttpResponse<String> options = client.optionsDocumentTags(optionReq);
        assertPreflightedCorsHeaders(options.headers());

        // given
        GetDocumentTagsRequest req = new GetDocumentTagsRequest().documentId(documentId);
        // when
        response = client.getDocumentTagsAsHttpResponse(req);

        // then
        assertEquals("200", String.valueOf(response.statusCode()));
        assertRequestCorsHeaders(response.headers());

        Map<String, Object> map = toMap(response);
        List<Map<String, Object>> list = (List<Map<String, Object>>) map.get("tags");
        assertEquals(1, list.size());
        map = list.get(0);
        assertEquals("test", map.get("key"));
        assertEquals("[somevalue0, somevalue1]", map.get("values").toString());
        assertNull(map.get("value"));
        verifyUserId(map);
        assertNotNull(map.get("insertedDate"));

      } finally {
        deleteDocument(client, documentId);
      }
    }
  }

  /**
   * Test /documents/{documentId}/tags/{tagKey} VALUE.
   * 
   * @throws Exception Exception
   */
  @Test
  @Timeout(unit = TimeUnit.SECONDS, value = TEST_TIMEOUT)
  public void testDocumentsTags03() throws Exception {

    for (FormKiqClientV1 client : getFormKiqDefaultClients()) {

      // given
      String documentId = addDocumentWithoutFile(client, null, null);
      GetDocumentTagsKeyRequest req =
          new GetDocumentTagsKeyRequest().documentId(documentId).tagKey("category");
      OptionsDocumentTagsKeyRequest oreq =
          new OptionsDocumentTagsKeyRequest().documentId(documentId).tagKey("category");

      try {
        // when
        HttpResponse<String> response =
            client.addDocumentTagAsHttpResponse(new AddDocumentTagRequest().documentId(documentId)
                .tagKey("category").tagValue("somevalue"));

        // then
        assertEquals("201", String.valueOf(response.statusCode()));

        // then
        response = client.getDocumentTagAsHttpResponse(req);

        assertEquals("200", String.valueOf(response.statusCode()));
        assertRequestCorsHeaders(response.headers());

        HttpResponse<String> options = client.optionsDocumentTag(oreq);
        assertPreflightedCorsHeaders(options.headers());

        Map<String, Object> map = toMap(response);
        assertEquals("category", map.get("key"));
        assertEquals("somevalue", map.get("value"));
        verifyUserId(map);
        assertEquals("userdefined", map.get("type"));
        assertEquals(documentId, map.get("documentId"));
        assertNotNull(map.get("insertedDate"));

        // given
        GetDocumentTagsKeyRequest getreq =
            new GetDocumentTagsKeyRequest().documentId(documentId).tagKey("category");
        SetDocumentTagKeyRequest updatereq = new SetDocumentTagKeyRequest().documentId(documentId)
            .tagKey("category").tagValue("This is a sample");

        // when
        response = client.updateDocumentTagAsHttpResponse(updatereq);

        // then
        assertEquals("200", String.valueOf(response.statusCode()));

        // when
        response = client.getDocumentTagAsHttpResponse(getreq);

        // then
        assertEquals("200", String.valueOf(response.statusCode()));

        map = toMap(response);
        assertEquals("category", map.get("key"));
        assertEquals("This is a sample", map.get("value"));
        verifyUserId(map);
        assertEquals("userdefined", map.get("type"));
        assertEquals(documentId, map.get("documentId"));
        assertNotNull(map.get("insertedDate"));

        deleteDocumentTag(client, documentId);

      } finally {
        deleteDocument(client, documentId);
      }
    }
  }

  /**
   * Test /documents/{documentId}/tags/{tagKey} update VALUES to VALUE.
   * 
   * @throws Exception Exception
   */
  @Test
  @Timeout(unit = TimeUnit.SECONDS, value = TEST_TIMEOUT)
  public void testDocumentsTags04() throws Exception {

    for (FormKiqClientV1 client : getFormKiqDefaultClients()) {

      // given
      String documentId = addDocumentWithoutFile(client, null, null);
      GetDocumentTagsKeyRequest req =
          new GetDocumentTagsKeyRequest().documentId(documentId).tagKey("category");
      OptionsDocumentTagsKeyRequest oreq =
          new OptionsDocumentTagsKeyRequest().documentId(documentId).tagKey("category");

      try {
        // when
        HttpResponse<String> response =
            client.addDocumentTagAsHttpResponse(new AddDocumentTagRequest().documentId(documentId)
                .tagKey("category").tagValues(Arrays.asList("somevalue0", "somevalue1")));

        // then
        assertEquals("201", String.valueOf(response.statusCode()));

        // then
        response = client.getDocumentTagAsHttpResponse(req);

        assertEquals("200", String.valueOf(response.statusCode()));
        assertRequestCorsHeaders(response.headers());

        HttpResponse<String> options = client.optionsDocumentTag(oreq);
        assertPreflightedCorsHeaders(options.headers());

        Map<String, Object> map = toMap(response);
        assertEquals("category", map.get("key"));
        assertEquals("[somevalue0, somevalue1]", map.get("values").toString());
        assertNull(map.get("value"));
        verifyUserId(map);
        assertEquals("userdefined", map.get("type"));
        assertEquals(documentId, map.get("documentId"));
        assertNotNull(map.get("insertedDate"));

        // given
        GetDocumentTagsKeyRequest getreq =
            new GetDocumentTagsKeyRequest().documentId(documentId).tagKey("category");
        SetDocumentTagKeyRequest updatereq = new SetDocumentTagKeyRequest().documentId(documentId)
            .tagKey("category").tagValue("This is a sample");

        // when
        response = client.updateDocumentTagAsHttpResponse(updatereq);

        // then
        assertEquals("200", String.valueOf(response.statusCode()));

        // when
        response = client.getDocumentTagAsHttpResponse(getreq);

        // then
        assertEquals("200", String.valueOf(response.statusCode()));

        map = toMap(response);
        assertEquals("category", map.get("key"));
        assertEquals("This is a sample", map.get("value"));
        verifyUserId(map);
        assertEquals("userdefined", map.get("type"));
        assertEquals(documentId, map.get("documentId"));
        assertNotNull(map.get("insertedDate"));

        deleteDocumentTag(client, documentId);

      } finally {
        deleteDocument(client, documentId);
      }
    }
  }

  /**
   * Test DELETE /documents/{documentId}/tags/{tagKey}/{tagValue}.
   * 
   * @throws Exception Exception
   */
  @Test
  @Timeout(unit = TimeUnit.SECONDS, value = TEST_TIMEOUT)
  public void testDocumentsTags05() throws Exception {

    for (FormKiqClientV1 client : getFormKiqDefaultClients()) {

      // given
      String documentId = addDocumentWithoutFile(client, null, null);
      GetDocumentTagsKeyRequest req =
          new GetDocumentTagsKeyRequest().documentId(documentId).tagKey("category");
      OptionsDocumentTagsKeyRequest oreq =
          new OptionsDocumentTagsKeyRequest().documentId(documentId).tagKey("category");

      try {
        // when
        HttpResponse<String> response =
            client.addDocumentTagAsHttpResponse(new AddDocumentTagRequest().documentId(documentId)
                .tagKey("category").tagValues(Arrays.asList("somevalue0", "somevalue1")));

        // then
        assertEquals("201", String.valueOf(response.statusCode()));

        // then
        response = client.getDocumentTagAsHttpResponse(req);

        assertEquals("200", String.valueOf(response.statusCode()));
        assertRequestCorsHeaders(response.headers());

        HttpResponse<String> options = client.optionsDocumentTag(oreq);
        assertPreflightedCorsHeaders(options.headers());

        Map<String, Object> map = toMap(response);
        assertEquals("[somevalue0, somevalue1]", map.get("values").toString());

        // given
        DeleteDocumentTagRequest tagreq = new DeleteDocumentTagRequest().documentId(documentId)
            .tagKey("category").tagValue("somevalue1");

        // when
        response = client.deleteDocumentTagAsHttpResponse(tagreq);

        // then
        assertEquals("200", String.valueOf(response.statusCode()));
        response = client.getDocumentTagAsHttpResponse(req);
        map = toMap(response);
        assertEquals("somevalue0", map.get("value").toString());
        assertNull(map.get("values"));

        deleteDocumentTag(client, documentId);

      } finally {
        deleteDocument(client, documentId);
      }
    }
  }

  /**
   * Test POST /documents/{documentId}/tags with multiple tags.
   * 
   * @throws Exception Exception
   */
  @SuppressWarnings("unchecked")
  @Test
  @Timeout(unit = TimeUnit.SECONDS, value = TEST_TIMEOUT)
  public void testDocumentsTags06() throws Exception {

    for (FormKiqClientV1 client : getFormKiqDefaultClients()) {

      // given
      String documentId = addDocumentWithoutFile(client, null, null);
      AddDocumentTag tag0 = new AddDocumentTag().key("test1").value("somevalue");
      AddDocumentTag tag1 = new AddDocumentTag().key("test2");
      AddDocumentTag tag2 = new AddDocumentTag().key("test3").values(Arrays.asList("abc", "xyz"));

      List<AddDocumentTag> tags = Arrays.asList(tag0, tag1, tag2);
      AddDocumentTagRequest request = new AddDocumentTagRequest().documentId(documentId).tags(tags);
      OptionsDocumentTagsRequest optionReq =
          new OptionsDocumentTagsRequest().documentId(documentId);

      try {
        // when
        HttpResponse<String> response = client.addDocumentTagAsHttpResponse(request);

        // then
        assertEquals("201", String.valueOf(response.statusCode()));
        assertRequestCorsHeaders(response.headers());

        HttpResponse<String> options = client.optionsDocumentTags(optionReq);
        assertPreflightedCorsHeaders(options.headers());

        // given
        GetDocumentTagsRequest req = new GetDocumentTagsRequest().documentId(documentId);
        // when
        response = client.getDocumentTagsAsHttpResponse(req);

        // then
        assertEquals("200", String.valueOf(response.statusCode()));
        assertRequestCorsHeaders(response.headers());

        Map<String, Object> map = toMap(response);
        List<Map<String, Object>> list = (List<Map<String, Object>>) map.get("tags");
        assertEquals(tags.size(), list.size());
        map = list.get(0);
        assertEquals("test1", map.get("key"));
        assertEquals("somevalue", map.get("value"));
        verifyUserId(map);
        assertNotNull(map.get("insertedDate"));

        map = list.get(1);
        assertEquals("test2", map.get("key"));
        assertEquals("", map.get("value"));
        verifyUserId(map);
        assertNotNull(map.get("insertedDate"));

        map = list.get(2);
        assertEquals("test3", map.get("key"));
        assertNull(map.get("value"));
        assertEquals("[abc, xyz]", map.get("values").toString());
        verifyUserId(map);
        assertNotNull(map.get("insertedDate"));

      } finally {
        deleteDocument(client, documentId);
      }
    }
  }

  /**
   * Test PATCH / PUT /documents/{documentId}/tags.
   * 
   * @throws Exception Exception
   */
  @Test
  @Timeout(unit = TimeUnit.SECONDS, value = TEST_TIMEOUT)
  public void testDocumentsTags07() throws Exception {

    String siteId = null;
    for (FormKiqClientV1 client : getFormKiqDefaultClients()) {
      // given
      String content = "this is some data";
      List<AddDocumentTag> tags =
          Arrays.asList(new AddDocumentTag().key("category").value("person"),
              new AddDocumentTag().key("user").value("111"));

      // when
      AddDocumentResponse addDocument = client.addDocument(new AddDocumentRequest().siteId(siteId)
          .document(new AddDocument().content(content).tags(tags)));

      // then
      String documentId = addDocument.documentId();

      waitForDocumentContent(client, siteId, documentId);
      GetDocumentTagsKeyRequest getReq =
          new GetDocumentTagsKeyRequest().siteId(siteId).documentId(documentId).tagKey("category");
      assertEquals("person", client.getDocumentTag(getReq).value());

      // given
      UpdateDocumentTagsRequest updateTagReq =
          new UpdateDocumentTagsRequest().siteId(siteId).documentId(documentId)
              .tags(Arrays.asList(new AddDocumentTag().key("playerId").value("555"),
                  new AddDocumentTag().key("category").values(Arrays.asList("c0", "c1"))));

      // when
      assertTrue(client.updateDocumentTags(updateTagReq));

      // then
      int i = 0;
      final int expected = 3;
      GetDocumentTagsRequest getTagsReq =
          new GetDocumentTagsRequest().siteId(siteId).documentId(documentId);
      DocumentTags taglist = client.getDocumentTags(getTagsReq);
      assertEquals(expected, taglist.tags().size());
      assertEquals("category", taglist.tags().get(i).key());
      assertEquals("c0,c1", Strings.join(taglist.tags().get(i++).values(), ","));

      assertEquals("playerId", taglist.tags().get(i).key());
      assertEquals("555", taglist.tags().get(i++).value());
      assertEquals("user", taglist.tags().get(i).key());
      assertEquals("111", taglist.tags().get(i++).value());

      // given
      SetDocumentTagsRequest setTagsReq =
          new SetDocumentTagsRequest().siteId(siteId).documentId(documentId).tags(tags);

      // when
      assertTrue(client.setDocumentTags(setTagsReq));

      // then
      i = 0;
      taglist = client.getDocumentTags(getTagsReq);
      assertEquals(2, taglist.tags().size());
      assertEquals("category", taglist.tags().get(i).key());
      assertEquals("person", taglist.tags().get(i++).value());

      assertEquals("user", taglist.tags().get(i).key());
      assertEquals("111", taglist.tags().get(i++).value());
    }
  }

  /**
   * Verify UserId.
   * 
   * @param map {@link Map}
   */
  private void verifyUserId(final Map<String, Object> map) {
    if ("testadminuser@formkiq.com".equals(map.get("userId"))) {
      assertEquals("testadminuser@formkiq.com", map.get("userId"));
    } else if (map.get("userId").toString().contains(":user/")) {
      assertTrue(map.get("userId").toString().contains(":user/"));
    } else {
      assertEquals("My API Key", map.get("userId"));
    }
  }
}
