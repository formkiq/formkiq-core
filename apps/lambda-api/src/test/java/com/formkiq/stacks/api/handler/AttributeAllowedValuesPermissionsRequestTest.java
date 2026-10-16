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

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;

import com.formkiq.aws.dynamodb.ID;
import com.formkiq.aws.services.lambda.ApiResponseStatus;
import com.formkiq.client.invoker.ApiException;
import com.formkiq.client.model.AddAttributeSchemaRequired;
import com.formkiq.client.model.AddClassification;
import com.formkiq.client.model.AddClassificationRequest;
import com.formkiq.client.model.SetSchemaAttributes;
import com.formkiq.client.model.SetSitesSchemaRequest;
import com.formkiq.testutils.api.SetBearerPermissionMap;
import com.formkiq.testutils.api.attributes.AddAttributeRequestBuilder;
import com.formkiq.testutils.api.attributes.GetAttributeAllowedValuesRequestBuilder;
import com.formkiq.testutils.api.schemas.AddClassificationRequestBuilder;
import com.formkiq.testutils.api.schemas.GetClassificationAttributeAllowedValuesRequestBuilder;
import com.formkiq.testutils.api.schemas.GetSitesSchemaAttributeAllowedValuesRequestBuilder;
import com.formkiq.testutils.api.schemas.SetSitesSchemaRequestBuilder;
import org.junit.jupiter.api.Test;

/** API permission tests for attribute, site schema, and classification allowed values. */
public class AttributeAllowedValuesPermissionsRequestTest extends AbstractApiClientRequestTest {

  /** Attribute key shared by the allowed-value fixtures. */
  private static final String ATTRIBUTE_KEY = "category";

  /** Allowed values shared by the site schema and classification. */
  private static final List<String> ALLOWED_VALUES = List.of("approved");

  private String createAllowedValues(final String siteId) throws ApiException {
    setBearerToken(siteId);
    new AddAttributeRequestBuilder().keyAsString(ATTRIBUTE_KEY).submitOk(client, siteId);
    var attributes = new SetSchemaAttributes().addRequiredItem(
        new AddAttributeSchemaRequired().attributeKey(ATTRIBUTE_KEY).allowedValues(ALLOWED_VALUES));
    new SetSitesSchemaRequestBuilder()
        .withSetSitesSchemaRequest(
            new SetSitesSchemaRequest().name("Allowed values").attributes(attributes))
        .submitOk(client, siteId);
    return new AddClassificationRequestBuilder()
        .request(new AddClassificationRequest()
            .classification(new AddClassification().name("Approval").attributes(attributes)))
        .submitOk(client, siteId).response().getClassificationId();
  }

  private void setPermission(final String siteId, final String permission) {
    new SetBearerPermissionMap().apply(client, List.of(siteId), Map.of(siteId, List.of(permission)),
        Map.of());
  }

  /** Permission coverage for GET /attributes/{key}/allowedValues. */
  @Test
  public void testGetAttributeAllowedValuesPermissions() throws ApiException {
    // given
    String siteId = ID.uuid();
    createAllowedValues(siteId);
    var request = new GetAttributeAllowedValuesRequestBuilder().withKey(ATTRIBUTE_KEY);
    setPermission(siteId, "READ_ATTRIBUTES");

    // when
    var scopedResponse = request.submitOk(client, siteId).response();

    // then
    assertEquals(ALLOWED_VALUES, scopedResponse.getAllowedValues());

    // when
    setPermission(siteId, "READ");
    var parentResponse = request.submitOk(client, siteId).response();

    // then
    assertEquals(ALLOWED_VALUES, parentResponse.getAllowedValues());

    // when
    setPermission(siteId, "READ_SCHEMAS");
    var deniedResponse = request.submitError(client, siteId);

    // then
    assertEquals(ApiResponseStatus.SC_UNAUTHORIZED.getStatusCode(),
        deniedResponse.exception().getCode());
  }

  /**
   * Permission coverage for GET
   * /sites/{siteId}/classifications/{classificationId}/attributes/{key}/allowedValues.
   */
  @Test
  public void testGetClassificationAllowedValuesPermissions() throws ApiException {
    // given
    String siteId = ID.uuid();
    String classificationId = createAllowedValues(siteId);
    var request = new GetClassificationAttributeAllowedValuesRequestBuilder().withKey(ATTRIBUTE_KEY)
        .withClassificationId(classificationId);
    setPermission(siteId, "READ_ATTRIBUTES");

    // when
    var scopedResponse = request.submitOk(client, siteId).response();

    // then
    assertEquals(ALLOWED_VALUES, scopedResponse.getAllowedValues());

    // when
    setPermission(siteId, "READ");
    var parentResponse = request.submitOk(client, siteId).response();

    // then
    assertEquals(ALLOWED_VALUES, parentResponse.getAllowedValues());

    // when
    setPermission(siteId, "READ_SCHEMAS");
    var deniedResponse = request.submitError(client, siteId);

    // then
    assertEquals(ApiResponseStatus.SC_UNAUTHORIZED.getStatusCode(),
        deniedResponse.exception().getCode());
  }

  /** Permission coverage for GET /sites/{siteId}/schema/document/attributes/{key}/allowedValues. */
  @Test
  public void testGetSitesSchemaAllowedValuesPermissions() throws ApiException {
    // given
    String siteId = ID.uuid();
    createAllowedValues(siteId);
    var request = new GetSitesSchemaAttributeAllowedValuesRequestBuilder().withKey(ATTRIBUTE_KEY);
    setPermission(siteId, "READ_ATTRIBUTES");

    // when
    var scopedResponse = request.submitOk(client, siteId).response();

    // then
    assertEquals(ALLOWED_VALUES, scopedResponse.getAllowedValues());

    // when
    setPermission(siteId, "READ");
    var parentResponse = request.submitOk(client, siteId).response();

    // then
    assertEquals(ALLOWED_VALUES, parentResponse.getAllowedValues());

    // when
    setPermission(siteId, "READ_SCHEMAS");
    var deniedResponse = request.submitError(client, siteId);

    // then
    assertEquals(ApiResponseStatus.SC_UNAUTHORIZED.getStatusCode(),
        deniedResponse.exception().getCode());
  }

}
