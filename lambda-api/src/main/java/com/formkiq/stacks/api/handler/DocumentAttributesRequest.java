package com.formkiq.stacks.api.handler;

import java.util.Collection;
import com.formkiq.graalvm.annotations.Reflectable;

/**
 * Document Attributes Request.
 */
@Reflectable
public class DocumentAttributesRequest {

  /** {@link Collection} {@link DocumentAttribute}. */
  private Collection<DocumentAttribute> attributes;

  /**
   * constructor.
   */
  public DocumentAttributesRequest() {

  }

  /**
   * Set {@link Collection} {@link DocumentAttribute}.
   * 
   * @param documentAttributes {@link Collection} {@link DocumentAttribute}
   * @return {@link DocumentAttributesRequest}
   */
  public DocumentAttributesRequest attributes(
      final Collection<DocumentAttribute> documentAttributes) {
    this.attributes = documentAttributes;
    return this;
  }

  /**
   * Get {@link Collection} {@link DocumentAttribute}.
   * 
   * @return {@link Collection} {@link DocumentAttribute}
   */
  public Collection<DocumentAttribute> getAttributes() {
    return this.attributes;
  }
}
