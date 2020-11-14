/**
 *
 * FormKiQ License
 *
 * Copyright (c) 2018 FormKiQ, INC
 * 
 * This code is the property of FormKiQ, INC. In the Software Development Agreement signed by both
 * FormKiQ and your company, FormKiQ grants you a limited license to use, modify, and create
 * derivative works of this code. Please consult the Software Development Agreement for the complete
 * terms under which you may use this code.
 *
 */
package com.formkiq.stacks.api.awstest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import com.formkiq.stacks.client.FormKiqClientV1;
import com.formkiq.stacks.client.requests.AddDocumentTagRequest;
import com.formkiq.stacks.client.requests.DeleteDocumentTagRequest;
import com.formkiq.stacks.client.requests.GetDocumentTagsKeyRequest;
import com.formkiq.stacks.client.requests.GetDocumentTagsRequest;
import com.formkiq.stacks.client.requests.OptionsDocumentTagsKeyRequest;
import com.formkiq.stacks.client.requests.OptionsDocumentTagsRequest;
import com.formkiq.stacks.client.requests.UpdateDocumentTagKeyRequest;

/**
 * GET, OPTIONS, POST /documents/{documentId}/tags tests.
 *
 */
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
   * Test /documents/{documentId}/tags.
   * 
   * @throws Exception Exception
   */
  @SuppressWarnings("unchecked")
  @Test(timeout = TEST_TIMEOUT)
  public void testDocumentsTags01() throws Exception {

    for (FormKiqClientV1 client : getFormKiqClients()) {

      // given
      String documentId = addDocumentWithoutFile(client);
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
        assertEquals(2, list.size());
        map = list.get(0);
        assertEquals("test", map.get("key"));
        assertEquals("somevalue", map.get("value"));
        verifyUserId(map);
        assertNotNull(map.get("insertedDate"));

        map = list.get(1);
        assertEquals("untagged", map.get("key"));
        assertNull(map.get("value"));
        verifyUserId(map);
        assertNotNull(map.get("insertedDate"));

      } finally {
        deleteDocument(client, documentId);
      }
    }

  }

  /**
   * Test /documents/{documentId}/tags/{tagKey}.
   * 
   * @throws Exception Exception
   */
  @Test(timeout = TEST_TIMEOUT)
  public void testDocumentsTags02() throws Exception {

    for (FormKiqClientV1 client : getFormKiqClients()) {

      // given
      String documentId = addDocumentWithoutFile(client);
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
        UpdateDocumentTagKeyRequest updatereq = new UpdateDocumentTagKeyRequest()
            .documentId(documentId).tagKey("category").tagValue("This is a sample");

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
   * Verify UserId.
   * 
   * @param map {@link Map}
   */
  private void verifyUserId(final Map<String, Object> map) {
    if ("testadminuser@formkiq.com".equals(map.get("userId"))) {
      assertEquals("testadminuser@formkiq.com", map.get("userId"));
    } else {
      assertTrue(map.get("userId").toString().contains(":user/"));
    }
  }
}
