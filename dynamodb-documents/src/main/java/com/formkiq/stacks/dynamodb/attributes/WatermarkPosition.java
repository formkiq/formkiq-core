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
 * Watermark Position.
 */
@Reflectable
public class WatermarkPosition {
  /** X Offset. */
  private Double xOffset;
  /** Y Offset. */
  private Double yOffset;
  /** X Anchor. */
  private WatermarkXanchor xAnchor;
  /** Y Anchor. */
  private WatermarkYanchor yAnchor;

  /**
   * constructor.
   */
  public WatermarkPosition() {

  }

  /**
   * Get X Offset.
   * 
   * @return Double
   */
  public Double getxOffset() {
    return this.xOffset;
  }

  /**
   * Set x offset.
   * 
   * @param offset Double
   */
  public void setxOffset(final Double offset) {
    this.xOffset = offset;
  }

  /**
   * Get Y offset.
   * 
   * @return Double
   */
  public Double getyOffset() {
    return this.yOffset;
  }

  /**
   * Set Y Offset.
   * 
   * @param offset Double
   */
  public void setyOffset(final Double offset) {
    this.yOffset = offset;
  }

  /**
   * Get X Anchor.
   * 
   * @return WatermarkAnchor
   */
  public WatermarkXanchor getxAnchor() {
    return this.xAnchor;
  }

  /**
   * Set X Anchor.
   * 
   * @param anchor {@link WatermarkXanchor}
   */
  public void setxAnchor(final WatermarkXanchor anchor) {
    this.xAnchor = anchor;
  }

  /**
   * Get Y Anchor.
   * 
   * @return WatermarkAnchor
   */
  public WatermarkYanchor getyAnchor() {
    return this.yAnchor;
  }

  /**
   * Set Y Anchor.
   * 
   * @param anchor {@link WatermarkYanchor}
   */
  public void setyAnchor(final WatermarkYanchor anchor) {
    this.yAnchor = anchor;
  }
}
