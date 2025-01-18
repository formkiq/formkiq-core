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

import static com.formkiq.aws.dynamodb.SiteIdKeyGenerator.createS3Key;
import static com.formkiq.aws.dynamodb.objects.Objects.throwIfNull;
import static com.formkiq.aws.services.lambda.ApiResponseStatus.SC_NOT_FOUND;
import static com.formkiq.aws.services.lambda.ApiResponseStatus.SC_OK;
import static software.amazon.awssdk.utils.StringUtils.isEmpty;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import com.formkiq.aws.dynamodb.model.DocumentItem;
import com.formkiq.aws.dynamodb.objects.MimeType;
import com.formkiq.aws.dynamodb.objects.Strings;
import com.formkiq.aws.s3.PresignGetUrlConfig;
import com.formkiq.aws.s3.S3PresignerService;
import com.formkiq.aws.dynamodb.ApiAuthorization;
import com.formkiq.aws.services.lambda.ApiGatewayRequestEvent;
import com.formkiq.aws.services.lambda.ApiGatewayRequestEventUtil;
import com.formkiq.aws.services.lambda.ApiGatewayRequestHandler;
import com.formkiq.aws.services.lambda.ApiRequestHandlerResponse;
import com.formkiq.aws.services.lambda.exceptions.BadException;
import com.formkiq.aws.services.lambda.exceptions.DocumentNotFoundException;
import com.formkiq.module.lambdaservices.AwsServiceCache;
import com.formkiq.plugins.useractivity.UserActivityPlugin;
import com.formkiq.stacks.api.ApiEmptyResponse;
import com.formkiq.stacks.api.ApiUrlResponse;
import com.formkiq.stacks.dynamodb.DocumentService;
import com.formkiq.stacks.dynamodb.DocumentVersionService;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/** {@link ApiGatewayRequestHandler} for "/documents/{documentId}/url". */
public class DocumentIdUrlRequestHandler
    implements ApiGatewayRequestHandler, ApiGatewayRequestEventUtil {

  /**
   * constructor.
   *
   */
  public DocumentIdUrlRequestHandler() {}

  @Override
  public ApiRequestHandlerResponse get(final ApiGatewayRequestEvent event,
      final ApiAuthorization authorization, final AwsServiceCache awsservice) throws Exception {

    String documentId = event.getPathParameters().get("documentId");
    String siteId = authorization.getSiteId();
    String versionKey = getVersionKey(event);

    Map<String, AttributeValue> versionAttributes =
        getVersionAttributes(awsservice, siteId, documentId, versionKey);
    DocumentItem item =
        getDocumentItem(awsservice, siteId, documentId, versionKey, versionAttributes);
    String versionId = getVersionId(awsservice, versionAttributes, versionKey);

    boolean inline = "true".equals(getParameter(event, "inline"));
    URL url = getS3Url(authorization, awsservice, event, item, versionId, inline);

    if (url != null) {
      if (awsservice.containsExtension(UserActivityPlugin.class)) {
        UserActivityPlugin plugin = awsservice.getExtension(UserActivityPlugin.class);
        plugin.addDocumentViewActivity(siteId, documentId, versionKey);
      }
    }

    return url != null
        ? new ApiRequestHandlerResponse(SC_OK, new ApiUrlResponse(url.toString(), documentId))
        : new ApiRequestHandlerResponse(SC_NOT_FOUND, new ApiEmptyResponse());
  }

  private String getVersionKey(final ApiGatewayRequestEvent event) {
    String versionKey = getParameter(event, "versionKey");
    if (!isEmpty(versionKey) && !versionKey.startsWith("document#")) {
      versionKey = URLDecoder.decode(versionKey, StandardCharsets.UTF_8);
    }
    return versionKey;
  }

  /**
   * Look at {@link ApiGatewayRequestEvent} for "duration".
   * 
   * @param event {@link ApiGatewayRequestEvent}
   * @return int
   */
  private int getDurationHours(final ApiGatewayRequestEvent event) {
    final int defaultDurationHours = 48;

    Map<String, String> map =
        event.getQueryStringParameters() != null ? event.getQueryStringParameters()
            : new HashMap<>();
    String durationHours = map.getOrDefault("duration", "" + defaultDurationHours);

    try {
      return Integer.parseInt(durationHours);
    } catch (NumberFormatException e) {
      return defaultDurationHours;
    }
  }

  @Override
  public String getRequestUrl() {
    return "/documents/{documentId}/url";
  }

  /**
   * Get S3 URL.
   *
   * @param authorization {@link ApiAuthorization}
   * @param awsservice {@link AwsServiceCache}
   * @param event {@link ApiGatewayRequestEvent}
   * @param item {@link DocumentItem}
   * @param versionId {@link String}
   * @param inline boolean
   * @return {@link URL}
   * @throws URISyntaxException URISyntaxException
   * @throws MalformedURLException MalformedURLException
   */
  private URL getS3Url(final ApiAuthorization authorization, final AwsServiceCache awsservice,
      final ApiGatewayRequestEvent event, final DocumentItem item, final String versionId,
      final boolean inline) throws URISyntaxException, MalformedURLException {

    final String documentId = item.getDocumentId();

    String siteId = authorization.getSiteId();

    awsservice.getLogger().debug(
        "Finding S3 Url for document '" + item.getDocumentId() + "' version = '" + versionId + "'");

    String s3Bucket = awsservice.environment("DOCUMENTS_S3_BUCKET");
    String filename = getFilename(item);

    PresignGetUrlConfig config = new PresignGetUrlConfig();

    config.contentType(item.getContentType());
    String s3key = createS3Key(siteId, documentId);

    if (isS3Link(item)) {
      URI u = new URI(item.getDeepLinkPath());
      s3Bucket = u.getHost();
      s3key = u.getPath().startsWith("/") ? u.getPath().substring(1) : u.getPath();
      filename = Strings.getFilename(item.getDeepLinkPath());

    } else if (!isEmpty(item.getDeepLinkPath())) {

      s3Bucket = null;
      s3key = item.getDeepLinkPath();
      filename = Strings.getFilename(item.getDeepLinkPath());
    }

    config.contentDispositionByPath(filename, inline);

    int hours = getDurationHours(event);
    Duration duration = Duration.ofHours(hours);

    S3PresignerService s3Service = awsservice.getExtension(S3PresignerService.class);
    return s3Bucket != null ? s3Service.presignGetUrl(s3Bucket, s3key, duration, versionId, config)
        : new URL(s3key);
  }

  private String getFilename(final DocumentItem item) {

    MimeType mt = MimeType.fromContentType(item.getContentType());

    String ext = mt.getExtension();
    String filename = item.getDocumentId();
    if (!isEmpty(ext)) {
      filename += "." + ext;
    }

    if (item.getPath() != null) {
      filename = Strings.getFilename(item.getPath());
    }

    return filename;
  }

  private boolean isS3Link(final DocumentItem item) {
    return !isEmpty(item.getDeepLinkPath()) && item.getDeepLinkPath().startsWith("s3://");
  }

  private Map<String, AttributeValue> getVersionAttributes(final AwsServiceCache awsservice,
      final String siteId, final String documentId, final String versionKey) {
    DocumentVersionService versionService = awsservice.getExtension(DocumentVersionService.class);
    return versionService.get(siteId, documentId, versionKey);
  }

  private DocumentItem getDocumentItem(final AwsServiceCache awsservice, final String siteId,
      final String documentId, final String versionKey,
      final Map<String, AttributeValue> versionAttributes) throws Exception {

    DocumentVersionService versionService = awsservice.getExtension(DocumentVersionService.class);
    DocumentService documentService = awsservice.getExtension(DocumentService.class);

    DocumentItem item = versionService.getDocumentItem(documentService, siteId, documentId,
        versionKey, versionAttributes);
    throwIfNull(item, new DocumentNotFoundException(documentId));
    return item;
  }

  private String getVersionId(final AwsServiceCache awsservice,
      final Map<String, AttributeValue> versionAttributes, final String versionKey)
      throws Exception {

    String versionId = null;
    if (versionKey != null) {

      DocumentVersionService versionService = awsservice.getExtension(DocumentVersionService.class);
      versionId = versionService.getVersionId(versionAttributes);

      throwIfNull(versionId, new BadException("invalid versionKey '" + versionKey + "'"));
    }

    return versionId;
  }
}
