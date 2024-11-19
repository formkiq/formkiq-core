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
package com.formkiq.aws.s3;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

/**
 * Hash Calculator.
 */
public class ChecksumCalculator {

  /** Hex conversion. */
  public static final int HEX = 0xff;

  /**
   * Encode bytes to Base64 {@link String}.
   * 
   * @param bytes byte[]
   * @return String
   */
  public String encodeToBas64String(final byte[] bytes) {
    return Base64.getEncoder().encodeToString(bytes);
  }

  /**
   * Calculates the SHA-256 hash of a byte array.
   *
   * @param data the input byte array
   * @return the SHA-256 hash as a byte[]
   * @throws NoSuchAlgorithmException if SHA-256 algorithm is not available
   */
  public byte[] calculateSha256(final byte[] data) throws NoSuchAlgorithmException {
    MessageDigest digest = MessageDigest.getInstance("SHA-256");
    return digest.digest(data);
  }

  /**
   * Calculates the SHA-1 hash of a {@link String}.
   *
   * @param data the input byte array
   * @return the SHA-1 hash as a byte[]
   * @throws NoSuchAlgorithmException if SHA-1 algorithm is not available
   */
  public byte[] calculateSha1(final byte[] data) throws NoSuchAlgorithmException {
    MessageDigest digest = MessageDigest.getInstance("SHA-1");
    return digest.digest(data);
  }

  /**
   * Converts a byte array to a hexadecimal string.
   *
   * @param bytes the input byte array
   * @return a string representing the hexadecimal format of the byte array
   */
  public String bytesToHex(final byte[] bytes) {
    StringBuilder hexString = new StringBuilder(2 * bytes.length);
    for (byte b : bytes) {
      String hex = Integer.toHexString(HEX & b);
      if (hex.length() == 1) {
        hexString.append('0');
      }
      hexString.append(hex);
    }
    return hexString.toString();
  }

  /**
   * Convert Hex String to byte[].
   * 
   * @param hex {@link String}
   * @return byte[]
   */
  public byte[] hexStringToByteArray(final String hex) {
    final int radix = 16;
    final int shift = 4;
    int length = hex.length();
    byte[] bytes = new byte[length / 2];

    for (int i = 0; i < length; i += 2) {
      int firstDigit = Character.digit(hex.charAt(i), radix);
      int secondDigit = Character.digit(hex.charAt(i + 1), radix);
      if (firstDigit == -1 || secondDigit == -1) {
        throw new IllegalArgumentException("Invalid hex string");
      }
      bytes[i / 2] = (byte) ((firstDigit << shift) + secondDigit);
    }

    return bytes;
  }
}
