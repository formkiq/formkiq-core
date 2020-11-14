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
package com.formkiq.stacks.api;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import java.util.Map;
import org.junit.Test;

/**
 * 
 * Unit Test for {@link ApiRequestMapper}.
 *
 */
public class ApiRequestMapperTest {

  /**
   * Is Enable Public Urls.
   */
  @Test
  public void testIsEnablePublicUrls01() {
    assertFalse(ApiRequestMapper.isEnablePublicUrls(Map.of()));

    assertFalse(ApiRequestMapper.isEnablePublicUrls(Map.of("ENABLE_PUBLIC_URLS", "false")));

    assertTrue(ApiRequestMapper.isEnablePublicUrls(Map.of("ENABLE_PUBLIC_URLS", "true")));
  }
}
