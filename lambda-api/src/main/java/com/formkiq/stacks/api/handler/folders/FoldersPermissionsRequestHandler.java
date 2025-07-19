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
package com.formkiq.stacks.api.handler.folders;

import com.formkiq.aws.dynamodb.ApiAuthorization;
import com.formkiq.aws.services.lambda.ApiGatewayRequestEvent;
import com.formkiq.aws.services.lambda.ApiGatewayRequestEventUtil;
import com.formkiq.aws.services.lambda.ApiGatewayRequestHandler;
import com.formkiq.aws.services.lambda.ApiRequestHandlerResponse;
import com.formkiq.module.lambdaservices.AwsServiceCache;
import com.formkiq.stacks.dynamodb.folders.FolderIndexProcessor;
import com.formkiq.validation.ValidationBuilder;

/** {@link ApiGatewayRequestHandler} for "/folders/permissions". */
public class FoldersPermissionsRequestHandler
    implements ApiGatewayRequestHandler, ApiGatewayRequestEventUtil {

  /**
   * constructor.
   *
   */
  public FoldersPermissionsRequestHandler() {}

  @Override
  public ApiRequestHandlerResponse put(final ApiGatewayRequestEvent event,
      final ApiAuthorization authorization, final AwsServiceCache awsservice) throws Exception {

    String siteId = authorization.getSiteId();
    SetFolderPermissionsRequest req = fromBodyToObject(event, SetFolderPermissionsRequest.class);

    validate(req);

    FolderIndexProcessor processor = awsservice.getExtension(FolderIndexProcessor.class);

    processor.setPermissions(siteId, req.path(), req.roles());

    return ApiRequestHandlerResponse.builder().ok().body("message", "Folder permissions set")
        .build();
  }

  private void validate(final SetFolderPermissionsRequest req) {
    ValidationBuilder vb = new ValidationBuilder();
    vb.isRequired(null, req, "invalid body");
    vb.check();

    vb.isRequired(null, req.path());
    vb.isRequired(null, req.roles(), "'roles' is required");
    vb.check();
  }

  @Override
  public String getRequestUrl() {
    return "/folders/permissions";
  }
}
