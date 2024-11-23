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
package com.formkiq.stacks.api.validators;

import com.formkiq.aws.dynamodb.DynamicObject;
import com.formkiq.aws.dynamodb.DynamoDbService;
import com.formkiq.module.actions.Action;
import com.formkiq.module.actions.services.ActionsValidator;
import com.formkiq.module.actions.services.ActionsValidatorImpl;
import com.formkiq.module.lambdaservices.AwsServiceCache;
import com.formkiq.stacks.dynamodb.ConfigService;
import com.formkiq.validation.ValidationError;
import com.formkiq.validation.ValidationException;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Api Validators.
 */
public interface ApiValidator {

  default void validateActions(AwsServiceCache awsservice, final String siteId,
      List<Action> actions) throws ValidationException {

    DynamoDbService db = awsservice.getExtension(DynamoDbService.class);
    ActionsValidator validator = new ActionsValidatorImpl(db);

    ConfigService configsService = awsservice.getExtension(ConfigService.class);
    DynamicObject configs = configsService.get(siteId);
    List<Collection<ValidationError>> errors = validator.validation(siteId, actions, configs);

    Optional<Collection<ValidationError>> firstError =
        errors.stream().filter(e -> !e.isEmpty()).findFirst();

    if (firstError.isPresent()) {
      throw new ValidationException(firstError.get());
    }
  }
}
