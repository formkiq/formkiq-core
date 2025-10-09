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
package com.formkiq.aws.services.lambda;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import java.util.function.Function;

/**
 * Convert {@link JwtClaims} to {@link ApiGatewayRequestEvent}.
 */
public class JwtClaimsToApiGatewayEvent implements Function<JwtClaims, ApiGatewayRequestEvent> {
  /** {@link Gson}. */
  private Gson gson = new GsonBuilder().create();

  @Override
  public ApiGatewayRequestEvent apply(final JwtClaims claims) {
    JsonObject claimsObject = new JsonObject();
    claimsObject.addProperty("cognito:groups", claims.cognitoGroups().toString().replace(",", " "));
    claimsObject.addProperty("sitesClaims", claims.sitesClaims());
    claimsObject.addProperty("username", claims.username());

    JsonObject authorizer = new JsonObject();
    authorizer.add("claims", claimsObject);

    JsonObject requestContext = new JsonObject();
    requestContext.add("authorizer", authorizer);

    JsonObject root = new JsonObject();
    root.add("requestContext", requestContext);

    String json = gson.toJson(root);
    return gson.fromJson(json, ApiGatewayRequestEvent.class);
  }
}
