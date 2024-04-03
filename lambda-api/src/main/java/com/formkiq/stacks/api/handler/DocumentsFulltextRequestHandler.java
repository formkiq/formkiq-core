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

import static com.formkiq.aws.services.lambda.ApiResponseStatus.SC_OK;
import static com.formkiq.module.http.HttpResponseStatus.is2XX;
import static com.formkiq.module.http.HttpResponseStatus.is404;
import java.net.http.HttpResponse;
import java.util.Map;
import java.util.stream.Collectors;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.formkiq.aws.dynamodb.model.DocumentMapToDocument;
import com.formkiq.aws.services.lambda.ApiAuthorization;
import com.formkiq.aws.services.lambda.ApiGatewayRequestEvent;
import com.formkiq.aws.services.lambda.ApiGatewayRequestEventUtil;
import com.formkiq.aws.services.lambda.ApiGatewayRequestHandler;
import com.formkiq.aws.services.lambda.ApiMapResponse;
import com.formkiq.aws.services.lambda.ApiRequestHandlerResponse;
import com.formkiq.aws.services.lambda.exceptions.BadException;
import com.formkiq.aws.services.lambda.exceptions.DocumentNotFoundException;
import com.formkiq.module.lambdaservices.AwsServiceCache;
import com.formkiq.module.typesense.TypeSenseService;

/** {@link ApiGatewayRequestHandler} for "/documents/{documentId}/fulltext". */
public class DocumentsFulltextRequestHandler
    implements ApiGatewayRequestHandler, ApiGatewayRequestEventUtil {

  /** {@link DocumentsFulltextRequestHandler} URL. */
  public static final String URL = "/documents/{documentId}/fulltext";

  /**
   * Build Document from Reqeust Body.
   * 
   * @param body {@link Map}
   * @return {@link Map}
   * @throws BadException BadException
   */
  private Map<String, Object> buildDocumentFromRequestBody(final Map<String, Object> body)
      throws BadException {

    DocumentMapToDocument fulltext = new DocumentMapToDocument();
    Map<String, Object> document = fulltext.apply(body);

    if (body.containsKey("contentUrls")) {
      throw new BadException("'contentUrls' are not supported by Typesense");
    }

    if (body.containsKey("tags")) {
      throw new BadException("'tags' are not supported with Typesense");
    }

    return document;
  }

  private TypeSenseService checkTypesenseInstalled(final AwsServiceCache awsservice)
      throws BadException {
    if (!awsservice.hasModule("typesense")) {
      throw new BadException("'typesense' is not configured");
    }

    return awsservice.getExtension(TypeSenseService.class);
  }

  @Override
  public ApiRequestHandlerResponse delete(final LambdaLogger logger,
      final ApiGatewayRequestEvent event, final ApiAuthorization authorization,
      final AwsServiceCache awsservice) throws Exception {

    TypeSenseService typeSenseService = checkTypesenseInstalled(awsservice);

    String siteId = authorization.getSiteId();
    String documentId = event.getPathParameters().get("documentId");

    typeSenseService.deleteDocument(siteId, documentId);

    Map<String, Object> map = Map.of("message", "Deleted document '" + documentId + "'");
    return new ApiRequestHandlerResponse(SC_OK, new ApiMapResponse(map));
  }

  @SuppressWarnings("unchecked")
  @Override
  public ApiRequestHandlerResponse get(final LambdaLogger logger,
      final ApiGatewayRequestEvent event, final ApiAuthorization authorization,
      final AwsServiceCache awsservice) throws Exception {

    String siteId = authorization.getSiteId();
    String documentId = event.getPathParameters().get("documentId");

    TypeSenseService typeSenseService = checkTypesenseInstalled(awsservice);

    HttpResponse<String> response = typeSenseService.getDocument(siteId, documentId);

    if (is2XX(response)) {

      Map<String, Object> body = GSON.fromJson(response.body(), Map.class);
      body.put("documentId", body.get("id"));
      body.remove("id");
      body.remove("metadata#");

      Map<String, Object> metadata =
          body.entrySet().stream().filter(e -> e.getKey().startsWith("metadata#")).collect(
              Collectors.toMap(e -> e.getKey().replaceAll("metadata#", ""), Map.Entry::getValue));

      metadata.keySet().forEach(k -> body.remove("metadata#" + k));

      body.put("metadata", metadata);

      ApiMapResponse resp = new ApiMapResponse();
      resp.setMap(body);
      return new ApiRequestHandlerResponse(SC_OK, resp);
    }

    return handleError(response, documentId);
  }

  @Override
  public String getRequestUrl() {
    return URL;
  }

  private ApiRequestHandlerResponse handleError(final HttpResponse<String> response,
      final String documentId) throws DocumentNotFoundException, BadException {
    if (is404(response)) {
      throw new DocumentNotFoundException(documentId);
    }

    throw new BadException(response.body());
  }

  @Override
  public ApiRequestHandlerResponse patch(final LambdaLogger logger,
      final ApiGatewayRequestEvent event, final ApiAuthorization authorization,
      final AwsServiceCache awsservice) throws Exception {

    TypeSenseService typeSenseService = checkTypesenseInstalled(awsservice);

    String siteId = authorization.getSiteId();
    String documentId = event.getPathParameters().get("documentId");

    Map<String, Object> body = fromBodyToMap(event);

    Map<String, Object> document = buildDocumentFromRequestBody(body);

    HttpResponse<String> response = typeSenseService.updateDocument(siteId, documentId, document);

    if (is2XX(response)) {
      Map<String, Object> map = Map.of("message", "Updated document to Typesense");
      return new ApiRequestHandlerResponse(SC_OK, new ApiMapResponse(map));
    }

    return handleError(response, documentId);
  }

  @Override
  public ApiRequestHandlerResponse put(final LambdaLogger logger,
      final ApiGatewayRequestEvent event, final ApiAuthorization authorization,
      final AwsServiceCache awsservice) throws Exception {

    TypeSenseService typeSenseService = checkTypesenseInstalled(awsservice);

    String siteId = authorization.getSiteId();
    String documentId = event.getPathParameters().get("documentId");

    Map<String, Object> body = fromBodyToMap(event);

    Map<String, Object> document = buildDocumentFromRequestBody(body);

    HttpResponse<String> response =
        typeSenseService.addOrUpdateDocument(siteId, documentId, document);

    if (is2XX(response)) {
      Map<String, Object> map = Map.of("message", "Add document to Typesense");
      return new ApiRequestHandlerResponse(SC_OK, new ApiMapResponse(map));
    }

    return handleError(response, documentId);
  }
}
