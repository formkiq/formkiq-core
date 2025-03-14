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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import com.formkiq.aws.dynamodb.PaginationMapToken;
import com.formkiq.aws.dynamodb.PaginationResult;
import com.formkiq.aws.dynamodb.PaginationResults;
import com.formkiq.aws.dynamodb.model.DocumentItem;
import com.formkiq.aws.dynamodb.model.DocumentTag;
import com.formkiq.aws.dynamodb.model.DynamicDocumentItem;
import com.formkiq.stacks.dynamodb.attributes.AttributeValidationType;
import com.formkiq.stacks.dynamodb.attributes.AttributeValidationAccess;
import com.formkiq.stacks.dynamodb.attributes.DocumentAttributeRecord;
import com.formkiq.stacks.dynamodb.attributes.DocumentAttributeValueType;
import com.formkiq.stacks.dynamodb.documents.DocumentPublicationRecord;
import com.formkiq.validation.ValidationException;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/** Services for Querying, Updating Documents. */
public interface DocumentService {

  /** The Default maximum results returned. */
  int MAX_RESULTS = 10;

  /** Soft Deleted Prefix. */
  String SOFT_DELETE = "softdelete#";

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
   * Add Tags to {@link Collection} of Documents.
   * 
   * @param siteId Optional Grouping siteId
   * @param tags {@link Map} {@link Collection} {@link DocumentTag}
   * @param timeToLive {@link String}
   */
  void addTags(String siteId, Map<String, Collection<DocumentTag>> tags, String timeToLive);

  /**
   * Add Tags to Document.
   * 
   * @param siteId Optional Grouping siteId
   * @param documentId {@link String}
   * @param tags {@link Collection} {@link DocumentTag}
   * @param timeToLive {@link String}
   */
  void addTags(String siteId, String documentId, Collection<DocumentTag> tags, String timeToLive);

  /**
   * Delete Document.
   * 
   * @param siteId Optional Grouping siteId
   * @param documentId {@link String}
   * @param softDelete Whether to soft delete document
   * @return boolean whether a document was deleted
   */
  boolean deleteDocument(String siteId, String documentId, boolean softDelete);

  /**
   * Delete Document Attribute.
   * 
   * @param siteId {@link String}
   * @param documentId {@link String}
   * @param attributeKey {@link String}
   * @param validation {@link AttributeValidationType}
   * @param validationAccess {@link AttributeValidationAccess}
   * @return {@link List} {@link DocumentAttributeRecord}
   * @throws ValidationException ValidationException
   */
  List<DocumentAttributeRecord> deleteDocumentAttribute(String siteId, String documentId,
      String attributeKey, AttributeValidationType validation,
      AttributeValidationAccess validationAccess) throws ValidationException;

  /**
   * Delete Document Attribute Value.
   * 
   * @param siteId {@link String}
   * @param documentId {@link String}
   * @param attributeKey {@link String}
   * @param attributeValue {@link String}
   * @param validationAccess {@link AttributeValidationAccess}
   * @return boolean
   * @throws ValidationException ValidationException
   */
  boolean deleteDocumentAttributeValue(String siteId, String documentId, String attributeKey,
      String attributeValue, AttributeValidationAccess validationAccess) throws ValidationException;

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
   */
  void deleteDocumentFormats(String siteId, String documentId);

  /**
   * Delete {@link DocumentTag} by TagKey.
   * 
   * @param siteId {@link String}
   * @param documentId {@link String}
   * @param tagKey {@link String}
   */
  void deleteDocumentTag(String siteId, String documentId, String tagKey);

  /**
   * Delete Document Tags.
   * 
   * @param siteId Optional Grouping siteId
   * @param documentId {@link String}
   */
  void deleteDocumentTags(String siteId, String documentId);

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
   * Delete Presets by type.
   * 
   * @param siteId Optional Grouping siteId
   * @param type {@link String}
   * @deprecated method needs to be updated
   */
  @Deprecated
  void deletePresets(String siteId, String type);

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
   * Returns whether document exists.
   * 
   * @param siteId {@link String}
   * @param documentId {@link String}
   * @return boolean
   */
  boolean exists(String siteId, String documentId);

  /**
   * Whether Document Attribute exists.
   *
   * @param siteId {@link String}
   * @param documentId {@link String}
   * @param attributeKey {@link String}
   * @return {@link List} {@link DocumentAttributeRecord}
   */
  boolean existsDocumentAttribute(String siteId, String documentId, String attributeKey);

  /**
   * Find {@link DocumentItem}.
   * 
   * @param siteId Optional Grouping siteId
   * @param documentId {@link String}
   * 
   * @return {@link DocumentItem}
   */
  DocumentItem findDocument(String siteId, String documentId);

  /**
   * Find {@link DocumentItem}.
   * 
   * @param siteId Optional Grouping siteId
   * @param documentId {@link String}
   * @param includeChildDocuments boolean
   * @param token {@link PaginationMapToken}
   * @param limit int
   * @return {@link PaginationResults} {@link DocumentItem}
   */
  PaginationResult<DocumentItem> findDocument(String siteId, String documentId,
      boolean includeChildDocuments, PaginationMapToken token, int limit);

  /**
   * Find Document Attribute.
   * 
   * @param siteId {@link String}
   * @param documentId {@link String}
   * @param attributeKey {@link String}
   * @return {@link List} {@link DocumentAttributeRecord}
   */
  List<DocumentAttributeRecord> findDocumentAttribute(String siteId, String documentId,
      String attributeKey);

  /**
   * Find {@link DocumentAttributeRecord}.
   * 
   * @param siteId Optional Grouping siteId
   * @param documentId {@link String}
   * @param pagination {@link PaginationMapToken}
   * @param limit int
   * @return {@link PaginationResults} {@link DocumentAttributeRecord}
   */
  PaginationResults<DocumentAttributeRecord> findDocumentAttributes(String siteId,
      String documentId, PaginationMapToken pagination, int limit);

  /**
   * Find {@link DocumentAttributeRecord} by Type.
   * 
   * @param siteId {@link String}
   * @param documentId {@link String}
   * @param valueType {@link DocumentAttributeValueType}
   * @param pagination {@link PaginationMapToken}
   * @param limit int
   * @return {@link PaginationResults} {@link DocumentAttributeRecord}
   */
  PaginationResults<DocumentAttributeRecord> findDocumentAttributesByType(String siteId,
      String documentId, DocumentAttributeValueType valueType, PaginationMapToken pagination,
      int limit);

  /**
   * Get Document Format.
   * 
   * @param siteId {@link String}
   * @param documentId {@link String}
   * @param contentType {@link String}
   * @return {@link Optional} {@link DocumentFormat}
   */
  Optional<DocumentFormat> findDocumentFormat(String siteId, String documentId, String contentType);

  /**
   * Get Document Formats.
   * 
   * @param siteId {@link String}
   * @param documentId {@link String}
   * @param token {@link PaginationMapToken}
   * @param maxresults int
   * @return {@link PaginationResults} {@link DocumentFormat}
   */
  PaginationResults<DocumentFormat> findDocumentFormats(String siteId, String documentId,
      PaginationMapToken token, int maxresults);

  /**
   * Find {@link DocumentItem}.
   * 
   * @param siteId Optional Grouping siteId
   * @param documentIds {@link List} {@link String}
   * @return {@link List} {@link DocumentItem}
   */
  List<DocumentItem> findDocuments(String siteId, List<String> documentIds);

  /**
   * Find {@link DocumentItem} by Inserted Date. Order in descending order.
   * 
   * @param siteId Optional Grouping siteId
   * @param date {@link ZonedDateTime}
   * @param token {@link PaginationMapToken}
   * @param maxresults int
   * @return {@link PaginationResults} {@link DocumentItem}
   */
  PaginationResults<DocumentItem> findDocumentsByDate(String siteId, ZonedDateTime date,
      PaginationMapToken token, int maxresults);

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
   * Find Document Tag Value.
   * 
   * @param siteId Optional Grouping siteId
   * @param documentId {@link String}
   * @param tagKey {@link String}
   * 
   * @return {@link DocumentTag}
   */
  DocumentTag findDocumentTag(String siteId, String documentId, String tagKey);

  /**
   * Find Tags for {@link DocumentItem}.
   * 
   * @param siteId Optional Grouping siteId
   * @param documentId {@link String}
   * @param pagination {@link PaginationMapToken}
   * @param maxresults int
   * @return {@link PaginationResults} {@link DocumentTag}
   */
  PaginationResults<DocumentTag> findDocumentTags(String siteId, String documentId,
      PaginationMapToken pagination, int maxresults);

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
   * Get Presets.
   * 
   * @param siteId {@link String}
   * @param id {@link String}
   * @param type {@link String}
   * @param name {@link String}
   * @param token {@link PaginationMapToken}
   * @param maxresults int
   * @return {@link PaginationResults} {@link Preset}
   * @deprecated method needs to be updated
   */
  @Deprecated
  PaginationResults<Preset> findPresets(String siteId, String id, String type, String name,
      PaginationMapToken token, int maxresults);

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
   * @param token {@link PaginationMapToken}
   * @param maxresults int
   * @return {@link PaginationResults} {@link PresetTag}
   * @deprecated method needs to be updated
   */
  @Deprecated
  PaginationResults<PresetTag> findPresetTags(String siteId, String id, PaginationMapToken token,
      int maxresults);

  /**
   * Find Deleted {@link DocumentItem}.
   * 
   * @param siteId Optional Grouping siteId
   * @param token {@link Map}
   * @param limit int
   * @return {@link PaginationResults} {@link DocumentItem}
   */
  PaginationResults<DocumentItem> findSoftDeletedDocuments(String siteId,
      Map<String, AttributeValue> token, int limit);

  /**
   * Is Folder Exists.
   * 
   * @param siteId {@link String}
   * @param path {@link String}
   * @return boolean
   */
  boolean isFolderExists(String siteId, String path);

  /**
   * Remove Tag from Document.
   * 
   * @param siteId Optional Grouping siteId
   * @param documentId {@link String}
   * @param tagKey {@link String}
   * @param tagValue {@link String}
   * @return boolean
   */
  boolean removeTag(String siteId, String documentId, String tagKey, String tagValue);

  /**
   * Remove Tags from Document.
   * 
   * @param siteId Optional Grouping siteId
   * @param documentId {@link String}
   * @param tags Tag Names.
   */
  void removeTags(String siteId, String documentId, Collection<String> tags);

  /**
   * Restore Soft Deleted Documents.
   * 
   * @param siteId {@link String}
   * @param documentId {@link String}
   * @return boolean
   */
  boolean restoreSoftDeletedDocument(String siteId, String documentId);

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
   * @param documentId {@link String}
   * @param attributes {@link Collection} {@link DocumentAttributeRecord}
   * @param validation {@link AttributeValidationType}
   * @param validationAccess {@link AttributeValidationAccess}
   * @throws ValidationException ValidationException
   */
  void saveDocumentAttributes(String siteId, String documentId,
      Collection<DocumentAttributeRecord> attributes, AttributeValidationType validation,
      AttributeValidationAccess validationAccess) throws ValidationException;

  /**
   * Save Document Format.
   * 
   * @param siteId {@link String}
   * @param format {@link DocumentFormat}
   * @return {@link DocumentFormat}
   */
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
   * @param documentId {@link String}
   * @param attributes {@link Map}
   */
  void updateDocument(String siteId, String documentId, Map<String, AttributeValue> attributes);

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
   * Get Publish Document.
   *
   * @param siteId {@link String}
   * @param documentId {@link String}
   * @return DocumentPublicationRecord
   */
  DocumentPublicationRecord findPublishDocument(String siteId, String documentId);

  /**
   * Delete Publish Document.
   * 
   * @param siteId {@link String}
   * @param documentId {@link String}
   * @return boolean
   */
  boolean deletePublishDocument(String siteId, String documentId);

  /**
   * Reindex Document Attributes.
   * 
   * @param siteId {@link String}
   * @param documentId {@link String}
   */
  void reindexDocumentAttributes(String siteId, String documentId) throws ValidationException;
}
