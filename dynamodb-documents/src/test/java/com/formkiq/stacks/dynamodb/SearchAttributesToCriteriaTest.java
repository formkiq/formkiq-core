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
    SearchAttributeCriteria attribute = new SearchAttributeCriteria().key("category").eq("person");

    // when
    SearchAttributeCriteria sac = apply(List.of(attribute));

    // then
    assertEquals("category", sac.getKey());
    assertEquals("person", sac.getEq());
    assertNull(sac.getRange());
    assertNull(sac.getBeginsWith());
    assertNull(sac.getEqOr());
  }

  /**
   * Single attribute BEGINS.
   */
  @Test
  void testAttribute02() {
    // given
    SearchAttributeCriteria attribute =
        new SearchAttributeCriteria().key("category").beginsWith("o");

    // when
    SearchAttributeCriteria sac = apply(List.of(attribute));

    // then
    assertEquals("category", sac.getKey());
    assertEquals("", sac.getEq());
    assertNull(sac.getRange());
    assertEquals("o", sac.getBeginsWith());
    assertNull(sac.getEqOr());
  }

  /**
   * Single attribute String Range.
   */
  @Test
  void testAttribute03() {
    // given
    SearchAttributeCriteria attribute = new SearchAttributeCriteria().key("category")
        .range(new SearchTagCriteriaRange().type("string").start("A").end("B"));

    // when
    SearchAttributeCriteria sac = apply(List.of(attribute));

    // then
    assertEquals("category", sac.getKey());
    assertNull(sac.getEq());
    assertEquals("string", sac.getRange().getType());
    assertEquals("A", sac.getRange().getStart());
    assertEquals("B", sac.getRange().getEnd());
    assertNull(sac.getBeginsWith());
    assertNull(sac.getEqOr());
  }

  /**
   * Single attribute Number Range.
   */
  @Test
  void testAttribute04() {
    // given
    SearchAttributeCriteria attribute = new SearchAttributeCriteria().key("category")
        .range(new SearchTagCriteriaRange().type("number").start("20100101").end("20100131"));

    // when
    SearchAttributeCriteria sac = apply(List.of(attribute));

    // then
    assertEquals("category", sac.getKey());
    assertNull(sac.getEq());
    assertEquals("number", sac.getRange().getType());
    assertEquals("000000020100101.0000", sac.getRange().getStart());
    assertEquals("000000020100131.0000", sac.getRange().getEnd());
    assertNull(sac.getBeginsWith());
    assertNull(sac.getEqOr());
  }

  /**
   * Single attribute eqOr Range.
   */
  @Test
  void testAttribute05() {
    // given
    SearchAttributeCriteria attribute =
        new SearchAttributeCriteria().key("category").eqOr(List.of("A", "B", "C"));

    // when
    SearchAttributeCriteria sac = apply(List.of(attribute));

    // then
    assertEquals("category", sac.getKey());
    assertNull(sac.getEq());
    assertNull(sac.getRange());
    assertNull(sac.getBeginsWith());
    assertEquals("A,B,C", String.join(",", sac.getEqOr()));
  }

  /**
   * multiple attribute EQ.
   */
  @Test
  void testAttribute06() {
    // given
    SearchAttributeCriteria attribute0 = new SearchAttributeCriteria().key("category").eq("person");
    SearchAttributeCriteria attribute1 = new SearchAttributeCriteria().key("userId").eq("111");

    // when
    SearchAttributeCriteria sac = apply(List.of(attribute0, attribute1));

    // then
    assertEquals("category::userId", sac.getKey());
    assertEquals("person::111", sac.getEq());
    assertNull(sac.getRange());
    assertNull(sac.getBeginsWith());
    assertNull(sac.getEqOr());
  }

  /**
   * Single attribute BEGINS.
   */
  @Test
  void testAttribute07() {
    // given
    SearchAttributeCriteria attribute0 = new SearchAttributeCriteria().key("category").eq("o");
    SearchAttributeCriteria attribute1 =
        new SearchAttributeCriteria().key("player").beginsWith("111");

    // when
    SearchAttributeCriteria sac = apply(List.of(attribute0, attribute1));

    // then
    assertEquals("category::player", sac.getKey());
    assertNull(sac.getEq());
    assertNull(sac.getRange());
    assertEquals("o::111", sac.getBeginsWith());
    assertNull(sac.getEqOr());
  }

  /**
   * Single attribute String Range.
   */
  @Test
  void testAttribute08() {
    // given
    SearchAttributeCriteria attribute0 = new SearchAttributeCriteria().key("category").eq("person");
    SearchTagCriteriaRange range = new SearchTagCriteriaRange().type("string").start("A").end("B");
    SearchAttributeCriteria attribute1 = new SearchAttributeCriteria().key("player").range(range);

    // when
    SearchAttributeCriteria sac = apply(List.of(attribute0, attribute1));

    // then
    assertEquals("category::player", sac.getKey());
    assertNull(sac.getEq());
    assertEquals("string", sac.getRange().getType());
    assertEquals("person::A", sac.getRange().getStart());
    assertEquals("person::B", sac.getRange().getEnd());
    assertNull(sac.getBeginsWith());
    assertNull(sac.getEqOr());
  }

  /**
   * Single attribute Number Range.
   */
  @Test
  void testAttribute09() {
    // given
    SearchAttributeCriteria attribute0 = new SearchAttributeCriteria().key("category").eq("person");
    SearchAttributeCriteria attribute1 = new SearchAttributeCriteria().key("player")
        .range(new SearchTagCriteriaRange().type("number").start("20100101").end("20100131"));

    // when
    SearchAttributeCriteria sac = apply(List.of(attribute0, attribute1));

    // then
    assertEquals("category::player", sac.getKey());
    assertNull(sac.getEq());
    assertEquals("number", sac.getRange().getType());
    assertEquals("person::000000020100101.0000", sac.getRange().getStart());
    assertEquals("person::000000020100131.0000", sac.getRange().getEnd());
    assertNull(sac.getBeginsWith());
    assertNull(sac.getEqOr());
  }

  /**
   * multiple attribute eqOr Range.
   */
  @Test
  void testAttribute10() {
    // given
    SearchAttributeCriteria attribute0 =
        new SearchAttributeCriteria().key("category").eqOr(List.of("A", "B", "C"));
    SearchAttributeCriteria attribute1 =
        new SearchAttributeCriteria().key("player").eqOr(List.of("111", "222"));

    // when
    SearchAttributeCriteria sac = apply(List.of(attribute0, attribute1));

    // then
    assertEquals("category::player", sac.getKey());
    assertNull(sac.getEq());
    assertNull(sac.getRange());
    assertNull(sac.getBeginsWith());

    final int expected = 6;
    assertEquals(expected, sac.getEqOr().size());
    assertEquals("A::111,A::222,B::111,B::222,C::111,C::222", String.join(",", sac.getEqOr()));
  }

  /**
   * multiple attribute eqOr Range.
   */
  @Test
  void testAttribute11() {
    // given
    SearchAttributeCriteria attribute0 =
        new SearchAttributeCriteria().key("category").eqOr(List.of("A", "B", "C"));
    SearchAttributeCriteria attribute1 =
        new SearchAttributeCriteria().key("player").eqOr(List.of("111", "222"));
    SearchAttributeCriteria attribute2 =
        new SearchAttributeCriteria().key("user").eqOr(List.of("55", "66"));

    // when
    SearchAttributeCriteria sac = apply(List.of(attribute0, attribute1, attribute2));

    // then
    assertEquals("category::player::user", sac.getKey());
    assertNull(sac.getEq());
    assertNull(sac.getRange());
    assertNull(sac.getBeginsWith());

    final int expected = 12;
    assertEquals(expected, sac.getEqOr().size());
    assertEquals(
        "A::111::55,A::111::66,A::222::55,A::222::66,B::111::55,B::111::66,"
            + "B::222::55,B::222::66,C::111::55,C::111::66,C::222::55,C::222::66",
        String.join(",", sac.getEqOr()));
  }
}
