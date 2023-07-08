package com.formkiq.testutils.aws;

import io.jsonwebtoken.Jwts;

/**
 * 
 * Jwt Token Encoder.
 *
 */
public class JwtTokenEncoder {

  /**
   * Encode Cognito Api Gateway Jwt Token.
   * 
   * @param groups {@link String}
   * @param username {@link String}
   * @return {@link String}
   */
  public static String encodeCognito(final String[] groups, final String username) {
    String jws = Jwts.builder().setSubject("FormKiQ").claim("cognito:groups", groups)
        .claim("cognito:username", username).compact();
    return jws;
  }
}
