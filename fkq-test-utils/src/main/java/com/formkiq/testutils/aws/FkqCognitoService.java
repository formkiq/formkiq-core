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
import com.formkiq.aws.cognito.CognitoConnectionBuilder;
import com.formkiq.aws.cognito.CognitoService;
import com.formkiq.stacks.client.FormKiqClientConnection;
import com.formkiq.stacks.client.FormKiqClientV1;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.regions.Region;
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
  /** {@link FkqSsmService}. */
  private FkqSsmService ssm;
  /** {@link CognitoService}. */
  private CognitoService service;
  /** FormKiQ Http Api Url. */
  private String rootHttpUrl;
  /** FormKiQ IAM Api Url. */
  private String rootRestUrl;
  /** FormKiQ Key Api Url. */
  private String rootKeyUrl;

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

    this.rootHttpUrl =
        this.ssm.getParameterValue("/formkiq/" + appEnvironment + "/api/DocumentsHttpUrl");

    this.rootRestUrl =
        this.ssm.getParameterValue("/formkiq/" + appEnvironment + "/api/DocumentsIamUrl");

    this.rootKeyUrl =
        this.ssm.getParameterValue("/formkiq/" + appEnvironment + "/api/DocumentsKeyUrl");

    String cognitoUserPoolId =
        this.ssm.getParameterValue("/formkiq/" + appEnvironment + "/cognito/UserPoolId");

    String cognitoClientId =
        this.ssm.getParameterValue("/formkiq/" + appEnvironment + "/cognito/UserPoolClientId");

    String cognitoIdentitypool =
        this.ssm.getParameterValue("/formkiq/" + appEnvironment + "/cognito/IdentityPoolId");

    CognitoConnectionBuilder adminBuilder =
        new CognitoConnectionBuilder(cognitoClientId, cognitoUserPoolId, cognitoIdentitypool)
            .setCredentials(awsProfile).setRegion(awsRegion);
    this.service = new CognitoService(adminBuilder);
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
   * Get FormKiQ Http Url.
   * 
   * @param token {@link AuthenticationResultType}
   * @return {@link FormKiqClientV1}
   */
  public FormKiqClientV1 getFormKiqClient(final AuthenticationResultType token) {
    FormKiqClientConnection connection = new FormKiqClientConnection(this.rootHttpUrl)
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
   * Get FormKiQ IAM Url.
   * 
   * @return {@link FormKiqClientV1}
   */
  public FormKiqClientV1 getFormKiqClient() {
    try (ProfileCredentialsProvider credentials =
        ProfileCredentialsProvider.builder().profileName(this.awsprofile).build()) {
      FormKiqClientConnection connection = new FormKiqClientConnection(this.rootRestUrl)
          .region(this.awsregion).credentials(credentials.resolveCredentials())
          .header("Origin", Arrays.asList("http://localhost"))
          .header("Access-Control-Request-Method", Arrays.asList("GET"));
      return new FormKiqClientV1(connection);
    }
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
}
