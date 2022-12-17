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

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import com.formkiq.stacks.client.FormKiqClientV1;
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

    AuthenticationResultType token = cognito.login(ADMIN_EMAIL, USER_PASSWORD);
    clientToken = cognito.getFormKiqClient(token);
    clientIam = cognito.getFormKiqClient();
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

  private static void setupServices() {

    String awsprofile = System.getProperty("testprofile");
    Region awsregion = Region.of(System.getProperty("testregion"));
    String appenvironment = System.getProperty("testappenvironment");

    cognito = new FkqCognitoService(awsprofile, awsregion, appenvironment);
  }
}
