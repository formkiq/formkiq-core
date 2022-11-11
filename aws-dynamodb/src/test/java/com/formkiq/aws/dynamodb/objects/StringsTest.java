package com.formkiq.aws.dynamodb.objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

/**
 * 
 * Unit Test {@link Strings}.
 *
 */
class StringsTest {

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
  }
}
