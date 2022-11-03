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
package com.formkiq.stacks.dynamodb;

import static com.formkiq.stacks.dynamodb.DocumentVersionService.VERSION_ATTRIBUTE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import com.formkiq.aws.dynamodb.DbKeys;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/**
 * 
 * Unit Test for {@link DocumentVersionServiceDynamoDb}.
 *
 */
class DocumentVersionServiceDynamoDbTest implements DbKeys {

  /** {@link DocumentVersionServiceDynamoDb}. */
  private DocumentVersionServiceDynamoDb service = new DocumentVersionServiceDynamoDb();

  /**
   * Test First Time.
   */
  @Test
  void testAddDocumentVersionAttributes01() {
    // given
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {

      String documentId = UUID.randomUUID().toString();
      Map<String, AttributeValue> previous = keysDocument(siteId, documentId);
      Map<String, AttributeValue> current = keysDocument(siteId, documentId);

      // when
      this.service.addDocumentVersionAttributes(previous, current);

      // then
      assertTrue(previous.get(SK).s().startsWith("document#"));
      assertTrue(previous.get(SK).s().endsWith("#v1"));
      assertEquals("1", previous.get(VERSION_ATTRIBUTE).s());
      assertEquals("1", previous.get(VERSION_ATTRIBUTE).s());

      assertEquals("document", current.get(SK).s());
      assertEquals("2", current.get(VERSION_ATTRIBUTE).s());
      assertEquals("2", current.get(VERSION_ATTRIBUTE).s());
    }
  }

  /**
   * Test 2nd Time.
   */
  @Test
  void testAddDocumentVersionAttributes02() {
    // given
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {

      String documentId = UUID.randomUUID().toString();
      Map<String, AttributeValue> previous = keysDocument(siteId, documentId);

      Map<String, AttributeValue> current = keysDocument(siteId, documentId);
      current.put(VERSION_ATTRIBUTE, AttributeValue.fromS("2"));

      // when
      this.service.addDocumentVersionAttributes(previous, current);

      // then
      assertTrue(previous.get(SK).s().startsWith("document#"));
      assertTrue(previous.get(SK).s().endsWith("#v2"));
      assertEquals("2", previous.get(VERSION_ATTRIBUTE).s());
      assertEquals("2", previous.get(VERSION_ATTRIBUTE).s());

      assertEquals("document", current.get(SK).s());
      assertEquals("3", current.get(VERSION_ATTRIBUTE).s());
      assertEquals("3", current.get(VERSION_ATTRIBUTE).s());
    }
  }

  /**
   * Test 100th Time.
   */
  @Test
  void testAddDocumentVersionAttributes03() {
    // given
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {

      String documentId = UUID.randomUUID().toString();
      Map<String, AttributeValue> previous = keysDocument(siteId, documentId);

      Map<String, AttributeValue> current = keysDocument(siteId, documentId);
      current.put(VERSION_ATTRIBUTE, AttributeValue.fromS("100"));

      // when
      this.service.addDocumentVersionAttributes(previous, current);

      // then
      assertTrue(previous.get(SK).s().startsWith("document#"));
      assertTrue(previous.get(SK).s().endsWith("#v100"));
      assertEquals("100", previous.get(VERSION_ATTRIBUTE).s());
      assertEquals("100", previous.get(VERSION_ATTRIBUTE).s());

      assertEquals("document", current.get(SK).s());
      assertEquals("101", current.get(VERSION_ATTRIBUTE).s());
      assertEquals("101", current.get(VERSION_ATTRIBUTE).s());
    }
  }
}
