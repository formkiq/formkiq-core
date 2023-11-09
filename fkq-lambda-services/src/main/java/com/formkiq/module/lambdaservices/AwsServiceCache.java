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
import software.amazon.awssdk.regions.Region;

/**
 * Get Aws Services from Cache.
 *
 */
public class AwsServiceCache {

  /** Is Debug Mode. */
  private boolean debug;
  /** Enable X Ray. */
  private boolean enableXray;
  /** Environment {@link Map}. */
  private Map<String, String> environment;
  /** {@link AwsServiceExtension}. */
  private final Map<Class<?>, AwsServiceExtension<?>> extensions = new HashMap<>();
  /** FormKiQ Type. */
  private String formKiQType;
  /** {@link Region}. */
  private Region region;

  /**
   * constructor.
   */
  public AwsServiceCache() {}

  /**
   * Contains {@link AwsServiceExtension}.
   * 
   * @param <T> Type of Class.
   * @param clazz {@link Class}
   * @return Class instance
   */
  public <T> boolean containsExtension(final Class<T> clazz) {
    return this.extensions.containsKey(clazz);
  }

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
   * De-register Extension.
   * 
   * @param <T> Type of Class
   * @param clazz {@link Class}
   */
  public <T> void deregister(final Class<T> clazz) {
    this.extensions.remove(clazz);
  }

  /**
   * Get Enable X Ray.
   * 
   * @return boolean
   */
  public boolean enableXray() {
    return this.enableXray;
  }

  /**
   * Set Enable X Ray.
   * 
   * @param enabled boolean
   * @return {@link AwsServiceCache}
   */
  public AwsServiceCache enableXray(final boolean enabled) {
    this.enableXray = enabled;
    return this;
  }

  /**
   * Get Environment parameters.
   * 
   * @return {@link Map}
   */
  public Map<String, String> environment() {
    return this.environment;
  }

  /**
   * Set Environment {@link Map} parameters.
   * 
   * @param map {@link Map}
   * @return {@link AwsServiceCache}
   */
  public AwsServiceCache environment(final Map<String, String> map) {
    this.environment = new HashMap<>(map);
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
    if (this.extensions.containsKey(clazz)) {
      return (T) this.extensions.get(clazz).loadService(this);
    }

    throw new RuntimeException("class " + clazz.getName() + " is not registered");
  }

  /**
   * Load {@link AwsServiceExtension}.
   * 
   * @param <T> Type of Class.
   * @param clazz {@link Class}
   * @return Class instance
   */
  @SuppressWarnings("unchecked")
  public <T> T getExtensionOrNull(final Class<T> clazz) {

    T result = null;

    if (this.extensions.containsKey(clazz)) {
      result = (T) this.extensions.get(clazz).loadService(this);
    }

    return result;
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

  /**
   * Get {@link Region}.
   * 
   * @return {@link Region}
   */
  public Region region() {
    return this.region;
  }

  /**
   * Set {@link Region}.
   * 
   * @param awsRegion {@link Region}
   * @return {@link AwsServiceCache}
   */
  public AwsServiceCache region(final Region awsRegion) {
    this.region = awsRegion;
    return this;
  }

  /**
   * Registers an {@link AwsServiceExtension}.
   * 
   * @param clazz {@link Class}
   * @param <T> Type of Class
   * @param extension {@link AwsServiceExtension}
   */
  public <T> void register(final Class<T> clazz, final AwsServiceExtension<T> extension) {
    this.extensions.put(clazz, extension);
  }
}
