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
package com.formkiq.module.lambda.ocr.tesseract;

import java.awt.image.BufferedImage;
import java.io.File;
import net.sourceforge.tess4j.TesseractException;

/**
 * 
 * Return Test data for {@link TesseractWrapper}.
 *
 */
public class TesseractWrapperData implements TesseractWrapper {

  /** {@link String}. */
  private String data;

  /**
   * constructor.
   * 
   * @param s {@link String}
   */
  public TesseractWrapperData(final String s) {
    this.data = s;
  }

  @Override
  public String doOcr(final File imageFile) throws TesseractException {
    return this.data;
  }

  @Override
  public String doOcr(final BufferedImage image) throws TesseractException {
    return "BufferedImage: " + this.data;
  }
}
