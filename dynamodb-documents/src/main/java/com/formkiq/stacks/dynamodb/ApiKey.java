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
package com.formkiq.stacks.dynamodb;

import static com.formkiq.aws.dynamodb.SiteIdKeyGenerator.getSiteIdName;
import static software.amazon.awssdk.services.dynamodb.model.AttributeValue.fromL;
import static software.amazon.awssdk.services.dynamodb.model.AttributeValue.fromS;

import com.formkiq.aws.dynamodb.DynamoDbKey;
import com.formkiq.aws.dynamodb.builder.DynamoDbAttributeMapBuilder;
import com.formkiq.aws.dynamodb.builder.DynamoDbEntityBuilder;
import com.formkiq.aws.dynamodb.builder.DynamoDbTypes;

import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Record representing an API key, including its DynamoDB key structure, metadata, permissions, and
 * serialization helpers.
 */
public record ApiKey(DynamoDbKey key, String siteId, String apiKey, String name, String userId,
    Date insertedDate, List<ApiKeyPermission> permissions, Collection<String> groups) {

  /**
   * Expected full length of generated API keys.
   */
  static final int API_KEY_LENGTH = 51;

  /**
   * Number of characters from the beginning of the API key to reveal when masking. Must be even.
   */
  private static final int MASK = 8;

  /**
   * Canonical constructor enforcing required fields and defensive copies.
   *
   * @param key DynamoDB key associated with this API key record
   * @param apiKey raw API key string
   * @param name name of the API key
   * @param userId creating user id (optional)
   * @param insertedDate date of insertion (non-null)
   * @param permissions list of permissions (optional)
   */
  public ApiKey {
    Objects.requireNonNull(key, "key must not be null");
    Objects.requireNonNull(apiKey, "apiKey must not be null");
    Objects.requireNonNull(name, "name must not be null");
    Objects.requireNonNull(insertedDate, "insertedDate must not be null");

    permissions = permissions == null ? List.of() : List.copyOf(permissions);
    groups = groups == null ? List.of() : List.copyOf(groups);

    insertedDate = new Date(insertedDate.getTime());
  }

  /**
   * Creates an ApiKey instance from a DynamoDB attribute map.
   *
   * @param attributes DynamoDB attribute map
   * @return populated ApiKey record
   * @throws NullPointerException if attributes is null
   */
  public static ApiKey fromAttributeMap(final Map<String, AttributeValue> attributes) {
    Objects.requireNonNull(attributes, "attributes must not be null");

    DynamoDbKey key = DynamoDbKey.fromAttributeMap(attributes);

    String apiKey = DynamoDbTypes.toString(attributes.get("apiKey"));
    String siteId = DynamoDbTypes.toString(attributes.get("siteId"));
    String name = DynamoDbTypes.toString(attributes.get("name"));
    String userId = DynamoDbTypes.toString(attributes.get("userId"));
    Collection<String> groups = DynamoDbTypes.toStrings(attributes.get("groups"));
    Date insertedDate = DynamoDbTypes.toDate(attributes.get("inserteddate"));

    List<ApiKeyPermission> permissions = attributes.containsKey("permissions")
        ? attributes.get("permissions").l().stream().map(AttributeValue::s)
            .map(ApiKeyPermission::valueOf).toList()
        : List.of();

    return new ApiKey(key, siteId, apiKey, name, userId, insertedDate, permissions, groups);
  }

  /**
   * Converts this record into a DynamoDB attribute map suitable for storage.
   *
   * @return map of attribute names to AttributeValue
   */
  public Map<String, AttributeValue> getAttributes() {
    DynamoDbAttributeMapBuilder map = key.getAttributesBuilder().withString("apiKey", apiKey)
        .withStrings("groups", groups).withString("name", name).withString("userId", userId)
        .withString("siteId", siteId).withDate("inserteddate", insertedDate);

    if (!permissions.isEmpty()) {
      map.withAttributeValue("permissions",
          fromL(permissions.stream().map(p -> fromS(p.name())).collect(Collectors.toList())));
    }

    return map.build();
  }

  /**
   * Static helper that masks an API key.
   *
   * @param keyValue raw API key
   * @return masked API key representation
   */
  public static String mask(final String keyValue) {
    return keyValue != null && keyValue.length() == API_KEY_LENGTH ? keyValue.subSequence(0, MASK)
        + "****************" + keyValue.substring(keyValue.length() - MASK / 2) : keyValue;
  }

  /**
   * Creates a builder for constructing {@link ApiKey} records.
   *
   * @return new Builder instance
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Builder for {@link ApiKey}, allowing fluent construction and key computation.
   */
  public static class Builder implements DynamoDbEntityBuilder<ApiKey> {

    /** Raw API key value. */
    private String apiKey;

    /** Name for the API key. */
    private String name;

    /** User who created the API key. */
    private String userId;

    /** Time the record was inserted. */
    private Date insertedDate = new Date();

    /** Permission list associated with this API key. */
    private Collection<ApiKeyPermission> permissions = List.of();
    /** Groups associated with this API key. */
    private Collection<String> groups = List.of();

    /**
     * Sets the raw API key.
     *
     * @param key API key value
     * @return this builder instance
     */
    public Builder apiKey(final String key) {
      this.apiKey = key;
      return this;
    }

    /**
     * Builds a populated {@link ApiKey} record using the provided siteId.
     *
     * @param siteId site identifier used when constructing the DynamoDB key
     * @return new ApiKey record
     */
    @Override
    public ApiKey build(final String siteId) {
      Objects.requireNonNull(insertedDate, "insertedDate must not be null");

      DynamoDbKey key = buildKey(siteId);
      List<ApiKeyPermission> perms = permissions == null ? List.of() : List.copyOf(permissions);
      List<String> g = groups == null ? List.of() : List.copyOf(groups);

      return new ApiKey(key, getSiteIdName(siteId), apiKey, name, userId, insertedDate, perms, g);
    }

    /**
     * Computes the DynamoDB key for this API key.
     *
     * @param siteId site identifier
     * @return constructed DynamoDbKey
     */
    @Override
    public DynamoDbKey buildKey(final String siteId) {
      Objects.requireNonNull(apiKey, "apiKey must not be null");
      Objects.requireNonNull(name, "name must not be null");

      String pk = "apikeys#";
      String sk = "apikey#" + apiKey;
      String gsi1Pk = "apikeys#";
      String gsi1Sk = "apikey#" + name + "#" + apiKey;
      String gsi2Pk = "apikeys#";
      String gsi2Sk = "apikey#" + mask(apiKey);

      return DynamoDbKey.builder().pk(null, pk).sk(sk).gsi1Pk(siteId, gsi1Pk).gsi1Sk(gsi1Sk)
          .gsi2Pk(siteId, gsi2Pk).gsi2Sk(gsi2Sk).build();
    }

    /**
     * Sets the list of permissions granted to this API key.
     *
     * @param apiKeyGroups list of Groups values
     * @return this builder instance
     */
    public Builder groups(final Collection<String> apiKeyGroups) {
      this.groups = apiKeyGroups;
      return this;
    }

    /**
     * Sets the insertion timestamp.
     *
     * @param date timestamp of record creation
     * @return this builder instance
     */
    public Builder insertedDate(final Date date) {
      this.insertedDate = date == null ? null : new Date(date.getTime());
      return this;
    }

    /**
     * Sets the display name of the API key.
     *
     * @param apiKeyName name of the API key
     * @return this builder instance
     */
    public Builder name(final String apiKeyName) {
      this.name = apiKeyName;
      return this;
    }

    /**
     * Sets the list of permissions granted to this API key.
     *
     * @param apiKeyPermissions list of ApiKeyPermission values
     * @return this builder instance
     */
    public Builder permissions(final Collection<ApiKeyPermission> apiKeyPermissions) {
      this.permissions = apiKeyPermissions;
      return this;
    }

    /**
     * Sets the user who created the API key.
     *
     * @param user user id
     * @return this builder instance
     */
    public Builder userId(final String user) {
      this.userId = user;
      return this;
    }
  }
}
