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

import com.formkiq.aws.dynamodb.attributes.AttributeDataType;
import com.formkiq.aws.dynamodb.attributes.AttributeType;

/**
 * Builder for {@link AttributeRecord}.
 */
public class AttributeRecordBuilder {

  /** {@link AttributeDataType}. */
  private AttributeDataType dataType;
  /** Attribute Key. */
  private String key;
  /** {@link AttributeType}. */
  private AttributeType type;
  /** Attribute Value Regex Validation. */
  private String validationRegex;
  /** {@link Watermark}. */
  private Watermark watermark;

  /**
   * Builds {@link AttributeRecord}.
   *
   * @return {@link AttributeRecord}
   */
  public AttributeRecord build() {

    WatermarkPosition position = this.watermark != null ? this.watermark.position() : null;

    AttributeRecord record = new AttributeRecord().documentId(this.key).key(this.key)
        .type(this.type != null ? this.type : AttributeType.STANDARD)
        .setWatermarkText(this.watermark != null ? this.watermark.text() : null)
        .setWatermarkImageDocumentId(
            this.watermark != null ? this.watermark.imageDocumentId() : null)
        .setWatermarkRotation(this.watermark != null ? this.watermark.rotation() : null)
        .setWatermarkScale(this.watermark != null ? this.watermark.scale() : null)
        .setWatermarkFontSize(this.watermark != null ? this.watermark.fontSize() : null)
        .setValidationRegex(this.validationRegex)
        .dataType(this.dataType != null ? this.dataType : AttributeDataType.STRING);

    if (position != null) {
      updateWatermarkPosition(record, position);
    }

    return record;
  }

  /**
   * Set {@link AttributeDataType}.
   *
   * @param attributeDataType {@link AttributeDataType}
   * @return {@link AttributeRecordBuilder}
   */
  public AttributeRecordBuilder dataType(final AttributeDataType attributeDataType) {
    this.dataType = attributeDataType;
    return this;
  }

  /**
   * Set Attribute Key.
   *
   * @param attributeKey {@link String}
   * @return {@link AttributeRecordBuilder}
   */
  public AttributeRecordBuilder key(final String attributeKey) {
    this.key = attributeKey;
    return this;
  }

  /**
   * Set {@link AttributeType}.
   *
   * @param attributeType {@link AttributeType}
   * @return {@link AttributeRecordBuilder}
   */
  public AttributeRecordBuilder type(final AttributeType attributeType) {
    this.type = attributeType;
    return this;
  }

  private void updateWatermarkPosition(final AttributeRecord record,
      final WatermarkPosition position) {
    WatermarkXanchor xanchor =
        position != null && position.xAnchor() != null ? position.xAnchor() : null;
    WatermarkYanchor yanchor =
        position != null && position.yAnchor() != null ? position.yAnchor() : null;

    record.setWatermarkxOffset(position != null ? position.xOffset() : null)
        .setWatermarkyOffset(position != null ? position.yOffset() : null)
        .setWatermarkxAnchor(xanchor).setWatermarkyAnchor(yanchor);
  }

  /**
   * Set Attribute Value Regex Validation.
   *
   * @param regex {@link String}
   * @return {@link AttributeRecordBuilder}
   */
  public AttributeRecordBuilder validationRegex(final String regex) {
    this.validationRegex = regex;
    return this;
  }

  /**
   * Set {@link Watermark}.
   *
   * @param attributeWatermark {@link Watermark}
   * @return {@link AttributeRecordBuilder}
   */
  public AttributeRecordBuilder watermark(final Watermark attributeWatermark) {
    this.watermark = attributeWatermark;
    return this;
  }
}
