/**
 * MIT License
 * 
 * Copyright (c) 2018 - 2020 FormKiQ
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
 * NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package com.formkiq.stacks.api.awstest;

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
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import com.formkiq.client.api.ExamineObjectsApi;
import com.formkiq.client.invoker.ApiClient;
import com.formkiq.client.model.AddExaminePdfResponse;
import com.formkiq.client.model.GetExaminePdfResponse;
import com.formkiq.module.http.HttpResponseStatus;
import com.formkiq.testutils.aws.AbstractAwsIntegrationTest;
import com.formkiq.testutils.aws.LambdaContextRecorder;
import software.amazon.awssdk.utils.IoUtils;

class ObjectsExamineTest extends AbstractAwsIntegrationTest {

  /** JUnit Test Timeout. */
  private static final int TEST_TIMEOUT = 60;

  /** {@link HttpClient}. */
  private static HttpClient http = HttpClient.newHttpClient();

  /**
   * Test Examine PDF.
   * 
   * @throws Exception Exception
   */
  @Test
  @Timeout(unit = TimeUnit.SECONDS, value = TEST_TIMEOUT)
  void testExaminePdf01() throws Exception {
    // given
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {

      for (ApiClient client : getApiClients(siteId)) {

        ExamineObjectsApi examineApi = new ExamineObjectsApi(client);

        // when
        GetExaminePdfResponse response = examineApi.getExaminePdf(siteId);

        // then
        assertNotNull(response.getId());
        assertTrue(response.getUploadUrl().contains("/tempfiles/"));

        // given
        try (InputStream is = LambdaContextRecorder.class.getResourceAsStream("/form.pdf")) {

          byte[] data = IoUtils.toByteArray(is);

          // when
          HttpResponse<String> httpResponse = http.send(
              HttpRequest.newBuilder(new URI(response.getUploadUrl()))
                  .header("Content-Type", "application/pdf")
                  .method("PUT", BodyPublishers.ofByteArray(data)).build(),
              BodyHandlers.ofString());

          // then
          assertTrue(HttpResponseStatus.is2XX(httpResponse));
        }

        // given

        // when
        AddExaminePdfResponse examine = examineApi.addExaminePdf(response.getId(), siteId);

        // then
        final int expected = 17;
        assertEquals(expected, examine.getFileinfo().getFields().size());
        assertEquals("Height Formatted Field", examine.getFileinfo().getFields().get(0).getField());
        assertEquals("150", examine.getFileinfo().getFields().get(0).getValue());
      }
    }
  }
}
