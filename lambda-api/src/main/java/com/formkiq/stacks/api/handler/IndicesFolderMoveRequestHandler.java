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
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.formkiq.aws.services.lambda.ApiAuthorization;
import com.formkiq.aws.services.lambda.ApiGatewayRequestEvent;
import com.formkiq.aws.services.lambda.ApiGatewayRequestEventUtil;
import com.formkiq.aws.services.lambda.ApiGatewayRequestHandler;
import com.formkiq.aws.services.lambda.ApiMapResponse;
import com.formkiq.aws.services.lambda.ApiRequestHandlerResponse;
import com.formkiq.aws.services.lambda.exceptions.BadException;
import com.formkiq.module.lambdaservices.AwsServiceCache;
import com.formkiq.stacks.dynamodb.FolderIndexProcessor;
import com.formkiq.validation.ValidationError;
import com.formkiq.validation.ValidationErrorImpl;
import com.formkiq.validation.ValidationException;
import software.amazon.awssdk.utils.StringUtils;

/** {@link ApiGatewayRequestHandler} for "/indices/folder/move". */
public class IndicesFolderMoveRequestHandler
    implements ApiGatewayRequestHandler, ApiGatewayRequestEventUtil {

  /** {@link IndicesFolderMoveRequestHandler} URL. */
  public static final String URL = "/indices/{indexType}/move";

  /**
   * constructor.
   *
   */
  public IndicesFolderMoveRequestHandler() {}

  @Override
  public String getRequestUrl() {
    return URL;
  }

  /**
   * Run Move Folder Index.
   * 
   * @param logger {@link LambdaLogger}
   * @param event {@link ApiGatewayRequestEvent}
   * @param awsServices {@link AwsServiceCache}
   * @param siteId {@link String}
   * @param userId {@link String}
   * @return {@link ApiRequestHandlerResponse}
   * @throws BadException BadException
   * @throws ValidationException ValidationException
   * @throws IOException IOException
   */
  private ApiRequestHandlerResponse moveFolderIndex(final LambdaLogger logger,
      final ApiGatewayRequestEvent event, final AwsServiceCache awsServices, final String siteId,
      final String userId) throws BadException, ValidationException, IOException {

    Map<String, Object> body = fromBodyToMap(event);

    Collection<ValidationError> errors = validation(body);
    if (!errors.isEmpty()) {
      throw new ValidationException(errors);
    }

    FolderIndexProcessor indexProcessor = awsServices.getExtension(FolderIndexProcessor.class);

    try {
      indexProcessor.moveIndex(siteId, body.get("source").toString(), body.get("target").toString(),
          userId);
    } catch (IOException e) {
      throw new BadException(e.getMessage());
    }

    ApiMapResponse resp = new ApiMapResponse();
    resp.setMap(Map.of("message", "Folder moved"));
    return new ApiRequestHandlerResponse(SC_OK, resp);
  }

  @Override
  public ApiRequestHandlerResponse post(final LambdaLogger logger,
      final ApiGatewayRequestEvent event, final ApiAuthorization authorization,
      final AwsServiceCache awsServices) throws Exception {

    String siteId = authorization.getSiteId();
    String userId = authorization.getUsername();
    String type = event.getPathParameters().get("indexType");

    if ("folder".equals(type)) {
      return moveFolderIndex(logger, event, awsServices, siteId, userId);
    }

    throw new BadException("invalid 'indexType' parameter");
  }

  /**
   * Validate {@link Map}.
   * 
   * @param body {@link Map}
   * @return {@link Collection} {@link ValidationError}
   */
  private Collection<ValidationError> validation(final Map<String, Object> body) {

    Collection<ValidationError> errors = new ArrayList<>();

    if (body == null) {
      errors.add(new ValidationErrorImpl().error("invalid body"));
    } else {
      for (String key : Arrays.asList("source", "target")) {
        if (!body.containsKey(key) || StringUtils.isEmpty(body.get(key).toString())) {
          errors.add(new ValidationErrorImpl().key(key).error("attribute is required"));
        }
      }
    }

    return errors;
  }
}
