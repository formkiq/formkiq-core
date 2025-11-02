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

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Configuration holder for converting DynamoDB AttributeValue maps.
 */
public class AttributeValueToMapConfig {
  public static class Builder {
    /** Whether to remove Db Keys. */
    private boolean removeDbKeys = false;
    /** {@link Map} of Keys to rename. */
    private final Map<String, String> renameKeys = new HashMap<>();
    /** Delete Keys. */
    private final Set<String> deleteKeys = new HashSet<>();

    /**
     * Sets the output field name for the DynamoDB "documentId" attribute.
     * 
     * @param fromKey {@link String}
     * @param toKey {@link String}
     * @return Builder
     */
    public Builder addRenameKeys(final String fromKey, final String toKey) {
      this.renameKeys.put(fromKey, toKey);
      return this;
    }

    /**
     * Builds the configuration instance.
     * 
     * @return AttributeValueToMapConfig
     */
    public AttributeValueToMapConfig build() {
      return new AttributeValueToMapConfig(this);
    }

    /**
     * Remove Keys from Map.
     *
     * @param keys {@link String}
     * @return Builder
     */
    public Builder deleteKeys(final String... keys) {
      this.deleteKeys.addAll(List.of(keys));
      return this;
    }

    /**
     * {@link Builder}. If true, PK and SK keys will be omitted from the output map
     * 
     * @param toRemoveDbKeys boolean
     * @return Builder
     */
    public Builder removeDbKeys(final boolean toRemoveDbKeys) {
      this.removeDbKeys = toRemoveDbKeys;
      return this;
    }
  }

  /**
   * {@link Builder}.
   *
   * @return a new Builder instance
   */
  public static Builder builder() {
    return new Builder();
  }

  /** Whether to remove Db Keys. */
  private final boolean removeDbKeys;

  /** {@link Map} of Keys to rename. */
  private final Map<String, String> renameKeys;

  /** Delete Keys. */
  private final Set<String> deleteKeys;

  private AttributeValueToMapConfig(final Builder builder) {
    this.removeDbKeys = builder.removeDbKeys;
    this.renameKeys = builder.renameKeys;
    this.deleteKeys = builder.deleteKeys;
  }

  /**
   * Get Delete Keys.
   *
   * @return keys to delete
   */
  public Set<String> getDeleteKeys() {
    return this.deleteKeys;
  }

  /**
   * Get {@link Map}.
   * 
   * @return keys to rename
   */
  public Map<String, String> getRenameKeys() {
    return this.renameKeys;
  }

  /**
   * Is Remove Db Keys.
   * 
   * @return true if PK and SK attributes should be omitted
   */
  public boolean isRemoveDbKeys() {
    return removeDbKeys;
  }
}

