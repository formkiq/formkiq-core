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
package com.formkiq.stacks.api.handler.attributes;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import com.formkiq.aws.dynamodb.DynamodbRecordToMap;
import com.formkiq.stacks.dynamodb.attributes.AttributeDataType;
import com.formkiq.stacks.dynamodb.attributes.AttributeRecord;
import com.formkiq.stacks.dynamodb.attributes.Watermark;
import com.formkiq.stacks.dynamodb.attributes.WatermarkPosition;

/**
 * {@link Function} transform {@link AttributeRecord} to {@link Map}.
 */
public class AttributeRecordToMap implements Function<AttributeRecord, Map<String, Object>> {

  /** {@link DynamodbRecordToMap}. */
  private final DynamodbRecordToMap toMap = new DynamodbRecordToMap(List.of("documentId"));

  @Override
  public Map<String, Object> apply(final AttributeRecord a) {

    Map<String, Object> attr = new HashMap<>(toMap.apply(a));

    Set<Map.Entry<String, Object>> keys = attr.entrySet();
    keys.removeIf(e -> e.getKey().startsWith("watermark"));

    Watermark watermark = new Watermark();
    watermark.setScale(a.getWatermarkScale());
    watermark.setRotation(a.getWatermarkRotation());
    watermark.setText(a.getWatermarkText());
    watermark.setImageDocumentId(a.getWatermarkImageDocumentId());

    boolean addWatermark = hasWatermark(a);

    if (addWatermark) {
      WatermarkPosition pos = new WatermarkPosition();
      pos.setxAnchor(a.getWatermarkxAnchor());
      pos.setyAnchor(a.getWatermarkyAnchor());
      pos.setxOffset(a.getWatermarkxOffset());
      pos.setyOffset(a.getWatermarkyOffset());

      watermark.setPosition(pos);
    }

    if (addWatermark) {
      attr.put("watermark", watermark);
    }

    return attr;
  }

  private boolean hasWatermark(final AttributeRecord attribute) {
    return AttributeDataType.WATERMARK.equals(attribute.getDataType());
  }
}
