package com.formkiq.stacks.api;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import com.formkiq.aws.dynamodb.model.DocumentItem;
import com.formkiq.aws.dynamodb.model.DocumentTag;
import com.formkiq.aws.dynamodb.model.SearchQuery;
import com.formkiq.aws.dynamodb.model.SearchTagCriteria;
import com.formkiq.plugins.tagschema.DocumentTagSchemaPlugin;
import com.formkiq.plugins.validation.ValidationError;

/**
 * 
 * {@link DocumentTagSchemaPlugin} that returns test {@link ValidationError}.
 *
 */
public class DocumentTagSchemaReturnNewTags implements DocumentTagSchemaPlugin {

  @Override
  public Collection<DocumentTag> addCompositeKeys(final String siteId, final DocumentItem item,
      final Collection<DocumentTag> tags, final String userId, final boolean validateRequiredTags,
      final Collection<ValidationError> errors) {    
    Collection<DocumentTag> list = Arrays
        .asList(new DocumentTag(item.getDocumentId(), "testtag", "testvalue", new Date(), "joe"));
    return list;
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