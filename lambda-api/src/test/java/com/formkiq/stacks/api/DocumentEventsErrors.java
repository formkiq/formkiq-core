package com.formkiq.stacks.api;

import java.util.Arrays;
import java.util.Collection;
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
public class DocumentEventsErrors implements DocumentTagSchemaPlugin {
  private static final class DummyValidationError implements ValidationError {
    /** Test Error Message. */
    private String error = "test error";
    /** Test Key. */
    private String key = "type";
    @Override
    public String error() {
      return this.error;
    }

    @Override
    public String key() {
      return this.key;
    }
  }

  @Override
  public Collection<ValidationError> addTagsEvent(final String siteId, final DocumentItem item,
      final Collection<DocumentTag> tags) {
    return Arrays.asList(new DummyValidationError());
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
  public Collection<ValidationError> removeTagsEvent(final String siteId, final DocumentItem item,
      final Collection<String> tags) {
    return Arrays.asList(new DummyValidationError());
  }

  @Override
  public Collection<ValidationError> replaceTagsEvent(final String siteId, final DocumentItem item,
      final Collection<DocumentTag> tags) {
    return Arrays.asList(new DummyValidationError());
  }
}