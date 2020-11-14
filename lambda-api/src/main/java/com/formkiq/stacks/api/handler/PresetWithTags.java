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
package com.formkiq.stacks.api.handler;

import java.util.List;
import com.formkiq.graalvm.annotations.Reflectable;
import com.formkiq.stacks.dynamodb.Preset;
import com.formkiq.stacks.dynamodb.PresetTag;

/**
 * {@link Preset} with Tags.
 *
 */
@Reflectable
public class PresetWithTags {

  /** Preset Name. */
  @Reflectable
  private String name;
  /** {@link List} {@link PresetTag}. */
  @Reflectable
  private List<PresetTag> tags;

  /**
   * constructor.
   */
  public PresetWithTags() {}

  /**
   * Get Name.
   * 
   * @return {@link String}
   */
  public String getName() {
    return this.name;
  }

  /**
   * Set Name.
   * 
   * @param s {@link String}
   */
  public void setName(final String s) {
    this.name = s;
  }

  /**
   * Get {@link PresetTag}.
   * 
   * @return {@link List} {@link PresetTag}
   */
  public List<PresetTag> getTags() {
    return this.tags;
  }

  /**
   * Set {@link PresetTag}.
   * 
   * @param list {@link PresetTag}
   */
  public void setTags(final List<PresetTag> list) {
    this.tags = list;
  }
}
