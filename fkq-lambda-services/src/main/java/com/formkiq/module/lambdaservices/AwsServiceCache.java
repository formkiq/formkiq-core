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
package com.formkiq.module.lambdaservices;

import java.util.HashMap;
import java.util.Map;

/**
 * Get Aws Services from Cache.
 *
 */
public class AwsServiceCache {

  /** {@link AwsServiceExtension}. */
  private static final Map<Class<?>, AwsServiceExtension<?>> EXTENSIONS = new HashMap<>();

  /**
   * Registers an {@link AwsServiceExtension}.
   * 
   * @param clazz {@link Class}
   * @param <T> Type of Class
   * @param extension {@link AwsServiceExtension}
   */
  public static <T> void register(final Class<T> clazz, final AwsServiceExtension<T> extension) {
    EXTENSIONS.put(clazz, extension);
  }

  /** Is Debug Mode. */
  private boolean debug;
  /** Environment {@link Map}. */
  private Map<String, String> environment;
  /** FormKiQ Type. */
  private String formKiQType;

  /**
   * constructor.
   */
  public AwsServiceCache() {}

  /**
   * Is Debug.
   * 
   * @return boolean
   */
  public boolean debug() {
    return this.debug;
  }

  /**
   * Set Debug Mode.
   * 
   * @param isDebug boolean
   * @return {@link AwsServiceCache}s
   */
  public AwsServiceCache debug(final boolean isDebug) {
    this.debug = isDebug;
    return this;
  }

  /**
   * Set Environment {@link Map} parameters.
   * 
   * @param map {@link Map}
   * @return {@link AwsServiceCache}
   */
  public AwsServiceCache environment(final Map<String, String> map) {
    this.environment = map;
    this.formKiQType = map.containsKey("FormKiQType") ? map.get("FormKiQType") : "core";
    return this;
  }

  /**
   * Get Environment {@link Map} parameters.
   * 
   * @param key {@link String}
   * @return {@link String}
   */
  public String environment(final String key) {
    return this.environment.get(key);
  }

  /**
   * Get FormKiQ Type.
   * 
   * @return {@link String}
   */
  public String formKiQType() {
    return this.formKiQType;
  }

  /**
   * Load {@link AwsServiceExtension}.
   * 
   * @param <T> Type of Class.
   * @param clazz {@link Class}
   * @return Class instance
   */
  @SuppressWarnings("unchecked")
  public <T> T getExtension(final Class<T> clazz) {
    return (T) EXTENSIONS.get(clazz).loadService(this);
  }

  /**
   * Has Module.
   * 
   * @param module {@link String}
   * @return boolean
   */
  public boolean hasModule(final String module) {
    return "true".equals(environment("MODULE_" + module));
  }
}
