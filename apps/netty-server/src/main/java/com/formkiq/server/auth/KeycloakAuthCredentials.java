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

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.HttpURLConnection;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public final class KeycloakAuthCredentials implements IAuthCredentials {
  private static JsonObject post(final String url, final String requestBody) {
    HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url))
        .header("Content-Type", "application/x-www-form-urlencoded")
        .POST(HttpRequest.BodyPublishers.ofString(requestBody)).build();

    HttpResponse<String> response;

    try {
      response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
    } catch (Exception e) {
      e.printStackTrace(System.err);
      return null;
    }

    return (response.statusCode() == HttpURLConnection.HTTP_OK)
        ? JsonParser.parseString(response.body()).getAsJsonObject()
        : null;
  }

  /** Request body. */
  private final String requestBody;

  /** Keycloak token endpoint. */
  private final String tokenEndpoint;

  public KeycloakAuthCredentials(final String keycloakTokenEndpoint, final String keycloakClientId,
      final String keycloakClientSecret) {
    this.tokenEndpoint = keycloakTokenEndpoint;
    this.requestBody = "client_id=" + keycloakClientId + "&client_secret=" + keycloakClientSecret;
  }

  @Override
  public Tokens getTokens(final String username, final String password) {
    String fullRequestBody = this.requestBody + "&username=" + username + "&password=" + password
        + "&scope=openid" + "&grant_type=password";

    JsonObject responseJson = post(tokenEndpoint, fullRequestBody);

    if (responseJson != null) {
      String idToken = responseJson.get("id_token").getAsString();
      String accessToken = responseJson.get("access_token").getAsString();
      String refreshToken = responseJson.get("refresh_token").getAsString();

      return new Tokens(idToken, accessToken, refreshToken);
    }

    return null;
  }

  @Override
  public boolean isApiKeyValid(final String apiKey) {
    String fullRequestBody = this.requestBody + "&token=" + apiKey;

    JsonObject responseJson = post(tokenEndpoint + "/introspect", fullRequestBody);

    return responseJson != null && responseJson.has("active")
        && responseJson.get("active").getAsBoolean();
  }
}
