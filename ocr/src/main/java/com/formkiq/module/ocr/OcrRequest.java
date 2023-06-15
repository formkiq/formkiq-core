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
package com.formkiq.module.ocr;

import java.util.Collections;
import java.util.List;
import com.formkiq.graalvm.annotations.Reflectable;

/**
 * 
 * OCR Request.
 *
 */
@Reflectable
public class OcrRequest {

  /** Add Pdf Detected Characeters as Text. */
  private boolean addPdfDetectedCharactersAsText = false;
  /** Parse Types. */
  private List<String> parseTypes;

  /**
   * constructor.
   */
  public OcrRequest() {

  }

  /**
   * Get Parse Types.
   * 
   * @return {@link List} {@link String}
   */
  public List<String> getParseTypes() {
    return this.parseTypes != null ? Collections.unmodifiableList(this.parseTypes) : null;
  }

  /**
   * Is AddPdfDetectedCharactersAsText.
   * 
   * @return boolean
   */
  public boolean isAddPdfDetectedCharactersAsText() {
    return this.addPdfDetectedCharactersAsText;
  }

  /**
   * Set AddPdfDetectedCharactersAsText.
   * 
   * @param bool boolean
   */
  public void setAddPdfDetectedCharactersAsText(final boolean bool) {
    this.addPdfDetectedCharactersAsText = bool;
  }

  /**
   * Set Parse Types.
   * 
   * @param types {@link List} {@link String}
   */
  public void setParseTypes(final List<String> types) {
    this.parseTypes = types != null ? Collections.unmodifiableList(types) : null;
  }
}
