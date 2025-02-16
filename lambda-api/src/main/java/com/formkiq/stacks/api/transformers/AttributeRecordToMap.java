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
package com.formkiq.stacks.api.transformers;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import com.formkiq.stacks.dynamodb.attributes.AttributeRecord;
import com.formkiq.stacks.dynamodb.attributes.Watermark;
import com.formkiq.stacks.dynamodb.attributes.WatermarkAnchor;
import com.formkiq.stacks.dynamodb.attributes.WatermarkPosition;

import static com.formkiq.aws.dynamodb.objects.Strings.isEmpty;

/**
 * {@link Function} transform {@link AttributeRecord} to {@link Map}.
 */
public class AttributeRecordToMap implements Function<AttributeRecord, Map<String, Object>> {

  @Override
  public Map<String, Object> apply(final AttributeRecord a) {
    Map<String, Object> attr = new HashMap<>(
        Map.of("key", a.getKey(), "type", a.getType().name(), "dataType", a.getDataType().name()));

    boolean addWatermark = false;
    Watermark watermark = new Watermark();
    if (!isEmpty(a.getWatermarkText())) {
      addWatermark = true;
      watermark.setText(a.getWatermarkText());
    }

    if (!isEmpty(a.getWatermarkImageDocumentId())) {
      addWatermark = true;
      watermark.setImageDocumentId(a.getWatermarkImageDocumentId());
    }

    if (addWatermark) {
      WatermarkPosition pos = new WatermarkPosition();
      if (!isEmpty(a.getWatermarkxAnchor())) {
        pos.setxAnchor(WatermarkAnchor.valueOf(a.getWatermarkxAnchor()));
      }

      if (!isEmpty(a.getWatermarkyAnchor())) {
        pos.setyAnchor(WatermarkAnchor.valueOf(a.getWatermarkyAnchor()));
      }
      pos.setxOffset(a.getWatermarkxOffset());
      pos.setyOffset(a.getWatermarkyOffset());

      watermark.setPosition(pos);
    }

    if (addWatermark) {
      attr.put("watermark", watermark);
    }

    return attr;
  }
}
