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
package com.formkiq.testutils.api.documents;

import com.formkiq.aws.dynamodb.documents.DocumentArtifact;
import com.formkiq.client.api.DocumentsApi;
import com.formkiq.client.invoker.ApiClient;
import com.formkiq.client.model.GetDocumentResponse;
import com.formkiq.testutils.api.ApiHttpResponse;
import com.formkiq.testutils.api.HttpRequestBuilder;
import com.formkiq.urls.HttpStatus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Builder for Get Document Request.
 */
public class GetDocumentRequestBuilder implements HttpRequestBuilder<GetDocumentResponse> {

  /**
   * Assert Document Not Found.
   *
   * @param client {@link ApiClient}
   * @param siteId {@link String}
   * @param document {@link DocumentArtifact}
   * @return {@link GetDocumentResponse}
   */
  public static GetDocumentResponse assertDocumentFound(final ApiClient client, final String siteId,
      final DocumentArtifact document) {
    var resp = new GetDocumentRequestBuilder(document).submit(client, siteId);
    assertNotNull(resp.response());
    assertNull(resp.exception());
    return resp.response();
  }

  /**
   * Assert Document Not Found.
   * 
   * @param client {@link ApiClient}
   * @param siteId {@link String}
   * @param document {@link DocumentArtifact}
   */
  public static void assertDocumentNotFound(final ApiClient client, final String siteId,
      final DocumentArtifact document) {
    var resp = new GetDocumentRequestBuilder(document).submit(client, siteId);
    assertNull(resp.response());
    assertNotNull(resp.exception());
    assertEquals(HttpStatus.NOT_FOUND, resp.exception().getCode());
  }

  /** {@link DocumentArtifact}. */
  private final DocumentArtifact document;

  /**
   * constructor.
   * 
   * @param documentArtifact {@link DocumentArtifact}
   */
  public GetDocumentRequestBuilder(final DocumentArtifact documentArtifact) {
    this.document = documentArtifact;
  }

  /**
   * constructor.
   *
   * @param documentId {@link String}
   */
  public GetDocumentRequestBuilder(final String documentId) {
    this(DocumentArtifact.of(documentId, null));
  }

  @Override
  public ApiHttpResponse<GetDocumentResponse> submit(final ApiClient apiClient,
      final String siteId) {
    return executeApiCall(() -> new DocumentsApi(apiClient).getDocument(this.document.documentId(),
        siteId, this.document.artifactId(), null));
  }
}
