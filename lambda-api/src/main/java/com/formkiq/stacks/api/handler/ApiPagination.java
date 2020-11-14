/**
 *
 * FormKiQ License
 *
 * Copyright (c) 2018 FormKiQ, INC
 * 
 * This code is the property of FormKiQ, INC. In the Software Development Agreement signed by both
 * FormKiQ and your company, FormKiQ grants you a limited license to use, modify, and create
 * derivative works of this code. Please consult the Software Development Agreement for the complete
 * terms under which you may use this code.
 *
 */
package com.formkiq.stacks.api.handler;

import java.util.UUID;
import com.formkiq.graalvm.annotations.Reflectable;
import com.formkiq.stacks.dynamodb.PaginationMapToken;

/**
 * Class for holding results paginating information.
 *
 */
@Reflectable
public class ApiPagination {

  /** String Next Token. */
  @Reflectable
  private String next;
  /** Has Next Token. */
  @Reflectable
  private boolean hasNext;
  /** Previous Token. */
  @Reflectable
  private String previous;
  /** Query Limit. */
  @Reflectable
  private int limit;
  /** {@link PaginationMapToken}. */
  @Reflectable
  private PaginationMapToken startkey;

  /**
   * constructor.
   */
  public ApiPagination() {
    this.next = UUID.randomUUID().toString();
  }

  /**
   * Get Next Token.
   * 
   * @return {@link String}
   */
  public String getNext() {
    return this.next;
  }

  /**
   * Get Previous Token.
   * 
   * @return {@link String}
   */
  public String getPrevious() {
    return this.previous;
  }

  /**
   * Set Previous Token.
   * 
   * @param token {@link String}
   */
  public void setPrevious(final String token) {
    this.previous = token;
  }

  /**
   * Get {@link PaginationMapToken}.
   * 
   * @return {@link PaginationMapToken}
   */
  public PaginationMapToken getStartkey() {
    return this.startkey;
  }

  /**
   * Set {@link PaginationMapToken}.
   * 
   * @param token {@link PaginationMapToken}
   */
  public void setStartkey(final PaginationMapToken token) {
    this.startkey = token;
  }

  /**
   * Get Query Limit.
   * 
   * @return int
   */
  public int getLimit() {
    return this.limit;
  }

  /**
   * Set Query Limit.
   * 
   * @param querylimit int
   */
  public void setLimit(final int querylimit) {
    this.limit = querylimit;
  }

  /**
   * Has Previous Token.
   * 
   * @return boolean
   */
  public boolean hasPrevious() {
    return this.previous != null;
  }

  /**
   * Has Next.
   * 
   * @return boolean
   */
  public boolean hasNext() {
    return this.hasNext;
  }

  /**
   * Set whether Has Next.
   * 
   * @param nextpage boolean
   */
  public void setHasNext(final boolean nextpage) {
    this.hasNext = nextpage;
  }
}
