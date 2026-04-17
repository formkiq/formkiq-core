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
package com.formkiq.stacks.api.handler;

import com.formkiq.aws.dynamodb.DynamoDbService;
import com.formkiq.aws.dynamodb.DynamoDbServiceExtension;
import com.formkiq.aws.dynamodb.ID;
import com.formkiq.aws.dynamodb.model.DocumentTagRecord;
import com.formkiq.aws.dynamodb.model.DocumentTagRecordBuilder;
import com.formkiq.client.invoker.ApiException;
import com.formkiq.client.model.AddResponse;
import com.formkiq.client.model.DocumentTag;
import com.formkiq.client.model.SetResponse;
import com.formkiq.client.model.UpdateResponse;
import com.formkiq.module.lambdaservices.AwsServiceCache;
import com.formkiq.testutils.api.ApiHttpResponse;
import com.formkiq.testutils.api.documents.AddDocumentRequestBuilder;
import com.formkiq.testutils.api.documents.AddDocumentTagRequestBuilder;
import com.formkiq.testutils.api.documents.DeleteDocumentTagRequestBuilder;
import com.formkiq.testutils.api.documents.DeleteDocumentTagValueRequestBuilder;
import com.formkiq.testutils.api.documents.GetDocumentTagRequestBuilder;
import com.formkiq.testutils.api.documents.GetDocumentTagsRequestBuilder;
import com.formkiq.testutils.api.documents.SetDocumentTagValueRequestBuilder;
import com.formkiq.testutils.api.documents.SetDocumentTagsRequestBuilder;
import com.formkiq.testutils.api.documents.UpdateDocumentTagsRequestBuilder;
import com.formkiq.testutils.aws.DynamoDbExtension;
import com.formkiq.testutils.aws.LocalStackExtension;
import com.formkiq.urls.HttpStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Arrays;
import java.util.List;

import static com.formkiq.aws.dynamodb.objects.Objects.notNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Unit Tests for request /documents/{documentId}/tags. */
@ExtendWith(LocalStackExtension.class)
@ExtendWith(DynamoDbExtension.class)
public class DocumentsTagsRequestTest extends AbstractApiClientRequestTest {

  private List<DocumentTag> getDocumentTags(final String siteId, final String documentId)
      throws ApiException {
    return getDocumentTags(siteId, documentId, null);
  }

  private List<DocumentTag> getDocumentTags(final String siteId, final String documentId,
      final String limit) throws ApiException {
    return notNull(new GetDocumentTagsRequestBuilder(documentId).limit(limit).submit(client, siteId)
        .throwIfError().response().getTags());
  }

  /**
   * DELETE /documents/{documentId}/tags/{tagKey} request with Tag Value.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testHandleDeleteTagDocument01() throws Exception {
    for (String siteId : Arrays.asList(null, ID.uuid())) {
      // given
      setBearerToken(siteId);

      final String tagKey = "category";

      // when
      String documentId = new AddDocumentRequestBuilder().content().addTag(tagKey, tagKey)
          .addTag(tagKey + "2", tagKey).submit(client, siteId).throwIfError().response()
          .getDocumentId();

      var message = new DeleteDocumentTagRequestBuilder(documentId, tagKey).submit(client, siteId)
          .throwIfError().response().getMessage();

      // then
      assertEquals("Removed 'category' from document '" + documentId + "'.", message);
      List<DocumentTag> tags = getDocumentTags(siteId, documentId);
      assertEquals(1, tags.size());
      assertEquals("category2", tags.get(0).getKey());
    }
  }

  /**
   * DELETE /documents/{documentId}/tags/{tagKey} request without Tag Value.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testHandleDeleteTagDocument02() throws Exception {
    for (String siteId : Arrays.asList(null, ID.uuid())) {
      // given
      setBearerToken(siteId);

      final String tagKey = "category";

      // when
      String documentId = new AddDocumentRequestBuilder().content().addTag(tagKey, (String) null)
          .submit(client, siteId).throwIfError().response().getDocumentId();

      var message = new DeleteDocumentTagRequestBuilder(documentId, tagKey).submit(client, siteId)
          .throwIfError().response().getMessage();

      // then
      List<DocumentTag> tags = getDocumentTags(siteId, documentId);
      assertEquals("Removed 'category' from document '" + documentId + "'.", message);
      assertEquals(0, tags.size());
    }
  }

  /**
   * DELETE /documents/{documentId}/tags/{tagKey} request with Tag Values.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testHandleDeleteTagDocument03() throws Exception {
    for (String siteId : Arrays.asList(null, ID.uuid())) {
      // given
      setBearerToken(siteId);
      final String tagKey = "category";

      // when
      String documentId =
          new AddDocumentRequestBuilder().content().addTag(tagKey, List.of("abc", "xyz"))
              .submit(client, siteId).throwIfError().response().getDocumentId();

      var message = new DeleteDocumentTagRequestBuilder(documentId, tagKey).submit(client, siteId)
          .throwIfError().response().getMessage();

      // then
      assertEquals("Removed 'category' from document '" + documentId + "'.", message);
      List<DocumentTag> tags = getDocumentTags(siteId, documentId);
      assertEquals(0, tags.size());
    }
  }

  /**
   * DELETE /documents/{documentId}/tags/{tagKey}/{tagValue} request with Tag Value.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testHandleDeleteTagValue01() throws Exception {
    for (String siteId : Arrays.asList(null, ID.uuid())) {
      // given
      setBearerToken(siteId);
      final String tagKey = "category";
      final String tagValue = "person";

      // when
      String documentId = new AddDocumentRequestBuilder().content().addTag(tagKey, tagValue)
          .submit(client, siteId).throwIfError().response().getDocumentId();

      var message = new DeleteDocumentTagValueRequestBuilder(documentId, tagKey, tagValue)
          .submit(client, siteId).throwIfError().response().getMessage();

      // then
      assertEquals("Removed Tag value from document '" + documentId + "'.", message);
      List<DocumentTag> tags = getDocumentTags(siteId, documentId);
      assertEquals(0, tags.size());
    }
  }

  /**
   * DELETE /documents/{documentId}/tags/{tagKey}/{tagValue} request with Tag Values.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testHandleDeleteTagValue02() throws Exception {
    for (String siteId : Arrays.asList(null, ID.uuid())) {
      // given
      setBearerToken(siteId);
      final String tagKey = "category";

      // when
      String documentId =
          new AddDocumentRequestBuilder().content().addTag(tagKey, List.of("abc", "xyz"))
              .submit(client, siteId).throwIfError().response().getDocumentId();

      var message = new DeleteDocumentTagValueRequestBuilder(documentId, tagKey, "xyz")
          .submit(client, siteId).throwIfError().response().getMessage();

      // then
      List<DocumentTag> tags = getDocumentTags(siteId, documentId);
      assertEquals("Removed Tag value from document '" + documentId + "'.", message);

      assertEquals(1, tags.size());
      assertEquals("abc", tags.get(0).getValue());
      assertTrue(notNull(tags.get(0).getValues()).isEmpty());
    }
  }

  /**
   * DELETE /documents/{documentId}/tags/{tagKey}/{tagValue} wrong Tag Value.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testHandleDeleteTagValue03() throws Exception {
    for (String siteId : Arrays.asList(null, ID.uuid())) {
      // given
      setBearerToken(siteId);
      final String tagKey = "category";

      // when
      String documentId =
          new AddDocumentRequestBuilder().content().addTag(tagKey, List.of("abc", "xyz"))
              .submit(client, siteId).throwIfError().response().getDocumentId();

      var resp = new DeleteDocumentTagValueRequestBuilder(documentId, tagKey, "xyz123")
          .submit(client, siteId).exception();

      // then
      assertEquals(HttpStatus.NOT_FOUND, resp.getCode());
      assertEquals("{\"message\":\"Tag/Value combination not found.\"}", resp.getResponseBody());

      List<DocumentTag> tags = getDocumentTags(siteId, documentId);
      assertEquals(1, tags.size());
      assertNull(tags.get(0).getValue());
      assertEquals("abc,xyz", String.join(",", notNull(tags.get(0).getValues())));
    }
  }

  /**
   * Get /documents/{documentId}/tags tags request.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testHandleGetDocumentTags01() throws Exception {
    for (String siteId : Arrays.asList(null, ID.uuid())) {
      // given
      setBearerToken(siteId);
      String tagKey = "category";
      String tagvalue = ID.uuid();

      // when
      String documentId = new AddDocumentRequestBuilder().content().addTag(tagKey, tagvalue)
          .submit(client, siteId).throwIfError().response().getDocumentId();

      // then
      List<DocumentTag> tags = getDocumentTags(siteId, documentId);
      assertEquals(1, tags.size());
      assertEquals(tagKey, tags.get(0).getKey());
      assertEquals(tagvalue, tags.get(0).getValue());
      assertNull(tags.get(0).getDocumentId());
      assertEquals("joesmith", tags.get(0).getUserId());
    }
  }

  /**
   * GET /documents/{documentId}/tags request limit 1.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testHandleGetDocumentTags02() throws Exception {
    for (String siteId : Arrays.asList(null, ID.uuid())) {
      // given
      setBearerToken(siteId);

      // when
      String documentId = new AddDocumentRequestBuilder().content().addTag("category0", "person")
          .addTag("category1", "thing").submit(client, siteId).throwIfError().response()
          .getDocumentId();

      // then
      List<DocumentTag> tags = getDocumentTags(siteId, documentId, "1");
      assertEquals(1, tags.size());

      assertNull(tags.get(0).getDocumentId());
      assertNotNull(tags.get(0).getInsertedDate());
      assertEquals("category0", tags.get(0).getKey());
      assertEquals("userdefined", tags.get(0).getType());
      assertEquals("joesmith", tags.get(0).getUserId());
      assertEquals("person", tags.get(0).getValue());
    }
  }

  /**
   * GET /documents/{documentId}/tags values.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testHandleGetDocumentTags03() throws Exception {
    for (String siteId : Arrays.asList(null, ID.uuid())) {
      // given
      setBearerToken(siteId);

      // when
      String documentId = new AddDocumentRequestBuilder().content()
          .addTag("category0", List.of("abc", "xyz")).addTag("category1", List.of("bbb", "ccc"))
          .submit(client, siteId).throwIfError().response().getDocumentId();

      // then
      List<DocumentTag> tags = getDocumentTags(siteId, documentId, "1");
      assertEquals(1, tags.size());

      assertNull(tags.get(0).getDocumentId());
      assertNotNull(tags.get(0).getInsertedDate());
      assertEquals("category0", tags.get(0).getKey());
      assertEquals("userdefined", tags.get(0).getType());
      assertEquals("joesmith", tags.get(0).getUserId());
      assertNull(tags.get(0).getValue());
      assertEquals("abc,xyz", String.join(",", notNull(tags.get(0).getValues())));
    }
  }

  /**
   * GET /documents/{documentId}/tags/{tagKey} request. Document not found.
   *
   */
  @Test
  public void testHandleGetTags00() {
    // given
    String documentId = ID.uuid();

    // when
    var resp = new GetDocumentTagRequestBuilder(documentId, "category").submit(client, null);

    // then
    assertNull(resp.response());
    assertNotNull(resp.exception());
    assertEquals(HttpStatus.NOT_FOUND, resp.exception().getCode());
    assertEquals("{\"message\":\"Document " + documentId + " not found.\"}",
        resp.exception().getResponseBody());
  }

  /**
   * GET /documents/{documentId}/tags/{tagKey} request. Tag not found.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testHandleGetTags01() throws Exception {
    // given
    String documentId = new AddDocumentRequestBuilder().content().submit(client, null)
        .throwIfError().response().getDocumentId();

    // when
    var resp = new GetDocumentTagRequestBuilder(documentId, "category").submit(client, null);

    // then
    assertNull(resp.response());
    assertNotNull(resp.exception());
    assertEquals(HttpStatus.NOT_FOUND, resp.exception().getCode());
    assertEquals("{\"message\":\"Tag category not found.\"}", resp.exception().getResponseBody());
  }

  /**
   * GET /documents/{documentId}/tags/{tagKey} request. Tag and value found.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testHandleGetTags02() throws Exception {
    for (String siteId : Arrays.asList(null, ID.uuid())) {
      // given
      setBearerToken(siteId);

      // when
      String documentId = new AddDocumentRequestBuilder().content().addTag("category", "person")
          .submit(client, siteId).throwIfError().response().getDocumentId();

      // then
      var resp = new GetDocumentTagRequestBuilder(documentId, "category").submit(client, siteId)
          .throwIfError().response();
      assertEquals("category", resp.getKey());
      assertEquals("person", resp.getValue());
    }
  }


  /**
   * GET /documents/{documentId}/tags/{tagKey} request. Tag and NO value found.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testHandleGetTags03() throws Exception {
    for (String siteId : Arrays.asList(null, ID.uuid())) {
      // given
      setBearerToken(siteId);

      // when
      String documentId = new AddDocumentRequestBuilder().content().addTag("category")
          .submit(client, siteId).throwIfError().response().getDocumentId();

      // then
      var resp = new GetDocumentTagRequestBuilder(documentId, "category").submit(client, siteId)
          .throwIfError().response();

      assertEquals("category", resp.getKey());
      assertEquals("", resp.getValue());
    }
  }

  /**
   * GET /documents/{documentId}/tags/{tagKey} request. Tag and values found.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testHandleGetTags04() throws Exception {
    for (String siteId : Arrays.asList(null, ID.uuid())) {
      // given
      setBearerToken(siteId);

      // when
      String documentId =
          new AddDocumentRequestBuilder().content().addTag("category", List.of("abc", "xyz"))
              .submit(client, siteId).throwIfError().response().getDocumentId();

      // then
      var resp = new GetDocumentTagRequestBuilder(documentId, "category").submit(client, siteId)
          .throwIfError().response();
      assertEquals("category", resp.getKey());
      assertNull(resp.getValue());
      assertEquals("abc,xyz", String.join(",", notNull(resp.getValues())));
    }
  }

  /**
   * PATCH /documents/{documentId}/tags tags request.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testHandlePatchDocumentTags01() throws Exception {
    for (String siteId : Arrays.asList(null, ID.uuid())) {
      // given
      setBearerToken(siteId);

      final String tagKey0 = "category";
      final String tagValue0 = "job";
      final String tagKey1 = "type";
      final String tagValue1 = "other";

      String documentId = new AddDocumentRequestBuilder().content().addTag(tagKey0, "asdd")
          .addTag("othertag", "asdd").submit(client, siteId).throwIfError().response()
          .getDocumentId();

      // when
      var resp = new UpdateDocumentTagsRequestBuilder(documentId).addTag(tagKey0, tagValue0)
          .addTag(tagKey1, tagValue1).submit(client, siteId).throwIfError().response();

      // then
      assertEquals("Updated Tags", resp.getMessage());

      int i = 0;
      final int expectedCount = 3;
      List<DocumentTag> tags = getDocumentTags(siteId, documentId);
      assertEquals(expectedCount, tags.size());
      assertEquals(tagKey0, tags.get(i).getKey());
      assertEquals(tagValue0, tags.get(i).getValue());
      assertEquals("joesmith", tags.get(i++).getUserId());

      assertEquals("othertag", tags.get(i).getKey());
      assertEquals("asdd", tags.get(i).getValue());
      assertEquals("joesmith", tags.get(i++).getUserId());

      assertEquals(tagKey1, tags.get(i).getKey());
      assertEquals(tagValue1, tags.get(i).getValue());
      assertEquals("joesmith", tags.get(i).getUserId());
    }
  }

  /**
   * POST /documents/{documentId}/tags tags request. Add Tag Base 64
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testHandlePostDocumentTags02() throws Exception {
    for (String siteId : Arrays.asList(null, ID.uuid())) {
      // given
      setBearerToken(siteId);

      final String tagname = "category";
      final String tagvalue = "job";

      // when
      String documentId = new AddDocumentRequestBuilder().content().submit(client, siteId)
          .throwIfError().response().getDocumentId();

      var message = new AddDocumentTagRequestBuilder(documentId).addTag(tagname, tagvalue)
          .submit(client, siteId).throwIfError().response().getMessage();

      // then
      assertEquals("Created Tags.", message);

      List<DocumentTag> tags = getDocumentTags(siteId, documentId);
      assertEquals(1, tags.size());
      assertEquals(tagname, tags.get(0).getKey());
      assertEquals(tagvalue, tags.get(0).getValue());
      assertEquals("joesmith", tags.get(0).getUserId());
    }
  }

  /**
   * POST /documents/{documentId}/tags tags request. Add Tag Key ONLY no value
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testHandlePostDocumentTags04() throws Exception {
    for (String siteId : Arrays.asList(null, ID.uuid())) {
      // given
      setBearerToken(siteId);

      final String tagname = "category";
      final String tagvalue = "";

      // when
      String documentId = new AddDocumentRequestBuilder().content().submit(client, siteId)
          .throwIfError().response().getDocumentId();
      var message = new AddDocumentTagRequestBuilder(documentId).addTag(tagname)
          .submit(client, siteId).throwIfError().response().getMessage();

      // then
      assertEquals("Created Tags.", message);

      List<DocumentTag> tags = getDocumentTags(siteId, documentId);
      assertEquals(1, tags.size());
      assertEquals(tagname, tags.get(0).getKey());
      assertEquals(tagvalue, tags.get(0).getValue());
    }
  }

  /**
   * POST /documents/{documentId}/tags request.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testHandlePostDocumentTags05() throws Exception {
    for (String siteId : Arrays.asList(null, ID.uuid())) {
      // given
      setBearerToken(siteId);

      final String tagname = "category";
      final String tagvalue = "somevalue";

      // when
      String documentId = new AddDocumentRequestBuilder().content().submit(client, siteId)
          .throwIfError().response().getDocumentId();
      var message = new AddDocumentTagRequestBuilder(documentId).addTag("category", "somevalue")
          .submit(client, siteId).throwIfError().response().getMessage();

      // then
      assertEquals("Created Tags.", message);

      List<DocumentTag> tags = getDocumentTags(siteId, documentId);
      assertEquals(1, tags.size());
      assertEquals(tagname, tags.get(0).getKey());
      assertEquals(tagvalue, tags.get(0).getValue());
      assertEquals("joesmith", tags.get(0).getUserId());
    }
  }

  /**
   * POST /documents/{documentId}/tags multiple "tags" request.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testHandlePostDocumentTags08() throws Exception {
    for (String siteId : Arrays.asList(null, ID.uuid())) {
      // given
      setBearerToken(siteId);

      // when
      String documentId = new AddDocumentRequestBuilder().content().submit(client, siteId)
          .throwIfError().response().getDocumentId();

      new AddDocumentTagRequestBuilder(documentId).addTag("mine").addTag("playerId", "1")
          .addTag("caseId", List.of("123", "999")).submit(client, siteId).throwIfError();

      // then
      List<DocumentTag> tags = getDocumentTags(siteId, documentId);
      assertEquals(3, tags.size());
      assertEquals("caseId", tags.get(0).getKey());
      assertNull(tags.get(0).getValue());
      assertEquals("123,999", String.join(",", notNull(tags.get(0).getValues())));
      assertEquals("joesmith", tags.get(0).getUserId());

      assertEquals("mine", tags.get(1).getKey());
      assertEquals("", tags.get(1).getValue());
      assertTrue(notNull(tags.get(1).getValues()).isEmpty());
      assertEquals("joesmith", tags.get(1).getUserId());

      assertEquals("playerId", tags.get(2).getKey());
      assertEquals("1", tags.get(2).getValue());
      assertTrue(notNull(tags.get(2).getValues()).isEmpty());
      assertEquals("joesmith", tags.get(2).getUserId());
    }
  }

  /**
   * POST/PATCH/PUT /documents/{documentId}/tags with Document Missing.
   *
   */
  @Test
  public void testHandlePostDocumentTags11() {

    for (String siteId : Arrays.asList(null, ID.uuid())) {
      // given
      setBearerToken(siteId);
      final String documentId = ID.uuid();

      // when
      final ApiHttpResponse<AddResponse> resp0 = new AddDocumentTagRequestBuilder(documentId)
          .addTag("category", "job").submit(client, siteId);

      final ApiHttpResponse<SetResponse> resp1 = new SetDocumentTagsRequestBuilder(documentId)
          .addTag("category", "job").submit(client, siteId);

      final ApiHttpResponse<UpdateResponse> resp2 = new UpdateDocumentTagsRequestBuilder(documentId)
          .addTag("category", "job").submit(client, siteId);

      // then
      assertNotNull(resp0.exception());
      assertNotNull(resp1.exception());
      assertNotNull(resp2.exception());

      assertEquals(HttpStatus.NOT_FOUND, resp0.exception().getCode());
      assertEquals("{\"message\":\"Document " + documentId + " not found.\"}",
          resp0.exception().getResponseBody());

      assertEquals(HttpStatus.NOT_FOUND, resp1.exception().getCode());
      assertEquals("{\"message\":\"Document " + documentId + " not found.\"}",
          resp1.exception().getResponseBody());

      assertEquals(HttpStatus.NOT_FOUND, resp2.exception().getCode());
      assertEquals("{\"message\":\"Document " + documentId + " not found.\"}",
          resp2.exception().getResponseBody());
    }
  }

  /**
   * POST /documents/{documentId}/tags with duplicate keys.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testHandlePostDocumentTags12() throws Exception {
    for (String siteId : Arrays.asList(null, ID.uuid())) {
      // given
      setBearerToken(siteId);

      // when
      String documentId = new AddDocumentRequestBuilder().content().submit(client, siteId)
          .throwIfError().response().getDocumentId();

      var resp =
          new AddDocumentTagRequestBuilder(documentId).addTag("author", "William Shakespeare")
              .addTag("author", "Kevin Bacon").submit(client, siteId);

      // then
      assertNull(resp.response());
      assertNotNull(resp.exception());

      assertEquals(HttpStatus.BAD_REQUEST, resp.exception().getCode());
      assertEquals(
          "{\"message\":\"Tag key can only be included once in body; "
              + "please use 'values' to assign multiple tag values to that key\"}",
          resp.exception().getResponseBody());
    }
  }

  /**
   * POST /documents/{documentId}/tags tags request. Add Restricted Tag Name
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testHandlePostDocumentTags13() throws Exception {
    for (String siteId : Arrays.asList(null, ID.uuid())) {
      // given
      setBearerToken(siteId);
      String documentId = new AddDocumentRequestBuilder().content().submit(client, siteId)
          .throwIfError().response().getDocumentId();

      // when
      var resp = new AddDocumentTagRequestBuilder(documentId)
          .addTag("CLAMAV_SCAN_STATUS", "asdkjasd").submit(client, siteId);

      // then
      assertNotNull(resp.exception());
      assertEquals(HttpStatus.BAD_REQUEST, resp.exception().getCode());
      assertEquals(
          "{\"errors\":[{\"key\":\"CLAMAV_SCAN_STATUS\"," + "\"error\":\"unallowed tag key\"}]}",
          resp.exception().getResponseBody());
    }
  }

  /**
   * PUT /documents/{documentId}/tags tags request.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testHandlePutDocumentTags01() throws Exception {
    for (String siteId : Arrays.asList(null, ID.uuid())) {
      // given
      setBearerToken(siteId);

      final String tagKey0 = "category";
      final String tagValue0 = "job";
      final String tagKey1 = "type";
      final String tagValue1 = "other";

      String documentId = new AddDocumentRequestBuilder().content().addTag(tagKey0, tagValue0)
          .submit(client, siteId).throwIfError().response().getDocumentId();

      // when
      var resp = new SetDocumentTagsRequestBuilder(documentId).addTag(tagKey1, tagValue1)
          .submit(client, siteId).throwIfError().response();

      // then
      assertEquals("Set Tags", resp.getMessage());

      List<DocumentTag> tags = getDocumentTags(siteId, documentId);
      assertEquals(1, tags.size());
      assertEquals(tagKey1, tags.get(0).getKey());
      assertEquals(tagValue1, tags.get(0).getValue());
      assertEquals("joesmith", tags.get(0).getUserId());
    }
  }

  /**
   * PUT /documents/{documentId}/tags/{tagKey} VALUE request.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testHandlePutTags01() throws Exception {
    for (String siteId : Arrays.asList(null, ID.uuid())) {
      // given
      setBearerToken(siteId);

      String tagKey = "category";

      String documentId = new AddDocumentRequestBuilder().content().addTag(tagKey, "nope")
          .submit(client, siteId).throwIfError().response().getDocumentId();

      // when
      var resp = new SetDocumentTagValueRequestBuilder(documentId, tagKey).value("active")
          .submit(client, siteId).throwIfError();

      // then
      assertEquals("Updated tag 'category' on document '" + documentId + "'.",
          resp.response().getMessage());

      var get = new GetDocumentTagRequestBuilder(documentId, tagKey).submit(client, siteId)
          .throwIfError().response();
      assertEquals("active", get.getValue());
    }
  }

  /**
   * PUT /documents/{documentId}/tags/{tagKey} request where DocumentId is missing.
   *
   */
  @Test
  public void testHandlePutTags02() {
    // given
    for (String siteId : Arrays.asList(null, ID.uuid())) {
      setBearerToken(siteId);

      String tagKey = "category";
      String documentId = ID.uuid();

      // when
      var resp = new SetDocumentTagValueRequestBuilder(documentId, tagKey).value("active")
          .submit(client, siteId);

      // then
      assertNull(resp.response());
      assertNotNull(resp.exception());

      assertEquals(HttpStatus.NOT_FOUND, resp.exception().getCode());
      assertEquals("{\"message\":\"Document " + documentId + " not found.\"}",
          resp.exception().getResponseBody());
    }
  }

  /**
   * PUT /documents/{documentId}/tags/{tagKey} change VALUE to VALUES request.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testHandlePutTags05() throws Exception {
    for (String siteId : Arrays.asList(null, ID.uuid())) {
      // given
      setBearerToken(siteId);
      String tagKey = "category";
      String documentId = new AddDocumentRequestBuilder().content().addTag(tagKey, "nope")
          .submit(client, siteId).throwIfError().response().getDocumentId();

      // when
      new SetDocumentTagValueRequestBuilder(documentId, tagKey).addValue("abc").addValue("xyz")
          .submit(client, siteId).throwIfError();

      // then
      List<DocumentTag> tags = getDocumentTags(null, documentId);

      assertEquals(1, tags.size());
      assertEquals(tagKey, tags.get(0).getKey());
      assertNull(tags.get(0).getValue());
      assertEquals("abc,xyz", String.join(",", notNull(tags.get(0).getValues())));
      assertEquals("joesmith", tags.get(0).getUserId());
    }
  }

  /**
   * PUT /documents/{documentId}/tags/{tagKey} change VALUES to VALUE request.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testHandlePutTags06() throws Exception {
    for (String siteId : Arrays.asList(null, ID.uuid())) {
      // given
      setBearerToken(siteId);
      String tagKey = "category";
      String documentId =
          new AddDocumentRequestBuilder().content().addTag(tagKey, List.of("abc", "xyz"))
              .submit(client, siteId).throwIfError().response().getDocumentId();

      // when
      new SetDocumentTagValueRequestBuilder(documentId, tagKey).value("active")
          .submit(client, siteId).throwIfError();

      // then
      List<DocumentTag> tags = getDocumentTags(null, documentId);
      assertEquals(1, tags.size());
      assertEquals(tagKey, tags.get(0).getKey());
      assertEquals("active", tags.get(0).getValue());
      assertTrue(notNull(tags.get(0).getValues()).isEmpty());
      assertEquals("joesmith", tags.get(0).getUserId());
    }
  }

  /**
   * PUT /documents/{documentId}/tags/{tagKey} invalid Tag.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testHandlePutTags07() throws Exception {

    for (String siteId : Arrays.asList(null, ID.uuid())) {
      // given
      setBearerToken(siteId);

      String documentId = new AddDocumentRequestBuilder().content().submit(client, siteId)
          .throwIfError().response().getDocumentId();

      String tagKey = "CLAMAV_SCAN_STATUS";

      // when
      var resp = new SetDocumentTagValueRequestBuilder(documentId, tagKey).value("abc")
          .submit(client, siteId);

      // then
      assertNotNull(resp.exception());
      assertEquals(HttpStatus.NOT_FOUND, resp.exception().getCode());
      assertEquals("{\"message\":\"Tag CLAMAV_SCAN_STATUS not found.\"}",
          resp.exception().getResponseBody());
    }
  }

  /**
   * PUT /documents/{documentId}/tags/{tagKey} update reserved Tag.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testHandlePutTags08() throws Exception {

    AwsServiceCache services =
        getAwsServices().register(DynamoDbService.class, new DynamoDbServiceExtension());

    for (String siteId : Arrays.asList(null, ID.uuid())) {
      // given
      setBearerToken(siteId);

      String tagKey = "CLAMAV_SCAN_STATUS";

      String documentId = new AddDocumentRequestBuilder().content().submit(client, siteId)
          .throwIfError().response().getDocumentId();

      DynamoDbService db = services.getExtension(DynamoDbService.class);
      List<DocumentTagRecord> tags =
          new DocumentTagRecordBuilder().documentId(documentId).tagKey(tagKey).build(siteId);
      tags.forEach(tag -> db.putItem(tag.getAttributes()));

      // when
      var resp = new SetDocumentTagValueRequestBuilder(documentId, tagKey).value("abc")
          .submit(client, siteId);

      // then
      assertNotNull(resp.exception());
      assertEquals(HttpStatus.BAD_REQUEST, resp.exception().getCode());
      assertEquals(
          "{\"errors\":[{\"key\":\"CLAMAV_SCAN_STATUS\",\"error\":\"unallowed tag key\"}]}",
          resp.exception().getResponseBody());
    }
  }
}
