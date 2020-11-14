/**
 *
 * FormKiQ License
 *
 * Copyright (c) 2018 FormKiQ, INC
 * 
 * This code is the property of FormKiQ, INC. In the Software Development Agreement signed by both
 * FormKiQ and your company, FormKiQ grants you a limited license to use, modify, and create
 * derivative works of this code. Please consult the Software Development Agreement for the complete
 * terms under which you may use this code.
 *
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
import com.formkiq.aws.ssm.SsmConnectionBuilder;
import com.formkiq.aws.ssm.SsmService;
import com.formkiq.aws.ssm.SsmServiceImpl;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;


/** {@link RequestHandler} for installing the console. */
public class ConsoleInstallHandler implements RequestHandler<Map<String, Object>, Object> {

  /** Extra Mime Types. */
  private Map<String, String> mimeTypes = new HashMap<>();
  /** {@link S3Service}. */
  private S3Service s3;

  /** {@link SsmService}. */
  private SsmService ssm;

  /** Environment Variable {@link Map}. */
  private Map<String, String> environmentMap;

  /** constructor. */
  public ConsoleInstallHandler() {
    this(System.getenv(), new S3ConnectionBuilder().setRegion(Region.of(System.getenv("REGION"))),
        new SsmConnectionBuilder().setRegion(Region.of(System.getenv("REGION"))));
  }

  /**
   * constructor.
   *
   * @param map {@link Map}
   * @param s3builder {@link S3ConnectionBuilder}
   * @param ssmBuilder {@link SsmConnectionBuilder}
   */
  public ConsoleInstallHandler(final Map<String, String> map, final S3ConnectionBuilder s3builder,
      final SsmConnectionBuilder ssmBuilder) {
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
    this.ssm = new SsmServiceImpl(ssmBuilder);
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
        sendResponse(input, logger, context, "SUCCESS",
            "Request " + requestType + " was successful!");

      } else if (delete) {

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
   * Write Console Config including Cognito.
   *
   * @param logger {@link LambdaLogger}
   */
  private void createCognitoConfig(final LambdaLogger logger) {

    String appenv = this.environmentMap.get("appenvironment");
    String consoleversion = this.environmentMap.get("consoleversion");
    String region = this.environmentMap.get("REGION");
    String destinationBucket = getDestinationBucket(appenv, logger);
    String allowUserSelfRegistration = "false";

    String userpoolid =
        getParameterStoreValue("/formkiq/" + appenv + "/cognito/UserPoolId", logger);

    String clientid =
        getParameterStoreValue("/formkiq/" + appenv + "/cognito/UserPoolClientId", logger);

    String identitypool =
        getParameterStoreValue("/formkiq/" + appenv + "/cognito/IdentityPoolId", logger);

    String apiurl = getParameterStoreValue("/formkiq/" + appenv + "/api/DocumentsHttpUrl", logger);

    String json = String.format(
        "{%n\"version\":\"%s\",%n\"cognito\": {%n\"userPoolId\":\"%s\",%n" + "\"region\": \""
            + region + "\",%n\"clientId\":\"%s\",%n\"identityPoolId\":\"%s\","
            + "%n\"allowUserSelfRegistration\":" + allowUserSelfRegistration
            + "},\"apigateway\": {%n\"url\": \"%s\"%n}%n}",
        consoleversion, userpoolid, clientid, identitypool, apiurl);

    String fileName = consoleversion + "/config.json";

    try (S3Client s = this.s3.buildClient()) {
      this.s3.putObject(s, destinationBucket, fileName, json.getBytes(StandardCharsets.UTF_8),
          null);
    }

    logger.log("writing Cognito config: " + json);
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

    String appenv = this.environmentMap.get("appenvironment");
    String consoleversion = this.environmentMap.get("consoleversion");

    String distributionBucket = this.environmentMap.get("distributionbucket");
    String destinationBucket = getDestinationBucket(appenv, logger);

    String consoleZipKey = "formkiq-console/" + consoleversion + "/formkiq-console.zip";

    logger.log("unpacking " + consoleZipKey + " from bucket " + distributionBucket + " to bucket "
        + destinationBucket);

    try (S3Client s = this.s3.buildClient()) {
      InputStream stream = this.s3.getContentAsInputStream(s, distributionBucket, consoleZipKey);

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
   * Get Destination Bucket.
   *
   * @param appenv {@link String}
   * @param logger {@link LambdaLogger}
   * @return {@link String}
   */
  private String getDestinationBucket(final String appenv, final LambdaLogger logger) {
    String destinationBucket = getParameterStoreValue("/formkiq/" + appenv + "/s3/Console", logger);
    return destinationBucket;
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

      try (S3Client s = this.s3.buildClient()) {
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

          this.s3.putObject(s, destinationBucket, fileName, byteArray, mimeType);

          entry = zis.getNextEntry();
        }
      }
    }
  }

  /**
   * Get Parameter Store Value.
   *
   * @param parameterKey {@link String}
   * @param logger {@link LambdaLogger}
   * @return {@link String}
   */
  private String getParameterStoreValue(final String parameterKey, final LambdaLogger logger) {

    String value = this.ssm.getParameterValue(parameterKey);

    if (value == null) {
      logger.log("Property not found: " + parameterKey);
      throw new IllegalArgumentException("Cannot find Parameter Store Value: " + parameterKey);
    }

    return value;
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
}
