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
package com.formkiq.module.events;

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.net.URISyntaxException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import com.formkiq.testutils.aws.LocalStackExtension;
import com.formkiq.testutils.aws.TestServices;

/**
 * 
 * Unit Tests for {@link EventServiceSns}.
 *
 */
@ExtendWith(LocalStackExtension.class)
public class DocumentEventServiceSnsTest {

  /** {@link EventServiceSns}. */
  private static EventServiceSns service;

  /**
   * BeforeAll().
   * 
   * @throws URISyntaxException URISyntaxException
   */
  @BeforeAll
  public static void beforeAll() throws URISyntaxException {
    service = new EventServiceSns(TestServices.getSnsConnection(null), "test");
  }

  /**
   * Test convertToPrintableCharacters.
   */
  @Test
  public void testConvertToPrintableCharacters01() {
    String s = "öäü How to do in java .\" com A função, Ãugent";
    assertEquals("HowtodoinjavacomAfunougent", service.convertToPrintableCharacters(s));
    assertEquals("webhook/Mostpopularboynamesxlsx",
        service.convertToPrintableCharacters("webhook/\"Mostpopularboynames.xlsx\""));
    assertEquals("FK79tpEN51", service.convertToPrintableCharacters("FK79tpEN51"));
  }
}
