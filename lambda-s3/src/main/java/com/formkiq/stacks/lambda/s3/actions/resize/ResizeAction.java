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
package com.formkiq.stacks.lambda.s3.actions.resize;

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
import java.util.Map;

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
    Image srcImage = createSrcImage(siteId, documentId);
    Image resImage = createResImage(srcImage, action.parameters());
    saveResImage(resImage, srcImage.documentId());
  }

  private Image createSrcImage(final String siteId, final String documentId) throws IOException {
    byte[] imageData = getImageData(siteId, documentId);
    BufferedImage bufferedImage = ImageUtils.bufferedImageFromByteArray(imageData);
    String imageFormat = ImageUtils.getImageFormat(imageData);

    return new Image(siteId, documentId, imageData, bufferedImage, imageFormat,
        getPath(siteId, documentId));
  }

  private byte[] getImageData(final String siteId, final String documentId) {
    String s3key = SiteIdKeyGenerator.createS3Key(siteId, documentId);

    return s3Service.getContentAsBytes(documentsBucket, s3key);
  }

  private String getPath(final String siteId, final String documentId) {
    DocumentItem item = documentService.findDocument(siteId, documentId);

    if (item == null) {
      throw new IllegalArgumentException("Document not found: " + documentId);
    }

    return item.getPath();
  }

  private Image createResImage(final Image srcImage, final Map<String, Object> parameters)
      throws IOException {
    BufferedImage resizedImage = resizeImage(srcImage, parameters);
    String format = getFormat(srcImage, parameters);

    byte[] imageData = ImageUtils.bufferedImageToByteArray(resizedImage, format);
    // sometimes library fails to create image with desired format
    if (imageData.length == 0) {
      throw new IOException(
          "While converting <" + srcImage.documentId() + "> we got empty resulting image.");
    }

    String path = getResPath(srcImage, parameters, resizedImage, format);

    return new Image(srcImage.siteId(), ID.uuid(), imageData, resizedImage, format, path);
  }

  private static BufferedImage resizeImage(final Image srcImage,
      final Map<String, Object> parameters) throws IOException {
    String widthStr = (String) parameters.get("width");
    String heightStr = (String) parameters.get("height");
    boolean isKeepAspectRatio = "auto".equals(widthStr) || "auto".equals(heightStr);

    return ImageUtils.resize(srcImage.bufferedImage(), parseDimension(widthStr),
        parseDimension(heightStr), isKeepAspectRatio);
  }

  private static int parseDimension(final String dimensionStr) {
    return "auto".equals(dimensionStr) ? Integer.MAX_VALUE : Integer.parseInt(dimensionStr);
  }

  private static String getFormat(final Image srcImage, final Map<String, Object> parameters) {
    String format = (String) parameters.get("outputType");

    return format != null ? format : srcImage.format();
  }

  private static String getResPath(final Image srcImage, final Map<String, Object> parameters,
      final BufferedImage resizedImage, final String format) {
    String path = (String) parameters.get("path");

    if (path == null) {
      String imageFormatExtension =
          FilenameUtils.imageFormatToExtension(srcImage.path(), srcImage.format());
      path = FilenameUtils.getFileName(srcImage.path(), resizedImage.getWidth(),
          resizedImage.getHeight(), imageFormatExtension, format);
    }

    return path;
  }

  private void saveResImage(final Image resImage, final String sourceDocumentId)
      throws ValidationException {
    saveData(resImage);
    saveMetadata(resImage, sourceDocumentId);
  }

  private void saveData(final Image resImage) {
    String s3key = SiteIdKeyGenerator.createS3Key(resImage.siteId(), resImage.documentId());
    s3Service.putObject(documentsBucket, s3key, resImage.data(),
        "image/" + imageFormatToMimeType(resImage.format()));
  }

  private static String imageFormatToMimeType(final String format) {
    return "tif".equals(format) ? "tiff" : format;
  }

  private void saveMetadata(final Image resImage, final String sourceDocumentId)
      throws ValidationException {
    String username = ApiAuthorization.getAuthorization().getUsername();
    DocumentItem item = createDocumentItem(resImage, username);
    saveDocumentItem(resImage, sourceDocumentId, username, item);
  }

  private static DocumentItem createDocumentItem(final Image resImage, final String username) {
    DocumentItem item = new DocumentItemDynamoDb(resImage.documentId(), new Date(), username);
    item.setPath(resImage.path());
    item.setWidth(Integer.toString(resImage.getWidth()));
    item.setHeight(Integer.toString(resImage.getHeight()));

    return item;
  }

  private void saveDocumentItem(final Image resImage, final String sourceDocumentId,
      final String username, final DocumentItem item) throws ValidationException {
    Collection<DocumentAttributeRecord> documentAttributes =
        new DocumentAttributeRecordBuilder().apply(sourceDocumentId, resImage.documentId(),
            DocumentRelationshipType.RENDITION, null, username);
    SaveDocumentOptions options =
        new SaveDocumentOptions().validationAccess(AttributeValidationAccess.ADMIN_CREATE);
    documentService.saveDocument(resImage.siteId(), item, null, documentAttributes, options);
  }
}
