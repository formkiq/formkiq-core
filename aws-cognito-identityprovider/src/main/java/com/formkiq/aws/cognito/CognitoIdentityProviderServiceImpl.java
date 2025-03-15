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


import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminAddUserToGroupRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminConfirmSignUpRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminConfirmSignUpResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminCreateUserRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminCreateUserResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminDeleteUserRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminDisableUserRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminEnableUserRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminGetUserRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminGetUserResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminInitiateAuthRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminInitiateAuthResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminListGroupsForUserRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminListGroupsForUserResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminRemoveUserFromGroupRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminResetUserPasswordRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminRespondToAuthChallengeRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminRespondToAuthChallengeResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminSetUserPasswordRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminSetUserPasswordResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminUpdateUserAttributesRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminUpdateUserAttributesResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AssociateSoftwareTokenRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AssociateSoftwareTokenResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AttributeType;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AuthFlowType;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AuthenticationResultType;
import software.amazon.awssdk.services.cognitoidentityprovider.model.ChallengeNameType;
import software.amazon.awssdk.services.cognitoidentityprovider.model.ChangePasswordRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.ChangePasswordResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.ConfirmForgotPasswordRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.ConfirmForgotPasswordResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.CreateGroupRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.CreateGroupResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.DeleteGroupRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.ForgotPasswordRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.ForgotPasswordResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.GetGroupRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.GetGroupResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.GetUserRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.GetUserResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.InitiateAuthRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.InitiateAuthResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.ListGroupsRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.ListGroupsResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.ListUsersInGroupRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.ListUsersInGroupResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.ListUsersRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.ListUsersResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.UserType;
import software.amazon.awssdk.services.cognitoidentityprovider.model.VerifySoftwareTokenRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.VerifySoftwareTokenResponse;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 
 * Cognito Services.
 *
 */
public class CognitoIdentityProviderServiceImpl implements CognitoIdentityProviderService {

  /** Cognito User Attributes. */
  public static final Map<String, String> COGNITO_USER_ATTRIBUTES = initCognitoAttributes();

  private static Map<String, String> initCognitoAttributes() {
    Map<String, String> map = new HashMap<>();
    map.put("address", "address");
    map.put("birthdate", "birthdate");
    map.put("familyName", "family_name");
    map.put("family_name", "familyName");
    map.put("gender", "gender");
    map.put("givenName", "given_name");
    map.put("given_name", "givenName");
    map.put("locale", "locale");
    map.put("middleName", "middle_name");
    map.put("middle_name", "middleName");
    map.put("name", "name");
    map.put("nickname", "nickname");
    map.put("phoneNumber", "phone_number");
    map.put("phone_number", "phoneNumber");
    map.put("picture", "picture");
    map.put("preferredUsername", "preferred_username");
    map.put("preferred_username", "preferredUsername");
    map.put("profile", "profile");
    map.put("zoneinfo", "zoneinfo");
    map.put("updatedAt", "updated_at");
    map.put("updated_at", "updatedAt");
    map.put("website", "website");

    return map;
  }

  /** {@link String}. */
  private final String clientId;
  /** {@link CognitoIdentityProviderClient}. */
  private final CognitoIdentityProviderClient cognitoProvider;
  /** {@link String}. */
  private final String userPoolId;

  /**
   * Constructor.
   * 
   * @param builder {@link CognitoIdentityProviderConnectionBuilder}
   */
  public CognitoIdentityProviderServiceImpl(
      final CognitoIdentityProviderConnectionBuilder builder) {
    this.cognitoProvider = builder.build();

    this.clientId = builder.getClientId();
    this.userPoolId = builder.getUserPoolId();
  }

  @Override
  public CreateGroupResponse addGroup(final String groupName, final String groupDescription) {
    CreateGroupRequest req = CreateGroupRequest.builder().userPoolId(this.userPoolId)
        .groupName(groupName).description(groupDescription).build();
    return this.cognitoProvider.createGroup(req);
  }

  @Override
  public UserType addUser(final String username, final Map<String, String> userAttributes,
      final boolean emailVerified) {
    return addUser(username, null, userAttributes, emailVerified);
  }

  /**
   * Add Cognito User.
   * 
   * @param username {@link String}
   * @param temporaryPassword {@link String}
   * @param userAttributes {@link Map}
   * @param emailVerified boolean
   * @return {@link UserType}
   */
  public UserType addUser(final String username, final String temporaryPassword,
      final Map<String, String> userAttributes, final boolean emailVerified) {

    List<AttributeType> attributes = new ArrayList<>();

    attributes.add(AttributeType.builder().name("email").value(username).build());
    attributes.add(AttributeType.builder().name("email_verified")
        .value(String.valueOf(emailVerified)).build());

    if (userAttributes != null) {
      for (Map.Entry<String, String> e : userAttributes.entrySet()) {

        if (COGNITO_USER_ATTRIBUTES.containsKey(e.getKey())) {
          attributes.add(AttributeType.builder().name(COGNITO_USER_ATTRIBUTES.get(e.getKey()))
              .value(e.getValue()).build());
        }
      }
    }

    AdminCreateUserRequest cognitoRequest =
        AdminCreateUserRequest.builder().userPoolId(this.userPoolId).username(username)
            .temporaryPassword(temporaryPassword).userAttributes(attributes).build();

    AdminCreateUserResponse createUserResult = this.cognitoProvider.adminCreateUser(cognitoRequest);
    return createUserResult.user();
  }

  @Override
  public ChangePasswordResponse setChangePassword(final String accessToken,
      final String previousPassword, final String proposedPassword) {
    ChangePasswordRequest request = ChangePasswordRequest.builder().accessToken(accessToken)
        .previousPassword(previousPassword).proposedPassword(proposedPassword).build();
    return this.cognitoProvider.changePassword(request);
  }

  @Override
  public ForgotPasswordResponse forgotPassword(final String username) {
    ForgotPasswordRequest request =
        ForgotPasswordRequest.builder().clientId(this.clientId).username(username).build();
    return this.cognitoProvider.forgotPassword(request);
  }

  @Override
  public ConfirmForgotPasswordResponse forgotPasswordConfirm(final String username,
      final String code, final String password) {
    ConfirmForgotPasswordRequest request =
        ConfirmForgotPasswordRequest.builder().clientId(this.clientId).username(username)
            .confirmationCode(code).password(password).build();
    return this.cognitoProvider.confirmForgotPassword(request);
  }

  @Override
  public void addUserToGroup(final String email, final String groupname) {

    AdminAddUserToGroupRequest addUserToGroupRequest = AdminAddUserToGroupRequest.builder()
        .groupName(groupname).userPoolId(this.userPoolId).username(email).build();

    this.cognitoProvider.adminAddUserToGroup(addUserToGroupRequest);
  }

  @Override
  public void removeUserFromGroup(final String username, final String groupname) {

    AdminRemoveUserFromGroupRequest removeUserFromGroupRequest = AdminRemoveUserFromGroupRequest
        .builder().groupName(groupname).userPoolId(this.userPoolId).username(username).build();
    this.cognitoProvider.adminRemoveUserFromGroup(removeUserFromGroupRequest);
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
   * Finds User by Email.
   *
   * @param email {@link String}
   * @return {@link ListUsersResponse}
   */
  public ListUsersResponse findUserByEmail(final String email) {

    final String emailQuery = "email=\"" + email + "\"";
    ListUsersRequest usersRequest = ListUsersRequest.builder().userPoolId(this.userPoolId)
        .attributesToGet("email").filter(emailQuery).build();

    return this.cognitoProvider.listUsers(usersRequest);
  }

  @Override
  public ListUsersResponse listUsers(final String paginationToken, final Integer limit) {
    ListUsersRequest usersRequest = ListUsersRequest.builder().userPoolId(this.userPoolId)
        .paginationToken(paginationToken).limit(limit).build();
    return this.cognitoProvider.listUsers(usersRequest);
  }

  @Override
  public GetGroupResponse getGroup(final String groupName) {
    GetGroupRequest req =
        GetGroupRequest.builder().userPoolId(this.userPoolId).groupName(groupName).build();
    return this.cognitoProvider.getGroup(req);
  }

  @Override
  public GetUserResponse getUser(final AuthenticationResultType token) {
    GetUserRequest req = GetUserRequest.builder().accessToken(token.accessToken()).build();
    return this.cognitoProvider.getUser(req);
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

  @Override
  public ListGroupsResponse listGroups(final String token, final Integer limit) {
    return this.cognitoProvider.listGroups(ListGroupsRequest.builder().userPoolId(this.userPoolId)
        .nextToken(token).limit(limit).build());
  }

  @Override
  public AdminListGroupsForUserResponse listGroups(final String username, final String token,
      final Integer limit) {
    AdminListGroupsForUserRequest req = AdminListGroupsForUserRequest.builder()
        .userPoolId(this.userPoolId).username(username).nextToken(token).limit(limit).build();
    return this.cognitoProvider.adminListGroupsForUser(req);
  }

  @Override
  public ListUsersInGroupResponse listUsersInGroup(final String groupName, final String token,
      final Integer limit) {
    return this.cognitoProvider.listUsersInGroup(ListUsersInGroupRequest.builder()
        .userPoolId(this.userPoolId).groupName(groupName).nextToken(token).limit(limit).build());
  }

  @Override
  public InitiateAuthResponse loginUserPasswordAuth(final String email, final String password) {
    Map<String, String> authParams = Map.of("USERNAME", email, "PASSWORD", password);
    InitiateAuthRequest auth =
        InitiateAuthRequest.builder().authFlow(AuthFlowType.USER_PASSWORD_AUTH)
            .authParameters(authParams).clientId(this.clientId).build();
    return this.cognitoProvider.initiateAuth(auth);
  }

  /**
   * Login User using Admin Flow.
   *
   * @param email {@link String}
   * @param password {@link String}
   * @return {@link AuthenticationResultType}
   */
  public AuthenticationResultType loginAdminFlow(final String email, final String password) {
    AdminInitiateAuthResponse authResult = loginInternal(email, password);
    return authResult.authenticationResult();
  }

  /**
   * Internal Cognito Login Method.
   * 
   * @param email {@link String}
   * @param password {@link String}
   * @return {@link AdminInitiateAuthResponse}
   */
  private AdminInitiateAuthResponse loginInternal(final String email, final String password) {
    Map<String, String> authParams = Map.of("USERNAME", email, "PASSWORD", password);

    AdminInitiateAuthRequest authRequest =
        AdminInitiateAuthRequest.builder().authFlow(AuthFlowType.ADMIN_NO_SRP_AUTH)
            .userPoolId(this.userPoolId).clientId(this.clientId).authParameters(authParams).build();

    return this.cognitoProvider.adminInitiateAuth(authRequest);
  }

  @Override
  public InitiateAuthResponse initiateAuth(final String username, final String password) {
    AuthFlowType authFlow = AuthFlowType.USER_PASSWORD_AUTH;
    Map<String, String> authMap = Map.of("USERNAME", username, "PASSWORD", password);
    InitiateAuthRequest req =
        InitiateAuthRequest.builder().authFlow(authFlow).authParameters(authMap).build();
    return this.cognitoProvider.initiateAuth(req);
  }

  @Override
  public AssociateSoftwareTokenResponse associateSoftwareToken(final String session) {
    AssociateSoftwareTokenRequest req =
        AssociateSoftwareTokenRequest.builder().session(session).build();
    return this.cognitoProvider.associateSoftwareToken(req);
  }

  @Override
  public VerifySoftwareTokenResponse verifySoftwareToken(final String session,
      final String userCode, final String deviceName) {
    VerifySoftwareTokenRequest req = VerifySoftwareTokenRequest.builder().session(session)
        .userCode(userCode).friendlyDeviceName(deviceName).build();
    return this.cognitoProvider.verifySoftwareToken(req);
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

    AuthenticationResultType authentication;
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
            .username(username).password(password).permanent(permanent).build());
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

  @Override
  public void deleteUser(final String username) {
    AdminDeleteUserRequest req =
        AdminDeleteUserRequest.builder().userPoolId(this.userPoolId).username(username).build();
    this.cognitoProvider.adminDeleteUser(req);
  }

  @Override
  public void disableUser(final String username) {
    AdminDisableUserRequest req =
        AdminDisableUserRequest.builder().userPoolId(this.userPoolId).username(username).build();
    this.cognitoProvider.adminDisableUser(req);
  }

  @Override
  public void enableUser(final String username) {
    AdminEnableUserRequest req =
        AdminEnableUserRequest.builder().userPoolId(this.userPoolId).username(username).build();
    this.cognitoProvider.adminEnableUser(req);
  }

  @Override
  public void resetUserPassword(final String username) {
    AdminResetUserPasswordRequest req = AdminResetUserPasswordRequest.builder()
        .userPoolId(this.userPoolId).username(username).build();
    this.cognitoProvider.adminResetUserPassword(req);
  }

  @Override
  public void deleteGroup(final String groupName) {
    DeleteGroupRequest req =
        DeleteGroupRequest.builder().userPoolId(this.userPoolId).groupName(groupName).build();
    this.cognitoProvider.deleteGroup(req);
  }
}
