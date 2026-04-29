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
package com.formkiq.stacks.console;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import com.formkiq.aws.dynamodb.DynamoDbAwsServiceRegistry;
import com.formkiq.aws.s3.S3AwsServiceRegistry;
import com.formkiq.aws.s3.S3ServiceExtension;
import com.formkiq.module.lambdaservices.AwsServiceCache;
import com.formkiq.module.lambdaservices.AwsServiceCacheBuilder;
import com.formkiq.stacks.dynamodb.config.ConfigServiceExtension;
import org.json.JSONObject;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.formkiq.aws.s3.S3Service;
import com.formkiq.module.http.HttpService;
import com.formkiq.module.http.HttpServiceJdk11;
import com.formkiq.stacks.dynamodb.config.ConfigService;
import com.formkiq.stacks.dynamodb.config.SiteConfiguration;
import com.formkiq.stacks.dynamodb.config.SiteConfigurationWebUi;
import com.formkiq.urls.HttpStatus;
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;

/** {@link RequestHandler} for installing the console. */
public class ConsoleInstallHandler implements RequestHandler<Map<String, Object>, Object> {

  /** {@link AwsServiceCache}. */
  private static AwsServiceCache serviceCache;

  private static void initialize(final AwsServiceCache cache) {
    cache.register(ConfigService.class, new ConfigServiceExtension());
    cache.register(S3Service.class, new S3ServiceExtension());
    serviceCache = cache;
  }

  /** {@link HttpService}. */
  private final HttpService http = new HttpServiceJdk11();

  /** Extra Mime Types. */
  private final Map<String, String> mimeTypes = createMimeTypes();

  // static {
  //
  // AwsServiceCache cache = new AwsServiceCacheBuilder(System.getenv(), Map.of(),
  // EnvironmentVariableCredentialsProvider.create())
  // .addService(new DynamoDbAwsServiceRegistry(), new S3AwsServiceRegistry())
  // .build();
  //
  // initialize(cache);
  // }

  /** constructor. */
  public ConsoleInstallHandler() {
    AwsServiceCache cache = new AwsServiceCacheBuilder(System.getenv(), Map.of(),
        EnvironmentVariableCredentialsProvider.create())
        .addService(new DynamoDbAwsServiceRegistry(), new S3AwsServiceRegistry()).build();

    initialize(cache);
  }

  /**
   * constructor.
   *
   * @param cache {@link AwsServiceCache}
   */
  ConsoleInstallHandler(final AwsServiceCache cache) {
    initialize(cache);
  }

  /**
   * Write Console Config including Cognito.
   *
   * @param logger {@link LambdaLogger}
   * @param s3 {@link S3Service}
   */
  private void createCognitoConfig(final LambdaLogger logger, final S3Service s3) {

    String consoleVersion = serviceCache.environment("CONSOLE_VERSION");
    String destinationBucket = serviceCache.environment("CONSOLE_BUCKET");
    String brand = serviceCache.environment("BRAND");
    String authApi = serviceCache.environment("API_AUTH_URL");
    String cognitoHostedUi = serviceCache.environment("COGNITO_HOSTED_UI");
    String userAuthentication = serviceCache.environment("USER_AUTHENTICATION");
    String cognitoUserPoolId = serviceCache.environment("COGNITO_USER_POOL_ID");
    String congitoClientId = serviceCache.environment("COGNITO_USER_POOL_CLIENT_ID");

    String documentApi = serviceCache.environment("API_URL");
    String apiIamUrl = serviceCache.environment("API_IAM_URL");
    String apiKeyUrl = serviceCache.environment("API_KEY_URL");
    String cognitoSingleSignOnUrl = serviceCache.environment("COGNITO_SINGLE_SIGN_ON_URL");
    if (cognitoSingleSignOnUrl == null) {
      cognitoSingleSignOnUrl = "";
    }
    boolean ssoLoginRedirectEnabled = isSsoLoginRedirectEnabled();

    String json = String.format(
        "{%n" + "  \"documentApi\": \"%s\",%n" + "  \"userPoolId\": \"%s\",%n"
            + "  \"apiIamUrl\": \"%s\",%n" + "  \"apiKeyUrl\": \"%s\",%n"
            + "  \"clientId\": \"%s\",%n" + "  \"consoleVersion\": \"%s\",%n"
            + "  \"brand\": \"%s\",%n" + "  \"userAuthentication\": \"%s\",%n"
            + "  \"authApi\": \"%s\",%n" + "  \"cognitoHostedUi\": \"%s\",%n"
            + "  \"awsRegion\": \"%s\",%n" + "  \"cognitoSingleSignOnUrl\": \"%s\",%n"
            + "  \"ssoAutomaticSignIn\": %s%n" + "}",
        documentApi, cognitoUserPoolId, apiIamUrl, apiKeyUrl, congitoClientId, consoleVersion,
        brand, userAuthentication, authApi, cognitoHostedUi, serviceCache.region(),
        cognitoSingleSignOnUrl, ssoLoginRedirectEnabled);

    String fileName = consoleVersion + "/assets/config.json";

    s3.putObject(destinationBucket, fileName, json.getBytes(StandardCharsets.UTF_8), null, null);

    logger.log("writing Cognito config: " + json);
  }

  /**
   * Customize Cognito Email Templates.
   *
   * @param logger {@link LambdaLogger}
   * @param s3 {@link S3Service}
   * @throws IOException IOException
   */
  private void createCognitoEmail(final LambdaLogger logger, final S3Service s3)
      throws IOException {
    String cognitoConfigBucket = serviceCache.environment("COGNITO_CONFIG_BUCKET");
    String domain = serviceCache.environment("DOMAIN");

    String key1 = String.format("formkiq/cognito/%s/CustomMessage_AdminCreateUser/Message", domain);
    logger.log("writing cognito email key1 " + key1);

    try (InputStream is =
        getClass().getResourceAsStream("/emailtemplates/CustomMessage_AdminCreateUser/Message")) {
      s3.putObject(cognitoConfigBucket, key1, is, null);
    }

    String key2 = String.format("formkiq/cognito/%s/CustomMessage_SignUp/Message", domain);
    logger.log("writing cognito email key2 " + key2);
    try (InputStream is =
        getClass().getResourceAsStream("/emailtemplates/CustomMessage_SignUp/Message")) {
      s3.putObject(cognitoConfigBucket, key2, is, null);
    }

    String key3 = String.format("formkiq/cognito/%s/CustomMessage_ForgotPassword/Message", domain);
    logger.log("writing cognito email key3 " + key3);
    try (InputStream is =
        getClass().getResourceAsStream("/emailtemplates/CustomMessage_ForgotPassword/Message")) {
      s3.putObject(cognitoConfigBucket, key3, is, null);
    }
  }

  private Map<String, String> createMimeTypes() {
    Map<String, String> map = new HashMap<>();
    map.put(".woff2", "font/woff2");
    map.put(".eot", "application/vnd.ms-fontobject");
    map.put(".ico", "image/x-icon");
    map.put(".js", "application/javascript");
    map.put(".svg", "image/svg+xml");
    map.put(".ttf", "font/ttf");
    map.put(".woff", "font/woff");
    map.put(".css", "text/css");
    return map;
  }

  /**
   * Empty Console Bucket.
   *
   * @param logger {@link LambdaLogger}
   * @param s3 {@link S3Service}
   */
  private void deleteConsole(final LambdaLogger logger, final S3Service s3) {

    String destinationBucket = serviceCache.environment("CONSOLE_BUCKET");
    logger.log("deleting console from: " + destinationBucket);
    s3.deleteAllFiles(destinationBucket, true);

    String cognitoConfigBucket = serviceCache.environment("COGNITO_CONFIG_BUCKET");
    logger.log("deleting cognito config from: " + cognitoConfigBucket);
    s3.deleteAllFiles(cognitoConfigBucket, true);
  }

  /**
   * Get {@link HttpURLConnection}.
   *
   * @param responseUrl {@link String}
   * @return {@link HttpURLConnection}
   * @throws IOException IOException
   */
  protected HttpURLConnection getConnection(final String responseUrl) throws IOException {
    URL url = new URL(responseUrl);
    return (HttpURLConnection) url.openConnection();
  }

  /**
   * Get console zip as {@link InputStream}.
   *
   * @param consoleZipUrl {@link String}
   * @return {@link InputStream}
   * @throws IOException IOException
   */
  protected InputStream getConsoleZipInputStream(final String consoleZipUrl) throws IOException {

    HttpResponse<InputStream> response =
        this.http.getAsInputStream(consoleZipUrl, Optional.empty(), Optional.empty());

    if (response.statusCode() < HttpStatus.OK
        || response.statusCode() >= HttpStatus.MULTIPLE_CHOICES) {
      if (response.body() != null) {
        response.body().close();
      }
      throw new IOException("failed to download console zip from " + consoleZipUrl
          + ", status code " + response.statusCode());
    }

    return response.body();
  }

  /**
   * Get console zip URL.
   *
   * @return {@link String}
   */
  protected String getConsoleZipUrl() {
    return serviceCache.environment("CONSOLE_ZIP_URL");
  }

  /**
   * Handle Console Installation.
   *
   * @param input {@link Map}
   * @param context {@link Context}
   * @return {@link Object}
   */
  @Override
  public Object handleRequest(final Map<String, Object> input, final Context context) {

    LambdaLogger logger = context.getLogger();
    logger.log("received input: " + input);

    try {
      final String requestType = (String) input.get("RequestType");
      boolean unzip =
          ("Create".equalsIgnoreCase(requestType) || "Update".equalsIgnoreCase(requestType));
      boolean delete = "Delete".equalsIgnoreCase(requestType);

      var s3 = serviceCache.getExtension(S3Service.class);

      if (unzip) {

        unzipConsole(s3, input, context, logger);
        createCognitoConfig(logger, s3);
        createCognitoEmail(logger, s3);
        sendResponse(input, logger, context, "SUCCESS",
            "Request " + requestType + " was successful!");

      } else if (delete) {

        deleteConsole(logger, s3);

        sendResponse(input, logger, context, "SUCCESS",
            "Request " + requestType + " was successful!");

      } else {

        sendResponse(input, logger, context, "FAILURE",
            "received RequestType " + requestType + " skipping unpacking");
      }

    } catch (Exception e) {

      StringWriter sw = new StringWriter();
      PrintWriter pw = new PrintWriter(sw);
      e.printStackTrace(pw);

      sendResponse(input, logger, context, "FAILURE", sw.toString());
    }

    return null;
  }

  private boolean isSsoLoginRedirectEnabled() {
    ConfigService configService = serviceCache.getExtension(ConfigService.class);
    SiteConfiguration config = configService.get("global");
    SiteConfigurationWebUi webui = config.webui();
    return webui != null && webui.ssoAutomaticSignIn();
  }

  /**
   * Log Stack Trace.
   *
   * @param context {@link Context}
   * @param ex {@link Exception}
   */
  private void logStacktrace(final Context context, final Exception ex) {
    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);
    ex.printStackTrace(pw);
    context.getLogger().log(sw.toString());
  }

  /**
   * Send a response to CloudFormation regarding progress in creating resource.
   *
   * @param input {@link Map}
   * @param logger {@link LambdaLogger}
   * @param context {@link Context}
   * @param responseStatus {@link String}
   * @param message {@link String}
   */
  private void sendResponse(final Map<String, Object> input, final LambdaLogger logger,
      final Context context, final String responseStatus, final String message) {

    logger.log(message);

    try {

      JSONObject responseData = new JSONObject();
      responseData.put("Message", message);

      JSONObject responseBody = new JSONObject();
      responseBody.put("Status", responseStatus);
      responseBody.put("PhysicalResourceId",
          input.get("StackId") + "-" + input.get("LogicalResourceId"));
      responseBody.put("StackId", input.get("StackId"));
      responseBody.put("RequestId", input.get("RequestId"));
      responseBody.put("LogicalResourceId", input.get("LogicalResourceId"));
      responseBody.put("Data", responseData);

      final String responseBodyString = responseBody.toString();

      String responseUrl = (String) input.get("ResponseURL");
      logger.log("sending " + responseStatus + " to " + responseUrl);

      HttpURLConnection connection = getConnection(responseUrl);
      connection.setDoOutput(true);
      connection.setRequestMethod("PUT");
      connection.setRequestProperty("Content-Type", "");
      connection.setRequestProperty("Content-Length", "" + responseBodyString.length());

      OutputStreamWriter response =
          new OutputStreamWriter(connection.getOutputStream(), StandardCharsets.UTF_8);

      response.write(responseBodyString);
      response.close();

      logger.log("Response Code: " + connection.getResponseCode());

    } catch (IOException e) {
      logger.log("Unable to send response.");
      logStacktrace(context, e);
    }
  }

  /**
   * Unzip Console file.
   *
   * @param s3 {@link S3Service}
   * @param input {@link Map}
   * @param context {@link Context}
   * @param logger {@link LambdaLogger}
   */
  private void unzipConsole(final S3Service s3, final Map<String, Object> input,
      final Context context, final LambdaLogger logger) {

    String consoleversion = serviceCache.environment("CONSOLE_VERSION");
    String destinationBucket = serviceCache.environment("CONSOLE_BUCKET");
    String consoleZipUrl = getConsoleZipUrl();
    logger.log("unpacking " + consoleZipUrl + " to bucket " + destinationBucket);

    try (InputStream stream = getConsoleZipInputStream(consoleZipUrl)) {
      writeToBucket(s3, stream, destinationBucket, consoleversion);
    } catch (IOException e) {

      logStacktrace(context, e);
      sendResponse(input, logger, context, "FAILED", "Unable to Write files to Bucket.");
    }
  }

  /**
   * Write {@link InputStream} to Bucket.
   *
   * @param s3 {@link S3Service}
   * @param stream {@link InputStream}
   * @param destinationBucket {@link String}
   * @param consoleversion {@link String}
   *
   * @throws IOException IOException
   */
  private void writeToBucket(final S3Service s3, final InputStream stream,
      final String destinationBucket, final String consoleversion) throws IOException {

    try (ZipInputStream zis = new ZipInputStream(stream)) {

      ZipEntry entry = zis.getNextEntry();

      while (entry != null) {

        String fileName = consoleversion + "/" + entry.getName();
        String mimeType = URLConnection.guessContentTypeFromName(fileName);

        if (mimeType == null) {
          for (Map.Entry<String, String> e : this.mimeTypes.entrySet()) {
            if (fileName.endsWith(e.getKey())) {
              mimeType = e.getValue();
              break;
            }
          }
        }

        byte[] byteArray = S3Service.toByteArray(zis);

        s3.putObject(destinationBucket, fileName, byteArray, mimeType, null);

        entry = zis.getNextEntry();
      }
    }
  }
}
