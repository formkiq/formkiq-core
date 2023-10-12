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
package com.formkiq.testutils.aws;

import java.util.Arrays;
import com.formkiq.aws.cognito.CognitoIdentityProviderConnectionBuilder;
import com.formkiq.aws.cognito.CognitoIdentityProviderService;
import com.formkiq.stacks.client.FormKiqClientConnection;
import com.formkiq.stacks.client.FormKiqClientV1;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminGetUserResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AuthenticationResultType;
import software.amazon.awssdk.services.cognitoidentityprovider.model.UserType;

/**
 * 
 * FormKiQ Cognito Service.
 *
 */
public class FkqCognitoService {
  /** AWS Profile. */
  private String awsprofile;
  /** {@link Region}. */
  private Region awsregion;
  /** FormKiQ IAM Api Url. */
  private String rootIamUrl;
  /** FormKiQ Http Api Url. */
  private String rootJwtUrl;
  /** FormKiQ Key Api Url. */
  private String rootKeyUrl;
  /** {@link CognitoIdentityProviderService}. */
  private CognitoIdentityProviderService service;
  /** {@link FkqSsmService}. */
  private FkqSsmService ssm;

  /**
   * constructor.
   * 
   * @param awsProfile {@link String}
   * @param awsRegion {@link Region}
   * @param appEnvironment {@link String}
   */
  public FkqCognitoService(final String awsProfile, final Region awsRegion,
      final String appEnvironment) {

    this.awsprofile = awsProfile;
    this.awsregion = awsRegion;
    this.ssm = new FkqSsmService(awsProfile, awsRegion);

    this.rootJwtUrl =
        this.ssm.getParameterValue("/formkiq/" + appEnvironment + "/api/DocumentsHttpUrl");

    this.rootIamUrl =
        this.ssm.getParameterValue("/formkiq/" + appEnvironment + "/api/DocumentsIamUrl");

    this.rootKeyUrl =
        this.ssm.getParameterValue("/formkiq/" + appEnvironment + "/api/DocumentsKeyUrl");

    String cognitoUserPoolId =
        this.ssm.getParameterValue("/formkiq/" + appEnvironment + "/cognito/UserPoolId");

    String cognitoClientId =
        this.ssm.getParameterValue("/formkiq/" + appEnvironment + "/cognito/UserPoolClientId");

    CognitoIdentityProviderConnectionBuilder builder =
        new CognitoIdentityProviderConnectionBuilder(cognitoClientId, cognitoUserPoolId)
            .setCredentials(awsProfile).setRegion(awsRegion);

    this.service = new CognitoIdentityProviderService(builder);
  }

  /**
   * Add Cognito Group.
   * 
   * @param groupname {@link String}
   */
  public void addGroup(final String groupname) {
    this.service.addGroup(groupname);
  }

  /**
   * Add Cognito User.
   * 
   * @param email {@link String}
   * @param password {@link String}
   * @return {@link UserType}
   */
  public UserType addUser(final String email, final String password) {
    UserType userType = null;
    if (!this.service.isUserExists(email)) {
      String tempPassword = "!" + password + "!";
      userType = this.service.addUser(email, tempPassword);
      this.service.loginWithNewPassword(email, tempPassword, password);
    }
    return userType;
  }

  /**
   * Add Cognito User to Cognito Group.
   * 
   * @param email {@link String}
   * @param groupname {@link String}
   */
  public void addUserToGroup(final String email, final String groupname) {
    this.service.addUserToGroup(email, groupname);
  }

  /**
   * Get AWS Region.
   * 
   * @return {@link Region}
   */
  public Region getAwsregion() {
    return this.awsregion;
  }

  /**
   * Get FormKiQ IAM Url.
   * 
   * @return {@link FormKiqClientV1}
   */
  public FormKiqClientV1 getFormKiqClient() {
    try (ProfileCredentialsProvider credentials =
        ProfileCredentialsProvider.builder().profileName(this.awsprofile).build()) {
      FormKiqClientConnection connection = new FormKiqClientConnection(this.rootIamUrl)
          .region(this.awsregion).credentials(credentials.resolveCredentials())
          .header("Origin", Arrays.asList("http://localhost"))
          .header("Access-Control-Request-Method", Arrays.asList("GET"));
      return new FormKiqClientV1(connection);
    }
  }

  /**
   * Get FormKiQ Http Url.
   * 
   * @param token {@link AuthenticationResultType}
   * @return {@link FormKiqClientV1}
   */
  public FormKiqClientV1 getFormKiqClient(final AuthenticationResultType token) {
    FormKiqClientConnection connection = new FormKiqClientConnection(this.rootJwtUrl)
        .cognitoIdToken(token.idToken()).header("Origin", Arrays.asList("http://localhost"))
        .header("Access-Control-Request-Method", Arrays.asList("GET"));
    return new FormKiqClientV1(connection);
  }

  /**
   * Get FormKiQ Key API Url.
   * 
   * @param apiKey {@link String}
   * @return {@link FormKiqClientV1}
   */
  public FormKiqClientV1 getFormKiqClient(final String apiKey) {
    FormKiqClientConnection connection = new FormKiqClientConnection(this.rootKeyUrl)
        .cognitoIdToken(apiKey).header("Origin", Arrays.asList("http://localhost"))
        .header("Access-Control-Request-Method", Arrays.asList("GET"));
    return new FormKiqClientV1(connection);
  }

  /**
   * Get Iam Url.
   * 
   * @return {@link String}
   */
  public String getRootIamUrl() {
    return this.rootIamUrl;
  }

  /**
   * Get Root Http Url.
   * 
   * @return {@link String}
   */
  public String getRootJwtUrl() {
    return this.rootJwtUrl;
  }

  /**
   * Get Root Key Url.
   * 
   * @return {@link String}
   */
  public String getRootKeyUrl() {
    return this.rootKeyUrl;
  }

  /**
   * Get User.
   * 
   * @param username {@link String}
   * @return {@link AdminGetUserResponse}
   */
  public AdminGetUserResponse getUser(final String username) {
    return this.service.getUser(username);
  }

  /**
   * Does Cognito User Exist.
   * 
   * @param email {@link String}
   * @return boolean
   */
  public boolean isUserExists(final String email) {
    return this.service.isUserExists(email);
  }

  /**
   * Login User in NEW_PASSWORD_REQUIRED status.
   * 
   * @param email {@link String}
   * @param password {@link String}
   * @return {@link AuthenticationResultType}
   */
  public AuthenticationResultType login(final String email, final String password) {
    return this.service.login(email, password);
  }

  /**
   * Login User in NEW_PASSWORD_REQUIRED status.
   * 
   * @param email {@link String}
   * @param password {@link String}
   * @param newpassword {@link String}
   */
  public void loginWithNewPassword(final String email, final String password,
      final String newpassword) {
    this.service.loginWithNewPassword(email, password, newpassword);
  }
}
