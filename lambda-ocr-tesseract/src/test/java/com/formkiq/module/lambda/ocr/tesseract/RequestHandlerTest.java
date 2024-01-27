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
package com.formkiq.module.lambda.ocr.tesseract;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.Arrays;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import com.formkiq.client.api.ExamineObjectsApi;
import com.formkiq.client.invoker.ApiClient;
import com.formkiq.client.invoker.ApiException;
import com.formkiq.client.invoker.Configuration;
import com.formkiq.client.model.AddExaminePdfResponse;
import com.formkiq.client.model.GetExaminePdfResponse;
import com.formkiq.testutils.aws.DynamoDbExtension;
import com.formkiq.testutils.aws.FormKiqApiExtension;
import com.formkiq.testutils.aws.JwtTokenEncoder;
import com.formkiq.testutils.aws.LambdaContextRecorder;
import com.formkiq.testutils.aws.LocalStackExtension;

@ExtendWith(LocalStackExtension.class)
@ExtendWith(DynamoDbExtension.class)
class RequestHandlerTest {

  /** {@link HttpClient}. */
  private static HttpClient http = HttpClient.newHttpClient();
  /** FormKiQ Server. */
  @RegisterExtension
  static FormKiqApiExtension server = new FormKiqApiExtension(new FormKiQResponseCallback());

  /** {@link ApiClient}. */
  private ApiClient apiClient =
      Configuration.getDefaultApiClient().setReadTimeout(0).setBasePath(server.getBasePath());

  /** {@link ExamineObjectsApi}. */
  private ExamineObjectsApi examineApi = new ExamineObjectsApi(this.apiClient);

  /**
   * Set BearerToken.
   * 
   * @param siteId {@link String}
   */
  private void setBearerToken(final String siteId) {
    setBearerToken(siteId, "joesmith");
  }

  /**
   * Set BearerToken.
   * 
   * @param siteId {@link String}
   * @param username {@link String}
   */
  private void setBearerToken(final String siteId, final String username) {
    String jwt =
        JwtTokenEncoder.encodeCognito(new String[] {siteId != null ? siteId : "default"}, username);
    this.apiClient.addDefaultHeader("Authorization", jwt);
  }

  /**
   * Test Examine PDF.
   * 
   * @throws Exception Exception
   */
  @Test
  void testExaminePdf01() throws Exception {
    // given
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {

      setBearerToken(siteId);

      // when
      GetExaminePdfResponse response = this.examineApi.getExaminePdf(siteId);

      // then
      assertNotNull(response.getId());
      assertTrue(response.getUploadUrl().contains("/tempfiles/"));

      // given
      try (InputStream is = LambdaContextRecorder.class.getResourceAsStream("/form.pdf")) {

        // when
        HttpResponse<String> httpResponse = http.send(
            HttpRequest.newBuilder(new URI(response.getUploadUrl()))
                .header("Content-Type", "application/pdf")
                .method("PUT", BodyPublishers.ofInputStream(() -> is)).build(),
            BodyHandlers.ofString());

        // then
        assertEquals("", httpResponse.body());
      }

      // given

      // when
      AddExaminePdfResponse examine = this.examineApi.addExaminePdf(response.getId(), siteId);

      // then
      final int expected = 17;
      assertEquals(expected, examine.getFileinfo().getFields().size());
      assertEquals("Height Formatted Field", examine.getFileinfo().getFields().get(0).getField());
      assertEquals("150", examine.getFileinfo().getFields().get(0).getValue());
    }
  }

  /**
   * Test missing file.
   * 
   * @throws Exception Exception
   */
  @Test
  void testExaminePdf02() throws Exception {
    // given
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {

      String id = UUID.randomUUID().toString();
      setBearerToken(siteId);

      // when
      try {
        this.examineApi.addExaminePdf(id, siteId);
      } catch (ApiException e) {
        // then
        assertEquals("{\"message\":\"Document " + id + " not found.\"}", e.getResponseBody());
      }
    }
  }
}
