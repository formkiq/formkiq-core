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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import java.net.http.HttpHeaders;
import java.net.http.HttpResponse;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiPredicate;
import org.junit.Test;
import com.formkiq.stacks.client.FormKiqClientV1;
import com.formkiq.stacks.client.HttpService;
import com.formkiq.stacks.client.HttpServiceJava;
import com.formkiq.stacks.client.models.DocumentWithChildren;
import software.amazon.awssdk.core.sync.RequestBody;

/**
 * Process Urls.
 * <p>
 * POST /public/webhooks tests
 * </p>
 *
 */
public class PublicWebhooksRequestTest extends AbstractApiTest {

  /** Http Status OK. */
  private static final int STATUS_OK = 200;
  /** JUnit Test Timeout. */
  private static final int TEST_TIMEOUT = 20000;

  /**
   * Test POST /public/webhooks.
   * 
   * @throws Exception Exception
   */
  @Test//(timeout = TEST_TIMEOUT)
  public void testPublicWebhooks01() throws Exception {
    // given
    String rooturl = getRootHttpUrl() + "/public/webhooks";

    for (String path : Arrays.asList("/asdffgdfg", "")) {

      String urlpath = rooturl + path;
      
      Map<String, List<String>> headers = Map.of("Content-Type", Arrays.asList("text/plain"));
      Optional<HttpHeaders> o =
          Optional.of(HttpHeaders.of(headers, new BiPredicate<String, String>() {
            @Override
            public boolean test(final String t, final String u) {
              return true;
            }
          }));

      String content = "{\"name\":\"John Smith\"}";

      // when
      HttpService hs = new HttpServiceJava();
      HttpResponse<String> response = hs.post(urlpath, o, RequestBody.fromString(content));

      // then
      assertEquals(STATUS_OK, response.statusCode());
      Map<String, Object> map = toMap(response);
      
      for (FormKiqClientV1 client : getFormKiqClients()) {
        DocumentWithChildren document =
            getDocument(client, map.get("documentId").toString(), false);
        assertNotNull(document);
      }
    }
  }

  /**
   * Options.
   * 
   * @throws Exception Exception
   */
  @Test(timeout = TEST_TIMEOUT)
  public void testOptions01() throws Exception {
    // given
    String url = getRootHttpUrl() + "/public/webhooks";

    Map<String, List<String>> headers = Map.of("Content-Type", Arrays.asList("text/plain"));
    Optional<HttpHeaders> o =
        Optional.of(HttpHeaders.of(headers, new BiPredicate<String, String>() {
          @Override
          public boolean test(final String t, final String u) {
            return true;
          }
        }));


    // when
    HttpService hs = new HttpServiceJava();
    HttpResponse<String> response = hs.options(url, o);

    // then
    assertEquals(STATUS_OK, response.statusCode());
  }
}
