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

import com.formkiq.aws.dynamodb.ApiAuthorization;
import com.formkiq.aws.dynamodb.base64.Pagination;
import com.formkiq.aws.dynamodb.cache.CacheService;
import com.formkiq.aws.services.lambda.ApiGatewayRequestEvent;
import com.formkiq.module.lambdaservices.AwsServiceCache;
import com.formkiq.module.lambdaservices.ClassServiceExtension;
import com.formkiq.stacks.dynamodb.DocumentSearchResult;
import com.formkiq.stacks.dynamodb.DocumentSearchService;
import com.formkiq.stacks.dynamodb.DocumentService;
import com.google.gson.Gson;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/** Tests search response metadata without writing large DynamoDB fixtures. */
class SearchRequestHandlerTest {

  private <T> T stub(final Class<T> type, final BiFunction<String, Object[], Object> handler) {
    return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] {type},
        (proxy, method, args) -> handler.apply(method.getName(), args)));
  }

  /**
   * A next token alone does not determine whether a page was truncated by its budget.
   *
   * @param truncated whether the search stopped at its processing budget
   * @throws Exception if the request fails
   */
  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void testSearchResponseTruncated(final boolean truncated) throws Exception {
    // given
    Pagination<DocumentSearchResult> page = new Pagination<>(List.of(), "resume", truncated);
    DocumentSearchService search = stub(DocumentSearchService.class, (method, args) -> {
      assertEquals("search", method);
      return page;
    });
    CacheService cache = stub(CacheService.class, (method, args) -> {
      assertEquals("write", method);
      return null;
    });
    DocumentService documents = stub(DocumentService.class, (method, args) -> {
      throw new AssertionError("Unexpected document call: " + method);
    });
    AwsServiceCache services = new AwsServiceCache();
    services.register(Gson.class, new ClassServiceExtension<>(new Gson()));
    services.register(DocumentSearchService.class, new ClassServiceExtension<>(search));
    services.register(DocumentService.class, new ClassServiceExtension<>(documents));
    services.register(CacheService.class, new ClassServiceExtension<>(cache));
    ApiGatewayRequestEvent event = new ApiGatewayRequestEvent();
    event.setBody("""
        {"query":{"attributes":[
          {"key":"customer","eq":"123"},
          {"key":"status","eq":"approved"}
        ]}}
        """);

    // when
    var response =
        new SearchRequestHandler().post(event, new ApiAuthorization().siteId("site"), services);

    // then
    assertEquals(200, response.statusCode());
    Map<?, ?> body = (Map<?, ?>) response.body();
    assertEquals(truncated, body.get("truncated"));
    assertEquals(List.of(), body.get("documents"));
    assertNotNull(body.get("next"));
  }
}
