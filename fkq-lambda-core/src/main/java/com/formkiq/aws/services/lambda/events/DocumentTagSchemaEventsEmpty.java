package com.formkiq.aws.services.lambda.events;

import java.util.Collection;
import java.util.Collections;
import com.formkiq.aws.dynamodb.model.DocumentItem;
import com.formkiq.aws.dynamodb.model.DocumentTag;
import com.formkiq.aws.dynamodb.model.DynamicDocumentItem;
import com.formkiq.aws.services.lambda.ValidationError;

/**
 * 
 * Empty {@link DocumentTagSchemaEvents}.
 *
 */
public class DocumentTagSchemaEventsEmpty implements DocumentTagSchemaEvents {
  @Override
  public Collection<ValidationError> addTagsEvent(final String siteId, final DocumentItem item,
      final Collection<DocumentTag> tags) {
    return Collections.emptyList();
  }

  @Override
  public Collection<ValidationError> addTagsEvent(final String siteId,
      final DynamicDocumentItem item) {
    return Collections.emptyList();
  }

  @Override
  public Collection<ValidationError> removeTagsEvent(final String siteId, final DocumentItem item,
      final Collection<String> tags) {
    return Collections.emptyList();
  }

  @Override
  public Collection<ValidationError> replaceTagsEvent(final String siteId, final DocumentItem item,
      final Collection<DocumentTag> tags) {
    return Collections.emptyList();
  }
}
