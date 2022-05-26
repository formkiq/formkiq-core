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
package com.formkiq.aws.dynamodb;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 
 * Dynamic Java {@link Object}.
 *
 */
public class DynamicObject extends HashMap<String, Object> {

  /** serialVersionUID. */
  private static final long serialVersionUID = 7039782531693529636L;

  /**
   * constructor.
   * 
   * @param map {@link Map}
   */
  public DynamicObject(final Map<String, Object> map) {
    super(map);
  }

  /**
   * Get {@link Boolean} value or Boolean.FALSE if doesn't exist.
   * 
   * @param key {@link String}
   * @return {@link Boolean}
   */
  public Boolean getBoolean(final String key) {
    Object obj = getOrDefault(key, null);
    return obj != null ? Boolean.valueOf(obj.toString()) : Boolean.FALSE;
  }

  /**
   * Get {@link Date} Value.
   * 
   * @param key {@link String}
   * @return {@link Date}
   */
  public Date getDate(final String key) {
    return (Date) getOrDefault(key, null);
  }

  /**
   * Get {@link List} {@link DynamicObject} Value.
   * 
   * @param key {@link String}
   * @return {@link Map}
   */
  @SuppressWarnings("unchecked")
  public List<DynamicObject> getList(final String key) {
    List<Map<String, Object>> list =
        (List<Map<String, Object>>) getOrDefault(key, new ArrayList<>());
    return list.stream().map(m -> new DynamicObject(m)).collect(Collectors.toList());
  }

  /**
   * Get {@link Long} Value.
   * 
   * @param key {@link String}
   * @return {@link Long}
   */
  public Long getLong(final String key) {
    return (Long) getOrDefault(key, null);
  }

  /**
   * Get {@link Map}.
   * 
   * @param key {@link String}
   * 
   * @return {@link DynamicObject}
   */
  @SuppressWarnings("unchecked")
  public DynamicObject getMap(final String key) {
    Map<String, Object> m = (Map<String, Object>) get(key);
    return new DynamicObject(m);
  }

  /**
   * Get {@link Map} Value.
   * 
   * @param key {@link String}
   * @return {@link Map}
   */
  @SuppressWarnings("unchecked")
  public DynamicObject getObject(final String key) {
    Map<String, Object> map = (Map<String, Object>) getOrDefault(key, new HashMap<>());
    return new DynamicObject(map);
  }

  /**
   * Get {@link String} Value.
   * 
   * @param key {@link String}
   * @return {@link String}
   */
  public String getString(final String key) {
    Object obj = getOrDefault(key, null);
    return obj != null ? obj.toString() : null;
  }

  /**
   * Get {@link List} {@link String} Value.
   * 
   * @param key {@link String}
   * @return {@link String}
   */
  @SuppressWarnings("unchecked")
  public List<String> getStringList(final String key) {
    Object obj = getOrDefault(key, null);
    return obj != null ? (List<String>) obj : null;
  }
  
  /**
   * Whether key has value.
   * 
   * @param key {@link String}
   * @return boolean
   */
  public boolean hasString(final String key) {
    String val = getString(key);
    return val != null && val.trim().length() > 0;
  }
}
