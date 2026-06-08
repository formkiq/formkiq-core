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
package com.formkiq.aws.dynamodb.documents;

import com.formkiq.aws.dynamodb.ID;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Unit Tests for {@link DeleteSoftDeletedDocumentArtifactsQuery}.
 */
public class DeleteSoftDeletedDocumentArtifactsQueryTest {

  /**
   * Build query for all soft-deleted artifacts belonging to a document.
   */
  @Test
  void testBuildDocumentArtifacts() {
    String siteId = ID.uuid();
    String documentId = ID.uuid();

    QueryRequest request =
        new DeleteSoftDeletedDocumentArtifactsQuery(DocumentArtifact.of(documentId, null))
            .build("Documents", siteId, null, 10);

    assertNull(request.indexName());
    assertEquals("#PK = :PK AND begins_with(#SK,:SK)", request.keyConditionExpression());
    assertEquals(siteId + "/softdelete#docs#", request.expressionAttributeValues().get(":PK").s());
    assertEquals("softdelete#document#art#", request.expressionAttributeValues().get(":SK").s());
    assertEquals(Boolean.TRUE, request.consistentRead());
  }

  /**
   * Build query for a single soft-deleted artifact.
   */
  @Test
  void testBuildSingleArtifact() {
    String siteId = ID.uuid();
    String documentId = ID.uuid();
    String artifactId = ID.uuid();

    QueryRequest request =
        new DeleteSoftDeletedDocumentArtifactsQuery(DocumentArtifact.of(documentId, artifactId))
            .build("Documents", siteId, null, 10);

    assertNull(request.indexName());
    assertEquals("#PK = :PK AND #SK = :SK", request.keyConditionExpression());
    assertEquals(siteId + "/softdelete#docs#", request.expressionAttributeValues().get(":PK").s());
    assertEquals("softdelete#document#art#" + artifactId,
        request.expressionAttributeValues().get(":SK").s());
    assertEquals(Boolean.TRUE, request.consistentRead());
  }
}
