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
package com.formkiq.strings;

import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

/**
 * Substitutes non-nested placeholders delimited by configurable literal markers. For example,
 * {@code new TemplateSubstitutor("{{", "}}").substitute("Hello {{name}}", Map.of("name", "Mike"))}
 * produces {@code Hello Mike}. Markers such as {@code ${...}}, {@code {...}}, {@code %...%}, and
 * {@code [[...]]} can be configured in the same way.
 *
 * <p>
 * Names are case-sensitive and passed to the resolver unchanged, including whitespace. Callers can
 * trim names or supply defaults through the resolver. Null results preserve the original
 * placeholder; empty results remove it. Empty placeholders and unclosed markers are preserved.
 * Values are inserted literally in a single pass, without recursively expanding placeholders in
 * replacement values. This class does not evaluate expressions or implement an escape syntax.
 *
 * <p>
 * Instances are immutable and may be shared. Maps and resolvers are supplied per call; their thread
 * safety and any value caching are the caller's responsibility.
 *
 * <p>
 * Resolvers can calculate values on demand, including database queries and remote service calls.
 * Only names present in the template are resolved, in encounter order. Repeated names invoke the
 * resolver again. Use {@link #substituteWithResolver(String, TemplateValueResolver)} for lookups
 * that throw checked exceptions.
 */
public final class TemplateSubstitutor {

  /** Literal opening marker. */
  private final String openingMarker;
  /** Literal closing marker. */
  private final String closingMarker;

  /**
   * Creates a substitutor with literal, nonempty markers, which may be identical.
   *
   * @param opening the opening marker
   * @param closing the closing marker
   * @throws NullPointerException if either marker is null
   * @throws IllegalArgumentException if either marker is empty
   */
  public TemplateSubstitutor(final String opening, final String closing) {
    this.openingMarker = Objects.requireNonNull(opening, "opening");
    this.closingMarker = Objects.requireNonNull(closing, "closing");
    if (opening.isEmpty() || closing.isEmpty()) {
      throw new IllegalArgumentException("Template markers must not be empty");
    }
  }

  /**
   * Substitutes placeholders using a resolver invoked for each nonempty placeholder name. For
   * example, {@code name -> values.get(name.trim())} enables whitespace trimming, while a resolver
   * that throws for missing names enables strict validation. Resolver exceptions propagate to the
   * caller.
   *
   * @param template the template, or null
   * @param resolver resolves a name to a value, or null to preserve the placeholder
   * @return the substituted template, or null when the template is null
   * @throws NullPointerException if resolver is null
   */
  public String substitute(final String template, final Function<String, ?> resolver) {
    Objects.requireNonNull(resolver, "resolver");
    return substituteWithResolver(template, resolver::apply);
  }

  /**
   * Substitutes placeholders using map values converted with {@link Object#toString()}. Missing
   * keys and null values leave their placeholders unchanged.
   *
   * @param template the template, or null
   * @param values the values indexed by placeholder name
   * @return the substituted template, or null when the template is null
   * @throws NullPointerException if values is null
   */
  public String substitute(final String template, final Map<String, ?> values) {
    Objects.requireNonNull(values, "values");
    return substitute(template, values::get);
  }

  /**
   * Resolves placeholders synchronously on demand, allowing checked exceptions from database or
   * service lookups to propagate unchanged. Resolution stops at the first exception. For example:
   *
   * <pre>{@code
   * substitutor.substituteWithResolver("Customer: {{customerName}}",
   *     name -> customerService.lookup(documentId, name));
   * }</pre>
   *
   * <p>
   * The resolver receives each complete placeholder name unchanged, so callers can interpret
   * expressions such as {@code database:customerName} or {@code service:status}. Null results
   * preserve placeholders; other values are converted with {@link Object#toString()}. Calls are
   * made once per occurrence without caching or retries. The caller controls lookup timeouts,
   * caching, and any fallback behavior.
   *
   * @param <E> the checked exception type thrown by the resolver
   * @param template the template, or null
   * @param resolver resolves a name through a calculation, lookup, or other operation
   * @return the substituted template, or null when the template is null
   * @throws E if the resolver fails
   * @throws NullPointerException if resolver is null
   */
  public <E extends Exception> String substituteWithResolver(final String template,
      final TemplateValueResolver<E> resolver) throws E {
    Objects.requireNonNull(resolver, "resolver");
    if (template == null) {
      return null;
    }

    StringBuilder result = new StringBuilder();
    int position = 0;
    int opening = template.indexOf(this.openingMarker, position);
    while (opening >= 0) {
      int nameStart = opening + this.openingMarker.length();
      int closing = template.indexOf(this.closingMarker, nameStart);
      if (closing < 0) {
        break;
      }

      result.append(template, position, opening);
      String name = template.substring(nameStart, closing);
      Object value = name.isEmpty() ? null : resolver.resolve(name);
      position = closing + this.closingMarker.length();
      if (value == null) {
        result.append(template, opening, position);
      } else {
        result.append(value);
      }
      opening = template.indexOf(this.openingMarker, position);
    }
    return result.append(template, position, template.length()).toString();
  }

}
