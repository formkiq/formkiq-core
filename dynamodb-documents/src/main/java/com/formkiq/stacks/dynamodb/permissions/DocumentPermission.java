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
package com.formkiq.stacks.dynamodb.permissions;

import static com.formkiq.aws.dynamodb.SiteIdKeyGenerator.createDatabaseKey;
import java.util.Map;
import com.formkiq.aws.dynamodb.DbKeys;
import com.formkiq.aws.dynamodb.DynamodbRecord;
import com.formkiq.graalvm.annotations.Reflectable;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/**
 * 
 * Document Permission object.
 *
 */
@Reflectable
public class DocumentPermission implements DynamodbRecord {

  /** Document Id. */
  @Reflectable
  private String documentId;
  /** Name of Permission. */
  @Reflectable
  private String name;
  /** Type of Permission. */
  @Reflectable
  private Permission permission;
  /** Type of Permission. */
  @Reflectable
  private PermissionType type;
  /** Creator of permission. */
  @Reflectable
  private String userId;

  /**
   * constructor.
   */
  public DocumentPermission() {

  }

  /**
   * Get Document Id.
   * 
   * @return {@link String}
   */
  public String documentId() {
    return this.documentId;
  }

  /**
   * Set Document Id.
   * 
   * @param document {@link String}
   * @return {@link DocumentPermission}
   */
  public DocumentPermission documentId(final String document) {
    this.documentId = document;
    return this;
  }

  @Override
  public Map<String, AttributeValue> getAttributes(final String siteId) {

    return Map.of(DbKeys.PK, AttributeValue.fromS(pk(siteId)), DbKeys.SK,
        AttributeValue.fromS(sk()), "documentId", AttributeValue.fromS(this.documentId), "name",
        AttributeValue.fromS(this.name), "type", AttributeValue.fromS(this.type.name()),
        "permission", AttributeValue.fromS(this.permission.name()), "userId",
        AttributeValue.fromS(this.userId));
  }

  /**
   * Get Permission Name.
   * 
   * @return {@link String}
   */
  public String name() {
    return this.name;
  }

  /**
   * Set Permission Name.
   * 
   * @param permissionName {@link String}
   * @return {@link DocumentPermission}
   */
  public DocumentPermission name(final String permissionName) {
    this.name = permissionName;
    return this;
  }

  /**
   * Get Document {@link Permission}.
   * 
   * @return {@link Permission}a
   */
  public Permission permission() {
    return this.permission;
  }

  /**
   * Set Document {@link Permission}.
   * 
   * @param documentPermission {@link Permission}
   * @return {@link DocumentPermission}
   */
  public DocumentPermission permission(final Permission documentPermission) {
    this.permission = documentPermission;
    return this;
  }

  @Override
  public String pk(final String siteId) {
    String pk = "docs#" + this.documentId;
    return createDatabaseKey(siteId, pk);
  }

  @Override
  public String sk() {
    return "permission#" + this.type.name() + "_" + this.permission.name() + "#" + this.name;
  }

  /**
   * Get {@link PermissionType}.
   * 
   * @return {@link PermissionType}
   */
  public PermissionType type() {
    return this.type;
  }

  /**
   * Set {@link PermissionType}.
   * 
   * @param permissionType {@link PermissionType}
   * @return {@link DocumentPermission}
   */
  public DocumentPermission type(final PermissionType permissionType) {
    this.type = permissionType;
    return this;
  }

  /**
   * Get UserId.
   * 
   * @return {@link String}
   */
  public String userId() {
    return this.userId;
  }

  /**
   * Set User Id.
   * 
   * @param createdBy {@link String}
   * @return {@link DocumentPermission}
   */
  public DocumentPermission userId(final String createdBy) {
    this.userId = createdBy;
    return this;
  }
}
