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

import net.coobird.thumbnailator.Thumbnails;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;

class ImageUtils {
  static BufferedImage bufferedImageFromByteArray(final byte[] imageData) throws IOException {
    try (ByteArrayInputStream bais = new ByteArrayInputStream(imageData)) {
      return ImageIO.read(bais);
    }
  }

  static byte[] bufferedImageToByteArray(final BufferedImage image, final String format)
      throws IOException {
    ByteArrayOutputStream imageBaos = new ByteArrayOutputStream();
    ImageIO.write(image, format, imageBaos);

    return imageBaos.toByteArray();
  }

  static BufferedImage resize(final BufferedImage image, final int width, final int height,
      final boolean isKeepAspectRatio) throws IOException {
    return Thumbnails.of(image).size(width, height).keepAspectRatio(isKeepAspectRatio)
        .asBufferedImage();
  }

  static String getImageFormat(final byte[] imageData) throws IOException {
    try (ByteArrayInputStream bais = new ByteArrayInputStream(imageData);
        ImageInputStream iis = ImageIO.createImageInputStream(bais)) {
      Iterator<ImageReader> imageReaders = ImageIO.getImageReaders(iis);

      if (imageReaders.hasNext()) {
        ImageReader reader = imageReaders.next();
        String format = reader.getFormatName();
        reader.dispose();

        return format.toLowerCase();
      }

      return null;
    }
  }
}
