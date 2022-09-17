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

import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import com.formkiq.aws.dynamodb.PaginationMapToken;
import com.formkiq.aws.dynamodb.PaginationResults;
import com.formkiq.aws.dynamodb.model.DocumentItem;
import com.formkiq.aws.dynamodb.model.DocumentTag;
import com.formkiq.aws.dynamodb.model.DynamicDocumentItem;
import com.formkiq.plugins.tagschema.DocumentTagLoader;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

/** Services for Querying, Updating Documents. */
public interface DocumentService extends DocumentTagLoader {

  /** The Default maximum results returned. */
  int MAX_RESULTS = 10;
  /** System Defined Tags. */
  Set<String> SYSTEM_DEFINED_TAGS =
      Set.of("untagged", "path", "CLAMAV_SCAN_STATUS", "CLAMAV_SCAN_TIMESTAMP", "userId");

  /**
   * Add Tags to Document.
   * 
   * @param dbClient {@link DynamoDbClient}
   * 
   * @param siteId Optional Grouping siteId
   * @param documentId {@link String}
   * @param tags {@link Collection} {@link DocumentTag}
   * @param timeToLive {@link String}
   */
  void addTags(DynamoDbClient dbClient, String siteId, String documentId,
      Collection<DocumentTag> tags, String timeToLive);

  /**
   * Delete Document.
   * 
   * @param dbClient {@link DynamoDbClient}
   *
   * @param siteId Optional Grouping siteId
   * @param documentId {@link String}
   */
  void deleteDocument(DynamoDbClient dbClient, String siteId, String documentId);

  /**
   * Delete Document Format.
   * 
   * @param dbClient {@link DynamoDbClient}
   *
   * @param siteId Optional Grouping siteId
   * @param documentId {@link String}
   * @param contentType {@link String}
   * @deprecated method needs to be updated
   */
  @Deprecated
  void deleteDocumentFormat(DynamoDbClient dbClient, String siteId, String documentId,
      String contentType);

  /**
   * Delete All Document Formats.
   * 
   * @param dbClient {@link DynamoDbClient}
   *
   * @param siteId Optional Grouping siteId
   * @param documentId {@link String}
   */
  void deleteDocumentFormats(DynamoDbClient dbClient, String siteId, String documentId);

  /**
   * Delete {@link DocumentTag} by TagKey.
   * 
   * @param dbClient {@link DynamoDbClient}
   * @param siteId {@link String}
   * @param documentId {@link String}
   * @param tagKey {@link String}
   */
  void deleteDocumentTag(DynamoDbClient dbClient, String siteId, String documentId, String tagKey);

  /**
   * Delete Document Tags.
   * 
   * @param dbClient {@link DynamoDbClient}
   * @param siteId Optional Grouping siteId
   * @param documentId {@link String}
   */
  void deleteDocumentTags(DynamoDbClient dbClient, String siteId, String documentId);

  /**
   * Delete Preset by id and type.
   * 
   * @param dbClient {@link DynamoDbClient}
   * @param siteId Optional Grouping siteId
   * @param id {@link String}
   * @deprecated method needs to be updated
   */
  @Deprecated
  void deletePreset(DynamoDbClient dbClient, String siteId, String id);

  /**
   * Delete Presets by type.
   * 
   * @param dbClient {@link DynamoDbClient}
   * @param siteId Optional Grouping siteId
   * @param type {@link String}
   * @deprecated method needs to be updated
   */
  @Deprecated
  void deletePresets(DynamoDbClient dbClient, String siteId, String type);

  /**
   * Delete Preset Tag.
   * 
   * @param dbClient {@link DynamoDbClient}
   * @param siteId {@link String}
   * @param id {@link String}
   * @param tag {@link String}
   * @deprecated method needs to be updated
   */
  @Deprecated
  void deletePresetTag(DynamoDbClient dbClient, String siteId, String id, String tag);

  /**
   * Delete Preset Tag.
   * 
   * @param dbClient {@link DynamoDbClient}
   * @param siteId {@link String}
   * @param id {@link String}
   * @deprecated method needs to be updated
   */
  @Deprecated
  void deletePresetTags(DynamoDbClient dbClient, String siteId, String id);

  /**
   * Returns whether document exists.
   * 
   * @param dbClient {@link DynamoDbClient}
   * @param siteId {@link String}
   * @param documentId {@link String}
   * @return boolean
   */
  boolean exists(DynamoDbClient dbClient, String siteId, String documentId);

  /**
   * Find {@link DocumentItem}.
   * 
   * @param dbClient {@link DynamoDbClient}
   * @param siteId Optional Grouping siteId
   * @param documentId {@link String}
   * 
   * @return {@link DocumentItem}
   */
  DocumentItem findDocument(DynamoDbClient dbClient, String siteId, String documentId);

  /**
   * Find {@link DocumentItem}.
   * 
   * @param dbClient {@link DynamoDbClient}
   * @param siteId Optional Grouping siteId
   * @param documentId {@link String}
   * @param includeChildDocuments boolean
   * @param token {@link PaginationMapToken}
   * @param limit int
   * @return {@link PaginationResults} {@link DocumentItem}
   */
  PaginationResult<DocumentItem> findDocument(DynamoDbClient dbClient, String siteId,
      String documentId, boolean includeChildDocuments, PaginationMapToken token, int limit);

  /**
   * Get Document Format.
   * 
   * @param dbClient {@link DynamoDbClient}
   * @param siteId {@link String}
   * @param documentId {@link String}
   * @param contentType {@link String}
   * @return {@link Optional} {@link DocumentFormat}
   */
  Optional<DocumentFormat> findDocumentFormat(DynamoDbClient dbClient, String siteId,
      String documentId, String contentType);

  /**
   * Get Document Formats.
   * 
   * @param dbClient {@link DynamoDbClient}
   * @param siteId {@link String}
   * @param documentId {@link String}
   * @param token {@link PaginationMapToken}
   * @param maxresults int
   * @return {@link PaginationResults} {@link DocumentFormat}
   */
  PaginationResults<DocumentFormat> findDocumentFormats(DynamoDbClient dbClient, String siteId,
      String documentId, PaginationMapToken token, int maxresults);

  /**
   * Find {@link DocumentItem}.
   * 
   * @param dbClient {@link DynamoDbClient}
   * @param siteId Optional Grouping siteId
   * @param documentIds {@link List} {@link String}
   * @return {@link List} {@link DocumentItem}
   */
  List<DocumentItem> findDocuments(DynamoDbClient dbClient, String siteId,
      List<String> documentIds);

  /**
   * Find {@link DocumentItem} by Inserted Date. Order in descending order.
   * 
   * @param dbClient {@link DynamoDbClient}
   * @param siteId Optional Grouping siteId
   * @param date {@link ZonedDateTime}
   * @param token {@link PaginationMapToken}
   * @param maxresults int
   * @return {@link PaginationResults} {@link DocumentItem}
   */
  PaginationResults<DocumentItem> findDocumentsByDate(DynamoDbClient dbClient, String siteId,
      ZonedDateTime date, PaginationMapToken token, int maxresults);

  /**
   * Find Document Tags for number of DocumentIds.
   * 
   * @param dbClient {@link DynamoDbClient}
   * @param siteId {@link String}
   * @param documentIds {@link List} {@link String}
   * @param tags {@link List} {@link String}
   * @return {@link Map}
   */
  Map<String, Collection<DocumentTag>> findDocumentsTags(DynamoDbClient dbClient, String siteId,
      List<String> documentIds, List<String> tags);

  /**
   * Find Document Tag Value.
   * 
   * @param dbClient {@link DynamoDbClient}
   * @param siteId Optional Grouping siteId
   * @param documentId {@link String}
   * @param tagKey {@link String}
   * 
   * @return {@link DocumentTag}
   */
  DocumentTag findDocumentTag(DynamoDbClient dbClient, String siteId, String documentId,
      String tagKey);

  /**
   * Find Tags for {@link DocumentItem}.
   * 
   * @param dbClient {@link DynamoDbClient}
   * @param siteId Optional Grouping siteId
   * @param documentId {@link String}
   * @param pagination {@link PaginationMapToken}
   * @param maxresults int
   * @return {@link PaginationResults} {@link DocumentTag}
   */
  PaginationResults<DocumentTag> findDocumentTags(DynamoDbClient dbClient, String siteId,
      String documentId, PaginationMapToken pagination, int maxresults);

  /**
   * Find most recent inserted document {@link ZonedDateTime}.
   * 
   * @param dbClient {@link DynamoDbClient}
   * @return {@link ZonedDateTime}
   */
  ZonedDateTime findMostDocumentDate(DynamoDbClient dbClient);

  /**
   * Find Preset.
   * 
   * @param dbClient {@link DynamoDbClient}
   * @param siteId {@link String}
   * @param id {@link String}
   * @return {@link Optional} {@link PresetTag}
   * @deprecated method needs to be updated
   */
  @Deprecated
  Optional<Preset> findPreset(DynamoDbClient dbClient, String siteId, String id);

  /**
   * Get Presets.
   * 
   * @param dbClient {@link DynamoDbClient}
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
  PaginationResults<Preset> findPresets(DynamoDbClient dbClient, String siteId, String id,
      String type, String name, PaginationMapToken token, int maxresults);

  /**
   * Find Preset Tag.
   * 
   * @param dbClient {@link DynamoDbClient}
   * @param siteId {@link String}
   * @param id {@link String}
   * @param tagKey {@link String}
   * @return {@link Optional} {@link PresetTag}
   * @deprecated method needs to be updated
   */
  @Deprecated
  Optional<PresetTag> findPresetTag(DynamoDbClient dbClient, String siteId, String id,
      String tagKey);

  /**
   * Find Preset Tags.
   * 
   * @param dbClient {@link DynamoDbClient}
   * @param siteId {@link String}
   * @param id {@link String}
   * @param token {@link PaginationMapToken}
   * @param maxresults int
   * @return {@link PaginationResults} {@link PresetTag}
   * @deprecated method needs to be updated
   */
  @Deprecated
  PaginationResults<PresetTag> findPresetTags(DynamoDbClient dbClient, String siteId, String id,
      PaginationMapToken token, int maxresults);

  /**
   * Remove Tag from Document.
   * 
   * @param dbClient {@link DynamoDbClient}
   * @param siteId Optional Grouping siteId
   * @param documentId {@link String}
   * @param tagKey {@link String}
   * @param tagValue {@link String}
   * @return boolean
   */
  boolean removeTag(DynamoDbClient dbClient, String siteId, String documentId, String tagKey,
      String tagValue);

  /**
   * Remove Tags from Document.
   * 
   * @param dbClient {@link DynamoDbClient}
   * @param siteId Optional Grouping siteId
   * @param documentId {@link String}
   * @param tags Tag Names.
   */
  void removeTags(DynamoDbClient dbClient, String siteId, String documentId,
      Collection<String> tags);

  /**
   * Save Document and Tags.
   * 
   * @param dbClient {@link DynamoDbClient}
   * @param siteId Optional Grouping siteId
   * @param document {@link DocumentItem}
   * @param tags {@link Collection} {@link DocumentTag}
   */
  void saveDocument(DynamoDbClient dbClient, String siteId, DocumentItem document,
      Collection<DocumentTag> tags);

  /**
   * Save Document Format.
   * 
   * @param dbClient {@link DynamoDbClient}
   * @param siteId {@link String}
   * @param format {@link DocumentFormat}
   * @return {@link DocumentFormat}
   */
  DocumentFormat saveDocumentFormat(DynamoDbClient dbClient, String siteId, DocumentFormat format);

  /**
   * Save {@link DynamicDocumentItem}.
   * 
   * @param dbClient {@link DynamoDbClient}
   * @param siteId {@link String}
   * @param doc {@link DynamicDocumentItem}
   * @return {@link DocumentItem}
   */
  DocumentItem saveDocumentItemWithTag(DynamoDbClient dbClient, String siteId,
      DynamicDocumentItem doc);

  /**
   * Save Preset.
   * 
   * @param dbClient {@link DynamoDbClient}
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
  Preset savePreset(DynamoDbClient dbClient, String siteId, String id, String type, Preset preset,
      List<PresetTag> tags);
}
