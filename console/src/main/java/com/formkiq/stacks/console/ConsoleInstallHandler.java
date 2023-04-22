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
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.json.JSONObject;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.formkiq.aws.s3.S3ConnectionBuilder;
import com.formkiq.aws.s3.S3Service;
import software.amazon.awssdk.regions.Region;

/** {@link RequestHandler} for installing the console. */
public class ConsoleInstallHandler implements RequestHandler<Map<String, Object>, Object> {

  /** Environment Variable {@link Map}. */
  private Map<String, String> environmentMap;
  /** Extra Mime Types. */
  private Map<String, String> mimeTypes = new HashMap<>();
  /** {@link S3Service}. */
  private S3Service s3;

  /** {@link S3Service}. */
  private S3Service s3UsEast1;

  /** constructor. */
  public ConsoleInstallHandler() {
    this(System.getenv(),
        new S3ConnectionBuilder("true".equals(System.getenv("ENABLE_AWS_X_RAY")))
            .setRegion(Region.US_EAST_1),
        new S3ConnectionBuilder("true".equals(System.getenv("ENABLE_AWS_X_RAY")))
            .setRegion(Region.of(System.getenv("REGION"))));
  }

  /**
   * constructor.
   *
   * @param map {@link Map}
   * @param s3builderUsEast1 {@link S3ConnectionBuilder}
   * @param s3builder {@link S3ConnectionBuilder}
   */
  public ConsoleInstallHandler(final Map<String, String> map,
      final S3ConnectionBuilder s3builderUsEast1, final S3ConnectionBuilder s3builder) {

    this.environmentMap = map;

    this.mimeTypes.put(".woff2", "font/woff2");
    this.mimeTypes.put(".eot", "application/vnd.ms-fontobject");
    this.mimeTypes.put(".ico", "image/x-icon");
    this.mimeTypes.put(".js", "application/javascript");
    this.mimeTypes.put(".svg", "image/svg+xml");
    this.mimeTypes.put(".ttf", "font/ttf");
    this.mimeTypes.put(".woff", "font/woff");
    this.mimeTypes.put(".css", "text/css");

    this.s3 = new S3Service(s3builder);
    this.s3UsEast1 = new S3Service(s3builderUsEast1);
  }

  /**
   * Write Console Config including Cognito.
   *
   * @param logger {@link LambdaLogger}
   */
  private void createCognitoConfig(final LambdaLogger logger) {

    String consoleVersion = this.environmentMap.get("CONSOLE_VERSION");
    String destinationBucket = this.environmentMap.get("CONSOLE_BUCKET");
    String brand = this.environmentMap.get("BRAND");
    String authApi = this.environmentMap.get("API_AUTH_URL");
    String cognitoHostedUi = this.environmentMap.get("COGNITO_HOSTED_UI");
    String userAuthentication = this.environmentMap.get("USER_AUTHENTICATION");
    String cognitoUserPoolId = this.environmentMap.get("COGNITO_USER_POOL_ID");
    String congitoClientId = this.environmentMap.get("COGNITO_USER_POOL_CLIENT_ID");

    String documentApi = this.environmentMap.get("API_URL");

    String json = String.format(
        "{%n" + "  \"documentApi\": \"%s\",%n" + "  \"userPoolId\": \"%s\",%n"
            + "  \"clientId\": \"%s\",%n" + "  \"consoleVersion\": \"%s\",%n"
            + "  \"brand\": \"%s\",%n" + "  \"userAuthentication\": \"%s\",%n"
            + "  \"authApi\": \"%s\",%n" + "  \"cognitoHostedUi\": \"%s\"%n" + "}",
        documentApi, cognitoUserPoolId, congitoClientId, consoleVersion, brand, userAuthentication,
        authApi, cognitoHostedUi);

    String fileName = consoleVersion + "/assets/config.json";

    this.s3.putObject(destinationBucket, fileName, json.getBytes(StandardCharsets.UTF_8), null);

    logger.log("writing Cognito config: " + json);
  }

  /**
   * Customize Cognito Email Templates.
   * 
   * @param logger {@link LambdaLogger}
   * @throws IOException IOException
   */
  private void createCognitoEmail(final LambdaLogger logger) throws IOException {
    String cognitoConfigBucket = this.environmentMap.get("COGNITO_CONFIG_BUCKET");
    String domain = this.environmentMap.get("DOMAIN");

    String key1 = String.format("formkiq/cognito/%s/CustomMessage_AdminCreateUser/Message", domain);

    try (InputStream is =
        getClass().getResourceAsStream("/emailtemplates/CustomMessage_AdminCreateUser/Message")) {
      this.s3.putObject(cognitoConfigBucket, key1, is, null);
    }

    String key2 = String.format("formkiq/cognito/%s/CustomMessage_SignUp/Message", domain);
    try (InputStream is =
        getClass().getResourceAsStream("/emailtemplates/CustomMessage_SignUp/Message")) {
      this.s3.putObject(cognitoConfigBucket, key2, is, null);
    }

    String key3 = String.format("formkiq/cognito/%s/CustomMessage_ForgotPassword/Message", domain);
    try (InputStream is =
        getClass().getResourceAsStream("/emailtemplates/CustomMessage_ForgotPassword/Message")) {
      this.s3.putObject(cognitoConfigBucket, key3, is, null);
    }
  }

  /**
   * Empty Console Bucket.
   * 
   * @param logger {@link LambdaLogger}
   */
  private void deleteConsole(final LambdaLogger logger) {
    String destinationBucket = this.environmentMap.get("CONSOLE_BUCKET");
    logger.log("deleting console from: " + destinationBucket);
    this.s3.deleteAllFiles(destinationBucket);

    String cognitoConfigBucket = this.environmentMap.get("COGNITO_CONFIG_BUCKET");
    logger.log("deleting cognito config from: " + cognitoConfigBucket);
    this.s3.deleteAllFiles(cognitoConfigBucket);
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
    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
    return connection;
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
      boolean unzip = requestType != null
          && ("Create".equalsIgnoreCase(requestType) || "Update".equalsIgnoreCase(requestType));
      boolean delete = requestType != null && "Delete".equalsIgnoreCase(requestType);

      if (unzip) {

        unzipConsole(input, requestType, context, logger);
        createCognitoConfig(logger);
        createCognitoEmail(logger);
        sendResponse(input, logger, context, "SUCCESS",
            "Request " + requestType + " was successful!");

      } else if (delete) {

        deleteConsole(logger);

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
   * @param input {@link Map}
   * @param requestType {@link String}
   * @param context {@link Context}
   * @param logger {@link LambdaLogger}
   */
  private void unzipConsole(final Map<String, Object> input, final String requestType,
      final Context context, final LambdaLogger logger) {

    String consoleversion = this.environmentMap.get("CONSOLE_VERSION");

    String distributionBucket = this.environmentMap.get("DISTRIBUTION_BUCKET");
    String destinationBucket = this.environmentMap.get("CONSOLE_BUCKET");

    String consoleZipKey = "formkiq-console/" + consoleversion + "/formkiq-console.zip";

    logger.log("unpacking " + consoleZipKey + " from bucket " + distributionBucket + " to bucket "
        + destinationBucket);

    InputStream stream = this.s3UsEast1.getContentAsInputStream(distributionBucket, consoleZipKey);

    try {

      writeToBucket(stream, destinationBucket, consoleversion);

    } catch (IOException e) {

      logStacktrace(context, e);
      sendResponse(input, logger, context, "FAILED", "Unable to Write files to Bucket.");

    } finally {

      try {
        stream.close();
      } catch (IOException e) {
        logger.log("cannot close stream " + e.toString());
      }
    }
  }

  /**
   * Write {@link InputStream} to Bucket.
   *
   * @param stream {@link InputStream}
   * @param destinationBucket {@link String}
   * @param consoleversion {@link String}
   * 
   * @throws IOException IOException
   */
  private void writeToBucket(final InputStream stream, final String destinationBucket,
      final String consoleversion) throws IOException {

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

        this.s3.putObject(destinationBucket, fileName, byteArray, mimeType);

        entry = zis.getNextEntry();
      }
    }
  }
}
