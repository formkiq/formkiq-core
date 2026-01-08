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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 *
 * Objects Helper.
 *
 */
public class Objects {

  /** Double Format. */
  public static final String DOUBLE_FORMAT = "%020.4f";

  /** Double Format. */
  public static final String INT_ZERO_PAD_FORMAT = "%06d";

  /**
   * Merge two {@link Collection} together.
   *
   * @param <T> Type of {@link Collection}
   * @param t0 {@link Collection}
   * @param t1 {@link Collection}
   * @return {@link Collection}
   */
  public static <T> Collection<T> concat(final Collection<T> t0, final Collection<T> t1) {
    return Stream.concat(notNull(t0).stream(), notNull(t1).stream()).toList();
  }

  /**
   * Merge two {@link List} together.
   *
   * @param <T> Type of {@link List}
   * @param t0 {@link List}
   * @param t1 {@link List}
   * @return {@link List}
   */
  public static <T> List<T> concat(final List<T> t0, final List<T> t1) {
    return Stream.concat(notNull(t0).stream(), notNull(t1).stream()).toList();
  }

  /**
   * Format {@link Double}.
   * 
   * @param val {@link Double}
   * @return {@link String}
   */
  public static String formatDouble(final Double val) {

    String s;

    double dv = val.doubleValue();
    if (dv == (long) dv) {
      s = String.format("%d", Long.valueOf((long) dv));
    } else {
      s = String.format("%s", val);
    }

    return s;
  }

  /**
   * Format {@link Double} using a {@link String} format.
   * 
   * @param val {@link Double}
   * @param format {@link String}
   * @return {@link String}
   */
  public static String formatDouble(final Double val, final String format) {
    return String.format(format, val);
  }

  /**
   * Format {@link Double} using a {@link String} format.
   * 
   * @param val int
   * @param format {@link String}
   * @return {@link String}
   */
  public static String formatInt(final int val, final String format) {
    return String.format(format, Integer.valueOf(val));
  }

  /**
   * Returns the {@link Object} or default value.
   * 
   * @param o {@link java.util.Objects}
   * @param defaultValue {@link Object}
   * @param <T> Type of Object
   * @return {@link Object}
   */
  public static <T> T getNotNullOrDefault(final T o, final T defaultValue) {
    return o != null ? o : defaultValue;
  }

  /**
   * Is {@link Collection} empty.
   * 
   * @param c {@link Collection}
   * @return boolean
   */
  public static boolean isEmpty(final Collection<?> c) {
    return notNull(c).isEmpty();
  }

  /**
   * Get Last Element of {@link List}.
   *
   * @param <T> Type of Object
   * @param list {@link List}
   * @return {@link Object}
   */
  public static <T> T last(final List<T> list) {
    return !notNull(list).isEmpty() ? list.get(list.size() - 1) : null;
  }

  /**
   * Get Last Element of {@link List}.
   *
   * @param <T> Type of Object
   * @param list {@link List}
   * @return {@link Object}
   */
  public static <T> T last(final String[] list) {
    return list == null || list.length == 0 ? null : (T) list[list.length - 1];
  }

  /**
   * Merge two maps into a new {@link Map} where values from the {@code first} map take precedence
   * when the same key exists in both maps.
   * <p>
   * Merge behavior:
   * <ul>
   * <li>If a key exists only in {@code second}, its value is included.</li>
   * <li>If a key exists in both maps, the value from {@code first} is used.</li>
   * <li>If either map is {@code null}, it is treated as empty.</li>
   * </ul>
   *
   * <p>
   * The returned map is a mutable {@link HashMap} and modifications to it will not affect the input
   * maps.
   *
   * @param first the primary map whose values win on key collisions (may be {@code null})
   * @param second the secondary map whose values are used only when a key is absent from
   *        {@code first} (may be {@code null})
   * @param <K> the key type
   * @param <V> the value type
   * @return a new {@link Map} containing the merged entries
   */
  public static <K, V> Map<K, V> merge(final Map<? extends K, ? extends V> first,
      final Map<? extends K, ? extends V> second) {

    Map<K, V> result = new HashMap<>();

    if (second != null) {
      result.putAll(second);
    }
    if (first != null) {
      result.putAll(first);
    }

    return result;
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
