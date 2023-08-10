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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Random;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Generates random files.
 */
public class FileGenerator {

  /** {@link Random}. */
  private Random random = new Random();

  /** Buffer Size. */
  private static final int BUFFER_SIZE = 1024;

  /**
   * Generates Zip file of a certain size.
   * 
   * @param targetSize long
   * @return {@link File}
   * @throws IOException IOException
   */
  public File generateZipFile(final long targetSize) throws IOException {

    File file = File.createTempFile("tmp", ".bin");
    file.deleteOnExit();

    FileOutputStream fos = new FileOutputStream(file);
    ZipOutputStream zipOut = new ZipOutputStream(fos);

    // Create a large entry with random data
    ZipEntry entry = new ZipEntry("large_entry.txt");
    zipOut.putNextEntry(entry);

    // Generate random data
    byte[] buffer = new byte[BUFFER_SIZE];
    long writtenBytes = 0;
    while (writtenBytes < targetSize) {
      this.random.nextBytes(buffer);
      zipOut.write(buffer);
      writtenBytes += buffer.length;
    }

    zipOut.closeEntry();
    zipOut.close();
    fos.close();

    return file;
  }
}
