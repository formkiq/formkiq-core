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
package com.formkiq.module.lambdaservices.logger;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class LogMessageBuilderTest {

  @Test
  void testInvalidJsonNeverLogsOriginalBody() {
    for (String json : List.of("", "  ", "<html>private-error</html>", "private-token",
        "{\"email\":\"private@example.com\"", "{\"status\":true} private-token",
        "{email:'private@example.com'}")) {
      assertEquals("Request | body=[REDACTED]",
          LogMessageBuilder.title("Request").propertyJson("body", json).build());
      assertEquals("Request | body=[REDACTED]",
          LogMessageBuilder.title("Request").propertyJson("body", json, List.of("email")).build());
    }
  }

  @Test
  void testNullPropertiesAndExistingProperties() {
    String log = LogMessageBuilder.title("Request").property("statusCode", 201)
        .properties(Map.of("method", "POST")).propertyJson(null, "{}").propertyJson("missing", null)
        .propertyJson("body", "null").build();
    assertEquals("Request | statusCode=201 | method=POST | body=null", log);
  }

  @Test
  void testPropertyJsonPreservesUnlistedValues() {
    String json = """
        {"status":"sent", "newOption":true, "count":2, "items":[1,null,"text\\nline"]}
        """;
    String log = LogMessageBuilder.title("Request").property("statusCode", 201)
        .propertyJson("body", json).build();
    assertEquals("Request | statusCode=201 | body={\"status\":\"sent\",\"newOption\":true,"
        + "\"count\":2,\"items\":[1,null,\"text\\nline\"]}", log);
  }

  @Test
  void testRedactionAppliesOnlyToSelectedProperty() {
    String json = "{\"name\":\"example\",\"status\":\"sent\"}";
    String log = LogMessageBuilder.title("Request").propertyJson("request", json, List.of("name"))
        .propertyJson("response", json).build();
    assertEquals("Request | request={\"name\":\"[REDACTED]\",\"status\":\"sent\"}"
        + " | response={\"name\":\"example\",\"status\":\"sent\"}", log);
  }

  @Test
  void testRedactsFieldsRecursivelyAndCaseInsensitively() {
    String json = """
        [{"Email":"private@example.com","nested":{"EMAIL":null,"status":"sent"},
          "items":[{"email":123,"visible":true},[{"email":false}]],
          "credentials":{"token":"private-token"},"documents":["private-document"]}]
        """;
    String log = LogMessageBuilder.title("Request")
        .propertyJson("body", json, List.of("eMaIl", "credentials", "documents")).build();
    assertEquals("Request | body=[{\"Email\":\"[REDACTED]\","
        + "\"nested\":{\"EMAIL\":\"[REDACTED]\",\"status\":\"sent\"},"
        + "\"items\":[{\"email\":\"[REDACTED]\",\"visible\":true},"
        + "[{\"email\":\"[REDACTED]\"}]],\"credentials\":\"[REDACTED]\","
        + "\"documents\":\"[REDACTED]\"}]", log);
  }
}
