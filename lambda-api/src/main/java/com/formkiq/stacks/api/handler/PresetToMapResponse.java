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

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import com.formkiq.stacks.dynamodb.Preset;

/**
 * 
 * {@link Function} convert {@link Preset} to {@link Map}.
 *
 */
public class PresetToMapResponse implements Function<Preset, Map<String, Object>> {

  /** Site Id. */
  private String site;

  /**
   * constructor.
   * 
   * @param siteId {@link String}
   */
  public PresetToMapResponse(final String siteId) {
    this.site = siteId;
  }

  @Override
  public Map<String, Object> apply(final Preset preset) {
    Map<String, Object> r = new HashMap<>();
    r.put("insertedDate", preset.getInsertedDate());
    r.put("id", preset.getId());
    r.put("name", preset.getName());
    r.put("siteId", this.site);
    r.put("type", preset.getType());
    r.put("userId", preset.getUserId());
    return r;
  }

}
