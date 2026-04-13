/**
 * MIT License
 * 
 * Copyright (c) 2018 - 2020 FormKiQ
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.formkiq.aws.dynamodb;

import com.formkiq.aws.dynamodb.documents.DocumentArtifact;

import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * 
 * Site Id Key Generator.
 *
 */
public final class SiteIdKeyGenerator {

  /** Default Site Id. */
  public static final String DEFAULT_SITE_ID = "default";
  /** S3 key parts with site id. */
  private static final int S3_KEY_PARTS_WITH_SITE_ID = 3;

  /**
   * {@link Pattern} to split a {@link String} instead SiteId / DocumentId. (?<!/) # assert that the
   * previous character is not a colon / # match a literal : character (?!/) # assert that the next
   * character is not a colon
   */
  private static final Pattern SITE_DOC_ID = Pattern.compile("(?<!/)/(?!/)");

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
   * @param document {@link DocumentArtifact}
   * @return {@link String}
   */
  public static String createS3Key(final String siteId, final DocumentArtifact document) {
    return createS3Key(siteId, document.documentId(), document.artifactId());
  }

  /**
   * Create S3 Key.
   * 
   * @param siteId {@link String}
   * @param id {@link String}
   * @param artifactId {@link String}
   * @return {@link String}
   */
  public static String createS3Key(final String siteId, final String id, final String artifactId) {
    String key = artifactId != null ? id + "/artifacts/" + artifactId : id;
    return createDatabaseKey(siteId, key);
  }

  /**
   * Create S3 Key.
   * 
   * @param siteId {@link String}
   * @param id {@link String}
   * @param artifactId {@link String}
   * @param contentType {@link String}
   * @return {@link String}
   * @deprecated use {@link #createS3Key(String, String, String)}
   */
  @Deprecated
  public static String createS3Key(final String siteId, final String id, final String artifactId,
      final String contentType) {
    String key = artifactId != null ? id + "/" + artifactId : id;
    if (contentType != null) {
      key += "/" + contentType;
    }
    return createDatabaseKey(siteId, key);
  }

  /**
   * Split {@link String} by Deliminator.
   * 
   * @param s {@link String}
   * @param element int
   * @return {@link String}
   */
  public static String getDeliminator(final String s, final int element) {
    String[] strs = s.split(DbKeys.TAG_DELIMINATOR);
    return strs.length > element ? strs[element] : null;
  }

  /**
   * Get DocumentId from {@link String}.
   * 
   * @param s {@link String}
   * @return {@link String}
   */
  public static String getDocumentId(final String s) {
    String[] split = split(s);
    return split[1];
  }

  /**
   * Parse S3 Key into parts.
   *
   * @param key {@link String}
   * @return {@link S3KeyParts}
   */
  public static S3KeyParts getS3KeyParts(final String key) {
    String siteId = null;
    String documentId = null;
    String artifactId = null;

    if (key != null) {

      boolean hasArtifacts = key.contains("/artifacts/");
      String[] ss = key.split("/");
      List<String> parts = Stream.of(ss).filter(s -> !"artifacts".equals(s)).toList();
      int len = parts.size();

      if (len == S3_KEY_PARTS_WITH_SITE_ID) {
        siteId = parts.get(0);
        documentId = parts.get(1);
        artifactId = parts.get(2);
      } else if (hasArtifacts && len == 2) {
        documentId = parts.get(0);
        artifactId = parts.get(1);
      } else if (len == 2) {
        siteId = parts.get(0);
        documentId = parts.get(1);
      } else if (len == 1) {
        documentId = parts.get(0);
      }
    }

    return new S3KeyParts(siteId, documentId, artifactId);
  }

  /**
   * Get SiteId from {@link String}.
   * 
   * @param s {@link String}
   * @return {@link String}
   */
  public static String getSiteId(final String s) {
    String[] split = split(s);
    String siteId = split[0];
    return !DEFAULT_SITE_ID.equals(siteId) ? siteId : null;
  }

  /**
   * Get Not Null Site Name.
   * 
   * @param siteId {@link String}
   * @return String
   */
  public static String getSiteIdName(final String siteId) {
    return isDefaultSiteId(siteId) ? DEFAULT_SITE_ID : siteId;
  }

  /**
   * Whether {@link String} has '//'.
   * 
   * @param s {@link String}
   * @return boolean
   */
  private static boolean hasDoubleSlash(final String s) {
    return s.indexOf("//") != -1;
  }

  /**
   * Is SiteId the Default site.
   * 
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
    } else if (siteId == null && s != null) {
      text = s.replaceAll("^" + DEFAULT_SITE_ID + "\\/", "");
    }

    return text;
  }

  private static String[] split(final String s) {

    String siteId = null;
    String documentId = null;

    if (s != null) {

      String[] strs = SITE_DOC_ID.split(s);

      if (strs.length == 1) {
        documentId = strs[0];
      } else if (strs.length == 2) {
        if (!hasDoubleSlash(strs[0])) {

          siteId = strs[0];
          documentId = strs[1];

        } else {

          documentId = String.join("", strs);
        }
      } else {

        siteId = strs[0];
        StringBuilder buf = new StringBuilder(strs[1]);
        for (int i = 1; i < strs.length; i++) {
          buf.append(strs[i]);
        }

        documentId = buf.toString();
      }
    }

    return new String[] {siteId, documentId};
  }

  /**
   * private constructor.
   */
  private SiteIdKeyGenerator() {}

  /**
   * S3 key parts.
   *
   * @param siteId {@link String}
   * @param documentId {@link String}
   * @param artifactId {@link String}
   */
  public record S3KeyParts(String siteId, String documentId, String artifactId) {
  }
}
