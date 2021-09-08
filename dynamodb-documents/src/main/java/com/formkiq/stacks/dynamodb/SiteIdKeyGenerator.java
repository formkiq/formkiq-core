/**
 * MIT License
 * 
 * Copyright (c) 2018 - 2020 FormKiQ
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
 * NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package com.formkiq.stacks.dynamodb;

import static com.formkiq.stacks.dynamodb.DbKeys.TAG_DELIMINATOR;

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
   * Split {@link String} by Deliminator.
   * 
   * @param s {@link String}
   * @param element int
   * @return {@link String}
   */
  public static String getDeliminator(final String s, final int element) {
    String[] strs = s.split(TAG_DELIMINATOR);
    return strs.length > element ? strs[element] : null;
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
   * Is SiteId the Default site.
   * @param siteId {@link String}
   * @return boolean
   */
  public static boolean isDefaultSiteId(final String siteId) {
    return siteId == null || DEFAULT_SITE_ID.equals(siteId);
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
