package com.formkiq.plugins.tagschema;

import java.util.Collection;
import com.formkiq.aws.dynamodb.model.DocumentTag;

/**
 * 
 * Interface for loading of {@link DocumentTag}.
 *
 */
public interface DocumentTagLoader {

  /**
   * Load {@link DocumentTag} by Tag Key.
   * @param siteId {@link String}
   * @param documentId {@link String}
   * @param tagKeys {@link Collection} {@link String}
   * @return {@link Collection} {@link DocumentTag}
   */
  Collection<DocumentTag> findDocumentTags(String siteId, String documentId,
      Collection<String> tagKeys);
}
