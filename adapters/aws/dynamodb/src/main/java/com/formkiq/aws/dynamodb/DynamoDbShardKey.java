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

import com.formkiq.aws.dynamodb.builder.DynamoDbAttributeMapBuilder;
import com.formkiq.aws.dynamodb.builder.DynamoDbTypes;
import com.formkiq.graalvm.annotations.Reflectable;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.IntStream;

import static com.formkiq.aws.dynamodb.DbKeys.GSI1_PK;
import static com.formkiq.aws.dynamodb.DbKeys.GSI2_PK;
import static com.formkiq.aws.dynamodb.DbKeys.PK;
import static com.formkiq.aws.dynamodb.DbKeys.SHARD;
import static com.formkiq.aws.dynamodb.objects.Strings.isEmpty;
import static software.amazon.awssdk.services.dynamodb.model.AttributeValue.fromS;

/**
 * {@link DynamoDbKey} that is shared.
 * 
 * @param key {@link String}
 * @param pkShard {@link String}
 * @param pkGsi1Shard {@link String}
 * @param pkGsi2Shard {@link String}
 */
@Reflectable
public record DynamoDbShardKey(DynamoDbKey key, String pkShard, String pkGsi1Shard,
    String pkGsi2Shard) {

  /**
   * Canonical constructor to enforce non-null keys.
   */
  public DynamoDbShardKey {
    Objects.requireNonNull(key, "key must not be null");
  }

  /**
   * Returns a list of Shard Suffix.
   * 
   * @param shardCount int
   * @return {@link List} {@link String}
   */
  public static List<String> getShardsSuffix(final int shardCount) {
    return IntStream.rangeClosed(0, shardCount).mapToObj(DynamoDbShardKey::formatShard).toList();
  }

  /**
   * Get Shard Key.
   *
   * @param shardCount int
   * @return {@link String}
   */
  public static String getShardKey(final int shardCount) {
    int shardNo = ThreadLocalRandom.current().nextInt(shardCount);
    return formatShard(shardNo);
  }

  private static String formatShard(final int shardNo) {
    return "s" + String.format("%02d", shardNo);
  }

  /**
   * Remove Shard Suffix.
   * 
   * @param s {@link String}
   * @return {@link String}
   */
  public static String removeShardSuffix(final String s) {
    return !isEmpty(s) && s.matches(".*##s\\d{2}$") ? s.substring(0, s.lastIndexOf("##")) : s;
  }

  /**
   * Add Shard Suffix.
   * 
   * @param s {@link String}
   * @param shard {@link String}
   * @return {@link String}
   */
  public static String addShardSuffix(final String s, final String shard) {
    return !isEmpty(s) && !isEmpty(shard) ? s + "##" + shard : s;
  }

  /**
   * Return Key {@link Map}.
   *
   * @return {@link Map}
   */
  public Map<String, AttributeValue> toMap() {
    Map<String, AttributeValue> keyMap = new HashMap<>(key.toMap());

    if (pkShard != null) {
      keyMap.put(PK + SHARD, fromS(pkShard));
      keyMap.put(GSI1_PK + SHARD, fromS(pkGsi1Shard));
      keyMap.put(GSI2_PK + SHARD, fromS(pkGsi2Shard));
    }

    return keyMap;
  }

  /**
   * Constructs a {@code DynamoDbKey} from a map of DynamoDB attributes.
   * <p>
   * Extracts the key fields "PK", "SK", "GSI1PK", "GSI1SK", "GSI2PK", "GSI2SK" from the provided
   * attribute map, using their string values. Missing attributes result in null component values.
   *
   * @param attributes the map of attribute names to {@link AttributeValue}
   * @return a new {@code DynamoDbShardKey} instance
   * @throws NullPointerException if {@code attributes} is null
   */
  public static DynamoDbShardKey fromAttributeMap(final Map<String, AttributeValue> attributes) {
    Objects.requireNonNull(attributes, "attributes must not be null");
    DynamoDbKey dbKey = DynamoDbKey.fromAttributeMap(attributes);
    return new DynamoDbShardKey(dbKey, DynamoDbTypes.toString(attributes.get(PK + SHARD)),
        DynamoDbTypes.toString(attributes.get(GSI1_PK + SHARD)),
        DynamoDbTypes.toString(attributes.get(GSI2_PK + SHARD)));
  }

  /**
   * Builds the DynamoDB item attribute map for this key structure, including the provided siteId
   * for multi-tenant partitioning.
   * <p>
   * Only non-null values are included, utilizing the {@link DynamoDbAttributeMapBuilder} to support
   * string, number, and boolean types.
   *
   * @return a Map of attribute names to {@link AttributeValue} instances
   */
  public DynamoDbAttributeMapBuilder getAttributesBuilder() {
    return key.getAttributesBuilder().withString(PK + SHARD, pkShard)
        .withString(GSI1_PK + SHARD, pkGsi1Shard).withString(GSI2_PK + SHARD, pkGsi2Shard);
  }

  /**
   * Creates a new {@link DynamoDbKey.Builder} for {@link DynamoDbShardKey}.
   *
   * @return a Builder instance
   */
  public static DynamoDbShardKey.Builder builder() {
    return new Builder();
  }

  /**
   * Builder for {@link DynamoDbKey} to facilitate fluent construction.
   */
  public static class Builder {

    /** {@link DynamoDbKey}. */
    private DynamoDbKey key;
    /** PK. */
    private String pkShard;
    /** GSI1 PK. */
    private String gsi1PkShard;

    /** GSI2 PK. */
    private String gsi2PkShard;

    /**
     * Builds the {@link DynamoDbKey} instance.
     *
     * @return a newly constructed DynamoDbKey
     */
    public DynamoDbShardKey build() {
      DynamoDbKey skey = new DynamoDbKey(addShardSuffix(key.pk(), pkShard), key.sk(),
          addShardSuffix(key.gsi1Pk(), gsi1PkShard), key.gsi1Sk(),
          addShardSuffix(key.gsi2Pk(), gsi2PkShard), key.gsi2Sk());
      return new DynamoDbShardKey(skey, pkShard, gsi1PkShard, gsi2PkShard);
    }

    /**
     * Sets {@link DynamoDbKey}.
     * 
     * @param dynamoDbKey {@link DynamoDbKey}
     *
     * @return this Builder instance
     */
    public Builder key(final DynamoDbKey dynamoDbKey) {
      key = dynamoDbKey;
      return this;
    }

    /**
     * Set gsi1Pk Shard.
     *
     * @param shard {@link String}
     * @return this Builder instance
     */
    public Builder pkGsi1Shard(final String shard) {
      gsi1PkShard = shard;
      return this;
    }

    /**
     * Generates gsi1Pk Shard.
     *
     * @param shardCount int
     * @return this Builder instance
     */
    public Builder pkGsi1Shard(final int shardCount) {
      gsi1PkShard = getShardKey(shardCount);
      return this;
    }

    /**
     * Set gsi2Pk Shard.
     *
     * @param shard {@link String}
     * @return this Builder instance
     */
    public Builder pkGsi2Shard(final String shard) {
      gsi2PkShard = shard;
      return this;
    }

    /**
     * Generates gsi2Pk Shard.
     *
     * @param shardCount int
     * @return this Builder instance
     */
    public Builder pkGsi2Shard(final int shardCount) {
      gsi2PkShard = getShardKey(shardCount);
      return this;
    }

    /**
     * Set PK Shard.
     *
     * @param shard {@link String}
     * @return this Builder instance
     */
    public Builder pkShard(final String shard) {
      pkShard = shard;
      return this;
    }

    /**
     * Generates PK Shard.
     *
     * @param shardCount int
     * @return this Builder instance
     */
    public Builder pkShard(final int shardCount) {
      pkShard = getShardKey(shardCount);
      return this;
    }
  }
}
