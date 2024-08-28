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
package com.formkiq.stacks.api.transformers;

import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AttributeType;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit Tests for {@link UserAttributesToMap}.
 */
public class UserAttributesToMapTest {

  /** {@link UserAttributesToMap}. */
  private final UserAttributesToMap func = new UserAttributesToMap();

  /**
   * Test simple address.
   */
  @Test
  void testApply01() {
    // given
    List<AttributeType> attributes =
        List.of(AttributeType.builder().name("address").value("123 main").build());

    // when
    Map<String, Object> apply = func.apply(attributes);

    // then
    assertEquals(1, apply.size());
    assertEquals("123 main", apply.get("address"));
  }

  /**
   * Test FamilyName.
   */
  @Test
  void testApply02() {
    // given
    List<AttributeType> attributes =
        List.of(AttributeType.builder().name("familyName").value("Smith").build());

    // when
    Map<String, Object> apply = func.apply(attributes);

    // then
    assertEquals(1, apply.size());
    assertEquals("Smith", apply.get("family_name"));
  }

  /**
   * Test FamilyName reverse.
   */
  @Test
  void testApply03() {
    // given
    List<AttributeType> attributes =
        List.of(AttributeType.builder().name("family_name").value("Smith").build());

    // when
    Map<String, Object> apply = func.apply(attributes);

    // then
    assertEquals(1, apply.size());
    assertEquals("Smith", apply.get("familyName"));
  }
}
