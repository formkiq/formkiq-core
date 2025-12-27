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

import com.formkiq.aws.dynamodb.DbKeys;
import com.formkiq.aws.dynamodb.model.SearchAttributeCriteria;
import com.formkiq.aws.dynamodb.model.SearchTagCriteriaRange;
import com.formkiq.aws.dynamodb.objects.Objects;
import com.formkiq.stacks.dynamodb.schemas.SchemaCompositeKeyRecord;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.formkiq.aws.dynamodb.objects.Objects.last;
import static com.formkiq.aws.dynamodb.objects.Objects.notNull;
import static com.formkiq.aws.dynamodb.objects.Strings.isEmpty;

/**
 * {@link Function} to convert {@link List} {@link SearchAttributeCriteria} to a single
 * {@link SearchAttributeCriteria}.
 */
public class SearchAttributesToCriteria
    implements Function<List<SearchAttributeCriteria>, SearchAttributeCriteria> {

  /** {@link SchemaCompositeKeyRecord}. */
  private final SchemaCompositeKeyRecord compositeKey;

  /**
   * constructor.
   * 
   * @param schemaCompositeKey {@link SchemaCompositeKeyRecord}
   */
  public SearchAttributesToCriteria(final SchemaCompositeKeyRecord schemaCompositeKey) {
    this.compositeKey = schemaCompositeKey;
  }

  @Override
  public SearchAttributeCriteria apply(final List<SearchAttributeCriteria> searchAttributes) {

    Map<String, SearchAttributeCriteria> map = searchAttributes.stream()
        .collect(Collectors.toMap(SearchAttributeCriteria::key, Function.identity()));

    List<String> compositeKeys = compositeKey != null ? compositeKey.getKeys()
        : searchAttributes.stream().map(SearchAttributeCriteria::key).toList();

    String eq = compositeKeys.stream().map(map::get).filter(java.util.Objects::nonNull)
        .map(SearchAttributeCriteria::eq).filter(java.util.Objects::nonNull)
        .collect(Collectors.joining(DbKeys.COMPOSITE_KEY_DELIM));

    Collection<String> eqOrs = generateEqOr(compositeKeys, map);

    String lastKey = last(compositeKeys);
    SearchAttributeCriteria last = map.get(lastKey);
    String beginsWith = last.beginsWith();
    SearchTagCriteriaRange range = last.range();

    if (!isEmpty(beginsWith) && !isEmpty(eq)) {
      beginsWith = eq + DbKeys.COMPOSITE_KEY_DELIM + beginsWith;
      eq = null;
    }

    if (range != null) {
      range = generateRange(range, eq);
      eq = null;
    }

    if (eqOrs != null) {
      eq = null;
    }

    String attributeKey = String.join(DbKeys.COMPOSITE_KEY_DELIM, compositeKeys);
    return new SearchAttributeCriteria(attributeKey, beginsWith, eq, eqOrs, range);
  }

  private Collection<String> generateEqOr(final List<String> compositeKeys,
      final Map<String, SearchAttributeCriteria> map) {

    List<ArrayList<String>> eqOrs = notNull(compositeKeys.stream().map(map::get)
        .map(c -> new ArrayList<>(notNull(c.eqOr()))).filter(l -> !l.isEmpty()).toList());

    Collection<String> strs = null;

    int size = eqOrs.size();
    if (size > 1) {

      int count = (int) eqOrs.stream().mapToLong(Collection::size).reduce(1, (a, b) -> a * b);

      List<Integer> positions = new ArrayList<>(IntStream.range(0, size).mapToObj(i -> 0).toList());

      List<List<String>> sbs = new ArrayList<>();

      for (int i = 0; i < count; i++) {
        sbs.add(new ArrayList<>());
      }

      for (int i = 0; i < count; i++) {
        String s = getString(eqOrs, positions);
        sbs.get(i).add(s);
        increment(eqOrs, positions);
      }

      strs = sbs.stream().map(l -> String.join(DbKeys.COMPOSITE_KEY_DELIM, l)).sorted().toList();

    } else if (size == 1) {
      strs = eqOrs.get(0);
    }

    return strs;
  }

  private SearchTagCriteriaRange generateRange(final SearchTagCriteriaRange range,
      final String eq) {
    boolean number = "number".equalsIgnoreCase(range.type());

    String start = range.start();
    String end = range.end();

    if (number) {

      try {
        start = Objects.formatDouble(Double.valueOf(range.start()), Objects.DOUBLE_FORMAT);
        end = Objects.formatDouble(Double.valueOf(range.end()), Objects.DOUBLE_FORMAT);
      } catch (NumberFormatException e) {
        start = range.start();
        end = range.end();
      }
    }

    String s = !isEmpty(eq) ? eq + DbKeys.COMPOSITE_KEY_DELIM + start : start;
    String e = !isEmpty(eq) ? eq + DbKeys.COMPOSITE_KEY_DELIM + end : end;

    return new SearchTagCriteriaRange(s, e, range.type());
  }

  private String getString(final List<ArrayList<String>> strs, final List<Integer> position) {

    List<String> sb = new ArrayList<>();

    for (int i = 0; i < position.size(); i++) {
      int pos = position.get(i);
      sb.add(strs.get(i).get(pos));
    }

    return String.join(DbKeys.COMPOSITE_KEY_DELIM, sb);
  }

  private void increment(final List<ArrayList<String>> strs, final List<Integer> positions) {
    for (int i = positions.size() - 1; i >= 0; i--) {

      if (positions.get(i) < strs.get(i).size() - 1) {
        positions.set(i, positions.get(i) + 1);
        break;
      } else {
        positions.set(i, 0);
      }
    }

  }
}
