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
package com.formkiq.aws.ssm;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

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
   * @param builder {@link SsmConnectionBuilder}.
   * @param ttl Cache Time to Live
   * @param ttlUnit Cache Time to Live {@link TimeUnit}
   */
  public SsmServiceCache(final SsmConnectionBuilder builder, final long ttl,
      final TimeUnit ttlUnit) {
    this.ssm = new SsmServiceImpl(builder);
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
  public String getParameterValue(final String key) {

    String value = null;

    if (!isCached(key) || isExpired(key)) {
      value = this.ssm.getParameterValue(key);
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
  public void putParameter(final String key, final String value) {
    this.ssm.putParameter(key, value);
    addToCache(key, value);
  }

  @Override
  public void removeParameter(final String key) {
    this.ssm.removeParameter(key);
    this.cache.remove(key);
    this.timestamps.remove(key);
  }
}
