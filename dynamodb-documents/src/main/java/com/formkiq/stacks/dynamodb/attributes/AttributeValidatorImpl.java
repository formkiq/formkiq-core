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
package com.formkiq.stacks.dynamodb.attributes;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import com.formkiq.aws.dynamodb.BatchGetConfig;
import com.formkiq.aws.dynamodb.DbKeys;
import com.formkiq.aws.dynamodb.DynamoDbService;
import com.formkiq.aws.dynamodb.objects.Strings;
import com.formkiq.validation.ValidationError;
import com.formkiq.validation.ValidationErrorImpl;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/**
 * {@link AttributeValidator} implementation.
 */
public class AttributeValidatorImpl implements AttributeValidator, DbKeys {

  /** {@link DynamoDbService}. */
  private DynamoDbService db;

  /**
   * constructor.
   * 
   * @param dbService {@link DynamoDbService}
   */
  public AttributeValidatorImpl(final DynamoDbService dbService) {
    this.db = dbService;
  }

  @Override
  public Collection<ValidationError> validate(final String siteId,
      final Collection<DocumentAttributeRecord> searchAttributes) {

    Collection<ValidationError> errors = new ArrayList<>();

    validateRequired(searchAttributes, errors);

    if (errors.isEmpty()) {
      validateAttributeExists(siteId, searchAttributes, errors);
    }

    return errors;
  }

  private void validateAttributeExists(final String siteId,
      final Collection<DocumentAttributeRecord> searchAttributes,
      final Collection<ValidationError> errors) {

    List<Map<String, AttributeValue>> keys = searchAttributes.stream().map(a -> {
      AttributeRecord r = new AttributeRecord().documentId(a.getKey());
      return Map.of(PK, r.fromS(r.pk(siteId)), SK, r.fromS(r.sk()));
    }).toList();

    BatchGetConfig config = new BatchGetConfig().projectionExpression("PK,SK,#key")
        .expressionAttributeNames(Map.of("#key", "key"));

    List<Map<String, AttributeValue>> batch = this.db.getBatch(config, keys);
    if (batch.size() != searchAttributes.size()) {

      Set<String> got = batch.stream().map(a -> a.get("key").s()).collect(Collectors.toSet());
      List<String> askedFor = searchAttributes.stream().map(a -> a.getKey()).toList();

      for (String asked : askedFor) {

        if (!got.contains(asked)) {
          String errorMsg = "attribute '" + asked + "' not found";
          errors.add(new ValidationErrorImpl().key(asked).error(errorMsg));
        }
      }
    }
  }

  private void validateRequired(final Collection<DocumentAttributeRecord> searchAttributes,
      final Collection<ValidationError> errors) {

    for (DocumentAttributeRecord a : searchAttributes) {
      if (Strings.isEmpty(a.getKey())) {
        errors.add(new ValidationErrorImpl().error("'key' is missing from attribute"));
      }
    }
  }
}
