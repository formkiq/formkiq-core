package com.formkiq.stacks.api;

import java.util.Arrays;
import java.util.Collection;
import com.formkiq.aws.dynamodb.model.DocumentItem;
import com.formkiq.aws.dynamodb.model.DocumentTag;
import com.formkiq.aws.dynamodb.model.DynamicDocumentItem;
import com.formkiq.aws.services.lambda.events.DocumentTagSchemaEvents;
import com.formkiq.aws.services.lambda.validation.ValidationError;

/**
 * 
 * {@link DocumentTagSchemaEvents} that returns test {@link ValidationError}.
 *
 */
public class DocumentEventsErrors implements DocumentTagSchemaEvents {
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
  public Collection<ValidationError> addTagsEvent(final String siteId,
      final DynamicDocumentItem item, final Collection<DocumentTag> tags) {
    return Arrays.asList(new DummyValidationError());
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