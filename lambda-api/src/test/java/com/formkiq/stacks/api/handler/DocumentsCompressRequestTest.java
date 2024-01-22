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

import static com.formkiq.testutils.aws.TestServices.STAGE_BUCKET_NAME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import com.formkiq.aws.s3.S3Service;
import com.formkiq.client.invoker.ApiException;
import com.formkiq.client.model.AddDocumentUploadRequest;
import com.formkiq.client.model.DocumentsCompressRequest;
import com.formkiq.client.model.DocumentsCompressResponse;
import com.formkiq.testutils.aws.DynamoDbExtension;
import com.formkiq.testutils.aws.LocalStackExtension;
import com.formkiq.testutils.aws.TestServices;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import software.amazon.awssdk.services.s3.model.ListObjectsResponse;

/** POST /documents/compress tests. */
@ExtendWith(DynamoDbExtension.class)
@ExtendWith(LocalStackExtension.class)
public class DocumentsCompressRequestTest extends AbstractApiClientRequestTest {

  /** To test objects put to the staging S3. **/
  private static S3Service s3 = null;
  /** {@link Gson}. */
  private Gson gson = new GsonBuilder().create();

  /**
   * BeforeAll.
   * 
   * @throws URISyntaxException URISyntaxException
   */
  @BeforeAll
  public static void beforeAll() throws URISyntaxException {
    s3 = new S3Service(TestServices.getS3Connection(null));
  }

  /**
   * Create Dummy document.
   * 
   * @param siteId {@link String}
   * @return {@link String}
   * @throws ApiException ApiException
   */
  private String createDocument(final String siteId) throws ApiException {
    AddDocumentUploadRequest req = new AddDocumentUploadRequest();
    return this.documentsApi.addDocumentUpload(req, siteId, null, null, null).getDocumentId();
  }

  /**
   * Test compress documents where they do not all exist.
   * 
   * @throws ApiException ApiException
   */
  @Test
  void testHandlePostDocumentsCompress01() throws ApiException {
    // given
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {

      for (Boolean read : Arrays.asList(Boolean.FALSE, Boolean.TRUE)) {

        setBearerToken(siteId, read.booleanValue());

        String doc1 = UUID.randomUUID().toString();
        List<String> documentIds = Arrays.asList(doc1);
        DocumentsCompressRequest req = new DocumentsCompressRequest().documentIds(documentIds);

        // when
        try {
          this.documentsApi.compressDocuments(req, siteId);
          fail();
        } catch (ApiException e) {
          assertEquals("{\"errors\":[{\"key\":\"documentId\",\"error\":\"Document '" + doc1
              + "' does not exist\"}]}", e.getResponseBody());
        }
      }
    }
  }

  /**
   * Test empty documentIds.
   * 
   * @throws ApiException ApiException
   */
  @Test
  void testHandlePostDocumentsCompress02() throws ApiException {
    // given
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {

      setBearerToken(siteId);

      DocumentsCompressRequest req = new DocumentsCompressRequest();

      // when
      try {
        this.documentsApi.compressDocuments(req, siteId);
        fail();
      } catch (ApiException e) {
        assertEquals("{\"errors\":[{\"key\":\"documentIds\",\"error\":\"is required\"}]}",
            e.getResponseBody());
      }
    }
  }

  /**
   * Test compressing documentids.
   * 
   * @throws Exception Exception
   */
  @SuppressWarnings("unchecked")
  @Test
  void testHandlePostDocumentsCompress03() throws Exception {
    // given
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {

      setBearerToken(siteId);

      String doc0 = createDocument(siteId);
      String doc1 = createDocument(siteId);

      List<String> documentIds = Arrays.asList(doc0, doc1);
      DocumentsCompressRequest req = new DocumentsCompressRequest().documentIds(documentIds);

      // when
      DocumentsCompressResponse response = this.documentsApi.compressDocuments(req, siteId);

      // then
      String url = response.getDownloadUrl();
      if (siteId != null) {
        assertTrue(url.contains("/" + STAGE_BUCKET_NAME + "/tempfiles/" + siteId));
      } else {
        assertTrue(url.contains("/" + STAGE_BUCKET_NAME + "/tempfiles/"));
        assertFalse(url.contains("/" + STAGE_BUCKET_NAME + "/tempfiles/" + siteId));
      }

      ListObjectsResponse listObjects = s3.listObjects(STAGE_BUCKET_NAME, "tempfiles/");
      assertEquals(1, listObjects.contents().size());
      String key = listObjects.contents().get(0).key();
      assertTrue(key.endsWith(".json"));

      String content = s3.getContentAsString(STAGE_BUCKET_NAME, key, null);
      s3.deleteObject(STAGE_BUCKET_NAME, key, null);

      Map<String, Object> s3FileMap = this.gson.fromJson(content, Map.class);
      assertTrue(s3FileMap.containsKey("downloadUrl"));
      final URI downloadUrl = new URI(s3FileMap.get("downloadUrl").toString());
      assertTrue(url.contains(downloadUrl.getPath()));

      assertEquals(siteId != null ? siteId : "default", s3FileMap.get("siteId"));
      assertEquals(documentIds, s3FileMap.get("documentIds"));
    }
  }
}
