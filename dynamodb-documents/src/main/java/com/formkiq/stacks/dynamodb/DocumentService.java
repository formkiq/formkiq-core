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
package com.formkiq.stacks.dynamodb;

import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/** Services for Querying, Updating Documents. */
public interface DocumentService {

  /** Date Format. */
  String DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ssZ";
  /** The Default maximum results returned. */
  int MAX_RESULTS = 10;
  /** System Defined Tags. */
  Set<String> SYSTEM_DEFINED_TAGS =
      Set.of("untagged", "path", "CLAMAV_SCAN_STATUS", "CLAMAV_SCAN_TIMESTAMP");

  /**
   * Add Tags to Document.
   * 
   * @param siteId Optional Grouping siteId
   * @param documentId {@link String}
   * @param tags {@link Collection} {@link DocumentTag}
   */
  void addTags(String siteId, String documentId, Collection<DocumentTag> tags);

  /**
   * Delete Document.
   *
   * @param siteId Optional Grouping siteId
   * @param documentId {@link String}
   */
  void deleteDocument(String siteId, String documentId);

  /**
   * Delete Document Format.
   *
   * @param siteId Optional Grouping siteId
   * @param documentId {@link String}
   * @param contentType {@link String}
   */
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
   */
  void deletePreset(String siteId, String id);

  /**
   * Delete Presets by type.
   *
   * @param siteId Optional Grouping siteId
   * @param type {@link String}
   */
  void deletePresets(String siteId, String type);

  /**
   * Delete Preset Tag.
   * 
   * @param siteId {@link String}
   * @param id {@link String}
   * @param tag {@link String}
   */
  void deletePresetTag(String siteId, String id, String tag);

  /**
   * Delete Preset Tag.
   * 
   * @param siteId {@link String}
   * @param id {@link String}
   */
  void deletePresetTags(String siteId, String id);

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
   * @return {@link DocumentItem}
   */
  DocumentItem findDocument(String siteId, String documentId, boolean includeChildDocuments);

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
   * @param documentIds {@link String}
   * @return {@link List} {@link DocumentItem}
   */
  List<DocumentItem> findDocuments(String siteId, Collection<String> documentIds);

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
   * Get Presets.
   * 
   * @param siteId {@link String}
   * @param id {@link String}
   * @param type {@link String}
   * @param name {@link String}
   * @param token {@link PaginationMapToken}
   * @param maxresults int
   * @return {@link PaginationResults} {@link Preset}
   */
  PaginationResults<Preset> findPresets(String siteId, String id, String type, String name,
      PaginationMapToken token, int maxresults);

  /**
   * Find Preset.
   * 
   * @param siteId {@link String}
   * @param id {@link String}
   * @return {@link Optional} {@link PresetTag}
   */
  Optional<Preset> findPreset(String siteId, String id);

  /**
   * Find Preset Tag.
   * 
   * @param siteId {@link String}
   * @param id {@link String}
   * @param tagKey {@link String}
   * @return {@link Optional} {@link PresetTag}
   */
  Optional<PresetTag> findPresetTag(String siteId, String id, String tagKey);

  /**
   * Find Preset Tags.
   * 
   * @param siteId {@link String}
   * @param id {@link String}
   * @param token {@link PaginationMapToken}
   * @param maxresults int
   * @return {@link PaginationResults} {@link PresetTag}
   */
  PaginationResults<PresetTag> findPresetTags(String siteId, String id, PaginationMapToken token,
      int maxresults);

  /**
   * Remove Tags from Document.
   *
   * @param siteId Optional Grouping siteId
   * @param documentId {@link String}
   * @param tags Tag Names.
   */
  void removeTags(String siteId, String documentId, Collection<String> tags);

  /**
   * Save Document and Tags.
   *
   * @param siteId Optional Grouping siteId
   * @param document {@link DocumentItem}
   * @param tags {@link Collection} {@link DocumentTag}
   */
  void saveDocument(String siteId, DocumentItem document, Collection<DocumentTag> tags);

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
   */
  DocumentItem saveDocumentItemWithTag(String siteId, DynamicDocumentItem doc);

  /**
   * Save Preset.
   * 
   * @param siteId {@link String}
   * @param id {@link String}
   * @param type {@link String}
   * @param preset {@link Preset}
   * @param tags {@link List} {@link PresetTag}
   * @return {@link Preset}
   */
  Preset savePreset(String siteId, String id, String type, Preset preset, List<PresetTag> tags);
}
