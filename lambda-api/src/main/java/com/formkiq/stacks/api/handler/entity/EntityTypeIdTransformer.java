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
package com.formkiq.stacks.api.handler.entity;

import com.formkiq.aws.dynamodb.DynamoDbService;
import com.formkiq.aws.dynamodb.entity.EntityTypeNamespace;
import com.formkiq.aws.services.lambda.ApiGatewayRequestEvent;
import com.formkiq.module.lambdaservices.AwsServiceCache;
import com.formkiq.aws.dynamodb.entity.FindEntityTypeByName;

import java.util.function.BiFunction;
import java.util.function.Function;


/**
 * {@link Function} to convert {@link ApiGatewayRequestEvent} to EntityTypeId {@link String}.
 */
public class EntityTypeIdTransformer implements BiFunction<String, ApiGatewayRequestEvent, String> {

  /** Document Table Name. */
  private final String tableName;
  /** {@link DynamoDbService}. */
  private final DynamoDbService db;

  /**
   * constructor.
   * 
   * @param awsservice {@link AwsServiceCache}
   */
  public EntityTypeIdTransformer(final AwsServiceCache awsservice) {
    this.tableName = awsservice.environment("DOCUMENTS_TABLE");
    this.db = awsservice.getExtension(DynamoDbService.class);
  }

  @Override
  public String apply(final String siteId, final ApiGatewayRequestEvent event) {
    EntityTypeNamespace namespace = new QueryParameterNamespace().apply(event);
    String entityTypeId = event.getPathParameter("entityTypeId");

    return new FindEntityTypeByName().find(db, tableName, siteId,
        new FindEntityTypeByName.EntityTypeName(namespace, entityTypeId));
  }
}
