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
package com.formkiq.stacks.dynamodb;

import com.formkiq.aws.dynamodb.model.SearchAttributeCriteria;
import com.formkiq.aws.dynamodb.model.SearchTagCriteriaRange;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/** Unit Test for {@link SearchAttributesToCriteria}. */
public class SearchAttributesToCriteriaTest {

  private SearchAttributeCriteria apply(final List<SearchAttributeCriteria> attributes) {
    SearchAttributesToCriteria c = new SearchAttributesToCriteria(null);
    return c.apply(attributes);
  }

  /**
   * Single attribute EQ.
   */
  @Test
  void testAttribute01() {
    // given
    SearchAttributeCriteria attribute =
        new SearchAttributeCriteria("category", null, "person", null, null);

    // when
    SearchAttributeCriteria sac = apply(List.of(attribute));

    // then
    assertEquals("category", sac.key());
    assertEquals("person", sac.eq());
    assertNull(sac.range());
    assertNull(sac.beginsWith());
    assertNull(sac.eqOr());
  }

  /**
   * Single attribute BEGINS.
   */
  @Test
  void testAttribute02() {
    // given
    SearchAttributeCriteria attribute =
        new SearchAttributeCriteria("category", "o", null, null, null);

    // when
    SearchAttributeCriteria sac = apply(List.of(attribute));

    // then
    assertEquals("category", sac.key());
    assertEquals("", sac.eq());
    assertNull(sac.range());
    assertEquals("o", sac.beginsWith());
    assertNull(sac.eqOr());
  }

  /**
   * Single attribute String Range.
   */
  @Test
  void testAttribute03() {
    // given
    SearchAttributeCriteria attribute = new SearchAttributeCriteria("category", null, null, null,
        new SearchTagCriteriaRange("A", "B", "string"));

    // when
    SearchAttributeCriteria sac = apply(List.of(attribute));

    // then
    assertEquals("category", sac.key());
    assertNull(sac.eq());
    assertEquals("string", sac.range().type());
    assertEquals("A", sac.range().start());
    assertEquals("B", sac.range().end());
    assertNull(sac.beginsWith());
    assertNull(sac.eqOr());
  }

  /**
   * Single attribute Number Range.
   */
  @Test
  void testAttribute04() {
    // given
    SearchAttributeCriteria attribute = new SearchAttributeCriteria("category", null, null, null,
        new SearchTagCriteriaRange("20100101", "20100131", "number"));

    // when
    SearchAttributeCriteria sac = apply(List.of(attribute));

    // then
    assertEquals("category", sac.key());
    assertNull(sac.eq());
    assertEquals("number", sac.range().type());
    assertEquals("000000020100101.0000", sac.range().start());
    assertEquals("000000020100131.0000", sac.range().end());
    assertNull(sac.beginsWith());
    assertNull(sac.eqOr());
  }

  /**
   * Single attribute eqOr Range.
   */
  @Test
  void testAttribute05() {
    // given
    SearchAttributeCriteria attribute =
        new SearchAttributeCriteria("category", null, null, List.of("A", "B", "C"), null);

    // when
    SearchAttributeCriteria sac = apply(List.of(attribute));

    // then
    assertEquals("category", sac.key());
    assertNull(sac.eq());
    assertNull(sac.range());
    assertNull(sac.beginsWith());
    assertEquals("A,B,C", String.join(",", sac.eqOr()));
  }

  /**
   * multiple attribute EQ.
   */
  @Test
  void testAttribute06() {
    // given
    SearchAttributeCriteria attribute0 =
        new SearchAttributeCriteria("category", null, "person", null, null);
    SearchAttributeCriteria attribute1 =
        new SearchAttributeCriteria("userId", null, "111", null, null);

    // when
    SearchAttributeCriteria sac = apply(List.of(attribute0, attribute1));

    // then
    assertEquals("category::userId", sac.key());
    assertEquals("person::111", sac.eq());
    assertNull(sac.range());
    assertNull(sac.beginsWith());
    assertNull(sac.eqOr());
  }

  /**
   * Single attribute BEGINS.
   */
  @Test
  void testAttribute07() {
    // given
    SearchAttributeCriteria attribute0 =
        new SearchAttributeCriteria("category", null, "o", null, null);
    SearchAttributeCriteria attribute1 =
        new SearchAttributeCriteria("player", "111", null, null, null);

    // when
    SearchAttributeCriteria sac = apply(List.of(attribute0, attribute1));

    // then
    assertEquals("category::player", sac.key());
    assertNull(sac.eq());
    assertNull(sac.range());
    assertEquals("o::111", sac.beginsWith());
    assertNull(sac.eqOr());
  }

  /**
   * Single attribute String Range.
   */
  @Test
  void testAttribute08() {
    // given
    SearchAttributeCriteria attribute0 =
        new SearchAttributeCriteria("category", null, "person", null, null);
    SearchTagCriteriaRange range = new SearchTagCriteriaRange("A", "B", "string");
    SearchAttributeCriteria attribute1 =
        new SearchAttributeCriteria("player", null, null, null, range);

    // when
    SearchAttributeCriteria sac = apply(List.of(attribute0, attribute1));

    // then
    assertEquals("category::player", sac.key());
    assertNull(sac.eq());
    assertEquals("string", sac.range().type());
    assertEquals("person::A", sac.range().start());
    assertEquals("person::B", sac.range().end());
    assertNull(sac.beginsWith());
    assertNull(sac.eqOr());
  }

  /**
   * Single attribute Number Range.
   */
  @Test
  void testAttribute09() {
    // given
    SearchAttributeCriteria attribute0 =
        new SearchAttributeCriteria("category", null, "person", null, null);
    SearchAttributeCriteria attribute1 = new SearchAttributeCriteria("player", null, null, null,
        new SearchTagCriteriaRange("20100101", "20100131", "number"));

    // when
    SearchAttributeCriteria sac = apply(List.of(attribute0, attribute1));

    // then
    assertEquals("category::player", sac.key());
    assertNull(sac.eq());
    assertEquals("number", sac.range().type());
    assertEquals("person::000000020100101.0000", sac.range().start());
    assertEquals("person::000000020100131.0000", sac.range().end());
    assertNull(sac.beginsWith());
    assertNull(sac.eqOr());
  }

  /**
   * multiple attribute eqOr Range.
   */
  @Test
  void testAttribute10() {
    // given
    SearchAttributeCriteria attribute0 =
        new SearchAttributeCriteria("category", null, null, List.of("A", "B", "C"), null);
    SearchAttributeCriteria attribute1 =
        new SearchAttributeCriteria("player", null, null, List.of("111", "222"), null);

    // when
    SearchAttributeCriteria sac = apply(List.of(attribute0, attribute1));

    // then
    assertEquals("category::player", sac.key());
    assertNull(sac.eq());
    assertNull(sac.range());
    assertNull(sac.beginsWith());

    final int expected = 6;
    assertEquals(expected, sac.eqOr().size());
    assertEquals("A::111,A::222,B::111,B::222,C::111,C::222", String.join(",", sac.eqOr()));
  }

  /**
   * multiple attribute eqOr Range.
   */
  @Test
  void testAttribute11() {
    // given
    SearchAttributeCriteria attribute0 =
        new SearchAttributeCriteria("category", null, null, List.of("A", "B", "C"), null);
    SearchAttributeCriteria attribute1 =
        new SearchAttributeCriteria("player", null, null, List.of("111", "222"), null);
    SearchAttributeCriteria attribute2 =
        new SearchAttributeCriteria("user", null, null, List.of("55", "66"), null);

    // when
    SearchAttributeCriteria sac = apply(List.of(attribute0, attribute1, attribute2));

    // then
    assertEquals("category::player::user", sac.key());
    assertNull(sac.eq());
    assertNull(sac.range());
    assertNull(sac.beginsWith());

    final int expected = 12;
    assertEquals(expected, sac.eqOr().size());
    assertEquals(
        "A::111::55,A::111::66,A::222::55,A::222::66,B::111::55,B::111::66,"
            + "B::222::55,B::222::66,C::111::55,C::111::66,C::222::55,C::222::66",
        String.join(",", sac.eqOr()));
  }
}
