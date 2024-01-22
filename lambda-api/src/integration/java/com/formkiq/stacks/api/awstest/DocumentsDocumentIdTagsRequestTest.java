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

import static com.formkiq.testutils.aws.FkqDocumentService.addDocument;
import static com.formkiq.testutils.aws.FkqDocumentService.waitForDocumentContent;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.fail;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import com.formkiq.aws.services.lambda.ApiResponseStatus;
import com.formkiq.client.api.DocumentTagsApi;
import com.formkiq.client.api.DocumentsApi;
import com.formkiq.client.invoker.ApiClient;
import com.formkiq.client.invoker.ApiException;
import com.formkiq.client.model.AddDocumentRequest;
import com.formkiq.client.model.AddDocumentResponse;
import com.formkiq.client.model.AddDocumentTag;
import com.formkiq.client.model.AddDocumentTagsRequest;
import com.formkiq.client.model.DocumentTag;
import com.formkiq.client.model.GetDocumentTagResponse;
import com.formkiq.client.model.GetDocumentTagsResponse;
import com.formkiq.testutils.aws.AbstractAwsIntegrationTest;
import joptsimple.internal.Strings;

/**
 * GET, OPTIONS, POST /documents/{documentId}/tags tests.
 *
 */
@Execution(ExecutionMode.CONCURRENT)
public class DocumentsDocumentIdTagsRequestTest extends AbstractAwsIntegrationTest {

  /** JUnit Test Timeout. */
  private static final int TEST_TIMEOUT = 30;

  /**
   * Delete Document Tag.
   * 
   * @param client {@link ApiClient}
   * @param documentId {@link String}
   * @param tagKey {@link String}
   * @throws ApiException ApiException
   */
  private void deleteDocumentTag(final ApiClient client, final String documentId,
      final String tagKey) throws ApiException {
    DocumentTagsApi api = new DocumentTagsApi(client);
    api.deleteDocumentTag(documentId, tagKey, null);

    try {
      api.getDocumentTag(documentId, tagKey, null, null);
      fail();
    } catch (ApiException e) {
      assertEquals(ApiResponseStatus.SC_NOT_FOUND.getStatusCode(), e.getCode());
    }
  }

  /**
   * Test /documents/{documentId}/tags with tagValue.
   * 
   * @throws Exception Exception
   */
  @Test
  @Timeout(unit = TimeUnit.SECONDS, value = TEST_TIMEOUT)
  public void testDocumentsTags01() throws Exception {

    String siteId = null;

    for (ApiClient client : getApiClients(null)) {
      // given
      String documentId = addDocument(client, siteId, null, new byte[] {}, null, null);
      DocumentTagsApi api = new DocumentTagsApi(client);
      AddDocumentTagsRequest req = new AddDocumentTagsRequest()
          .addTagsItem(new AddDocumentTag().key("test").value("somevalue"));

      try {
        // when
        api.addDocumentTags(documentId, req, siteId, null);

        // when
        GetDocumentTagsResponse response =
            api.getDocumentTags(documentId, siteId, null, null, null, null);

        // then
        List<DocumentTag> list = response.getTags();
        assertEquals(1, list.size());
        assertEquals("test", list.get(0).getKey());
        assertEquals("somevalue", list.get(0).getValue());
        assertNotNull(list.get(0).getUserId());
        assertNotNull(list.get(0).getInsertedDate());

      } finally {
        deleteDocumentTag(client, documentId, "test");
      }
    }
  }

  /**
   * Test /documents/{documentId}/tags with tagValues.
   * 
   * @throws Exception Exception
   */
  @Test
  @Timeout(unit = TimeUnit.SECONDS, value = TEST_TIMEOUT)
  public void testDocumentsTags02() throws Exception {

    // given
    String siteId = null;
    for (ApiClient client : getApiClients(null)) {

      DocumentTagsApi api = new DocumentTagsApi(client);

      String documentId = addDocument(client, siteId, null, new byte[] {}, null, null);

      AddDocumentTagsRequest req = new AddDocumentTagsRequest().addTagsItem(
          new AddDocumentTag().key("test").values(Arrays.asList("somevalue0", "somevalue1")));

      try {
        // when
        api.addDocumentTags(documentId, req, siteId, null);

        // given
        // when
        GetDocumentTagsResponse response =
            api.getDocumentTags(documentId, siteId, null, null, null, null);

        // then
        List<DocumentTag> list = response.getTags();
        assertEquals(1, list.size());
        // map = list.get(0);
        assertEquals("test", list.get(0).getKey());
        assertEquals("somevalue0,somevalue1",
            list.get(0).getValues().stream().collect(Collectors.joining(",")));
        assertNull(list.get(0).getValue());
        assertNotNull(list.get(0).getUserId());
        assertNotNull(list.get(0).getInsertedDate());

      } finally {
        deleteDocumentTag(client, documentId, "test");
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

    // given
    String siteId = null;
    for (ApiClient client : getApiClients(null)) {

      DocumentTagsApi api = new DocumentTagsApi(client);

      String documentId = addDocument(client, siteId, null, new byte[] {}, null, null);
      AddDocumentTagsRequest req = new AddDocumentTagsRequest()
          .addTagsItem(new AddDocumentTag().key("category").value("somevalue"));

      try {
        // when
        api.addDocumentTags(documentId, req, siteId, null);

        // then
        GetDocumentTagResponse response = api.getDocumentTag(documentId, "category", siteId, null);
        assertEquals("category", response.getKey());
        assertEquals("somevalue", response.getValue());
        assertNotNull(response.getUserId());
        assertEquals("userdefined", response.getType());
        assertEquals(documentId, response.getDocumentId());
        assertNotNull(response.getInsertedDate());

        // given
        AddDocumentTagsRequest tags = new AddDocumentTagsRequest()
            .addTagsItem(new AddDocumentTag().key("category").value("This is a sample"));

        // when
        api.updateDocumentTags(documentId, tags, siteId);

        // then
        response = api.getDocumentTag(documentId, "category", siteId, null);

        assertEquals("category", response.getKey());
        assertEquals("This is a sample", response.getValue());
        assertNotNull(response.getUserId());
        assertEquals("userdefined", response.getType());
        assertEquals(documentId, response.getDocumentId());
        assertNotNull(response.getInsertedDate());

      } finally {
        deleteDocumentTag(client, documentId, "category");
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

    String siteId = null;
    for (ApiClient client : getApiClients(null)) {

      // given
      DocumentTagsApi api = new DocumentTagsApi(client);
      String documentId = addDocument(client, siteId, null, new byte[] {}, null, null);

      AddDocumentTagsRequest req = new AddDocumentTagsRequest().addTagsItem(
          new AddDocumentTag().key("category").values(Arrays.asList("somevalue0", "somevalue1")));

      try {
        // when
        api.addDocumentTags(documentId, req, siteId, null);

        // then
        GetDocumentTagResponse response = api.getDocumentTag(documentId, "category", siteId, null);

        assertEquals("category", response.getKey());
        assertEquals("somevalue0,somevalue1",
            response.getValues().stream().collect(Collectors.joining(",")));
        assertNull(response.getValue());
        assertNotNull(response.getUserId());
        assertEquals("userdefined", response.getType());
        assertEquals(documentId, response.getDocumentId());
        assertNotNull(response.getInsertedDate());

        // given
        AddDocumentTagsRequest updatereq = new AddDocumentTagsRequest()
            .addTagsItem(new AddDocumentTag().key("category").value("This is a sample"));

        // when
        api.updateDocumentTags(documentId, updatereq, siteId);

        // then
        response = api.getDocumentTag(documentId, "category", siteId, null);

        assertEquals("category", response.getKey());
        assertEquals("This is a sample", response.getValue());
        assertNotNull(response.getUserId());
        assertEquals("userdefined", response.getType());
        assertEquals(documentId, response.getDocumentId());
        assertNotNull(response.getInsertedDate());

      } finally {
        deleteDocumentTag(client, documentId, "category");
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

    String siteId = null;
    for (ApiClient client : getApiClients(null)) {

      // given
      DocumentTagsApi api = new DocumentTagsApi(client);
      String documentId = addDocument(client, siteId, null, new byte[] {}, null, null);

      AddDocumentTagsRequest req = new AddDocumentTagsRequest().addTagsItem(
          new AddDocumentTag().key("category").values(Arrays.asList("somevalue0", "somevalue1")));

      try {
        // when
        api.addDocumentTags(documentId, req, siteId, null);

        // then
        GetDocumentTagResponse response = api.getDocumentTag(documentId, "category", siteId, null);

        assertEquals("somevalue0,somevalue1",
            response.getValues().stream().collect(Collectors.joining(",")));

        // when
        api.deleteDocumentTagAndValue(documentId, "category", "somevalue1", siteId, null);

        // then
        response = api.getDocumentTag(documentId, "category", siteId, null);
        assertEquals("somevalue0", response.getValue().toString());

      } finally {
        deleteDocumentTag(client, documentId, "category");
      }
    }
  }

  /**
   * Test POST /documents/{documentId}/tags with multiple tags.
   * 
   * @throws Exception Exception
   */
  @Test
  @Timeout(unit = TimeUnit.SECONDS, value = TEST_TIMEOUT)
  public void testDocumentsTags06() throws Exception {

    String siteId = null;
    for (ApiClient client : getApiClients(null)) {

      // given
      DocumentTagsApi api = new DocumentTagsApi(client);
      String documentId = addDocument(client, siteId, null, new byte[] {}, null, null);

      AddDocumentTagsRequest req = new AddDocumentTagsRequest()
          .addTagsItem(new AddDocumentTag().key("test1").value("somevalue"))
          .addTagsItem(new AddDocumentTag().key("test2"))
          .addTagsItem(new AddDocumentTag().key("test3").values(Arrays.asList("abc", "xyz")));

      // when
      api.addDocumentTags(documentId, req, siteId, null);

      // then
      GetDocumentTagsResponse response =
          api.getDocumentTags(documentId, siteId, null, null, null, null);

      List<DocumentTag> list = response.getTags();
      assertEquals(req.getTags().size(), list.size());
      // map = list.get(0);
      assertEquals("test1", list.get(0).getKey());
      assertEquals("somevalue", list.get(0).getValue());
      assertNotNull(list.get(0).getUserId());
      assertNotNull(list.get(0).getInsertedDate());

      // map = list.get(1);
      assertEquals("test2", list.get(1).getKey());
      assertEquals("", list.get(1).getValue());
      assertNotNull(list.get(1).getUserId());
      assertNotNull(list.get(1).getInsertedDate());

      // map = list.get(2);
      assertEquals("test3", list.get(2).getKey());
      assertNull(list.get(2).getValue());
      assertEquals("abc,xyz", list.get(2).getValues().stream().collect(Collectors.joining(",")));
      assertNotNull(list.get(2).getUserId());
      assertNotNull(list.get(2).getInsertedDate());
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
    for (ApiClient client : getApiClients(siteId)) {

      // given
      DocumentsApi documentsApi = new DocumentsApi(client);
      DocumentTagsApi tagsApi = new DocumentTagsApi(client);

      String content = "this is some data";
      List<AddDocumentTag> tags =
          Arrays.asList(new AddDocumentTag().key("category").value("person"),
              new AddDocumentTag().key("user").value("111"));

      AddDocumentRequest addReq = new AddDocumentRequest().content(content).tags(tags);
      // when
      AddDocumentResponse addDocument = documentsApi.addDocument(addReq, siteId, null);

      // then
      String documentId = addDocument.getDocumentId();

      waitForDocumentContent(client, siteId, documentId);

      assertEquals("person",
          tagsApi.getDocumentTag(documentId, "category", siteId, null).getValue());

      // given
      AddDocumentTagsRequest updateTagReq = new AddDocumentTagsRequest()
          .addTagsItem(new AddDocumentTag().key("playerId").value("555"))
          .addTagsItem(new AddDocumentTag().key("category").values(Arrays.asList("c0", "c1")));

      // when
      tagsApi.updateDocumentTags(documentId, updateTagReq, siteId);

      // then
      int i = 0;
      final int expected = 3;

      List<DocumentTag> taglist =
          tagsApi.getDocumentTags(documentId, siteId, null, null, null, null).getTags();
      assertEquals(expected, taglist.size());
      assertEquals("category", taglist.get(i).getKey());
      assertEquals("c0,c1", Strings.join(taglist.get(i++).getValues(), ","));

      assertEquals("playerId", taglist.get(i).getKey());
      assertEquals("555", taglist.get(i++).getValue());
      assertEquals("user", taglist.get(i).getKey());
      assertEquals("111", taglist.get(i++).getValue());

      // given
      AddDocumentTagsRequest setTagsReq = new AddDocumentTagsRequest().tags(tags);

      // when
      tagsApi.setDocumentTags(documentId, setTagsReq, siteId);

      // then
      i = 0;
      taglist = tagsApi.getDocumentTags(documentId, siteId, null, null, null, null).getTags();
      assertEquals(2, taglist.size());
      assertEquals("category", taglist.get(i).getKey());
      assertEquals("person", taglist.get(i++).getValue());

      assertEquals("user", taglist.get(i).getKey());
      assertEquals("111", taglist.get(i++).getValue());
    }
  }
}
