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
  public void testFromAttributeMapDefaultsDispositionAction() {
    SiteConfiguration config = SiteConfiguration.builder()
        .document(new SiteConfigurationDocument(
            new SiteConfigurationDocumentContentTypes(List.of("text/plain"), null), null))
        .build("default");

    Map<String, AttributeValue> attributes = new HashMap<>(config.getAttributes());
    attributes.remove("documentDispositionAction");

    SiteConfiguration result = SiteConfiguration.fromAttributeMap(attributes);

    assertNotNull(result.document());
    assertNotNull(result.document().retentionAndDisposition());
    assertEquals(SiteConfigurationDocumentDispositionAction.SOFT_DELETE,
        result.document().retentionAndDisposition().dispositionAction());
  }

  @Test
  public void testGetAttributesPersistsDispositionAction() {
    SiteConfiguration config = SiteConfiguration.builder()
        .document(new SiteConfigurationDocument(null,
            new SiteConfigurationDocumentRetentionAndDisposition(
                SiteConfigurationDocumentDispositionAction.DELETE)))
        .build("default");

    SiteConfiguration result = SiteConfiguration.fromAttributeMap(config.getAttributes());

    assertNotNull(result.document());
    assertNotNull(result.document().retentionAndDisposition());
    assertEquals(SiteConfigurationDocumentDispositionAction.DELETE,
        result.document().retentionAndDisposition().dispositionAction());
  }
}
