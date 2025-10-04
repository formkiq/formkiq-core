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

import com.formkiq.graalvm.annotations.Reflectable;

/**
 * Watermark.
 */
@Reflectable
public class Watermark {
  /** Watermark text. */
  private String text;
  /** Watermark Rotation. */
  private Double rotation;
  /** Image Document Id. */
  private String imageDocumentId;
  /** Watermark Position. */
  private WatermarkPosition position;
  /** {@link WatermarkScale}. */
  private WatermarkScale scale;

  /**
   * constructor.
   */
  public Watermark() {

  }

  /**
   * Get Image Document Id.
   * 
   * @return String
   */
  public String getImageDocumentId() {
    return this.imageDocumentId;
  }

  /**
   * Get {@link WatermarkPosition}.
   * 
   * @return {@link WatermarkPosition}
   */
  public WatermarkPosition getPosition() {
    return this.position;
  }

  /**
   * Get {@link Double} rotation.
   * 
   * @return Double
   */
  public Double getRotation() {
    return this.rotation;
  }

  /**
   * Get {@link WatermarkScale}.
   * 
   * @return {@link WatermarkScale}
   */
  public WatermarkScale getScale() {
    return this.scale;
  }

  /**
   * Get Watermark text.
   * 
   * @return String
   */
  public String getText() {
    return this.text;
  }

  /**
   * Set Image Document Id.
   * 
   * @param documentId {@link String}
   * @return Watermark
   */
  public Watermark setImageDocumentId(final String documentId) {
    this.imageDocumentId = documentId;
    return this;
  }

  /**
   * Set {@link WatermarkPosition}.
   * 
   * @param watermarkPosition {@link WatermarkPosition}
   * @return Watermark
   */
  public Watermark setPosition(final WatermarkPosition watermarkPosition) {
    this.position = watermarkPosition;
    return this;
  }

  /**
   * Set Rotation.
   * 
   * @param watermarkRotation {@link Double}
   * @return Watermark
   */
  public Watermark setRotation(final Double watermarkRotation) {
    this.rotation = watermarkRotation;
    return this;
  }

  /**
   * Set Scale.
   * 
   * @param watermarkScale {@link WatermarkScale}
   * @return Watermark
   */
  public Watermark setScale(final WatermarkScale watermarkScale) {
    this.scale = watermarkScale;
    return this;
  }

  /**
   * Set Watermark text.
   * 
   * @param watermarkText {@link String}
   * @return Watermark
   */
  public Watermark setText(final String watermarkText) {
    this.text = watermarkText;
    return this;
  }
}
