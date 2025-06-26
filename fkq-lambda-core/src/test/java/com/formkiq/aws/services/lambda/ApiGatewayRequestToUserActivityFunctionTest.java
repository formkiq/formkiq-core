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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;

import com.formkiq.plugins.useractivity.UserActivity;
import com.formkiq.plugins.useractivity.UserActivityStatus;
import com.google.gson.Gson;
import org.junit.jupiter.api.Test;

/**
 * Unit Test for {@link ApiGatewayRequestToUserActivityFunction}.
 */
public class ApiGatewayRequestToUserActivityFunctionTest {

  /** {@link Gson}. */
  private final Gson gson = new Gson();
  /** {@link ApiGatewayRequestToUserActivityFunction}. */
  private final ApiGatewayRequestToUserActivityFunction function =
      new ApiGatewayRequestToUserActivityFunction();

  @Test
  public void testDocumentUrlToUserActivity01() throws IOException {
    // given
    ApiGatewayRequestEvent request =
        loadFile("src/test/resources/requests/get-documentid-url.json");

    // when
    UserActivity activity = function.apply(request).build();

    // then
    assertNotNull(activity);
    assertEquals("GET", activity.type());
    final long expectedTime = 1750944882199L;
    assertEquals(Instant.ofEpochMilli(expectedTime), activity.insertedDate());
    assertEquals(UserActivityStatus.SUCCESS, activity.status());
    assertEquals("1.73.5.111", activity.sourceIpAddress());
    assertNull(activity.message());
    assertEquals(request.getPathParameters().get("documentId"), activity.documentId());
  }

  private ApiGatewayRequestEvent loadFile(final String file) throws IOException {
    String json = Files.readString(Paths.get(file), StandardCharsets.UTF_8);
    return gson.fromJson(json, ApiGatewayRequestEvent.class);
  }
}
