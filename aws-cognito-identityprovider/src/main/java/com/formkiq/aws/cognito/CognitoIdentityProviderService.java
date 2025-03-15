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

import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminGetUserResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminListGroupsForUserResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AssociateSoftwareTokenResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AuthenticationResultType;
import software.amazon.awssdk.services.cognitoidentityprovider.model.ChangePasswordResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.ConfirmForgotPasswordResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.CreateGroupResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.ForgotPasswordResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.GetGroupResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.GetUserResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.InitiateAuthResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.ListGroupsResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.ListUsersInGroupResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.ListUsersResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.UserType;
import software.amazon.awssdk.services.cognitoidentityprovider.model.VerifySoftwareTokenResponse;

import java.util.Map;

/**
 * Cognito Identity Provier Service.
 */
public interface CognitoIdentityProviderService {

  /**
   * Get Group.
   *
   * @param groupName {@link String}.
   * @return {@link GetGroupResponse}
   */
  GetGroupResponse getGroup(String groupName);

  /**
   * Disable Username.
   *
   * @param username {@link String}
   */
  void disableUser(String username);

  /**
   * Enable Username.
   *
   * @param username {@link String}
   */
  void enableUser(String username);

  /**
   * Reset Password for Username.
   *
   * @param username {@link String}
   */
  void resetUserPassword(String username);

  /**
   * Change User Password.
   *
   * @param accessToken {@link String}
   * @param previousPassword {@link String}
   * @param proposedPassword {@link String}
   * @return ChangePasswordResponse
   */
  ChangePasswordResponse setChangePassword(String accessToken, String previousPassword,
      String proposedPassword);

  /**
   * List Cognito Groups.
   *
   * @param token {@link String}
   * @param limit {@link Integer}
   * @return {@link ListGroupsResponse}
   */
  ListGroupsResponse listGroups(String token, Integer limit);

  /**
   * List Groups for a user.
   *
   * @param username {@link String}
   * @param token {@link String}
   * @param limit {@link Integer}
   * @return AdminListGroupsForUserResponse
   */
  AdminListGroupsForUserResponse listGroups(String username, String token, Integer limit);

  /**
   * Sends Forgot Password Reset.
   *
   * @param username {@link String}
   * @return ForgotPasswordResponse
   */
  ForgotPasswordResponse forgotPassword(String username);

  /**
   * Login User using User Password Auth.
   *
   * @param email {@link String}
   * @param password {@link String}
   * @return {@link AuthenticationResultType}
   */
  InitiateAuthResponse loginUserPasswordAuth(String email, String password);

  /**
   * Confirms Forgot Password Reset.
   *
   * @param username {@link String}
   * @param code {@link String}
   * @param password {@link String}
   * @return ForgotPasswordResponse
   */
  ConfirmForgotPasswordResponse forgotPasswordConfirm(String username, String code,
      String password);

  /**
   * Add Cognito Group.
   *
   * @param groupName {@link String}
   * @param groupDescription {@link String}
   * @return {@link CreateGroupResponse}
   */
  CreateGroupResponse addGroup(String groupName, String groupDescription);

  /**
   * List Users.
   *
   * @param paginationToken {@link String}
   * @param limit {@link Integer}
   * @return {@link ListUsersResponse}
   */
  ListUsersResponse listUsers(String paginationToken, Integer limit);

  /**
   * Add Cognito User.
   *
   * @param username {@link String}
   * @param userAttributes {@link Map}
   * @param emailVerified boolean
   * @return {@link UserType}
   */
  UserType addUser(String username, Map<String, String> userAttributes, boolean emailVerified);

  /**
   * Remove Cognito User from Cognito Group.
   *
   * @param username {@link String}
   * @param groupname {@link String}
   */
  void removeUserFromGroup(String username, String groupname);

  /**
   * List Users in Cognito Groups.
   *
   * @param groupName {@link String}
   * @param token {@link String}
   * @param limit {@link Integer}
   * @return {@link ListUsersInGroupResponse}
   */
  ListUsersInGroupResponse listUsersInGroup(String groupName, String token, Integer limit);

  /**
   * Add Cognito User to Cognito Group.
   *
   * @param email {@link String}
   * @param groupname {@link String}
   */
  void addUserToGroup(String email, String groupname);

  /**
   * Get User.
   *
   * @param token {@link AuthenticationResultType}.
   * @return {@link GetUserResponse}
   */
  GetUserResponse getUser(AuthenticationResultType token);

  /**
   * Get User.
   *
   * @param username {@link String}
   * @return {@link AdminGetUserResponse}
   */
  AdminGetUserResponse getUser(String username);

  /**
   * Delete User.
   * 
   * @param username {@link String}
   */
  void deleteUser(String username);

  /**
   * Delete Group.
   *
   * @param groupName {@link String}
   */
  void deleteGroup(String groupName);

  /**
   * Login User.
   *
   * @param username {@link String}
   * @param password {@link String}
   * @return InitiateAuthResponse
   */
  InitiateAuthResponse initiateAuth(String username, String password);

  /**
   * Begins setup of time-based one-time password (TOTP) multi-factor authentication (MFA) for a
   * user.
   *
   * @param session {@link String}
   * @return AssociateSoftwareTokenResponse
   */
  AssociateSoftwareTokenResponse associateSoftwareToken(String session);

  /**
   * Verify Software Token.
   *
   * @param session {@link String}
   * @param userCode {@link String}
   * @param deviceName {@link String}
   * @return VerifySoftwareTokenResponse
   */
  VerifySoftwareTokenResponse verifySoftwareToken(String session, String userCode,
      String deviceName);
}
