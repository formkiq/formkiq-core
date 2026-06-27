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
package com.formkiq.stacks.api.handler.documents;

import com.formkiq.aws.dynamodb.ApiAuthorization;
import com.formkiq.aws.dynamodb.DynamoDbKey;
import com.formkiq.aws.dynamodb.DynamoDbService;
import com.formkiq.aws.dynamodb.documents.DocumentArtifact;
import com.formkiq.aws.dynamodb.documents.DocumentRecord;
import com.formkiq.aws.dynamodb.documents.DocumentRecordBuilder;
import com.formkiq.aws.dynamodb.useractivities.ActivityResourceType;
import com.formkiq.aws.dynamodb.useractivities.ChangeRecord;
import com.formkiq.aws.dynamodb.useractivities.UserActivityType;
import com.formkiq.aws.services.lambda.ApiGatewayRequestEvent;
import com.formkiq.aws.services.lambda.ApiGatewayRequestEventUtil;
import com.formkiq.aws.services.lambda.ApiGatewayRequestHandler;
import com.formkiq.aws.services.lambda.ApiRequestHandlerResponse;
import com.formkiq.aws.services.lambda.JsonToObject;
import com.formkiq.aws.services.lambda.exceptions.DocumentNotFoundException;
import com.formkiq.aws.services.lambda.exceptions.NotFoundException;
import com.formkiq.module.lambdaservices.AwsServiceCache;
import com.formkiq.plugins.useractivity.UserActivityContext;
import com.formkiq.stacks.api.models.documents.PromoteDocumentArtifactRequest;
import com.formkiq.stacks.dynamodb.DocumentService;
import software.amazon.awssdk.services.dynamodb.model.AttributeAction;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.AttributeValueUpdate;

import java.util.HashMap;
import java.util.Map;

import static com.formkiq.aws.dynamodb.objects.Objects.throwIfNull;
import static com.formkiq.strings.Strings.isEmpty;

/** {@link ApiGatewayRequestHandler} for "/documents/{documentId}/artifacts/promoteArtifact". */
public class DocumentIdArtifactsPromoteRequestHandler
    implements ApiGatewayRequestHandler, ApiGatewayRequestEventUtil {

  /** constructor. */
  public DocumentIdArtifactsPromoteRequestHandler() {}

  private void createAudit(final DocumentRecord root, final String documentId,
      final String artifactId) {
    Map<String, ChangeRecord> changes = new HashMap<>();
    changes.put("promotedArtifactId", new ChangeRecord(root.promotedArtifactId(), artifactId));
    UserActivityContext.set(ActivityResourceType.DOCUMENT, UserActivityType.UPDATE, changes,
        Map.of("documentId", documentId));
  }

  @Override
  public String getRequestUrl() {
    return "/documents/{documentId}/artifacts/promoteArtifact";
  }

  @Override
  public ApiRequestHandlerResponse put(final ApiGatewayRequestEvent event,
      final ApiAuthorization authorization, final AwsServiceCache awsservice) throws Exception {

    String siteId = authorization.getSiteId();
    String documentId = event.getPathParameter("documentId");
    PromoteDocumentArtifactRequest request =
        JsonToObject.fromJson(awsservice, event, PromoteDocumentArtifactRequest.class);

    DocumentArtifact rootDocument = DocumentArtifact.of(documentId, null);
    String artifactId = request != null ? request.artifactId() : null;

    DocumentService documentService = awsservice.getExtension(DocumentService.class);
    DocumentRecord root = documentService.findDocument(siteId, rootDocument);
    throwIfNull(root, new DocumentNotFoundException(documentId));

    if (!isEmpty(artifactId)) {
      DocumentArtifact artifactDocument = DocumentArtifact.of(documentId, artifactId);
      DocumentRecord artifact = documentService.findDocument(siteId, artifactDocument);
      throwIfNull(artifact,
          new NotFoundException("Document artifact '" + artifactId + "' not found."));
    }

    DynamoDbService db = awsservice.getExtension(DynamoDbService.class);
    DynamoDbKey key = new DocumentRecordBuilder().document(rootDocument).buildKey(siteId);
    AttributeValueUpdate promotedArtifactUpdate =
        isEmpty(artifactId) ? AttributeValueUpdate.builder().action(AttributeAction.DELETE).build()
            : AttributeValueUpdate.builder().action(AttributeAction.PUT)
                .value(AttributeValue.fromS(artifactId)).build();
    Map<String, AttributeValueUpdate> updateValues =
        Map.of("promotedArtifactId", promotedArtifactUpdate);

    db.updateItem(AttributeValue.fromS(key.pk()), AttributeValue.fromS(key.sk()), updateValues);
    createAudit(root, documentId, artifactId);

    return ApiRequestHandlerResponse.builder().ok()
        .body("message", "Promoted artifact '" + artifactId + "' on document '" + documentId + "'.")
        .build();
  }
}
