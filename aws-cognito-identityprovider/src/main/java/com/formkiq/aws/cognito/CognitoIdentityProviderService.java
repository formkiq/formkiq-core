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
import software.amazon.awssdk.services.cognitoidentityprovider.model.AttributeType;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AuthFlowType;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AuthenticationResultType;
import software.amazon.awssdk.services.cognitoidentityprovider.model.ChallengeNameType;
import software.amazon.awssdk.services.cognitoidentityprovider.model.CreateGroupRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.CreateGroupResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.DeleteGroupRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.GetGroupRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.GetGroupResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.GetUserRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.GetUserResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.ListGroupsRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.ListGroupsResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.ListUsersInGroupRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.ListUsersInGroupResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.ListUsersRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.ListUsersResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.UserType;

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
public class CognitoIdentityProviderService {

  /** Cognito User Attributes. */
  public static final Collection<String> COGNITO_USER_ATTRIBUTES =
      List.of("address", "birthdate", "family_name", "gender", "given_name", "locale",
          "middle_name", "name", "nickname", "phone_number", "picture", "preferred_username",
          "profile", "zoneinfo", "updated_at", "website");

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
  public CognitoIdentityProviderService(final CognitoIdentityProviderConnectionBuilder builder) {
    this.cognitoProvider = builder.build();

    this.clientId = builder.getClientId();
    this.userPoolId = builder.getUserPoolId();
  }

  /**
   * Add Cognito Group.
   * 
   * @param groupName {@link String}
   * @param groupDescription {@link String}
   * @return {@link CreateGroupResponse}
   */
  public CreateGroupResponse addGroup(final String groupName, final String groupDescription) {
    CreateGroupRequest req = CreateGroupRequest.builder().userPoolId(this.userPoolId)
        .groupName(groupName).description(groupDescription).build();
    return this.cognitoProvider.createGroup(req);
  }

  /**
   * Add Cognito User.
   *
   * @param username {@link String}
   * @param userAttributes {@link Map}
   * @param emailVerified boolean
   * @return {@link UserType}
   */
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
        attributes.add(AttributeType.builder().name(e.getKey()).value(e.getValue()).build());
      }
    }

    AdminCreateUserRequest cognitoRequest =
        AdminCreateUserRequest.builder().userPoolId(this.userPoolId).username(username)
            .temporaryPassword(temporaryPassword).userAttributes(attributes).build();

    AdminCreateUserResponse createUserResult = this.cognitoProvider.adminCreateUser(cognitoRequest);
    return createUserResult.user();
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
   * Remove Cognito User from Cognito Group.
   *
   * @param username {@link String}
   * @param groupname {@link String}
   */
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

  /**
   * List Users.
   *
   * @param paginationToken {@link String}
   * @param limit {@link Integer}
   * @return {@link ListUsersResponse}
   */
  public ListUsersResponse listUsers(final String paginationToken, final Integer limit) {
    ListUsersRequest usersRequest = ListUsersRequest.builder().userPoolId(this.userPoolId)
        .paginationToken(paginationToken).limit(limit).build();
    return this.cognitoProvider.listUsers(usersRequest);
  }

  /**
   * Get Group.
   *
   * @param groupName {@link String}.
   * @return {@link GetGroupResponse}
   */
  public GetGroupResponse getGroup(final String groupName) {
    GetGroupRequest req =
        GetGroupRequest.builder().userPoolId(this.userPoolId).groupName(groupName).build();
    return this.cognitoProvider.getGroup(req);
  }

  /**
   * Get User.
   * 
   * @param token {@link AuthenticationResultType}.
   * @return {@link GetUserResponse}
   */
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

  /**
   * List Cognito Groups.
   * 
   * @param token {@link String}
   * @param limit {@link Integer}
   * @return {@link ListGroupsResponse}
   */
  public ListGroupsResponse listGroups(final String token, final Integer limit) {
    return this.cognitoProvider.listGroups(ListGroupsRequest.builder().userPoolId(this.userPoolId)
        .nextToken(token).limit(limit).build());
  }

  /**
   * List Groups for a user.
   *
   * @param username {@link String}
   * @param token {@link String}
   * @param limit {@link Integer}
   * @return AdminListGroupsForUserResponse
   */
  public AdminListGroupsForUserResponse listGroups(final String username, final String token,
      final Integer limit) {
    AdminListGroupsForUserRequest req = AdminListGroupsForUserRequest.builder()
        .userPoolId(this.userPoolId).username(username).nextToken(token).limit(limit).build();
    return this.cognitoProvider.adminListGroupsForUser(req);
  }

  /**
   * List Users in Cognito Groups.
   * 
   * @param groupName {@link String}
   * @param token {@link String}
   * @param limit {@link Integer}
   * @return {@link ListUsersInGroupResponse}
   */
  public ListUsersInGroupResponse listUsersInGroup(final String groupName, final String token,
      final Integer limit) {
    return this.cognitoProvider.listUsersInGroup(ListUsersInGroupRequest.builder()
        .userPoolId(this.userPoolId).groupName(groupName).nextToken(token).limit(limit).build());
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
    HashMap<String, String> authParams = new HashMap<>();
    authParams.put("USERNAME", email);
    authParams.put("PASSWORD", password);

    AdminInitiateAuthRequest authRequest =
        AdminInitiateAuthRequest.builder().authFlow(AuthFlowType.ADMIN_NO_SRP_AUTH)
            .userPoolId(this.userPoolId).clientId(this.clientId).authParameters(authParams).build();

    return this.cognitoProvider.adminInitiateAuth(authRequest);
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

  public void deleteUser(final String username) {
    AdminDeleteUserRequest req =
        AdminDeleteUserRequest.builder().userPoolId(this.userPoolId).username(username).build();
    this.cognitoProvider.adminDeleteUser(req);
  }

  /**
   * Disable Username.
   * 
   * @param username {@link String}
   */
  public void disableUser(final String username) {
    AdminDisableUserRequest req =
        AdminDisableUserRequest.builder().userPoolId(this.userPoolId).username(username).build();
    this.cognitoProvider.adminDisableUser(req);
  }

  /**
   * Enable Username.
   * 
   * @param username {@link String}
   */
  public void enableUser(final String username) {
    AdminEnableUserRequest req =
        AdminEnableUserRequest.builder().userPoolId(this.userPoolId).username(username).build();
    this.cognitoProvider.adminEnableUser(req);
  }

  /**
   * Reset Password for Username.
   * 
   * @param username {@link String}
   */
  public void resetUserPassword(final String username) {
    AdminResetUserPasswordRequest req = AdminResetUserPasswordRequest.builder()
        .userPoolId(this.userPoolId).username(username).build();
    this.cognitoProvider.adminResetUserPassword(req);
  }

  /**
   * Delete Group.
   * 
   * @param groupName {@link String}
   */
  public void deleteGroup(final String groupName) {
    DeleteGroupRequest req =
        DeleteGroupRequest.builder().userPoolId(this.userPoolId).groupName(groupName).build();
    this.cognitoProvider.deleteGroup(req);
  }
}
