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
package com.formkiq.stacks.lambda.s3.actions;

import com.formkiq.aws.dynamodb.ApiAuthorization;
import com.formkiq.aws.dynamodb.ID;
import com.formkiq.aws.dynamodb.SiteIdKeyGenerator;
import com.formkiq.aws.dynamodb.model.DocumentItem;
import com.formkiq.aws.s3.S3Service;
import com.formkiq.module.actions.Action;
import com.formkiq.module.actions.ActionType;
import com.formkiq.module.lambdaservices.AwsServiceCache;
import com.formkiq.module.lambdaservices.logger.Logger;
import com.formkiq.stacks.dynamodb.DocumentItemDynamoDb;
import com.formkiq.stacks.dynamodb.DocumentService;
import com.formkiq.stacks.dynamodb.SaveDocumentOptions;
import com.formkiq.stacks.dynamodb.attributes.AttributeValidationAccess;
import com.formkiq.stacks.dynamodb.attributes.DocumentAttributeRecord;
import com.formkiq.stacks.dynamodb.attributes.DocumentAttributeRecordBuilder;
import com.formkiq.stacks.dynamodb.attributes.DocumentRelationshipType;
import com.formkiq.stacks.lambda.s3.DocumentAction;
import com.formkiq.validation.ValidationException;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Collection;
import java.util.Date;
import java.util.List;

/**
 * {@link DocumentAction} for RESIZE {@link ActionType}.
 */
public class ResizeAction implements DocumentAction {
  /** {@link DocumentService}. */
  private final DocumentService documentService;
  /** Documents S3 Bucket. */
  private final String documentsBucket;
  /** {@link S3Service}. */
  private final S3Service s3Service;

  public ResizeAction(final AwsServiceCache serviceCache) {
    this.documentService = serviceCache.getExtension(DocumentService.class);
    this.documentsBucket = serviceCache.environment("DOCUMENTS_S3_BUCKET");
    this.s3Service = serviceCache.getExtension(S3Service.class);
  }

  @Override
  public void run(final Logger logger, final String siteId, final String documentId,
      final List<Action> actions, final Action action) throws IOException, ValidationException {
    byte[] originalImage = getOriginalImage(siteId, documentId);
    BufferedImage resizedImage =
        resize(originalImage, action.parameters().get("width"), action.parameters().get("height"));
    saveResizedImage(siteId, documentId, resizedImage);
  }

  private byte[] getOriginalImage(final String siteId, final String documentId) {
    String s3key = SiteIdKeyGenerator.createS3Key(siteId, documentId);

    return s3Service.getContentAsBytes(documentsBucket, s3key);
  }

  private BufferedImage resize(final byte[] image, final String widthStr, final String heightStr)
      throws IOException {
    int width = parseDimension(widthStr);
    int height = parseDimension(heightStr);
    boolean isKeepAspectRatio = "auto".equals(widthStr) || "auto".equals(heightStr);
    BufferedImage bufferedImage = ImageUtils.bufferedImageFromByteArray(image);

    return ImageUtils.resize(bufferedImage, width, height, isKeepAspectRatio);
  }

  private static int parseDimension(final String dimensionStr) {
    return "auto".equals(dimensionStr) ? Integer.MAX_VALUE : Integer.parseInt(dimensionStr);
  }

  private void saveResizedImage(final String siteId, final String sourceDocumentId,
      final BufferedImage resizedImage) throws ValidationException, IOException {
    String newDocumentId = ID.uuid();
    saveData(siteId, resizedImage, newDocumentId);
    String username = ApiAuthorization.getAuthorization().getUsername();
    DocumentItem item = createDocumentItem(newDocumentId, resizedImage.getWidth(),
        resizedImage.getHeight(), username);
    saveDocumentItem(siteId, sourceDocumentId, newDocumentId, username, item);
  }

  private void saveData(final String siteId, final BufferedImage image, final String documentId)
      throws IOException {
    String s3key = SiteIdKeyGenerator.createS3Key(siteId, documentId);
    byte[] imageData = ImageUtils.bufferedImageToByteArray(image);
    s3Service.putObject(documentsBucket, s3key, imageData, "image/png");
  }

  private static DocumentItem createDocumentItem(final String documentId, final int imageWidth,
      final int imageHeight, final String username) {
    DocumentItem item = new DocumentItemDynamoDb(documentId, new Date(), username);
    item.setWidth(Integer.toString(imageWidth));
    item.setHeight(Integer.toString(imageHeight));

    return item;
  }

  private void saveDocumentItem(final String siteId, final String sourceDocumentId,
      final String newDocumentId, final String username, final DocumentItem item)
      throws ValidationException {
    Collection<DocumentAttributeRecord> documentAttributes = new DocumentAttributeRecordBuilder()
        .apply(sourceDocumentId, newDocumentId, DocumentRelationshipType.RENDITION, null, username);
    SaveDocumentOptions options =
        new SaveDocumentOptions().validationAccess(AttributeValidationAccess.CREATE);
    documentService.saveDocument(siteId, item, null, documentAttributes, options);
  }
}
