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
package com.formkiq.stacks.api.handler.entity;

import com.formkiq.aws.dynamodb.ApiAuthorization;
import com.formkiq.aws.dynamodb.DynamoDbKey;
import com.formkiq.aws.dynamodb.DynamoDbService;
import com.formkiq.aws.dynamodb.ID;
import com.formkiq.aws.dynamodb.entity.EntityTypeName;
import com.formkiq.aws.dynamodb.entity.EntityTypeNamespace;
import com.formkiq.aws.dynamodb.entity.EntityTypeRecord;
import com.formkiq.aws.services.lambda.ApiGatewayRequestEvent;
import com.formkiq.module.lambdaservices.AwsServiceCache;
import com.formkiq.module.lambdaservices.ClassServiceExtension;
import com.google.gson.Gson;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/** Tests entity identity resolution before permission selection. */
class EntityRequestHandlerInterceptorTest {

  private ApiGatewayRequestEvent event(final String entityTypeId) {
    ApiGatewayRequestEvent event = new ApiGatewayRequestEvent();
    event.setResource("/entities/{entityTypeId}");
    event.setHttpMethod("GET");
    event.setPathParameters(new HashMap<>(Map.of("entityTypeId", entityTypeId)));
    return event;
  }

  private AwsServiceCache services(final EntityTypeRecord entityType, final List<Object> reads) {
    Map<String, AttributeValue> attributes =
        entityType != null ? entityType.getAttributes() : Map.of();
    DynamoDbService db =
        (DynamoDbService) Proxy.newProxyInstance(DynamoDbService.class.getClassLoader(),
            new Class<?>[] {DynamoDbService.class}, (proxy, method, args) -> {
              if ("get".equals(method.getName()) || "getByQuery".equals(method.getName())) {
                reads.add(args[0]);
                return attributes;
              }
              throw new AssertionError("Unexpected database method: " + method.getName());
            });
    AwsServiceCache services =
        new AwsServiceCache().environment(Map.of("DOCUMENTS_TABLE", "Documents"));
    services.register(DynamoDbService.class, new ClassServiceExtension<>(db));
    services.register(Gson.class, new ClassServiceExtension<>(new Gson()));
    return services;
  }

  /**
   * New type definitions use their request identity when creating a missing UUID or posting.
   *
   * @param method creation method
   */
  @ParameterizedTest
  @ValueSource(strings = {"POST", "PUT"})
  void testCreateEntityType(final String method) {
    // given
    final ApiAuthorization authorization = new ApiAuthorization();
    List<Object> reads = new ArrayList<>();
    final var interceptor = new EntityRequestHandlerInterceptor(services(null, reads));
    ApiGatewayRequestEvent event = new ApiGatewayRequestEvent();
    event.setHttpMethod(method);
    event.setResource("/entityTypes");
    if ("PUT".equals(method)) {
      event.setResource("/entityTypes/{entityTypeId}");
      event.setPathParameters(new HashMap<>(Map.of("entityTypeId", ID.uuid())));
      event.addQueryParameter("createIfMissing", "true");
    }
    event.setBody("""
        {"entityType":{"namespace":"PRESET","name":"OtherPreset"}}
        """);

    // when
    interceptor.beforeProcessRequest(event, authorization);

    // then
    assertEquals(new EntityTypeName(EntityTypeNamespace.PRESET, "OtherPreset"),
        authorization.<EntityTypeName>getCacheObject(EntityTypeName.class.getName()));
    assertEquals("PUT".equals(method) ? 1 : 0, reads.size());
  }

  /** Existing types retain their stored identity even when the update body renames them. */
  @Test
  void testExistingPutUsesStoredIdentity() {
    // given
    final ApiAuthorization authorization = new ApiAuthorization();
    EntityTypeRecord entityType =
        EntityTypeRecord.builder(List.of("ExamplePreset")).documentId(ID.uuid())
            .namespace(EntityTypeNamespace.PRESET).name("ExamplePreset").build((String) null);
    final var interceptor =
        new EntityRequestHandlerInterceptor(services(entityType, new ArrayList<>()));
    ApiGatewayRequestEvent event = event(entityType.documentId());
    event.setHttpMethod("PUT");
    event.setResource("/entityTypes/{entityTypeId}");
    event.setBody("""
        {"entityType":{"namespace":"CUSTOM","name":"Different"}}
        """);

    // when
    interceptor.beforeProcessRequest(event, authorization);

    // then
    assertEquals(new EntityTypeName(EntityTypeNamespace.PRESET, "ExamplePreset"),
        authorization.<EntityTypeName>getCacheObject(EntityTypeName.class.getName()));
  }

  /** A missing type cannot retain stale identity metadata. */
  @Test
  void testMissingEntityType() {
    // given
    final ApiAuthorization authorization = new ApiAuthorization();
    final var interceptor = new EntityRequestHandlerInterceptor(services(null, new ArrayList<>()));
    ApiGatewayRequestEvent event = event(ID.uuid());
    authorization.addCacheObject(EntityTypeName.class.getName(),
        new EntityTypeName(EntityTypeNamespace.CUSTOM, "Stale"));

    // when
    interceptor.beforeProcessRequest(event, authorization);

    // then
    assertNull(authorization.<EntityTypeName>getCacheObject(EntityTypeName.class.getName()));
  }

  /** A missing PUT type without createIfMissing leaves validation to the handler. */
  @Test
  void testMissingPutWithoutCreate() {
    // given
    final ApiAuthorization authorization = new ApiAuthorization();
    final var interceptor = new EntityRequestHandlerInterceptor(services(null, new ArrayList<>()));
    ApiGatewayRequestEvent event = event(ID.uuid());
    event.setHttpMethod("PUT");
    event.setResource("/entityTypes/{entityTypeId}");

    // when
    interceptor.beforeProcessRequest(event, authorization);

    // then
    assertNull(authorization.<EntityTypeName>getCacheObject(EntityTypeName.class.getName()));
  }

  /**
   * Named requests retain their identity after the path is normalized to a UUID.
   *
   * @param namespace entity namespace
   */
  @ParameterizedTest
  @ValueSource(strings = {"PRESET", "CUSTOM"})
  void testNameResolution(final String namespace) {
    // given
    final ApiAuthorization authorization = new ApiAuthorization().siteId("finance");
    EntityTypeNamespace ns = EntityTypeNamespace.valueOf(namespace);
    EntityTypeRecord entityType = EntityTypeRecord.builder(List.of("ExamplePreset"))
        .documentId(ID.uuid()).namespace(ns).name("ExamplePreset").build("finance");
    List<Object> reads = new ArrayList<>();
    final var interceptor = new EntityRequestHandlerInterceptor(services(entityType, reads));
    ApiGatewayRequestEvent event = event("ExamplePreset");
    event.addQueryParameter("namespace", namespace);

    // when
    interceptor.beforeProcessRequest(event, authorization);

    // then
    assertEquals(entityType.documentId(), event.getPathParameter("entityTypeId"));
    assertEquals(new EntityTypeName(ns, "ExamplePreset"),
        authorization.<EntityTypeName>getCacheObject(EntityTypeName.class.getName()));
    assertEquals(2, reads.size());
    assertEquals("Documents", ((QueryRequest) reads.getFirst()).tableName());
    assertEquals(entityType.key().pk(), ((DynamoDbKey) reads.get(1)).pk());
  }

  /** Unrelated requests and type listings clear metadata without accessing the database. */
  @Test
  void testNoEntityType() {
    // given
    final ApiAuthorization authorization = new ApiAuthorization();
    final var interceptor = new EntityRequestHandlerInterceptor(new AwsServiceCache());
    ApiGatewayRequestEvent event = new ApiGatewayRequestEvent();
    event.setResource("/entityTypes");
    event.setHttpMethod("GET");
    authorization.addCacheObject(EntityTypeName.class.getName(),
        new EntityTypeName(EntityTypeNamespace.PRESET, "ExamplePreset"));

    // when
    interceptor.beforeProcessRequest(event, authorization);
    interceptor.beforeProcessRequest(null, authorization);

    // then
    assertNull(authorization.<EntityTypeName>getCacheObject(EntityTypeName.class.getName()));
  }

  /** Cached identity belongs only to the authorization instance processing the request. */
  @Test
  void testRequestCacheIsolation() {
    // given
    final ApiAuthorization authorization = new ApiAuthorization();
    final ApiAuthorization other = new ApiAuthorization();
    final var interceptor = new EntityRequestHandlerInterceptor(services(null, new ArrayList<>()));
    ApiGatewayRequestEvent event = new ApiGatewayRequestEvent();
    event.setHttpMethod("POST");
    event.setResource("/entityTypes");
    event.setBody("""
        {"entityType":{"namespace":"PRESET","name":"ExamplePreset"}}
        """);

    // when
    interceptor.beforeProcessRequest(event, authorization);

    // then
    assertEquals(new EntityTypeName(EntityTypeNamespace.PRESET, "ExamplePreset"),
        authorization.<EntityTypeName>getCacheObject(EntityTypeName.class.getName()));
    assertNull(other.getCacheObject(EntityTypeName.class.getName()));
  }

  /**
   * UUID requests use the stored identity even when the caller supplies a different namespace.
   *
   * @param namespace stored namespace
   * @param name stored name
   */
  @ParameterizedTest
  @CsvSource({"PRESET, ExamplePreset", "PRESET, OtherPreset", "CUSTOM, ExamplePreset"})
  void testUuidUsesStoredIdentity(final EntityTypeNamespace namespace, final String name) {
    // given
    final ApiAuthorization authorization = new ApiAuthorization().siteId("finance");
    EntityTypeRecord entityType = EntityTypeRecord.builder(List.of(name)).documentId(ID.uuid())
        .namespace(namespace).name(name).build("finance");
    List<Object> reads = new ArrayList<>();
    final var interceptor = new EntityRequestHandlerInterceptor(services(entityType, reads));
    ApiGatewayRequestEvent event = event(entityType.documentId());
    event.addQueryParameter("namespace", "CUSTOM");
    authorization.addCacheObject(EntityTypeName.class.getName(),
        new EntityTypeName(EntityTypeNamespace.PRESET, "Spoofed"));

    // when
    interceptor.beforeProcessRequest(event, authorization);

    // then
    assertEquals(new EntityTypeName(namespace, name),
        authorization.<EntityTypeName>getCacheObject(EntityTypeName.class.getName()));
    assertEquals(entityType.documentId(), event.getPathParameter("entityTypeId"));
    assertEquals(namespace.name(), event.getQueryStringParameter("namespace"));
    assertEquals(1, reads.size());
    assertEquals(entityType.key().pk(), ((DynamoDbKey) reads.getFirst()).pk());
  }

  /** A preset UUID without a namespace must not be classified as CUSTOM. */
  @Test
  void testUuidWithoutNamespace() {
    // given
    final ApiAuthorization authorization = new ApiAuthorization();
    EntityTypeRecord entityType =
        EntityTypeRecord.builder(List.of("ExamplePreset")).documentId(ID.uuid())
            .namespace(EntityTypeNamespace.PRESET).name("ExamplePreset").build((String) null);
    final var interceptor =
        new EntityRequestHandlerInterceptor(services(entityType, new ArrayList<>()));
    ApiGatewayRequestEvent event = event(entityType.documentId());

    // when
    interceptor.beforeProcessRequest(event, authorization);

    // then
    assertEquals(new EntityTypeName(EntityTypeNamespace.PRESET, "ExamplePreset"),
        authorization.<EntityTypeName>getCacheObject(EntityTypeName.class.getName()));
    assertEquals("PRESET", event.getQueryStringParameter("namespace"));
  }
}
