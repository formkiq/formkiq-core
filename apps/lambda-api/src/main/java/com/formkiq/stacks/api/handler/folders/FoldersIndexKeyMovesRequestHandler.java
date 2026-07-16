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
import com.formkiq.aws.dynamodb.ID;
import com.formkiq.aws.dynamodb.folderpermissions.StringToFolder;
import com.formkiq.aws.dynamodb.folders.FolderMoveRequest;
import com.formkiq.aws.s3.S3Service;
import com.formkiq.aws.services.lambda.ApiGatewayRequestEvent;
import com.formkiq.aws.services.lambda.ApiGatewayRequestEventUtil;
import com.formkiq.aws.services.lambda.ApiGatewayRequestHandler;
import com.formkiq.aws.services.lambda.ApiRequestHandlerResponse;
import com.formkiq.aws.services.lambda.JsonToObject;
import com.formkiq.module.lambdaservices.AwsServiceCache;
import com.formkiq.stacks.api.handler.IndexKeyToString;
import com.formkiq.stacks.dynamodb.folders.FolderIndexProcessor;
import com.formkiq.validation.ValidationBuilder;

import java.nio.charset.StandardCharsets;

import static com.formkiq.aws.dynamodb.SiteIdKeyGenerator.DEFAULT_SITE_ID;
import static com.formkiq.aws.dynamodb.SiteIdKeyGenerator.createS3Key;

/** {@link ApiGatewayRequestHandler} for "/folders/{indexKey}/moves". */
public class FoldersIndexKeyMovesRequestHandler
    implements ApiGatewayRequestHandler, ApiGatewayRequestEventUtil {

  /** Folder move staging key prefix. */
  public static final String FOLDER_MOVES_PREFIX = "tempfiles/moves/";

  /** Folder move request created message. */
  public static final String MESSAGE_FOLDER_MOVE_REQUEST_CREATED = "folder move request created";

  /**
   * constructor.
   *
   */
  public FoldersIndexKeyMovesRequestHandler() {}

  @Override
  public String getRequestUrl() {
    return "/folders/{indexKey}/moves";
  }

  @Override
  public ApiRequestHandlerResponse post(final ApiGatewayRequestEvent event,
      final ApiAuthorization authorization, final AwsServiceCache awsservice) throws Exception {

    MoveFolderRequest moveFolderRequest =
        JsonToObject.fromJson(awsservice, event, MoveFolderRequest.class);
    validate(moveFolderRequest);

    String siteId = authorization.getSiteId();
    String userId = authorization.getUsername();
    StringToFolder stringToFolder = new StringToFolder();
    FolderIndexProcessor indexProcessor = awsservice.getExtension(FolderIndexProcessor.class);
    String indexKey = new IndexKeyToString().apply(event.getPathParameter("indexKey"));
    String sourcePath = stringToFolder.apply(indexProcessor.toPath(siteId, indexKey));
    String targetPath = stringToFolder.apply(moveFolderRequest.path());

    FolderMoveRequest request = new FolderMoveRequest(sourcePath, targetPath,
        siteId == null ? DEFAULT_SITE_ID : siteId, userId);

    S3Service s3 = awsservice.getExtension(S3Service.class);
    String bucket = awsservice.environment("STAGE_DOCUMENTS_S3_BUCKET");
    String key = FOLDER_MOVES_PREFIX + createS3Key(siteId, ID.uuid(), null) + ".json";
    s3.putObject(bucket, key, GSON.toJson(request).getBytes(StandardCharsets.UTF_8),
        "application/json");

    return ApiRequestHandlerResponse.builder().created()
        .body("message", MESSAGE_FOLDER_MOVE_REQUEST_CREATED).build();
  }

  private void validate(final MoveFolderRequest body) {
    ValidationBuilder vb = new ValidationBuilder();
    vb.isRequired(null, body, "invalid body");
    vb.check();
    vb.isRequired("path", body.path());
    vb.check();
  }
}
