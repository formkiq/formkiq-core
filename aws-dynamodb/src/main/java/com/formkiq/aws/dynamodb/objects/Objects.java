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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 
 * Objects Helper.
 *
 */
public class Objects {

  /**
   * Get Last Element of {@link List}.
   * 
   * @param <T> Type of Object.
   * @param list {@link List}
   * @return {@link Object}
   */
  public static <T> T last(final List<T> list) {
    return !notNull(list).isEmpty() ? list.get(list.size() - 1) : null;
  }

  /**
   * Returns a {@link Collection} that is guarantee not to be null.
   * 
   * @param <T> Type
   * @param list {@link List}
   * @return {@link List}
   */
  public static <T> Collection<T> notNull(final Collection<T> list) {
    return list != null ? list : Collections.emptyList();
  }

  /**
   * Returns a {@link List} that is guarantee not to be null.
   * 
   * @param <T> Type
   * @param list {@link List}
   * @return {@link List}
   */
  public static <T> List<T> notNull(final List<T> list) {
    return list != null ? list : Collections.emptyList();
  }

  /**
   * Returns a {@link List} that is guarantee not to be null.
   * 
   * @param <T> Type
   * @param <S> Type
   * @param map {@link List}
   * @return {@link List}
   */
  public static <T, S> Map<T, S> notNull(final Map<T, S> map) {
    return map != null ? map : Collections.emptyMap();
  }

  /**
   * Parition a {@link List} into a certain max size.
   * 
   * @param <T> Type of {@link List}
   * @param list {@link List}
   * @param partitionSize max size
   * @return {@link List}
   */
  public static <T> List<List<T>> parition(final List<T> list, final int partitionSize) {

    List<List<T>> partitions = new ArrayList<>();

    for (int i = 0; i < list.size(); i += partitionSize) {
      partitions.add(list.subList(i, Math.min(i + partitionSize, list.size())));
    }

    return partitions;
  }

  /**
   * If {@link Object} is null throw {@link Exception}.
   * 
   * @param obj {@link Object}
   * @param ex {@link Exception}
   * @throws Exception Exception
   */
  public static void throwIfNull(final Object obj, final Exception ex) throws Exception {
    if (obj == null) {
      throw ex;
    }
  }
}
