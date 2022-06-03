package com.formkiq.plugins.tagschema;

import java.util.Collection;
import java.util.Collections;
import com.formkiq.aws.dynamodb.model.DocumentItem;
import com.formkiq.aws.dynamodb.model.DocumentTag;
import com.formkiq.aws.dynamodb.model.SearchQuery;
import com.formkiq.aws.dynamodb.model.SearchTagCriteria;
import com.formkiq.plugins.validation.ValidationError;

/**
 * 
 * Empty {@link DocumentTagSchemaPlugin}.
 *
 */
public class DocumentTagSchemaPluginEmpty implements DocumentTagSchemaPlugin {

  @Override
  public Collection<DocumentTag> addCompositeKeys(final String siteId, final DocumentItem item,
      final Collection<DocumentTag> tags, final String userId) {
    return tags;
  }

  @Override
  public SearchTagCriteria createMultiTagSearch(final SearchQuery query) {
    return query.tag();
  }

  @Override
  public boolean isActive() {
    return false;
  }

  @Override
  public Collection<ValidationError> validateAddTags(final String siteId, final DocumentItem item,
      final Collection<DocumentTag> tags, final boolean validateRequiredTags) {
    return Collections.emptyList();
  }

  @Override
  public Collection<ValidationError> validateRemoveTags(final String siteId,
      final DocumentItem item, final Collection<String> tags) {
    return Collections.emptyList();
  }

  @Override
  public Collection<ValidationError> validateReplaceTags(final String siteId,
      final DocumentItem item, final Collection<DocumentTag> tags) {
    return Collections.emptyList();
  }
}
