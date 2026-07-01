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
package com.formkiq.aws.dynamodb.documentattributes;

import com.formkiq.aws.dynamodb.DynamoDbService;
import com.formkiq.aws.dynamodb.DynamoDbServiceImpl;
import com.formkiq.aws.dynamodb.ID;
import com.formkiq.aws.dynamodb.entity.EntityAttribute;
import com.formkiq.aws.dynamodb.entity.EntityRecord;
import com.formkiq.aws.dynamodb.entity.RetentionMode;
import com.formkiq.testutils.aws.DynamoDbExtension;
import com.formkiq.testutils.aws.DynamoDbTestServices;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Arrays;
import java.util.List;

import static com.formkiq.aws.dynamodb.attributes.AttributeKeyReserved.RETENTION_MODE;
import static com.formkiq.aws.dynamodb.attributes.AttributeKeyReserved.RETENTION_POLICY;
import static com.formkiq.testutils.aws.DynamoDbExtension.DOCUMENTS_TABLE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/** Unit tests for {@link DocumentAttributeHasRetentionMode}. */
@ExtendWith(DynamoDbExtension.class)
public class DocumentAttributeHasRetentionModeTest {

  /** {@link DynamoDbService}. */
  private DynamoDbService db;

  private String addRetentionEntity(final String siteId, final String retentionMode) {
    String entityTypeId = RETENTION_POLICY.getKey();
    String entityId = ID.uuid();
    EntityRecord entityRecord = EntityRecord.builder().entityTypeId(entityTypeId)
        .documentId(entityId).name("Retention " + entityId).attributes(List.of(EntityAttribute
            .builder().key(RETENTION_MODE.getKey()).addStringValue(retentionMode).build()))
        .build(siteId);
    this.db.putItem(entityRecord.getAttributes());
    return new DocumentAttributeEntityKeyValue(entityTypeId, entityId).getStringValue();
  }

  /**
   * Before Each.
   *
   * @throws Exception Exception
   */
  @BeforeEach
  public void before() throws Exception {
    this.db =
        new DynamoDbServiceImpl(DynamoDbTestServices.getDynamoDbConnection(), DOCUMENTS_TABLE);
  }

  private DocumentAttributeRecord retentionPolicyAttribute(final String stringValue) {
    return new DocumentAttributeRecord().setKey(RETENTION_POLICY.getKey())
        .setStringValue(stringValue).setValueType(DocumentAttributeValueType.ENTITY);
  }

  /**
   * Non-retention document attributes do not have a retention mode.
   */
  @Test
  public void testNonRetentionAttributeReturnsNull() {
    for (String siteId : Arrays.asList(null, ID.uuid())) {
      DocumentAttributeRecord docAttr = new DocumentAttributeRecord().setKey("category")
          .setStringValue("invoice").setValueType(DocumentAttributeValueType.STRING);

      assertNull(new DocumentAttributeHasRetentionMode(this.db).apply(siteId, docAttr));
    }
  }

  /**
   * A RetentionPolicy attribute whose entity has GOVERNANCE mode returns GOVERNANCE.
   */
  @Test
  public void testRetentionPolicyWithGovernanceEntityReturnsGovernance() {
    for (String siteId : Arrays.asList(null, ID.uuid())) {
      String entityValue = addRetentionEntity(siteId, RetentionMode.GOVERNANCE.name());
      DocumentAttributeRecord docAttr = retentionPolicyAttribute(entityValue);

      assertEquals(RetentionMode.GOVERNANCE,
          new DocumentAttributeHasRetentionMode(this.db).apply(siteId, docAttr));
    }
  }

  /**
   * A RetentionPolicy attribute with a missing entity is still a retention key and returns
   * RETENTION_ONLY.
   */
  @Test
  public void testRetentionPolicyWithMissingEntityReturnsRetentionOnly() {
    for (String siteId : Arrays.asList(null, ID.uuid())) {
      DocumentAttributeRecord docAttr = retentionPolicyAttribute(
          new DocumentAttributeEntityKeyValue(RETENTION_POLICY.getKey(), ID.uuid())
              .getStringValue());

      assertEquals(RetentionMode.RETENTION_ONLY,
          new DocumentAttributeHasRetentionMode(this.db).apply(siteId, docAttr));
    }
  }

  /**
   * A RetentionPolicy attribute whose entity has RETENTION_ONLY mode returns RETENTION_ONLY.
   */
  @Test
  public void testRetentionPolicyWithRetentionOnlyEntityReturnsRetentionOnly() {
    for (String siteId : Arrays.asList(null, ID.uuid())) {
      String entityValue = addRetentionEntity(siteId, RetentionMode.RETENTION_ONLY.name());
      DocumentAttributeRecord docAttr = retentionPolicyAttribute(entityValue);

      assertEquals(RetentionMode.RETENTION_ONLY,
          new DocumentAttributeHasRetentionMode(this.db).apply(siteId, docAttr));
    }
  }
}
