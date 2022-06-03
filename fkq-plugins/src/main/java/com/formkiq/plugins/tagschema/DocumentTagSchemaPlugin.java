package com.formkiq.plugins.tagschema;

import java.util.Collection;
import com.formkiq.aws.dynamodb.model.DocumentItem;
import com.formkiq.aws.dynamodb.model.DocumentTag;
import com.formkiq.aws.dynamodb.model.SearchQuery;
import com.formkiq.aws.dynamodb.model.SearchTagCriteria;
import com.formkiq.plugins.validation.ValidationError;

/**
 * 
 * Document Events.
 *
 */
public interface DocumentTagSchemaPlugin {

  /**
   * Add Composite Keys.
   * @param siteId {@link String}
   * @param item {@link DocumentItem}
   * @param tags {@link Collection} {@link DocumentTag}
   * @param userId {@link String} 
   * @return {@link Collection} {@link DocumentTag}
   */
  Collection<DocumentTag> addCompositeKeys(String siteId, DocumentItem item,
      Collection<DocumentTag> tags, String userId);

  /**
   * Create Multi-Value {@link SearchTagCriteria}.
   * @param query {@link SearchQuery}
   * @return {@link SearchTagCriteria}
   */
  SearchTagCriteria createMultiTagSearch(SearchQuery query);
  
  /**
   * Is TagSchema Active.
   * @return boolean
   */
  boolean isActive();
  
  /**
   * Validate Add {@link DocumentTag}.
   * 
   * @param siteId {@link String}
   * @param item {@link DocumentItem}
   * @param tags {@link Collection} {@link DocumentTag}
   * @param validateRequiredTags boolean
   * @return {@link Collection} {@link ValidationError}
   */
  Collection<ValidationError> validateAddTags(String siteId, DocumentItem item,
      Collection<DocumentTag> tags, boolean validateRequiredTags);
  
  /**
   * Delete {@link DocumentTag} Event.
   * 
   * @param siteId {@link String}
   * @param item {@link DocumentItem}
   * @param tags {@link Collection} {@link String}
   * @return {@link Collection} {@link ValidationError}
   */
  Collection<ValidationError> validateRemoveTags(String siteId, DocumentItem item,
      Collection<String> tags);
  
  /**
   * Replace {@link DocumentTag} Event.
   * 
   * @param siteId {@link String}
   * @param item {@link DocumentItem}
   * @param tags {@link Collection} {@link DocumentTag}
   * @return {@link Collection} {@link ValidationError}
   */
  Collection<ValidationError> validateReplaceTags(String siteId, DocumentItem item,
      Collection<DocumentTag> tags);
}
