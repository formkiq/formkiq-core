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

import com.formkiq.aws.dynamodb.DbKeys;
import com.formkiq.aws.dynamodb.DynamoDbConnectionBuilder;
import com.formkiq.stacks.dynamodb.attributes.DocumentAttributeRecord;
import com.formkiq.stacks.dynamodb.attributes.DocumentAttributeValueType;
import com.formkiq.testutils.aws.DynamoDbExtension;
import com.formkiq.testutils.aws.DynamoDbTestServices;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.formkiq.testutils.aws.DynamoDbExtension.DOCUMENTS_VERSION_TABLE;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 
 * Unit Test for {@link DocumentVersionServiceDynamoDb}.
 *
 */
@ExtendWith(DynamoDbExtension.class)
class DocumentVersionServiceDynamoDbTest implements DbKeys {

  /** {@link DocumentVersionServiceDynamoDb}. */
  private static DocumentVersionServiceDynamoDb service;

  /**
   * Before All.
   */
  @BeforeAll
  public static void beforeAll() throws URISyntaxException {
    service = new DocumentVersionServiceDynamoDb();
    DynamoDbConnectionBuilder connection = DynamoDbTestServices.getDynamoDbConnection();
    service.initialize(Map.of("DOCUMENT_VERSIONS_TABLE", DOCUMENTS_VERSION_TABLE), connection);
  }

  /**
   * Add Records Version.
   * 
   * @throws URISyntaxException URISyntaxException
   */
  @Test
  void testAddRecords01() throws URISyntaxException {
    // given
    Date date = new Date();

    try (DynamoDbClient client = DynamoDbTestServices.getDynamoDbConnection().build()) {

      for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {

        String documentId = UUID.randomUUID().toString();
        DocumentAttributeRecord r = new DocumentAttributeRecord().setDocumentId(documentId)
            .setKey("category").setStringValue("document").setInsertedDate(date)
            .setValueType(DocumentAttributeValueType.STRING).setUserId("joe");
        Map<String, AttributeValue> orig = r.getAttributes(siteId);

        // when
        List<Map<String, AttributeValue>> list = service.addRecords(siteId, List.of(r));

        // then
        assertEquals(1, list.size());

        Map<String, AttributeValue> attr = list.get(0);
        assertEquals(orig.keySet().size() + 1, attr.keySet().size());
        assertEquals(orig.get(PK), attr.get(PK));
        assertEquals(orig.get(SK), attr.get("archive#" + SK));
        assertEquals(orig.get(GSI1_PK), attr.get(GSI1_PK));
        assertEquals(orig.get(GSI1_SK), attr.get(GSI1_SK));
        assertEquals(orig.get("key"), attr.get("key"));
        assertEquals(orig.get("documentId"), attr.get("documentId"));
        assertEquals(orig.get("userId"), attr.get("userId"));
        assertEquals(orig.get("stringValue"), attr.get("stringValue"));
        assertEquals(orig.get("inserteddate"), attr.get("inserteddate"));
        assertEquals("attr#category#" + attr.get("inserteddate").s() + "#document",
            attr.get(SK).s());
      }
    }
  }
}
