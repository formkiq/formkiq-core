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
package com.formkiq.stacks.dynamodb.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/** Unit tests for {@link SiteConfiguration}. */
public class SiteConfigurationTest {

  @Test
  public void testDocusignAccountIdWithoutCredentialsIsPersisted() {
    // given
    SiteConfiguration config = SiteConfiguration.builder()
        .docusign(new SiteConfigurationDocusign(null, null, null, null, null, null, "docusign"))
        .additionalDocusignAccounts(List.of()).build("default");

    // when
    SiteConfiguration result = SiteConfiguration.fromAttributeMap(config.getAttributes());

    // then
    assertEquals(config, result);
  }

  @Test
  public void testDocusignAccountsRetainExistingStorageFormat() {
    // given
    SiteConfigurationDocusign docusign =
        new SiteConfigurationDocusign("DEVELOPMENT", "default-user", "default-key", "private-key",
            "hmac", "https://default.example.com", "docusign");
    SiteConfigurationDocusign additional = new SiteConfigurationDocusign("PRODUCTION", "other-user",
        "other-key", "other-private-key", null, null, "additional-docusign");
    SiteConfiguration config = SiteConfiguration.builder().docusign(docusign)
        .additionalDocusignAccounts(List.of(additional)).build("default");

    // when
    Map<String, AttributeValue> attributes = config.getAttributes();
    final SiteConfiguration result = SiteConfiguration.fromAttributeMap(attributes);

    // then
    assertEquals("default-user", attributes.get(ConfigService.KEY_DOCUSIGN_USER_ID).s());
    assertEquals("docusign", attributes.get(ConfigService.KEY_DOCUSIGN_ACCOUNT_ID).s());
    assertEquals(1, attributes.get(ConfigService.ADDITIONAL_DOCUSIGN_ACCOUNTS).l().size());
    assertEquals(config, result);
  }

  @Test
  public void testDocusignEnvironmentIsNestedAndPersisted() {
    // given
    SiteConfiguration config = SiteConfiguration.builder()
        .docusign(new SiteConfigurationDocusign("PRODUCTION", null, null, null, null, null))
        .build("default");

    // when
    Map<String, AttributeValue> attributes = config.getAttributes();
    SiteConfiguration result = SiteConfiguration.fromAttributeMap(attributes);

    // then
    assertEquals("PRODUCTION", attributes.get(ConfigService.DOCUSIGN_ENVIRONMENT).s());
    assertEquals(config, result);
  }

  @Test
  public void testFromAttributeMapDefaultsDispositionAction() {
    // given
    SiteConfiguration config = SiteConfiguration.builder()
        .document(new SiteConfigurationDocument(
            new SiteConfigurationDocumentContentTypes(List.of("text/plain"), null), null))
        .build("default");

    Map<String, AttributeValue> attributes = new HashMap<>(config.getAttributes());
    attributes.remove("documentDispositionAction");

    // when
    SiteConfiguration result = SiteConfiguration.fromAttributeMap(attributes);

    // then
    assertNotNull(result.document());
    var retention = result.document().retentionAndDisposition();
    assertNotNull(retention);
    assertEquals(SiteConfigurationDocumentDispositionAction.SOFT_DELETE,
        retention.dispositionAction());
    assertEquals(-1L, retention.softDeleteRetentionInDays());
  }

  @Test
  public void testGetAttributesPersistsRetentionAndDisposition() {
    // given
    SiteConfiguration config = SiteConfiguration.builder()
        .document(new SiteConfigurationDocument(null,
            new SiteConfigurationDocumentRetentionAndDisposition(
                SiteConfigurationDocumentDispositionAction.DELETE, 30L)))
        .build("default");

    // when
    SiteConfiguration result = SiteConfiguration.fromAttributeMap(config.getAttributes());

    // then
    assertEquals(config, result);
  }
}
