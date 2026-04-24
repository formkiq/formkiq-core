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
package com.formkiq.stacks.dynamodb;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.formkiq.aws.dynamodb.documents.DocumentArtifact;
import com.formkiq.aws.dynamodb.documents.DocumentRecord;
import com.formkiq.aws.dynamodb.model.DocumentItem;
import com.formkiq.aws.dynamodb.model.DocumentTag;
import com.formkiq.aws.dynamodb.model.DynamicDocumentItem;
import com.formkiq.stacks.dynamodb.attributes.AttributeValidationType;
import com.formkiq.aws.dynamodb.attributes.AttributeValidationAccess;
import com.formkiq.aws.dynamodb.documentattributes.DocumentAttributeRecord;
import com.formkiq.aws.dynamodb.documentattributes.DocumentAttributeValueType;
import com.formkiq.aws.dynamodb.base64.Pagination;
import com.formkiq.stacks.dynamodb.documents.DocumentPublicationRecord;
import com.formkiq.validation.ValidationException;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/** Services for Querying, Updating Documents. */
public interface DocumentService {

  /** The Default maximum results returned. */
  int MAX_RESULTS = 10;

  /** System Defined Tags. */
  Set<String> SYSTEM_DEFINED_TAGS = Set.of("CLAMAV_SCAN_STATUS", "CLAMAV_SCAN_TIMESTAMP");

  /**
   * Add Folder Index.
   * 
   * @param siteId {@link String}
   * @param path {@link String}
   * @param userId {@link String}
   * @throws IOException IOException
   */
  void addFolderIndex(String siteId, String path, String userId) throws IOException;

  /**
   * Add Tags to Document.
   * 
   * @param siteId Optional Grouping siteId
   * @param document {@link DocumentArtifact}
   * @param tags {@link Collection} {@link DocumentTag}
   * @param timeToLive {@link String}
   */
  void addTags(String siteId, DocumentArtifact document, Collection<DocumentTag> tags,
      String timeToLive);

  /**
   * Add Tags to {@link Collection} of Documents.
   * 
   * @param siteId Optional Grouping siteId
   * @param tags {@link Map} {@link Collection} {@link DocumentTag}
   * @param timeToLive {@link String}
   */
  void addTags(String siteId, Map<DocumentArtifact, Collection<DocumentTag>> tags,
      String timeToLive);

  /**
   * Delete Document.
   * 
   * @param siteId Optional Grouping siteId
   * @param document {@link DocumentArtifact}
   * @param softDelete Whether to soft delete document
   * @return boolean whether a document was deleted
   */
  boolean deleteDocument(String siteId, DocumentArtifact document, boolean softDelete);

  /**
   * Delete Document Attribute.
   * 
   * @param siteId {@link String}
   * @param document {@link DocumentArtifact}
   * @param attributeKey {@link String}
   * @param validation {@link AttributeValidationType}
   * @param validationAccess {@link AttributeValidationAccess}
   * @return {@link List} {@link DocumentAttributeRecord}
   * @throws ValidationException ValidationException
   */
  List<DocumentAttributeRecord> deleteDocumentAttribute(String siteId, DocumentArtifact document,
      String attributeKey, AttributeValidationType validation,
      AttributeValidationAccess validationAccess) throws ValidationException;

  /**
   * Delete Document Attribute Value.
   * 
   * @param siteId {@link String}
   * @param document {@link DocumentArtifact}
   * @param attributeKey {@link String}
   * @param attributeValue {@link String}
   * @param validationAccess {@link AttributeValidationAccess}
   * @return boolean
   * @throws ValidationException ValidationException
   */
  boolean deleteDocumentAttributeValue(String siteId, DocumentArtifact document,
      String attributeKey, String attributeValue, AttributeValidationAccess validationAccess)
      throws ValidationException;

  /**
   * Delete Document Format.
   * 
   * @param siteId Optional Grouping siteId
   * @param documentId {@link String}
   * @param contentType {@link String}
   * @deprecated method needs to be updated
   */
  @Deprecated
  void deleteDocumentFormat(String siteId, String documentId, String contentType);

  /**
   * Delete All Document Formats.
   * 
   * @param siteId Optional Grouping siteId
   * @param documentId {@link String}
   * @deprecated method to be deleted
   */
  @Deprecated
  void deleteDocumentFormats(String siteId, String documentId);

  /**
   * Delete {@link DocumentTag} by TagKey.
   * 
   * @param siteId {@link String}
   * @param document {@link DocumentArtifact}
   * @param tagKey {@link String}
   */
  void deleteDocumentTag(String siteId, DocumentArtifact document, String tagKey);

  /**
   * Delete Document Tags.
   * 
   * @param siteId Optional Grouping siteId
   * @param document {@link DocumentArtifact}
   */
  void deleteDocumentTags(String siteId, DocumentArtifact document);

  /**
   * Delete Preset by id and type.
   * 
   * @param siteId Optional Grouping siteId
   * @param id {@link String}
   * @deprecated method needs to be updated
   */
  @Deprecated
  void deletePreset(String siteId, String id);

  /**
   * Delete Preset Tag.
   * 
   * @param siteId {@link String}
   * @param id {@link String}
   * @param tag {@link String}
   * @deprecated method needs to be updated
   */
  @Deprecated
  void deletePresetTag(String siteId, String id, String tag);

  /**
   * Delete Preset Tag.
   * 
   * @param siteId {@link String}
   * @param id {@link String}
   * @deprecated method needs to be updated
   */
  @Deprecated
  void deletePresetTags(String siteId, String id);

  /**
   * Delete Presets by type.
   * 
   * @param siteId Optional Grouping siteId
   * @param type {@link String}
   * @deprecated method needs to be updated
   */
  @Deprecated
  void deletePresets(String siteId, String type);

  /**
   * Delete Publish Document.
   * 
   * @param siteId {@link String}
   * @param documentId {@link String}
   * @return boolean
   */
  boolean deletePublishDocument(String siteId, String documentId);

  /**
   * Returns whether document exists.
   * 
   * @param siteId {@link String}
   * @param document {@link DocumentArtifact}
   * @return boolean
   */
  boolean exists(String siteId, DocumentArtifact document);

  /**
   * Whether Document Attribute exists.
   *
   * @param siteId {@link String}
   * @param document {@link DocumentArtifact}
   * @param attributeKey {@link String}
   * @return {@link List} {@link DocumentAttributeRecord}
   */
  boolean existsDocumentAttribute(String siteId, DocumentArtifact document, String attributeKey);

  /**
   * Find {@link DocumentItem}.
   *
   * @param siteId Optional Grouping siteId
   * @param document {@link DocumentArtifact}
   * @return {@link DocumentItem}
   */
  DocumentRecord findDocument(String siteId, DocumentArtifact document);

  /**
   * Find {@link DocumentItem}.
   * 
   * @param siteId Optional Grouping siteId
   * @param document {@link DocumentArtifact}
   * @param includeChildDocuments boolean
   * @param nextToken {@link String}
   * @param limit int
   * @return {@link Pagination} {@link DocumentItem}
   */
  Pagination<DocumentItem> findDocument(String siteId, DocumentArtifact document,
      boolean includeChildDocuments, String nextToken, int limit);

  /**
   * Find Document Attribute.
   * 
   * @param siteId {@link String}
   * @param document {@link DocumentArtifact}
   * @param attributeKey {@link String}
   * @return {@link List} {@link DocumentAttributeRecord}
   */
  List<DocumentAttributeRecord> findDocumentAttribute(String siteId, DocumentArtifact document,
      String attributeKey);

  /**
   * Find {@link DocumentAttributeRecord}.
   * 
   * @param siteId Optional Grouping siteId
   * @param document {@link DocumentArtifact}
   * @param nextToken {@link String}
   * @param limit int
   * @return {@link Pagination} {@link DocumentAttributeRecord}
   */
  Pagination<DocumentAttributeRecord> findDocumentAttributes(String siteId,
      DocumentArtifact document, String nextToken, int limit);

  /**
   * Find {@link DocumentAttributeRecord} by Type.
   * 
   * @param siteId {@link String}
   * @param document {@link DocumentArtifact}
   * @param valueType {@link DocumentAttributeValueType}
   * @param nextToken {@link String}
   * @param limit int
   * @return {@link Pagination} {@link DocumentAttributeRecord}
   */
  Pagination<DocumentAttributeRecord> findDocumentAttributesByType(String siteId,
      DocumentArtifact document, DocumentAttributeValueType valueType, String nextToken, int limit);

  /**
   * Get Document Format.
   * 
   * @param siteId {@link String}
   * @param documentId {@link String}
   * @param contentType {@link String}
   * @return {@link Optional} {@link DocumentFormat}
   * @deprecated method to be deleted
   */
  @Deprecated
  Optional<DocumentFormat> findDocumentFormat(String siteId, String documentId, String contentType);

  /**
   * Get Document Formats.
   * 
   * @param siteId {@link String}
   * @param documentId {@link String}
   * @param nextToken {@link String}
   * @param maxresults int
   * @return {@link Pagination} {@link DocumentFormat}
   * @deprecated method to be deleted
   */
  @Deprecated
  Pagination<DocumentFormat> findDocumentFormats(String siteId, String documentId, String nextToken,
      int maxresults);

  /**
   * Find Document Tag Value.
   * 
   * @param siteId Optional Grouping siteId
   * @param document {@link DocumentArtifact}
   * @param tagKey {@link String}
   * 
   * @return {@link DocumentTag}
   */
  DocumentTag findDocumentTag(String siteId, DocumentArtifact document, String tagKey);

  /**
   * Find Tags for {@link DocumentItem}.
   * 
   * @param siteId Optional Grouping siteId
   * @param document {@link DocumentArtifact}
   * @param nextToken {@link String}
   * @param maxresults int
   * @return {@link Pagination} {@link DocumentTag}
   */
  Pagination<DocumentTag> findDocumentTags(String siteId, DocumentArtifact document,
      String nextToken, int maxresults);

  /**
   * Find {@link DocumentItem}.
   * 
   * @param siteId Optional Grouping siteId
   * @param documents {@link List} {@link DocumentArtifact}
   * @return {@link List} {@link DocumentItem}
   */
  List<DocumentItem> findDocuments(String siteId, List<DocumentArtifact> documents);

  /**
   * Find {@link DocumentItem} by Inserted Date. Order in descending order.
   * 
   * @param siteId Optional Grouping siteId
   * @param date {@link ZonedDateTime}
   * @param nextToken {@link String}
   * @param maxresults int
   * @return {@link Pagination} {@link DocumentItem}
   */
  Pagination<DocumentItem> findDocumentsByDate(String siteId, ZonedDateTime date, String nextToken,
      int maxresults);

  /**
   * Find Document Tags for number of DocumentIds.
   * 
   * @param siteId {@link String}
   * @param documentIds {@link List} {@link String}
   * @param tags {@link List} {@link String}
   * @return {@link Map}
   */
  Map<String, Collection<DocumentTag>> findDocumentsTags(String siteId,
      Collection<String> documentIds, List<String> tags);

  /**
   * Find most recent inserted document {@link ZonedDateTime}.
   * 
   * @return {@link ZonedDateTime}
   */
  ZonedDateTime findMostDocumentDate();

  /**
   * Find Preset.
   * 
   * @param siteId {@link String}
   * @param id {@link String}
   * @return {@link Optional} {@link PresetTag}
   * @deprecated method needs to be updated
   */
  @Deprecated
  Optional<Preset> findPreset(String siteId, String id);

  /**
   * Find Preset Tag.
   * 
   * @param siteId {@link String}
   * @param id {@link String}
   * @param tagKey {@link String}
   * @return {@link Optional} {@link PresetTag}
   * @deprecated method needs to be updated
   */
  @Deprecated
  Optional<PresetTag> findPresetTag(String siteId, String id, String tagKey);

  /**
   * Find Preset Tags.
   * 
   * @param siteId {@link String}
   * @param id {@link String}
   * @param nextToken {@link String}
   * @param maxresults int
   * @return {@link Pagination} {@link PresetTag}
   * @deprecated method needs to be updated
   */
  @Deprecated
  Pagination<PresetTag> findPresetTags(String siteId, String id, String nextToken, int maxresults);

  /**
   * Get Presets.
   * 
   * @param siteId {@link String}
   * @param id {@link String}
   * @param type {@link String}
   * @param name {@link String}
   * @param nextToken {@link String}
   * @param maxresults int
   * @return {@link Pagination} {@link Preset}
   * @deprecated method needs to be updated
   */
  @Deprecated
  Pagination<Preset> findPresets(String siteId, String id, String type, String name,
      String nextToken, int maxresults);

  /**
   * Get Publish Document.
   *
   * @param siteId {@link String}
   * @param documentId {@link String}
   * @return DocumentPublicationRecord
   */
  DocumentPublicationRecord findPublishDocument(String siteId, String documentId);

  /**
   * Find Deleted {@link DocumentItem}.
   * 
   * @param siteId Optional Grouping siteId
   * @param nextToken {@link String}
   * @param limit int
   * @return {@link Pagination} {@link DocumentItem}
   */
  Pagination<DocumentItem> findSoftDeletedDocuments(String siteId, String nextToken, int limit);

  /**
   * Is Folder Exists.
   * 
   * @param siteId {@link String}
   * @param path {@link String}
   * @return boolean
   */
  boolean isFolderExists(String siteId, String path);

  /**
   * Publish Document.
   * 
   * @param siteId {@link String}
   * @param documentId {@link String}
   * @param s3version {@link String}
   * @param path {@link String}
   * @param contentType {@link String}
   * @param userId {@link String}
   */
  void publishDocument(String siteId, String documentId, String s3version, String path,
      String contentType, String userId);

  /**
   * Reindex Document Attributes.
   * 
   * @param siteId {@link String}
   * @param document {@link DocumentArtifact}
   */
  void reindexDocumentAttributes(String siteId, DocumentArtifact document)
      throws ValidationException;

  /**
   * Remove Tag from Document.
   * 
   * @param siteId Optional Grouping siteId
   * @param document {@link DocumentArtifact}
   * @param tagKey {@link String}
   * @param tagValue {@link String}
   * @return boolean
   */
  boolean removeTag(String siteId, DocumentArtifact document, String tagKey, String tagValue);

  /**
   * Remove Tags from Document.
   * 
   * @param siteId Optional Grouping siteId
   * @param document {@link DocumentArtifact}
   * @param tags Tag Names.
   */
  void removeTags(String siteId, DocumentArtifact document, Collection<String> tags);

  /**
   * Restore Soft Deleted Documents.
   * 
   * @param siteId {@link String}
   * @param document {@link DocumentArtifact}
   * @return boolean
   */
  boolean restoreSoftDeletedDocument(String siteId, DocumentArtifact document);

  /**
   * Save Document and Tags.
   * 
   * @param siteId Optional Grouping siteId
   * @param document {@link DocumentItem}
   * @param tags {@link Collection} {@link DocumentTag}
   * @throws ValidationException ValidationException
   */
  void saveDocument(String siteId, DocumentItem document, Collection<DocumentTag> tags)
      throws ValidationException;

  /**
   * Save Document and Tags.
   * 
   * @param siteId Optional Grouping siteId
   * @param document {@link DocumentItem}
   * @param tags {@link Collection} {@link DocumentTag}
   * @param documentAttributes {@link Collection} {@link DocumentAttributeRecord}
   * @param options {@link SaveDocumentOptions}
   * @throws ValidationException ValidationException
   */
  void saveDocument(String siteId, DocumentItem document, Collection<DocumentTag> tags,
      Collection<DocumentAttributeRecord> documentAttributes, SaveDocumentOptions options)
      throws ValidationException;

  /**
   * Save Document Attributes.
   * 
   * @param siteId {@link String}
   * @param document {@link DocumentArtifact}
   * @param attributes {@link Collection} {@link DocumentAttributeRecord}
   * @param validation {@link AttributeValidationType}
   * @param validationAccess {@link AttributeValidationAccess}
   * @throws ValidationException ValidationException
   */
  void saveDocumentAttributes(String siteId, DocumentArtifact document,
      Collection<DocumentAttributeRecord> attributes, AttributeValidationType validation,
      AttributeValidationAccess validationAccess) throws ValidationException;

  /**
   * Save Document Format.
   * 
   * @param siteId {@link String}
   * @param format {@link DocumentFormat}
   * @return {@link DocumentFormat}
   * @deprecated method to be deleted
   */
  @Deprecated
  DocumentFormat saveDocumentFormat(String siteId, DocumentFormat format);

  /**
   * Save {@link DynamicDocumentItem}.
   * 
   * @param siteId {@link String}
   * @param doc {@link DynamicDocumentItem}
   * @return {@link DocumentItem}
   * @throws ValidationException ValidationException
   */
  DocumentItem saveDocumentItemWithTag(String siteId, DynamicDocumentItem doc)
      throws ValidationException;

  /**
   * Save Preset.
   * 
   * @param siteId {@link String}
   * @param id {@link String}
   * @param type {@link String}
   * @param preset {@link Preset}
   * @param tags {@link List} {@link PresetTag}
   * @return {@link Preset}
   * @deprecated method needs to be updated
   */
  @Deprecated
  Preset savePreset(String siteId, String id, String type, Preset preset, List<PresetTag> tags);

  /**
   * Update Document Attributes.
   * 
   * @param siteId {@link String}
   * @param document {@link DocumentArtifact}
   * @param attributes {@link Map}
   */
  void updateDocument(String siteId, DocumentArtifact document,
      Map<String, AttributeValue> attributes);

  /**
   * Update Retention Policy Disposition Date for Last Modified.
   * 
   * @param siteId {@link String}
   * @param document {@link String}
   * @param lastModifiedDate {@link Date}
   */
  void updateRetentionPolicyDispositionDateLastModified(String siteId, DocumentArtifact document,
      Date lastModifiedDate);
}
