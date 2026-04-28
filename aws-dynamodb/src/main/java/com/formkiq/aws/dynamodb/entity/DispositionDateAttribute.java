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
package com.formkiq.aws.dynamodb.entity;

import com.formkiq.aws.dynamodb.attributes.AttributeKeyReserved;
import com.formkiq.aws.dynamodb.builder.DynamoDbTypes;
import com.formkiq.aws.dynamodb.documents.DocumentRecord;
import com.formkiq.aws.dynamodb.objects.DateUtil;

import java.time.temporal.ChronoUnit;
import java.util.Date;

import static com.formkiq.aws.dynamodb.attributes.AttributeKeyReserved.DISPOSITION_DATE;

/**
 * DispositionDate derived attribute.
 */
public class DispositionDateAttribute extends RetentionEffectiveEndDateAttribute {

  @Override
  public String calculate(final EntityRecord entityRecord, final DocumentRecord document) {

    var periodInDays = DynamoDbTypes.toLong(
        entityRecord.getAttributes().get(AttributeKeyReserved.DISPOSITION_DATE_IN_DAYS.getKey()),
        0L);

    var date = getDispositionField(entityRecord, document);

    var dispositionDate = Date.from(date.toInstant().plus(periodInDays, ChronoUnit.DAYS));
    return DateUtil.getYyyyMmDdFormatter().format(dispositionDate);
  }

  @Override
  public String getAttributeKey() {
    return DISPOSITION_DATE.getKey();
  }

  Date getDispositionField(final EntityRecord entityRecord, final DocumentRecord document) {
    var sourceType = getSourceType(entityRecord);

    return RetentionStartDateSourceType.DATE_LAST_MODIFIED.name().equals(sourceType)
        ? document.lastModifiedDate()
        : document.insertedDate();
  }

  String getSourceType(final EntityRecord entityRecord) {
    return DynamoDbTypes.toString(entityRecord.getAttributes()
        .get(AttributeKeyReserved.RETENTION_START_DATE_SOURCE_TYPE.getKey()));
  }
}
