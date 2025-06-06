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

import static com.formkiq.aws.dynamodb.SiteIdKeyGenerator.createDatabaseKey;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static software.amazon.awssdk.services.dynamodb.model.AttributeValue.fromS;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import com.formkiq.aws.dynamodb.DbKeys;
import com.formkiq.aws.dynamodb.ID;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/**
 * Unit Tests for {@link DocumentDeleteMoveAttributeFunction}.
 */
class DocumentDeleteMoveAttributeFunctionTest implements DbKeys {

  /**
   * Test Soft delete a document.
   */
  @Test
  void testTransformDocument() {
    // given
    for (String siteId : Arrays.asList(null, ID.uuid())) {
      String documentId = ID.uuid();

      Map<String, AttributeValue> attrs = keysDocument(siteId, documentId);

      // when
      DocumentDeleteMoveAttributeFunction fn =
          new DocumentDeleteMoveAttributeFunction(siteId, documentId);
      Map<String, AttributeValue> result = fn.transform(attrs);

      // then
      if (siteId != null) {
        assertEquals(siteId + "/softdelete#docs#", result.get(DbKeys.PK).s());
      } else {
        assertEquals("softdelete#docs#", result.get(DbKeys.PK).s());
      }

      assertEquals("softdelete#document#" + documentId, result.get(DbKeys.SK).s());

      // given
      DocumentRestoreMoveAttributeFunction rfn =
          new DocumentRestoreMoveAttributeFunction(siteId, documentId);

      // when
      Map<String, AttributeValue> restore = rfn.transform(result);

      // then
      if (siteId != null) {
        assertEquals(siteId + "/docs#" + documentId, restore.get(DbKeys.PK).s());
      } else {
        assertEquals("docs#" + documentId, restore.get(DbKeys.PK).s());
      }

      assertEquals("document", restore.get(DbKeys.SK).s());
    }
  }

  /**
   * Test Soft delete a document attribute.
   */
  @Test
  void testTransformDocumentAttribute() {
    // given
    for (String siteId : Arrays.asList(null, ID.uuid())) {
      String documentId = ID.uuid();

      Map<String, AttributeValue> attrs = new HashMap<>();
      attrs.put(PK, fromS(createDatabaseKey(siteId, "docs#" + documentId)));
      attrs.put(SK, fromS("attr#category#111"));
      attrs.put(GSI1_PK, fromS(createDatabaseKey(siteId, "docs#attr#category")));
      attrs.put(GSI1_SK, fromS("111"));

      // when
      DocumentDeleteMoveAttributeFunction fn =
          new DocumentDeleteMoveAttributeFunction(siteId, documentId);
      Map<String, AttributeValue> result = fn.transform(attrs);

      // then
      if (siteId != null) {
        assertEquals("softdelete#" + siteId + "/docs#" + documentId, result.get(DbKeys.PK).s());
        assertEquals("softdelete#" + siteId + "/docs#attr#category",
            result.get(DbKeys.GSI1_PK).s());
      } else {
        assertEquals("softdelete#docs#" + documentId, result.get(DbKeys.PK).s());
        assertEquals("softdelete#docs#attr#category", result.get(DbKeys.GSI1_PK).s());
      }

      assertEquals("softdelete#attr#category#111", result.get(DbKeys.SK).s());
      assertEquals("111", result.get(DbKeys.GSI1_SK).s());

      // given
      DocumentRestoreMoveAttributeFunction rfn =
          new DocumentRestoreMoveAttributeFunction(siteId, documentId);

      // when
      Map<String, AttributeValue> restore = rfn.transform(result);

      // then
      if (siteId != null) {
        assertEquals(siteId + "/docs#" + documentId, restore.get(DbKeys.PK).s());
        assertEquals(siteId + "/docs#attr#category", restore.get(DbKeys.GSI1_PK).s());
      } else {
        assertEquals("docs#" + documentId, restore.get(DbKeys.PK).s());
        assertEquals("docs#attr#category", restore.get(DbKeys.GSI1_PK).s());
      }

      assertEquals("attr#category#111", restore.get(DbKeys.SK).s());
      assertEquals("111", restore.get(DbKeys.GSI1_SK).s());
    }
  }
}
