/**
 *
 * FormKiQ License
 *
 * Copyright (c) 2018 FormKiQ, INC
 * 
 * This code is the property of FormKiQ, INC. In the Software Development Agreement signed by both
 * FormKiQ and your company, FormKiQ grants you a limited license to use, modify, and create
 * derivative works of this code. Please consult the Software Development Agreement for the complete
 * terms under which you may use this code.
 *
 */
package com.formkiq.stacks.common.objects;

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
    // this.object = new HashMap<>(map);
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
