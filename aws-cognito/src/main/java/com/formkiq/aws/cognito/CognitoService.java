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

import java.util.Arrays;
import java.util.Collection;
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
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminAddUserToGroupRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminConfirmSignUpRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminConfirmSignUpResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminCreateUserRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminCreateUserResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminGetUserRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminGetUserResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminInitiateAuthRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminInitiateAuthResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminRespondToAuthChallengeRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminRespondToAuthChallengeResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminSetUserPasswordRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminSetUserPasswordResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminUpdateUserAttributesRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminUpdateUserAttributesResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AttributeType;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AuthFlowType;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AuthenticationResultType;
import software.amazon.awssdk.services.cognitoidentityprovider.model.ChallengeNameType;
import software.amazon.awssdk.services.cognitoidentityprovider.model.CreateGroupRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.CreateGroupResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.GetUserRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.GetUserResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.ListUsersRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.ListUsersResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.UserType;

/**
 * 
 * Cognito Services.
 *
 */
public class CognitoService {

  /** {@link String}. */
  private String clientId;
  /** {@link CognitoIdentityClient}. */
  private CognitoIdentityClient cognitoClient;
  /** {@link CognitoIdentityProviderClient}. */
  private CognitoIdentityProviderClient cognitoProvider;
  /** {@link String}. */
  private String identityPool;
  /** {@link Region}. */
  private Region region;
  /** {@link String}. */
  private String userPoolId;

  /**
   * Constructor.
   * 
   * @param builder {@link CognitoConnectionBuilder}
   */
  public CognitoService(final CognitoConnectionBuilder builder) {
    this.cognitoProvider = builder.buildProvider();
    this.cognitoClient = builder.buildClient();

    this.clientId = builder.getClientId();
    this.userPoolId = builder.getUserPoolId();
    this.identityPool = builder.getIdentityPool();
    this.region = builder.getRegion();
  }

  /**
   * Add Cognito Group.
   * 
   * @param groupName {@link String}
   * @return {@link CreateGroupResponse}
   */
  public CreateGroupResponse addGroup(final String groupName) {
    CreateGroupRequest req =
        CreateGroupRequest.builder().userPoolId(this.userPoolId).groupName(groupName).build();
    return this.cognitoProvider.createGroup(req);
  }

  /**
   * Add Cognito User.
   * 
   * @param email {@link String}
   * @param password {@link String}
   * @return {@link UserType}
   */
  public UserType addUser(final String email, final String password) {

    AdminCreateUserRequest cognitoRequest = AdminCreateUserRequest.builder()
        .userPoolId(this.userPoolId).username(email).temporaryPassword(password)
        .userAttributes(Arrays.asList(AttributeType.builder().name("email").value(email).build(),
            AttributeType.builder().name("email_verified").value("true").build()))
        .build();

    AdminCreateUserResponse createUserResult = this.cognitoProvider.adminCreateUser(cognitoRequest);
    UserType user = createUserResult.user();

    return user;
  }

  /**
   * Add Cognito User to Cognito Group.
   * 
   * @param email {@link String}
   * @param groupname {@link String}
   */
  public void addUserToGroup(final String email, final String groupname) {

    AdminAddUserToGroupRequest addUserToGroupRequest = AdminAddUserToGroupRequest.builder()
        .groupName(groupname).userPoolId(this.userPoolId).username(email).build();

    this.cognitoProvider.adminAddUserToGroup(addUserToGroupRequest);
  }

  /**
   * Confirm User Signup.
   * 
   * @param username {@link String}
   * @return {@link AdminConfirmSignUpResponse}
   */
  public AdminConfirmSignUpResponse confirmSignUp(final String username) {
    AdminConfirmSignUpRequest req =
        AdminConfirmSignUpRequest.builder().userPoolId(this.userPoolId).username(username).build();
    return this.cognitoProvider.adminConfirmSignUp(req);
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
   * Finds User by Email.
   *
   * @param email {@link String}
   * @return {@link ListUsersResponse}
   */
  private ListUsersResponse findUserByEmail(final String email) {

    final String emailQuery = "email=\"" + email + "\"";
    ListUsersRequest usersRequest = ListUsersRequest.builder().userPoolId(this.userPoolId)
        .attributesToGet("email").filter(emailQuery).build();

    ListUsersResponse users = this.cognitoProvider.listUsers(usersRequest);
    return users;
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
   * Get User.
   * 
   * @param token {@link AuthenticationResultType}.
   * @return {@link GetUserResponse}
   */
  public GetUserResponse getUser(final AuthenticationResultType token) {
    GetUserRequest req = GetUserRequest.builder().accessToken(token.accessToken()).build();
    GetUserResponse resp = this.cognitoProvider.getUser(req);
    return resp;
  }

  /**
   * Get User.
   * 
   * @param username {@link String}
   * @return {@link AdminGetUserResponse}
   */
  public AdminGetUserResponse getUser(final String username) {
    return this.cognitoProvider.adminGetUser(
        AdminGetUserRequest.builder().userPoolId(this.userPoolId).username(username).build());
  }

  /**
   * Does Cognito User Exist.
   * 
   * @param email {@link String}
   * @return boolean
   */
  public boolean isUserExists(final String email) {
    return !findUserByEmail(email).users().isEmpty();
  }

  /**
   * Login User.
   *
   * @param email {@link String}
   * @param password {@link String}
   * @return {@link AuthenticationResultType}
   */
  public AuthenticationResultType login(final String email, final String password) {

    AdminInitiateAuthResponse authResult = loginInternal(email, password);

    AuthenticationResultType authentication = authResult.authenticationResult();
    return authentication;
  }

  /**
   * Internal Cognito Login Method.
   * 
   * @param email {@link String}
   * @param password {@link String}
   * @return {@link AdminInitiateAuthResponse}
   */
  private AdminInitiateAuthResponse loginInternal(final String email, final String password) {
    HashMap<String, String> authParams = new HashMap<String, String>();
    authParams.put("USERNAME", email);
    authParams.put("PASSWORD", password);

    AdminInitiateAuthRequest authRequest =
        AdminInitiateAuthRequest.builder().authFlow(AuthFlowType.ADMIN_NO_SRP_AUTH)
            .userPoolId(this.userPoolId).clientId(this.clientId).authParameters(authParams).build();

    AdminInitiateAuthResponse authResult = this.cognitoProvider.adminInitiateAuth(authRequest);
    return authResult;
  }

  /**
   * Login User in NEW_PASSWORD_REQUIRED status.
   * 
   * @param email {@link String}
   * @param password {@link String}
   * @param newpassword {@link String}
   * @return {@link AuthenticationResultType}
   */
  public AuthenticationResultType loginWithNewPassword(final String email, final String password,
      final String newpassword) {

    AuthenticationResultType authentication = null;
    final AdminInitiateAuthResponse authResult = loginInternal(email, password);

    if ("NEW_PASSWORD_REQUIRED".equals(authResult.challengeName().name())) {

      final Map<String, String> challengeResponses = new HashMap<>();
      challengeResponses.put("USERNAME", email);
      challengeResponses.put("PASSWORD", password);
      challengeResponses.put("NEW_PASSWORD", newpassword);

      // populate the challenge response
      AdminRespondToAuthChallengeRequest request = AdminRespondToAuthChallengeRequest.builder()
          .challengeName(ChallengeNameType.NEW_PASSWORD_REQUIRED)
          .challengeResponses(challengeResponses).clientId(this.clientId)
          .userPoolId(this.userPoolId).session(authResult.session()).build();

      AdminRespondToAuthChallengeResponse resultChallenge =
          this.cognitoProvider.adminRespondToAuthChallenge(request);
      authentication = resultChallenge.authenticationResult();

    } else {
      authentication = authResult.authenticationResult();
    }

    return authentication;
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

  /**
   * Sets User Password.
   * 
   * @param username {@link String}
   * @param password {@link String}
   * @param permanent <code>True</code> if the password is permanent, <code>False</code> if it is
   *        temporary.
   * @return {@link AdminSetUserPasswordResponse}
   */
  public AdminSetUserPasswordResponse setUserPassword(final String username, final String password,
      final boolean permanent) {
    return this.cognitoProvider
        .adminSetUserPassword(AdminSetUserPasswordRequest.builder().userPoolId(this.userPoolId)
            .username(username).password(password).permanent(Boolean.valueOf(permanent)).build());
  }

  /**
   * Update User Attributes.
   * 
   * @param username {@link String}
   * @param userAttributes {@link Collection} {@link AttributeType}
   * @return {@link AdminUpdateUserAttributesResponse}
   */
  public AdminUpdateUserAttributesResponse updateUserAttributes(final String username,
      final Collection<AttributeType> userAttributes) {
    return this.cognitoProvider.adminUpdateUserAttributes(AdminUpdateUserAttributesRequest.builder()
        .userPoolId(this.userPoolId).username(username).userAttributes(userAttributes).build());
  }
}
