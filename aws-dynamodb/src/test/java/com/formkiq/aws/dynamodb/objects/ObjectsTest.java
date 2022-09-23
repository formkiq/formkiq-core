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
package com.formkiq.aws.dynamodb.objects;

import static org.junit.Assert.assertEquals;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.Test;

/**
 * 
 * Unit Tests for {@link Objects}.
 *
 */
public class ObjectsTest {

  /**
   * Unit Test Paritioning.
   */
  @Test
  public void testParition01() {
    // given
    final int min = 1;
    final int max = 15;
    final int partitionSize = 4;
    final List<Integer> collection =
        IntStream.rangeClosed(min, max).boxed().collect(Collectors.toList());

    // when
    List<List<Integer>> result = Objects.parition(collection, partitionSize);

    // then
    int i = 0;
    final int expected = 4;
    assertEquals(expected, result.size());
    assertEquals("[1, 2, 3, 4]", result.get(i++).toString());
    assertEquals("[5, 6, 7, 8]", result.get(i++).toString());
    assertEquals("[9, 10, 11, 12]", result.get(i++).toString());
    assertEquals("[13, 14, 15]", result.get(i++).toString());
  }

  /**
   * Unit Test Paritioning.
   */
  @Test
  public void testParition02() {
    // given
    final int min = 17;
    final int max = 90;
    final int partitionSize = 7;
    final List<Integer> collection =
        IntStream.rangeClosed(min, max).boxed().collect(Collectors.toList());

    // when
    List<List<Integer>> result = Objects.parition(collection, partitionSize);

    // then
    int i = 0;
    final int expected = 11;
    assertEquals(expected, result.size());
    assertEquals("[17, 18, 19, 20, 21, 22, 23]", result.get(i++).toString());
    assertEquals("[24, 25, 26, 27, 28, 29, 30]", result.get(i++).toString());
    assertEquals("[31, 32, 33, 34, 35, 36, 37]", result.get(i++).toString());
    assertEquals("[38, 39, 40, 41, 42, 43, 44]", result.get(i++).toString());
    assertEquals("[45, 46, 47, 48, 49, 50, 51]", result.get(i++).toString());
    assertEquals("[52, 53, 54, 55, 56, 57, 58]", result.get(i++).toString());
    assertEquals("[59, 60, 61, 62, 63, 64, 65]", result.get(i++).toString());
    assertEquals("[66, 67, 68, 69, 70, 71, 72]", result.get(i++).toString());
    assertEquals("[73, 74, 75, 76, 77, 78, 79]", result.get(i++).toString());
    assertEquals("[80, 81, 82, 83, 84, 85, 86]", result.get(i++).toString());
    assertEquals("[87, 88, 89, 90]", result.get(i++).toString());
  }

}
