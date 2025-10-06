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
package com.formkiq.aws.services.lambda;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit Test for {@link AbstractRestApiRequestHandler}.
 */
public class RestApiRequestHandlerTest {

  @Test
  void testRedactAuthorizationToken() {
    // given
    String input = "{\"body\":\"{\\\"attribute\\\":{\\\"key\\\":\\\"security_null\\\","
        + "\\\"dataType\\\":\\\"STRING\\\",\\\"type\\\":\\\"STANDARD\\\"}}\","
        + "\"httpMethod\":\"POST\",\"headers\":{\"Authorization\":"
        + "\"eyJhbGciOiJub25lIn0.eyJzdWIiOiJGb3JtS2lRIiwiY29nbml0bzpncm91cHMiOlsiZGVmYXV"
        + "sdCJdLCJjb2duaXRvOnVzZXJuYW1lIjoiam9lc21pdGgifQ.\",\"Accept\":\"application/json\","
        + "\"User-Agent\":\"OpenAPI-Generator/1.18.0/java\",\"Connection\":\"Keep-Alive\","
        + "\"Host\":\"localhost:7193\",\"Accept-Encoding\":\"gzip\",\"Content-Length\":\"75\","
        + "\"Content-Type\":\"application/json; charset=utf-8\"},\"path\":\"/attributes\","
        + "\"pathParameters\":{},\"queryStringParameters\":{},\"requestContext\":"
        + "{\"authorizer\":{\"claims\":{\"cognito:groups\":\"[default]\",\"cognito:username\":"
        + "\"joesmith\"}}},\"resource\":\"/attributes\"}";

    // when
    String result = AbstractRestApiRequestHandler.redactAuthorizationToken(input);

    // then
    assertTrue(result.contains("{\"Authorization\":\"***\""));
    assertFalse(result.contains("eyJhbGciOiJub25"));
  }
}
