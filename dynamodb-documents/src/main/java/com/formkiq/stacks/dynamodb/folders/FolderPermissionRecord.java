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
package com.formkiq.stacks.dynamodb.folders;

import com.formkiq.aws.dynamodb.DynamoDbKey;
import com.formkiq.aws.dynamodb.builder.DynamoDbEntityBuilder;
import com.formkiq.aws.dynamodb.builder.DynamoDbTypes;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.Objects;

import static com.formkiq.aws.dynamodb.builder.DynamoDbTypes.toCustom;
import static com.formkiq.aws.dynamodb.folders.FolderIndexRecord.INDEX_FOLDER_SK;

/**
 * Record representing an entity, with its DynamoDB key structure and metadata.
 */
public record FolderPermissionRecord(DynamoDbKey key, String path, String type, Date insertedDate,
    String userId, Collection<FolderRolePermission> rolePermissions) {

  /** {@link FolderRolePermissionAttributeBuilder}. */
  private static final FolderRolePermissionAttributeBuilder FOLDER_ROLE_BUILDER =
      new FolderRolePermissionAttributeBuilder();

  /**
   * Canonical constructor to enforce non-null properties and defensive copy of Date.
   */
  public FolderPermissionRecord {
    Objects.requireNonNull(key, "key must not be null");
    Objects.requireNonNull(path, "path must not be null");
    Objects.requireNonNull(type, "type must not be null");
    Objects.requireNonNull(userId, "userId must not be null");
    Objects.requireNonNull(rolePermissions, "rolePermissions must not be null");
    if (rolePermissions.isEmpty()) {
      throw new IllegalArgumentException("rolePermissions must not be empty");
    }
    Objects.requireNonNull(insertedDate, "insertedDate must not be null");
    insertedDate = new Date(insertedDate.getTime());
  }

  /**
   * Constructs a {@code EntityTypeRecord} from a map of DynamoDB attributes.
   *
   * @param attributes the map of attribute names to {@link AttributeValue}
   * @return a new {@code EntityTypeRecord} instance
   * @throws NullPointerException if {@code attributes} is null
   */
  public static FolderPermissionRecord fromAttributeMap(
      final Map<String, AttributeValue> attributes) {
    Objects.requireNonNull(attributes, "attributes must not be null");
    DynamoDbKey key = DynamoDbKey.fromAttributeMap(attributes);
    return new FolderPermissionRecord(key, DynamoDbTypes.toString(attributes.get("path")),
        DynamoDbTypes.toString(attributes.get("type")),
        DynamoDbTypes.toDate(attributes.get("inserteddate")),
        DynamoDbTypes.toString(attributes.get("userId")),
        toCustom(null, attributes, FOLDER_ROLE_BUILDER));
  }

  /**
   * Builds the DynamoDB item attribute map for this entity, starting from the key attributes and
   * adding metadata fields.
   *
   * @return a Map of attribute names to {@link AttributeValue} instances
   */
  public Map<String, AttributeValue> getAttributes() {
    return key.getAttributesBuilder().withString("path", path).withString("type", type)
        .withString("userId", userId).withDate("inserteddate", insertedDate)
        .withCustom(null, this.rolePermissions, FOLDER_ROLE_BUILDER).build();
  }

  /**
   * Creates a new {@link Builder} for {@link FolderPermissionRecord}.
   *
   * @return a Builder instance
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Fluent builder for {@link FolderPermissionRecord} that computes the DynamoDbKey.
   */
  public static class Builder implements DynamoDbEntityBuilder<FolderPermissionRecord> {
    /** Inserted Date. */
    private final Date insertedDate = new Date();
    /** Folder Path. */
    private String path;
    /** Path Type. */
    private String type;
    /** User Id. */
    private String userId;
    /** Role Permissions. */
    private Collection<FolderRolePermission> rolePermissions = null;

    @Override
    public FolderPermissionRecord build(final String siteId) {
      DynamoDbKey key = buildKey(siteId);
      return new FolderPermissionRecord(key, path, type, insertedDate, userId, rolePermissions);
    }

    @Override
    public DynamoDbKey buildKey(final String siteId) {

      Objects.requireNonNull(path, "path must not be null");

      String pk = "global#folders#permissions";
      String sk = INDEX_FOLDER_SK + path;

      return DynamoDbKey.builder().pk(siteId, pk).sk(sk).build();
    }

    /**
     * Sets the path.
     *
     * @param folderPath the folder path
     * @return this Builder
     */
    public Builder path(final String folderPath) {
      this.path = new StringToFolder().apply(folderPath);
      return this;
    }

    /**
     * Sets the Role Permission.
     *
     * @param permissions {@link FolderRolePermission}
     * @return this Builder
     */
    public Builder rolePermissions(final Collection<FolderRolePermission> permissions) {
      this.rolePermissions = permissions;
      return this;
    }


    /**
     * Sets the Type.
     *
     * @param pathType the folder path type
     * @return this Builder
     */
    public Builder type(final String pathType) {
      this.type = pathType;
      return this;
    }

    /**
     * Sets the userId.
     *
     * @param pathUserId set user id
     * @return this Builder
     */
    public Builder userId(final String pathUserId) {
      this.userId = pathUserId;
      return this;
    }
  }
}
