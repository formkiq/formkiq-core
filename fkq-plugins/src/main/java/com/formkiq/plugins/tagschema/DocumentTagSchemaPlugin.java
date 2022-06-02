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
   * Add {@link DocumentTag} Event.
   * 
   * @param siteId {@link String}
   * @param item {@link DocumentItem}
   * @param tags {@link Collection} {@link DocumentTag}
   * @return {@link Collection} {@link ValidationError}
   */
  Collection<ValidationError> addTagsEvent(String siteId, DocumentItem item,
      Collection<DocumentTag> tags);

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
   * Delete {@link DocumentTag} Event.
   * 
   * @param siteId {@link String}
   * @param item {@link DocumentItem}
   * @param tags {@link Collection} {@link String}
   * @return {@link Collection} {@link ValidationError}
   */
  Collection<ValidationError> removeTagsEvent(String siteId, DocumentItem item,
      Collection<String> tags);
  
  /**
   * Replace {@link DocumentTag} Event.
   * 
   * @param siteId {@link String}
   * @param item {@link DocumentItem}
   * @param tags {@link Collection} {@link DocumentTag}
   * @return {@link Collection} {@link ValidationError}
   */
  Collection<ValidationError> replaceTagsEvent(String siteId, DocumentItem item,
      Collection<DocumentTag> tags);
}
