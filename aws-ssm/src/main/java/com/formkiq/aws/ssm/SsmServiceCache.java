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
package com.formkiq.aws.ssm;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import software.amazon.awssdk.services.ssm.SsmClient;

/**
 * 
 * Cache wrapper for {@link SsmServiceImpl}.
 *
 */
public class SsmServiceCache implements SsmService {

  /** {@link SsmService}. */
  private SsmService ssm;
  /** Cache {@link Map}. */
  private Map<String, String> cache = new ConcurrentHashMap<>();
  /** Timestamps Cache {@link Map}. */
  private Map<String, Long> timestamps = new ConcurrentHashMap<>();
  /** Ttl Nanos. */
  private long ttlMillis;

  /**
   * constructor.
   * 
   * @param ttl Cache Time to Live
   * @param ttlUnit Cache Time to Live {@link TimeUnit}
   */
  public SsmServiceCache(final long ttl, final TimeUnit ttlUnit) {
    this.ssm = new SsmServiceImpl();
    this.ttlMillis = ttlUnit.toMillis(ttl);
  }

  /**
   * Add to Cache.
   * 
   * @param key {@link String}
   * @param value {@link String}
   */
  private void addToCache(final String key, final String value) {
    if (key != null && value != null) {
      this.cache.put(key, value);
      this.timestamps.put(key, Long.valueOf(System.currentTimeMillis()));
    }
  }

  @Override
  public String getParameterValue(final SsmClient client, final String key) {

    String value = null;

    if (!isCached(key) || isExpired(key)) {
      value = this.ssm.getParameterValue(client, key);
      addToCache(key, value);
    } else {
      value = this.cache.get(key);
    }

    return value;
  }

  /**
   * Is Cache Expired.
   * 
   * @param key {@link String}
   * @return boolean
   */
  public boolean isCached(final String key) {
    return this.cache.containsKey(key);
  }

  /**
   * Is Cache Expired.
   * 
   * @param key {@link String}
   * @return boolean
   */
  public boolean isExpired(final String key) {
    long now = System.currentTimeMillis();
    long cachedTtl = this.timestamps.containsKey(key) ? this.timestamps.get(key).longValue() : 0;
    return (now - cachedTtl) > this.ttlMillis;
  }

  @Override
  public void putParameter(final SsmClient client, final String key, final String value) {
    this.ssm.putParameter(client, key, value);
    addToCache(key, value);
  }

  @Override
  public void removeParameter(final SsmClient client, final String key) {
    this.ssm.removeParameter(client, key);
    this.cache.remove(key);
    this.timestamps.remove(key);
  }
}
