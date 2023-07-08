package com.formkiq.testutils.aws;

import java.util.Base64;
import java.util.List;
import java.util.Map;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * 
 * Jwt Token Encoder.
 *
 */
public class JwtTokenDecoder {

  /** Groups. */
  private List<String> groups;
  /** Username. */
  private String username;

  /**
   * constructor.
   * 
   * @param jwt {@link String}
   */
  @SuppressWarnings("unchecked")
  public JwtTokenDecoder(final String jwt) {
    String[] split = jwt.split("\\.");
    Gson gson = new GsonBuilder().disableHtmlEscaping().create();
    String s = new String(Base64.getDecoder().decode(split[1]));
    Map<String, Object> map = gson.fromJson(s, Map.class);

    this.groups = (List<String>) map.get("cognito:groups");
    this.username = (String) map.get("cognito:username");
  }

  /**
   * Get Groups.
   * 
   * @return {@link List} {@link String}
   */
  public List<String> getGroups() {
    return this.groups;
  }

  /**
   * Get Username.
   * 
   * @return {@link String}
   */
  public String getUsername() {
    return this.username;
  }

}
