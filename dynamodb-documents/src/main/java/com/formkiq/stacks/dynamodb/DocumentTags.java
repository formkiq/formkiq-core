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
package com.formkiq.stacks.dynamodb;

import java.util.List;

/** {@link List} of {@link DocumentItemDynamoDb} tags. */
public class DocumentTags {

  /** {@link List} of {@link DocumentTag}. */
  private List<DocumentTag> tags;

  /** constructor. */
  public DocumentTags() {}

  /**
   * Get {@link DocumentTag}.
   *
   * @return {@link List} {@link DocumentTag}
   */
  public List<DocumentTag> getTags() {
    return this.tags;
  }

  /**
   * Set {@link DocumentTag}.
   *
   * @param list {@link List} {@link DocumentTag}
   */
  public void setTags(final List<DocumentTag> list) {
    this.tags = list;
  }
}
