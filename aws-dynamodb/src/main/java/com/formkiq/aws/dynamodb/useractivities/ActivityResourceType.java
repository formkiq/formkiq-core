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
package com.formkiq.aws.dynamodb.useractivities;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;

/**
 * Types of Activity Resources.
 */
public enum ActivityResourceType {
  /** Document Resource Type. */
  DOCUMENT("documents", "documentId", true),
  /** Document Attribute Resource Type. */
  DOCUMENT_ATTRIBUTE("documentAttributes", "documentId", false),
  /** Entity Resource Type. */
  ENTITY("entities", "entityId", true),
  /** Entity Type Resource Type. */
  ENTITY_TYPE("entityTypes", "entityTypeId", true),
  /** Workflow. */
  WORKFLOW("workflows", "workflowId", true),
  /** Ruleset Rule. */
  RULESET_RULE("rulesetRules", "ruleId", true),
  /** Ruleset. */
  RULESET("rulesets", "rulesetId", true),
  /** Attribute Key. */
  ATTRIBUTE_KEY("attributes", "attributeKey", true);

  public static ActivityResourceType find(final Map<String, String> map) {
    Optional<ActivityResourceType> o = Arrays.stream(values()).filter(a -> a.queryable)
        .filter(a -> map.containsKey(a.idParam)).findFirst();
    return o.orElse(null);
  }

  /** Resource Name. */
  private final String name;
  /** Document Id Param. */
  private final String idParam;

  /** Is Queryable. */
  private final boolean queryable;

  /**
   * constructor.
   * 
   * @param resourceName {@link String}
   * @param documentIdParam {@link String}
   * @param isQueryable whether field is queryable
   */
  ActivityResourceType(final String resourceName, final String documentIdParam,
      final boolean isQueryable) {
    this.name = resourceName;
    this.idParam = documentIdParam;
    this.queryable = isQueryable;
  }

  public String getDocumentIdParam() {
    return this.idParam;
  }

  public String getName() {
    return this.name;
  }

  public boolean isQueryable() {
    return this.queryable;
  }
}
