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
package com.formkiq.testutils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

/**
 * File Utils.
 */
public class FileUtil {

  /**
   * Load File from Classpath.
   *
   * @param path {@link String}
   * @return byte[]
   * @throws IOException IOException
   */
  public static byte[] loadFileAsBytes(final String path) throws IOException {
    return new FileUtil().loadFileByPath(path);
  }

  /**
   * constructor.
   */
  private FileUtil() {

  }

  /**
   * Load File from Classpath.
   *
   * @param path {@link String}
   * @return byte[]
   * @throws IOException IOException
   */
  private byte[] loadFileByPath(final String path) throws IOException {
    try (InputStream in = getClass().getResourceAsStream(path)) {
      return Objects.requireNonNull(in).readAllBytes();
    }
  }
}
