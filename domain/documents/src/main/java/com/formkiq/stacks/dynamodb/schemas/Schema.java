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
package com.formkiq.stacks.dynamodb.schemas;

import com.formkiq.graalvm.annotations.Reflectable;

/**
 * Attributes Schema.
 */
@Reflectable
public class Schema {

  /** {@link SchemaAttributes}. */
  private SchemaAttributes attributes;
  /** Schema Name. */
  private String name;

  /**
   * constructor.
   */
  public Schema() {

  }

  /**
   * Set {@link SchemaAttributes}.
   * 
   * @param schemaAttributes {@link SchemaAttributes}
   * @return {@link Schema}
   */
  public Schema attributes(final SchemaAttributes schemaAttributes) {
    this.attributes = schemaAttributes;
    return this;
  }

  /**
   * Get {@link SchemaAttributes}.
   * 
   * @return {@link SchemaAttributes}
   */
  public SchemaAttributes getAttributes() {
    return this.attributes;
  }

  /**
   * Get Schema Name.
   * 
   * @return {@link String}
   */
  public String getName() {
    return this.name;
  }

  /**
   * Set Schema Name.
   * 
   * @param schemaName {@link String}
   * @return {@link Schema}
   */
  public Schema name(final String schemaName) {
    this.name = schemaName;
    return this;
  }
}
