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
package com.formkiq.aws.cognito;

import java.util.HashMap;
import java.util.Map;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cognitoidentity.CognitoIdentityClient;
import software.amazon.awssdk.services.cognitoidentity.model.Credentials;
import software.amazon.awssdk.services.cognitoidentity.model.DescribeIdentityPoolRequest;
import software.amazon.awssdk.services.cognitoidentity.model.DescribeIdentityPoolResponse;
import software.amazon.awssdk.services.cognitoidentity.model.GetCredentialsForIdentityRequest;
import software.amazon.awssdk.services.cognitoidentity.model.GetCredentialsForIdentityResponse;
import software.amazon.awssdk.services.cognitoidentity.model.GetIdRequest;
import software.amazon.awssdk.services.cognitoidentity.model.GetIdResponse;
import software.amazon.awssdk.services.cognitoidentity.model.GetIdentityPoolRolesRequest;
import software.amazon.awssdk.services.cognitoidentity.model.GetIdentityPoolRolesResponse;
import software.amazon.awssdk.services.cognitoidentity.model.SetIdentityPoolRolesRequest;
import software.amazon.awssdk.services.cognitoidentity.model.SetIdentityPoolRolesResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AuthenticationResultType;

/**
 * 
 * Cognito Services.
 *
 */
public class CognitoIdentityService {

  /** {@link CognitoIdentityClient}. */
  private CognitoIdentityClient cognitoClient;
  /** {@link String}. */
  private String identityPool;
  /** {@link Region}. */
  private Region region;
  /** {@link String}. */
  private String userPoolId;

  /**
   * Constructor.
   * 
   * @param builder {@link CognitoIdentityConnectionBuilder}
   */
  public CognitoIdentityService(final CognitoIdentityConnectionBuilder builder) {
    this.cognitoClient = builder.buildClient();

    this.userPoolId = builder.getUserPoolId();
    this.identityPool = builder.getIdentityPool();
    this.region = builder.getRegion();
  }

  /**
   * Describe Identity Pool.
   *
   * @param identityPoolId {@link String}
   * @return {@link DescribeIdentityPoolResponse}
   */
  public DescribeIdentityPoolResponse describeIdentityPool(final String identityPoolId) {
    return this.cognitoClient.describeIdentityPool(
        DescribeIdentityPoolRequest.builder().identityPoolId(identityPoolId).build());
  }

  /**
   * Get {@link Credentials} from an {@link AuthenticationResultType}.
   * 
   * @param token {@link AuthenticationResultType}
   * @return {@link Credentials}
   */
  public Credentials getCredentials(final AuthenticationResultType token) {

    Map<String, String> logins = new HashMap<>();
    logins.put("cognito-idp." + this.region + ".amazonaws.com/" + this.userPoolId, token.idToken());

    GetIdResponse r = this.cognitoClient
        .getId(GetIdRequest.builder().logins(logins).identityPoolId(this.identityPool).build());
    GetCredentialsForIdentityRequest req = GetCredentialsForIdentityRequest.builder().logins(logins)
        .identityId(r.identityId()).build();

    GetCredentialsForIdentityResponse rr = this.cognitoClient.getCredentialsForIdentity(req);

    return rr.credentials();
  }

  /**
   * Get Identity Pool Roles.
   * 
   * @param identityPoolId {@link String}
   * @return {@link GetIdentityPoolRolesResponse}
   */
  public GetIdentityPoolRolesResponse getIdentityPoolRoles(final String identityPoolId) {
    return this.cognitoClient.getIdentityPoolRoles(
        GetIdentityPoolRolesRequest.builder().identityPoolId(identityPoolId).build());
  }

  /**
   * Set Identity Pool Roles.
   * 
   * @param request {@link SetIdentityPoolRolesRequest}
   * @return {@link SetIdentityPoolRolesResponse}
   */
  public SetIdentityPoolRolesResponse setIdentityPoolRoles(
      final SetIdentityPoolRolesRequest request) {
    return this.cognitoClient.setIdentityPoolRoles(request);
  }
}
