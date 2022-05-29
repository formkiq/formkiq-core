package com.formkiq.aws.services.lambda.events;

import java.util.Collection;
import com.formkiq.aws.dynamodb.model.DocumentItem;
import com.formkiq.aws.dynamodb.model.DocumentTag;
import com.formkiq.aws.dynamodb.model.DynamicDocumentItem;
import com.formkiq.aws.services.lambda.ValidationError;

/**
 * 
 * Document Events.
 *
 */
public interface DocumentTagSchemaEvents {

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
   * Add {@link DocumentTag} Event.
   * 
   * @param siteId {@link String}
   * @param item {@link DocumentItem}
   * @return {@link Collection} {@link ValidationError}
   */
  Collection<ValidationError> addTagsEvent(String siteId, DynamicDocumentItem item);

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
