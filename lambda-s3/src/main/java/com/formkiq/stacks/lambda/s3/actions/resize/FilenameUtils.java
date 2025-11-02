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

public class FilenameUtils {
  static boolean fileHasExtension(final String filePath, final String extension) {
    return filePath.toLowerCase().endsWith("." + extension.toLowerCase());
  }

  static String getFileName(final String inputFilePath, final int width, final int height,
      final String sourceFormat, final String destinationFormat) {
    String filePath = inputFilePath;

    if (fileHasExtension(filePath, sourceFormat)) {
      filePath = filePath.substring(0, filePath.length() - sourceFormat.length() - 1);
    }

    return filePath + "-" + width + "x" + height + "." + destinationFormat;
  }

  static String imageFormatToExtension(final String filePath, final String imageFormat) {
    if (fileHasExtension(filePath, "jpg")) {
      // imageFormat is always jpeg, but extension can be jpg or jpeg
      return "jpg";
    } else if (fileHasExtension(filePath, "tiff")) {
      // imageFormat is always tif, but extension can be tif or tiff
      return "tiff";
    }

    return imageFormat;
  }
}
