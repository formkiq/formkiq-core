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

import static com.formkiq.aws.dynamodb.SiteIdKeyGenerator.DEFAULT_SITE_ID;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import com.formkiq.aws.s3.S3PresignerService;
import com.formkiq.aws.s3.S3Service;
import com.formkiq.aws.sqs.SqsService;
import com.formkiq.aws.ssm.SsmService;
import com.formkiq.client.api.SystemManagementApi;
import com.formkiq.client.invoker.ApiClient;
import com.formkiq.client.invoker.ApiException;
import com.formkiq.client.model.AddApiKeyRequest;
import com.formkiq.client.model.AddApiKeyRequest.PermissionsEnum;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminGetUserResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AuthenticationResultType;
import software.amazon.awssdk.services.cognitoidentityprovider.model.UserStatusType;

/**
 * 
 * Abstract class of AWS Integration Tests.
 *
 */
public abstract class AbstractAwsIntegrationTest {

  /** Cognito User Email. */
  private static final String ADMIN_EMAIL = "testadminuser123@formkiq.com";
  /** {@link AuthenticationResultType}. */
  private static AuthenticationResultType adminToken;
  /** FormKiQ KEY API Client. */
  private static Map<String, String> apiKeys = new HashMap<>();
  /** Get App Environment. */
  private static String appenvironment;
  /** {@link String}. */
  private static String awsprofile;
  /** {@link Region}. */
  private static Region awsregion;
  /** {@link FkqCognitoService}. */
  private static FkqCognitoService cognito;
  /** {@link S3Service}. */
  private static S3Service s3;
  /** {@link S3PresignerService}. */
  private static S3PresignerService s3Presigner;
  /** Site Id. */
  public static final String SITE_ID = "8ab6a050-1fc4-11ed-861d-0242ac120002";
  /** {@link SqsService}. */
  private static SqsService sqs;
  /** {@link SsmService}. */
  private static SsmService ssm;
  /** Temporary Cognito Password. */
  private static final String TEMP_USER_PASSWORD = "TEMPORARY_PASSWORd1!";
  /** Cognito User Password. */
  protected static final String USER_PASSWORD = "TEMPORARY_PASSWORd1!";

  /**
   * Add User and/or Login Cognito.
   * 
   * @param username {@link String}
   * @param groupNames {@link List} {@link String}
   */
  public static void addAndLoginCognito(final String username, final List<String> groupNames) {

    if (!getCognito().isUserExists(username)) {

      getCognito().addUser(username, USER_PASSWORD);
      // getCognito().loginWithNewPassword(username, TEMP_USER_PASSWORD, USER_PASSWORD);

      for (String groupName : groupNames) {
        if (!groupName.startsWith(DEFAULT_SITE_ID) && !"authentication_only".equals(groupName)) {

          getCognito().addGroup(groupName);
        }
        getCognito().addUserToGroup(username, groupName);
      }

    } else {

      AdminGetUserResponse user = getCognito().getUser(username);
      if (UserStatusType.FORCE_CHANGE_PASSWORD.equals(user.userStatus())) {
        getCognito().loginWithNewPassword(username, TEMP_USER_PASSWORD, USER_PASSWORD);
      }
    }
  }

  /**
   * beforeclass.
   * 
   * @throws IOException IOException
   * @throws InterruptedException InterruptedException
   * @throws URISyntaxException URISyntaxException
   */
  @BeforeAll
  public static void beforeClass() throws IOException, InterruptedException, URISyntaxException {
    setupServices();

    cognito.addUser(ADMIN_EMAIL, USER_PASSWORD);
    cognito.addUserToGroup(ADMIN_EMAIL, "Admins");

    adminToken = cognito.login(ADMIN_EMAIL, USER_PASSWORD);
  }

  /**
   * Get Admin Token.
   * 
   * @return {@link AuthenticationResultType}
   */
  public static AuthenticationResultType getAdminToken() {
    return adminToken;
  }

  /**
   * Get {@link ApiClient}.
   * 
   * @param siteId {@link String}
   * @return {@link List} {@link ApiClient}
   * @throws ApiException ApiException
   */
  public static List<ApiClient> getApiClients(final String siteId) throws ApiException {

    try (ProfileCredentialsProvider p = ProfileCredentialsProvider.create(awsprofile)) {

      ApiClient jwtClient = new ApiClient().setReadTimeout(0).setBasePath(cognito.getRootJwtUrl());
      jwtClient.addDefaultHeader("Authorization", adminToken.accessToken());

      AwsCredentials credentials = p.resolveCredentials();

      ApiClient iamClient = new ApiClient().setReadTimeout(0).setBasePath(cognito.getRootIamUrl());
      iamClient.setAWS4Configuration(credentials.accessKeyId(), credentials.secretAccessKey(),
          cognito.getAwsregion().toString(), "execute-api");

      ApiClient keyClient = new ApiClient().setReadTimeout(0).setBasePath(cognito.getRootKeyUrl());
      String token = getApiKey(iamClient, siteId);
      keyClient.addDefaultHeader("Authorization", token);

      return Arrays.asList(jwtClient, iamClient, keyClient);
    }
  }

  /**
   * Get API Key for {@link String}.
   * 
   * @param client {@link ApiClient}
   * @param siteId {@link String}
   * @return {@link String}
   * @throws ApiException ApiException
   */
  private static String getApiKey(final ApiClient client, final String siteId) throws ApiException {

    String site = siteId != null ? siteId : DEFAULT_SITE_ID;

    if (!apiKeys.containsKey(site)) {

      SystemManagementApi api = new SystemManagementApi(client);

      List<PermissionsEnum> permissions =
          Arrays.asList(PermissionsEnum.READ, PermissionsEnum.DELETE, PermissionsEnum.WRITE);
      AddApiKeyRequest req = new AddApiKeyRequest().name("My Api Key").permissions(permissions);
      String apiKey = api.addApiKey(req, siteId).getApiKey();

      apiKeys.put(site, apiKey);
    }

    return apiKeys.get(site);
  }

  /**
   * Get App Environment.
   * 
   * @return {@link String}
   */
  public static String getAppenvironment() {
    return appenvironment;
  }

  /**
   * Get Aws Profile.
   * 
   * @return {@link String}
   */
  public static String getAwsprofile() {
    return awsprofile;
  }

  /**
   * Get Aws Region.
   * 
   * @return {@link Region}
   */
  public static Region getAwsregion() {
    return awsregion;
  }

  /**
   * Get {@link FkqCognitoService}.
   * 
   * @return {@link FkqCognitoService}
   */
  public static FkqCognitoService getCognito() {
    return cognito;
  }

  /**
   * Get {@link S3Service}.
   * 
   * @return {@link S3Service}
   */
  public static S3Service getS3() {
    return s3;
  }

  /**
   * Get {@link S3PresignerService}.
   * 
   * @return {@link S3PresignerService}
   */
  public static S3PresignerService getS3Presigner() {
    return s3Presigner;
  }

  /**
   * Get {@link SqsService}.
   * 
   * @return {@link SqsService}
   */
  public static SqsService getSqs() {
    return sqs;
  }

  /**
   * Get {@link SsmService}.
   * 
   * @return {@link SsmService}
   */
  public static SsmService getSsm() {
    return ssm;
  }

  private static void setupServices() {

    awsprofile = System.getProperty("testprofile");
    awsregion = Region.of(System.getProperty("testregion"));
    appenvironment = System.getProperty("testappenvironment");

    cognito = new FkqCognitoService(awsprofile, awsregion, appenvironment);
    ssm = new FkqSsmService(awsprofile, awsregion);
    sqs = new FkqSqsService(awsprofile, awsregion);
    s3 = new FkqS3Service(awsprofile, awsregion);
    s3Presigner = new FkqS3PresignerService(awsprofile, awsregion);
  }

  /**
   * Get Api Client for User.
   * 
   * @param email {@link String}
   * @param password {@link String}
   * @return {@link ApiClient}
   */
  public ApiClient getApiClientForUser(final String email, final String password) {
    AuthenticationResultType token = getCognito().login(email, password);
    ApiClient jwtClient = new ApiClient().setReadTimeout(0).setBasePath(cognito.getRootJwtUrl());
    jwtClient.addDefaultHeader("Authorization", token.accessToken());
    return jwtClient;
  }
}
