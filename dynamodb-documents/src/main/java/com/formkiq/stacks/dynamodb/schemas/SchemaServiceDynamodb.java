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
package com.formkiq.stacks.dynamodb.schemas;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import com.formkiq.aws.dynamodb.DynamoDbService;
import com.formkiq.validation.ValidationError;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/**
 * DynamoDB implementation for {@link SchemaService}.
 */
public class SchemaServiceDynamodb implements SchemaService {

  /** {@link DynamoDbService}. */
  private DynamoDbService db;

  /**
   * constructor.
   * 
   * @param dbService {@link DynamoDbService}
   * 
   */
  public SchemaServiceDynamodb(final DynamoDbService dbService) {
    this.db = dbService;
  }

  @Override
  public SiteSchemasRecord getSitesSchema(final String siteId) {
    SiteSchemasRecord r = new SiteSchemasRecord();

    Map<String, AttributeValue> attr = this.db.get(r.fromS(r.pk(siteId)), r.fromS(r.sk()));
    if (!attr.isEmpty()) {
      r = r.getFromAttributes(siteId, attr);
    } else {
      r = null;
    }

    return r;
  }

  @Override
  public Collection<ValidationError> setSitesSchema(final String siteId, final String name,
      final String schemaJson, final Schema schema) {

    // TODO validation
    // Todo update all documents if changed

    // If you add a new attribute to an existing schema, it would require a default attribute,
    // maybe?
    // default value
    // If you want to remove an attribute on a schema that doesn't allow extra fields, I think it
    // may need to just not allow for now. Later, it could try and do some painful check

    SiteSchemasRecord r = new SiteSchemasRecord().name(name).schema(schemaJson);
    this.db.putItem(r.getAttributes(siteId));
    return Collections.emptyList();
  }

}
