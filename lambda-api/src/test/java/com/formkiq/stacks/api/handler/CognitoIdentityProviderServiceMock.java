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
package com.formkiq.stacks.api.handler;

import com.formkiq.aws.cognito.CognitoIdentityProviderService;
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
import software.amazon.awssdk.services.cognitoidentityprovider.model.RespondToAuthChallengeResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.UserType;
import software.amazon.awssdk.services.cognitoidentityprovider.model.VerifySoftwareTokenResponse;

import java.util.Map;

/**
 * Mock {@link CognitoIdentityProviderService}.
 */
public class CognitoIdentityProviderServiceMock implements CognitoIdentityProviderService {
  @Override
  public GetGroupResponse getGroup(final String groupName) {
    return null;
  }

  @Override
  public void disableUser(final String username) {

  }

  @Override
  public void enableUser(final String username) {

  }

  @Override
  public void resetUserPassword(final String username) {

  }

  @Override
  public ChangePasswordResponse setChangePassword(final String accessToken,
      final String previousPassword, final String proposedPassword) {
    return null;
  }

  @Override
  public ListGroupsResponse listGroups(final String token, final Integer limit) {
    return null;
  }

  @Override
  public AdminListGroupsForUserResponse listGroups(final String username, final String token,
      final Integer limit) {
    return null;
  }

  @Override
  public ForgotPasswordResponse forgotPassword(final String username) {
    return null;
  }

  @Override
  public InitiateAuthResponse loginUserPasswordAuth(final String username, final String password) {
    InitiateAuthResponse.Builder b = InitiateAuthResponse.builder();
    switch (username) {
      case "mfa" -> b.challengeName("MFA_SETUP").session("AYABeCHQdmWZNwl7Egjpk");

      default ->
        b.authenticationResult(AuthenticationResultType.builder().accessToken("ABC").build());
    }

    return b.build();
  }

  @Override
  public ConfirmForgotPasswordResponse forgotPasswordConfirm(final String username,
      final String code, final String password) {
    return null;
  }

  @Override
  public CreateGroupResponse addGroup(final String groupName, final String groupDescription) {
    return null;
  }

  @Override
  public ListUsersResponse listUsers(final String paginationToken, final Integer limit) {
    return null;
  }

  @Override
  public UserType addUser(final String username, final Map<String, String> userAttributes,
      final boolean emailVerified) {
    return null;
  }

  @Override
  public void removeUserFromGroup(final String username, final String groupname) {

  }

  @Override
  public ListUsersInGroupResponse listUsersInGroup(final String groupName, final String token,
      final Integer limit) {
    return null;
  }

  @Override
  public void addUserToGroup(final String email, final String groupname) {

  }

  @Override
  public GetUserResponse getUser(final AuthenticationResultType token) {
    return null;
  }

  @Override
  public AdminGetUserResponse getUser(final String username) {
    return null;
  }

  @Override
  public void deleteUser(final String username) {

  }

  @Override
  public void deleteGroup(final String groupName) {

  }

  @Override
  public InitiateAuthResponse initiateAuth(final String username, final String password) {
    return null;
  }

  @Override
  public AssociateSoftwareTokenResponse associateSoftwareToken(final String session) {
    return AssociateSoftwareTokenResponse.builder().secretCode("12345").session("abcdef").build();
  }

  @Override
  public VerifySoftwareTokenResponse verifySoftwareToken(final String session,
      final String userCode, final String deviceName) {
    return VerifySoftwareTokenResponse.builder().session("9873432").status("SUCCESS").build();
  }

  @Override
  public RespondToAuthChallengeResponse responseToAuthChallenge(final String session,
      final String challengeName, final Map<String, String> challengeResponses) {
    AuthenticationResultType type = AuthenticationResultType.builder().accessToken("ABC").build();
    return RespondToAuthChallengeResponse.builder().authenticationResult(type).build();
  }
}
