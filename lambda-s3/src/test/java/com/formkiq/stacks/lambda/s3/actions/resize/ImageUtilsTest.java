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
package com.formkiq.stacks.lambda.s3.actions.resize;

import org.junit.jupiter.api.Test;
import software.amazon.awssdk.utils.IoUtils;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ImageUtilsTest {
  @Test
  public void testGetImageFormat() throws IOException {
    getImageFormatTemplate("input.bmp", "bmp");
    getImageFormatTemplate("input.gif", "gif");
    getImageFormatTemplate("input.jpg", "jpeg");
    getImageFormatTemplate("input.png", "png");
    getImageFormatTemplate("input.tiff", "tif");
  }

  private static void getImageFormatTemplate(final String inputFileName,
      final String expectedImageFormat) throws IOException {
    try (InputStream is = new FileInputStream("src/test/resources/resize/" + inputFileName)) {
      byte[] imageBytes = IoUtils.toByteArray(is);
      assertEquals(expectedImageFormat, ImageUtils.getImageFormat(imageBytes));
    }
  }
}
