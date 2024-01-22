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
package com.formkiq.aws.dynamodb.objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * 
 * Unit Test {@link Strings}.
 *
 */
class StringsTest {

  @Test
  void removeBackSlashes() {
    assertEquals("text", Strings.removeBackSlashes("text"));
    assertEquals("text", Strings.removeBackSlashes("/text"));
    assertEquals("text", Strings.removeBackSlashes("/text/"));
  }

  @Test
  void removeEndingPunctuation() {
    assertEquals("text", Strings.removeEndingPunctuation("text"));
    assertEquals("\"text\"", Strings.removeEndingPunctuation("\"text\","));
    assertEquals("\"text", Strings.removeEndingPunctuation("\"text!"));
    assertEquals("'text?'", Strings.removeEndingPunctuation("'text?'"));
    assertEquals("\"text'", Strings.removeEndingPunctuation("\"text'"));
  }

  @Test
  void replaceQuotes() {
    assertEquals("text", Strings.removeQuotes("text"));
    assertEquals("text", Strings.removeQuotes("\"text\""));
    assertEquals("text", Strings.removeQuotes("\"text"));
    assertEquals("text", Strings.removeQuotes("'text'"));
    assertEquals("text", Strings.removeQuotes("\"text'"));
  }

  @Test
  void testExtension() {
    assertEquals("txt", Strings.getExtension("test.txt"));
    assertEquals("txt", Strings.getExtension("/bleh/something/test.txt"));
    assertEquals("txt", Strings.getExtension("/bleh/something/test (something).txt"));
  }

  @Test
  void testFilename() {
    assertEquals("test.txt", Strings.getFilename("test.txt"));
    assertEquals("test.txt", Strings.getFilename("/bleh/something/test.txt"));
    assertEquals("test (something).txt",
        Strings.getFilename("/bleh/something/test (something).txt"));
    assertEquals("", Strings.getFilename("http://www.google.com"));
    assertEquals("something.pdf", Strings.getFilename("http://www.google.com/asd/something.pdf"));
  }

  @Test
  void testFindUrlMatch01() {
    // given
    List<String> strs =
        Arrays.asList("/documents/{documentId}/content", "/documents", "/documents/{documentId}");

    // when
    String result0 = Strings.findUrlMatch(strs, "/documents/123/content");
    String result1 = Strings.findUrlMatch(strs, "/documents/123");
    String result2 = Strings.findUrlMatch(strs, "/documents");

    // then
    assertEquals("/documents/{documentId}/content", result0);
    assertEquals("/documents/{documentId}", result1);
    assertEquals("/documents", result2);
  }
}
