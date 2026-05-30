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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class FilenameUtilsTest {
  @Test
  public void testFileHasExtension() {
    assertTrue(FilenameUtils.fileHasExtension("test_dir/test.png", "png"));
    assertTrue(FilenameUtils.fileHasExtension("test_dir/test.png", "PNG"));
    assertTrue(FilenameUtils.fileHasExtension("test_dir/test.PNG", "png"));
    assertTrue(FilenameUtils.fileHasExtension("test_dir/test.PNG", "PNG"));

    assertTrue(FilenameUtils.fileHasExtension("test_dir/test.bmp.png", "png"));

    assertFalse(FilenameUtils.fileHasExtension("test_dir/test", "png"));
    assertFalse(FilenameUtils.fileHasExtension("test_dir/test.bmp", "png"));
  }

  @Test
  public void testGetFileName() {
    assertEquals("test_dir/test-100x200.jpg",
        FilenameUtils.getFileName("test_dir/test.png", 100, 200, "png", "jpg"));
    assertEquals("test_dir/test-100x200.jpg",
        FilenameUtils.getFileName("test_dir/test.PNG", 100, 200, "png", "jpg"));
    assertEquals("test_dir/test-100x200.jpg",
        FilenameUtils.getFileName("test_dir/test.png", 100, 200, "PNG", "jpg"));
    assertEquals("test_dir/test-100x200.jpg",
        FilenameUtils.getFileName("test_dir/test.PNG", 100, 200, "PNG", "jpg"));
  }

  @Test
  public void testImageFormatToExtension() {
    assertEquals("bmp", FilenameUtils.imageFormatToExtension("test_dir/test.bmp", "bmp"));
    assertEquals("gif", FilenameUtils.imageFormatToExtension("test_dir/test.gif", "gif"));
    assertEquals("jpg", FilenameUtils.imageFormatToExtension("test_dir/test.jpg", "jpeg"));
    assertEquals("jpeg", FilenameUtils.imageFormatToExtension("test_dir/test.jpeg", "jpeg"));
    assertEquals("png", FilenameUtils.imageFormatToExtension("test_dir/test.png", "png"));
    assertEquals("tif", FilenameUtils.imageFormatToExtension("test_dir/test.tif", "tif"));
    assertEquals("tiff", FilenameUtils.imageFormatToExtension("test_dir/test.tiff", "tif"));
  }
}
