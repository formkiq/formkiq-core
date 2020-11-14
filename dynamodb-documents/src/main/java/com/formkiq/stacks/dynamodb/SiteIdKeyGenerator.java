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
package com.formkiq.stacks.dynamodb;

/**
 * 
 * Site Id Key Generator.
 *
 */
public final class SiteIdKeyGenerator {

  /** Default Site Id. */
  public static final String DEFAULT_SITE_ID = "default";

  /**
   * Build DynamoDB PK that handles with/out siteId.
   * 
   * @param siteId {@link String}
   * @param id {@link String}
   * @return {@link String}
   */
  public static String createDatabaseKey(final String siteId, final String id) {
    return siteId != null && !DEFAULT_SITE_ID.equals(siteId) ? siteId + "/" + id : id;
  }

  /**
   * Create S3 Key.
   * 
   * @param siteId {@link String}
   * @param id {@link String}
   * @return {@link String}
   */
  public static String createS3Key(final String siteId, final String id) {
    return createDatabaseKey(siteId, id);
  }

  /**
   * Create S3 Key.
   * 
   * @param siteId {@link String}
   * @param id {@link String}
   * @param contentType {@link String}
   * @return {@link String}
   */
  public static String createS3Key(final String siteId, final String id, final String contentType) {
    return createDatabaseKey(siteId, id + "/" + contentType);
  }

  /**
   * Get DocumentId from {@link String}.
   * 
   * @param s {@link String}
   * @return {@link String}
   */
  public static String getDocumentId(final String s) {
    int pos = s != null ? s.indexOf("/") : 0;
    return pos > 0 && s != null ? s.substring(pos + 1) : s;
  }

  /**
   * Get SiteId from {@link String}.
   * 
   * @param s {@link String}
   * @return {@link String}
   */
  public static String getSiteId(final String s) {
    int pos = s != null ? s.indexOf("/") : 0;
    String siteId = pos > 0 && s != null ? s.substring(0, pos) : null;
    return !DEFAULT_SITE_ID.equals(siteId) ? siteId : null;
  }

  /**
   * Remove Key siteId from {@link String}.
   * 
   * @param siteId {@link String}
   * @param s {@link String}
   * @return {@link String}
   */
  public static String resetDatabaseKey(final String siteId, final String s) {

    String text = s;
    if (siteId != null && s != null) {
      text = s.replaceAll("^" + siteId + "\\/", "");
    }

    return text;
  }

  /**
   * private constructor.
   */
  private SiteIdKeyGenerator() {}
}
