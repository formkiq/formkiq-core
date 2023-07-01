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

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import com.formkiq.aws.dynamodb.DbKeys;
import com.formkiq.stacks.dynamodb.permissions.DocumentPermission;
import com.formkiq.stacks.dynamodb.permissions.Permission;
import com.formkiq.stacks.dynamodb.permissions.PermissionType;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

class DocumentPermissionTest {

  @Test
  void test() {
    // given
    String documentId = UUID.randomUUID().toString();
    String name = "finance";
    Permission permission = Permission.DELETE;
    PermissionType type = PermissionType.GROUP;

    // when
    DocumentPermission p = new DocumentPermission().documentId(documentId).name(name).type(type)
        .permission(permission).userId("joe");

    // then
    assertEquals("docs#" + documentId, p.pk(null));
    assertEquals("permission#GROUP_DELETE#finance", p.sk());

    assertEquals("123/docs#" + documentId, p.pk("123"));
    assertEquals("permission#GROUP_DELETE#finance", p.sk());

    final int expected = 7;
    Map<String, AttributeValue> attributes = p.getAttributes(null);
    assertEquals(expected, attributes.size());
    assertEquals("docs#" + documentId, attributes.get(DbKeys.PK).s());
    assertEquals("permission#GROUP_DELETE#finance", attributes.get(DbKeys.SK).s());
    assertEquals(documentId, attributes.get("documentId").s());
    assertEquals("GROUP", attributes.get("type").s());
    assertEquals("finance", attributes.get("name").s());
    assertEquals("joe", attributes.get("userId").s());
    assertEquals("DELETE", attributes.get("permission").s());
  }
}
