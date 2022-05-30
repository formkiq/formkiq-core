package com.formkiq.aws.services.lambda.events;

import java.util.Collection;
import com.formkiq.aws.dynamodb.model.DocumentItem;
import com.formkiq.aws.dynamodb.model.DocumentTag;
import com.formkiq.aws.services.lambda.exceptions.BadException;
import com.formkiq.aws.services.lambda.validation.ValidationError;

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
   * @throws BadException BadException
   */
  Collection<ValidationError> addTagsEvent(String siteId, DocumentItem item,
      Collection<DocumentTag> tags) throws BadException;

  /**
   * Delete {@link DocumentTag} Event.
   * 
   * @param siteId {@link String}
   * @param item {@link DocumentItem}
   * @param tags {@link Collection} {@link String}
   * @return {@link Collection} {@link ValidationError}
   * @throws BadException BadException
   */
  Collection<ValidationError> removeTagsEvent(String siteId, DocumentItem item,
      Collection<String> tags) throws BadException;
  
  /**
   * Replace {@link DocumentTag} Event.
   * 
   * @param siteId {@link String}
   * @param item {@link DocumentItem}
   * @param tags {@link Collection} {@link DocumentTag}
   * @return {@link Collection} {@link ValidationError}
   * @throws BadException BadException
   */
  Collection<ValidationError> replaceTagsEvent(String siteId, DocumentItem item,
      Collection<DocumentTag> tags) throws BadException;
}
