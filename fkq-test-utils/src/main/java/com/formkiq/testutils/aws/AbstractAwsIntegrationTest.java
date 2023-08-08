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
import com.formkiq.client.api.SystemManagementApi;
import com.formkiq.client.invoker.ApiClient;
import com.formkiq.client.invoker.ApiException;
import com.formkiq.client.model.AddApiKeyRequest;
import com.formkiq.client.model.AddApiKeyRequest.PermissionsEnum;
import com.formkiq.stacks.client.FormKiqClientV1;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AuthenticationResultType;

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
  /** FormKiQ IAM Client. */
  private static FormKiqClientV1 clientIam;
  /** Client Token {@link FormKiqClientV1}. */
  private static FormKiqClientV1 clientToken;
  /** {@link FkqCognitoService}. */
  private static FkqCognitoService cognito;
  /** Site Id. */
  public static final String SITE_ID = "8ab6a050-1fc4-11ed-861d-0242ac120002";
  /** Cognito User Password. */
  protected static final String USER_PASSWORD = "TEMPORARY_PASSWORd1!";

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
    clientToken = cognito.getFormKiqClient(adminToken);
    clientIam = cognito.getFormKiqClient();
  }

  /**
   * Get {@link ApiClient}.
   * 
   * @param siteId {@link String}
   * @return {@link List} {@link ApiClient}
   * @throws ApiException ApiException
   */
  public static List<ApiClient> getApiClients(final String siteId) throws ApiException {

    String awsprofile = System.getProperty("testprofile");

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
   * Get {@link FkqCognitoService}.
   * 
   * @return {@link FkqCognitoService}
   */
  public static FkqCognitoService getCognito() {
    return cognito;
  }

  private static void setupServices() {

    String awsprofile = System.getProperty("testprofile");
    Region awsregion = Region.of(System.getProperty("testregion"));
    String appenvironment = System.getProperty("testappenvironment");

    cognito = new FkqCognitoService(awsprofile, awsregion, appenvironment);
  }

  /**
   * Get IAM {@link FormKiqClientV1}.
   * 
   * @return {@link FormKiqClientV1}
   */
  public FormKiqClientV1 getClientIam() {
    return clientIam;
  }

  /**
   * Get {@link FormKiqClientV1}.
   * 
   * @return {@link FormKiqClientV1}
   */
  public List<FormKiqClientV1> getClients() {
    return Arrays.asList(clientToken, clientIam);
  }

  /**
   * Get Token {@link FormKiqClientV1}.
   * 
   * @return {@link FormKiqClientV1}
   */
  public FormKiqClientV1 getClientToken() {
    return clientToken;
  }
}
