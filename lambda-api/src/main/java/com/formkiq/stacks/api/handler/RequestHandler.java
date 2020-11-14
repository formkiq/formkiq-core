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
package com.formkiq.stacks.api.handler;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.formkiq.stacks.api.ApiAuthorizer;
import com.formkiq.stacks.api.ApiGatewayRequestEvent;
import com.formkiq.stacks.api.BadException;
import com.formkiq.stacks.api.util.GsonUtil;
import com.formkiq.stacks.common.objects.DynamicObject;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

/** API Request Handler. */
public interface RequestHandler {

  /** {@link Gson}. */
  Gson GSON = GsonUtil.getInstance();

  /**
   * Get the Body from {@link ApiGatewayRequestEvent} and transform to Object.
   *
   * @param <T> Type of {@link Class}
   * @param logger {@link LambdaLogger}
   * @param event {@link ApiGatewayRequestEvent}
   * @param classOfT {@link Class}
   * @return T
   * @throws BadException BadException
   */
  default <T> T fromBodyToObject(final LambdaLogger logger, final ApiGatewayRequestEvent event,
      final Class<T> classOfT) throws BadException {

    String body = event.getBody();
    if (body == null) {
      throw new BadException("request body is required");
    }

    byte[] data = event.getBody().getBytes(StandardCharsets.UTF_8);

    if (Boolean.TRUE.equals(event.getIsBase64Encoded())) {
      data = Base64.getDecoder().decode(body);
    }

    Reader reader = new InputStreamReader(new ByteArrayInputStream(data), StandardCharsets.UTF_8);
    try {
      return GSON.fromJson(reader, classOfT);
    } catch (JsonSyntaxException e) {
      throw new BadException("invalid JSON body");
    } finally {
      try {
        reader.close();
      } catch (IOException e) {
        logger.log("Cannot close DocumentItemJSON: " + e.getMessage());
      }
    }
  }

  /**
   * Get the Body from {@link ApiGatewayRequestEvent} and transform to {@link Map}.
   *
   * @param logger {@link LambdaLogger}
   * @param event {@link ApiGatewayRequestEvent}
   * @return {@link Map}
   * @throws BadException BadException
   */
  @SuppressWarnings("unchecked")
  default Map<String, Object> fromBodyToMap(final LambdaLogger logger,
      final ApiGatewayRequestEvent event) throws BadException {
    return fromBodyToObject(logger, event, Map.class);
  }

  /**
   * Get the Body from {@link ApiGatewayRequestEvent} and transform to {@link DynamicObject}.
   *
   * @param logger {@link LambdaLogger}
   * @param event {@link ApiGatewayRequestEvent}
   * @return {@link DynamicObject}
   * @throws BadException BadException
   */
  @SuppressWarnings("unchecked")
  default DynamicObject fromBodyToDynamicObject(final LambdaLogger logger,
      final ApiGatewayRequestEvent event) throws BadException {
    return new DynamicObject(fromBodyToObject(logger, event, Map.class));
  }

  /**
   * Whether this {@link RequestHandler} is a READONLY Handler.
   * 
   * @param method {@link String}
   * @return boolean
   */
  boolean isReadonly(String method);

  /**
   * Processes {@link ApiGatewayRequestEvent} request.
   *
   * @param logger {@link LambdaLogger}
   * @param event {@link ApiGatewayRequestEvent}
   * @param authorizer {@link ApiAuthorizer}
   * @param awsServices {@link AwsServiceCache}
   * 
   * @return {@link ApiRequestHandlerResponse}
   * 
   * @throws Exception Exception
   */
  ApiRequestHandlerResponse process(LambdaLogger logger, ApiGatewayRequestEvent event,
      ApiAuthorizer authorizer, AwsServiceCache awsServices) throws Exception;

  /**
   * Sub list to a max limit.
   * 
   * @param <T> Type
   * @param list {@link List}
   * @param limit int
   * 
   * @return {@link List}
   */
  default <T> List<T> subList(final List<T> list, final int limit) {
    return list.size() > limit ? list.subList(0, limit) : list;
  }
}
