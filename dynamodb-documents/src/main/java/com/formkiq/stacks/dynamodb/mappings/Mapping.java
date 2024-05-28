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
package com.formkiq.stacks.dynamodb.mappings;

import com.formkiq.graalvm.annotations.Reflectable;

import java.util.List;

/**
 * Mapping.
 */
@Reflectable
public class Mapping {
  /** Mapping Name. */
  private String name;
  /** Mapping Description. */
  private String description;
  /** {@link List} {@link MappingAttribute}. */
  private List<MappingAttribute> attributes;

  /**
   * constructor.
   */
  public Mapping() {}

  /**
   * Get Mapping Name.
   * 
   * @return {@link String}
   */
  public String getName() {
    return this.name;
  }

  /**
   * Set Name.
   * 
   * @param mappingName {@link String}
   * @return {@link Mapping}
   */
  public Mapping setName(final String mappingName) {
    this.name = mappingName;
    return this;
  }

  /**
   * Get Mapping Description.
   * 
   * @return {@link String}
   */
  public String getDescription() {
    return this.description;
  }

  /**
   * Set Description.
   * 
   * @param mappingDescription {@link String}
   * @return {@link Mapping}
   */
  public Mapping setDescription(final String mappingDescription) {
    this.description = mappingDescription;
    return this;
  }

  /**
   * Get {@link List} {@link MappingAttribute}.
   * 
   * @return {@link List} {@link MappingAttribute}
   */
  public List<MappingAttribute> getAttributes() {
    return this.attributes;
  }

  /**
   * Set Mapping Attributes.
   * 
   * @param mappingAttributes {@link List} {@link MappingAttribute}
   * @return {@link Mapping}
   */
  public Mapping setAttributes(final List<MappingAttribute> mappingAttributes) {
    this.attributes = mappingAttributes;
    return this;
  }
}
