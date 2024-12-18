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
package com.formkiq.server.auth;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SimpleAuthCredentialsTest {
  /** Admin user constant. */
  private static final String ADMIN_USER = "username";

  /** Admin password constant. */
  private static final String ADMIN_PASSWORD = "password";

  /** API key constant. */
  private static final String API_KEY = "apiKey";

  /** SimpleAuthCredentials object for tests. */
  private static final SimpleAuthCredentials SIMPLE_AUTH_CREDENTIALS =
      new SimpleAuthCredentials(ADMIN_USER, ADMIN_PASSWORD, API_KEY);

  @Test
  public void testGetTokens() {
    Tokens tokens = SIMPLE_AUTH_CREDENTIALS.getTokens(ADMIN_USER, ADMIN_PASSWORD);

    assertNotNull(tokens);
    assertEquals(API_KEY, tokens.idToken());
    assertEquals(API_KEY, tokens.accessToken());
    assertEquals("", tokens.refreshToken());
  }

  @Test
  public void testGetTokensInvalid() {
    Tokens tokens = SIMPLE_AUTH_CREDENTIALS.getTokens("invalidUser", "invalidPassword");

    assertNull(tokens);
  }

  @Test
  public void testIsApiKeyValid() {
    assertTrue(SIMPLE_AUTH_CREDENTIALS.isApiKeyValid(API_KEY));
  }

  @Test
  public void testIsApiKeyInvalid() {
    assertFalse(SIMPLE_AUTH_CREDENTIALS.isApiKeyValid("invalidApiKey"));
  }
}
