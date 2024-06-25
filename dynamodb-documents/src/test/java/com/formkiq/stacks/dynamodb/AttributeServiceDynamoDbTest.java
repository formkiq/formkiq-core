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

import static com.formkiq.testutils.aws.DynamoDbExtension.DOCUMENTS_TABLE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collection;
import java.util.UUID;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import com.formkiq.aws.dynamodb.DbKeys;
import com.formkiq.aws.dynamodb.DynamoDbConnectionBuilder;
import com.formkiq.aws.dynamodb.DynamoDbService;
import com.formkiq.aws.dynamodb.DynamoDbServiceImpl;
import com.formkiq.stacks.dynamodb.attributes.AttributeDataType;
import com.formkiq.stacks.dynamodb.attributes.AttributeRecord;
import com.formkiq.stacks.dynamodb.attributes.AttributeService;
import com.formkiq.stacks.dynamodb.attributes.AttributeServiceDynamodb;
import com.formkiq.stacks.dynamodb.attributes.AttributeType;
import com.formkiq.testutils.aws.DynamoDbExtension;
import com.formkiq.testutils.aws.DynamoDbTestServices;
import com.formkiq.validation.ValidationError;

/**
 * 
 * Unit Test for {@link AttributeServiceDynamodb}.
 *
 */
@ExtendWith(DynamoDbExtension.class)
class AttributeServiceDynamoDbTest implements DbKeys {

  /** {@link AttributeService}. */
  private static AttributeService service;

  @BeforeAll
  public static void beforeAll() throws URISyntaxException {
    DynamoDbConnectionBuilder dynamoDbConnection = DynamoDbTestServices.getDynamoDbConnection();
    DynamoDbService db = new DynamoDbServiceImpl(dynamoDbConnection, DOCUMENTS_TABLE);
    service = new AttributeServiceDynamodb(db);
  }

  /**
   * Delete attribute.
   */
  @Test
  void testDelete01() {
    // given
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {

      String key = "category";
      service.addAttribute(siteId, key, AttributeDataType.STRING, AttributeType.STANDARD);
      AttributeRecord record = service.getAttribute(siteId, key);
      assertNotNull(record);

      // when
      Collection<ValidationError> errors = service.deleteAttribute(siteId, key);

      // then
      assertEquals(0, errors.size());
      assertNull(service.getAttribute(siteId, key));
    }
  }

  /**
   * Delete invalid attribute.
   */
  @Test
  void testDelete02() {
    // given
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {

      String key = "category";

      // when
      Collection<ValidationError> errors = service.deleteAttribute(siteId, key);

      // then
      assertEquals(1, errors.size());
      assertEquals("attribute 'key' not found", errors.iterator().next().error());
    }
  }

  /**
   * Set Attribute Type.
   */
  @Test
  void testSetAttributeType01() {
    // given
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {

      String key = "category";
      service.addAttribute(siteId, key, AttributeDataType.STRING, AttributeType.STANDARD);
      AttributeRecord record = service.getAttribute(siteId, key);
      assertEquals(AttributeType.STANDARD, record.getType());

      // when
      service.setAttributeType(siteId, key, AttributeType.OPA);

      // then
      record = service.getAttribute(siteId, key);
      assertEquals(AttributeType.OPA, record.getType());
    }
  }
}
