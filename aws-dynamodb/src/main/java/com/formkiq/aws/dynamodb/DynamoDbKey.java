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
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.Map;
import java.util.Objects;

import static com.formkiq.aws.dynamodb.DbKeys.PK;
import static com.formkiq.aws.dynamodb.DbKeys.SK;
import static software.amazon.awssdk.services.dynamodb.model.AttributeValue.fromS;

/**
 * Record representing the key structure for a DynamoDB item, including primary key (PK/SK) and two
 * global secondary indexes (GSI1 and GSI2).
 *
 * @param pk the primary partition key
 * @param sk the primary sort key
 * @param gsi1Pk the partition key for GSI1
 * @param gsi1Sk the sort key for GSI1
 * @param gsi2Pk the partition key for GSI2
 * @param gsi2Sk the sort key for GSI2
 */
public record DynamoDbKey(String pk, String sk, String gsi1Pk, String gsi1Sk, String gsi2Pk,
    String gsi2Sk) {

  /**
   * Canonical constructor to enforce non-null keys.
   */
  public DynamoDbKey {
    Objects.requireNonNull(pk, "pk must not be null");
    Objects.requireNonNull(sk, "sk must not be null");
  }

  /**
   * Return Key {@link Map}.
   * 
   * @return {@link Map}
   */
  public Map<String, AttributeValue> toMap() {
    return Map.of(PK, fromS(pk), SK, fromS(sk));
  }

  /**
   * Constructs a {@code DynamoDbKey} from a map of DynamoDB attributes.
   * <p>
   * Extracts the key fields "PK", "SK", "GSI1PK", "GSI1SK", "GSI2PK", "GSI2SK" from the provided
   * attribute map, using their string values. Missing attributes result in null component values.
   *
   * @param attributes the map of attribute names to {@link AttributeValue}
   * @return a new {@code DynamoDbKey} instance
   * @throws NullPointerException if {@code attributes} is null
   */
  public static DynamoDbKey fromAttributeMap(final Map<String, AttributeValue> attributes) {
    Objects.requireNonNull(attributes, "attributes must not be null");
    return new DynamoDbKey(DynamoDbTypes.toString(attributes.get(PK)),
        DynamoDbTypes.toString(attributes.get(SK)),
        DynamoDbTypes.toString(attributes.get(DbKeys.GSI1_PK)),
        DynamoDbTypes.toString(attributes.get(DbKeys.GSI1_SK)),
        DynamoDbTypes.toString(attributes.get(DbKeys.GSI2_PK)),
        DynamoDbTypes.toString(attributes.get(DbKeys.GSI2_SK)));
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
    return DynamoDbAttributeMapBuilder.builder().withString("PK", pk).withString("SK", sk)
        .withString("GSI1PK", gsi1Pk).withString("GSI1SK", gsi1Sk).withString("GSI2PK", gsi2Pk)
        .withString("GSI2SK", gsi2Sk);
  }

  /**
   * Creates a new {@link Builder} for {@link DynamoDbKey}.
   *
   * @return a Builder instance
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Builder for {@link DynamoDbKey} to facilitate fluent construction.
   */
  public static class Builder {
    /** PK. */
    private String pk;
    /** SK. */
    private String sk;
    /** GSI1 PK. */
    private String gsi1Pk;
    /** GSI1 SK. */
    private String gsi1Sk;
    /** GSI2 PK. */
    private String gsi2Pk;
    /** GSI2 SK. */
    private String gsi2Sk;

    /**
     * Builds the {@link DynamoDbKey} instance.
     *
     * @return a newly constructed DynamoDbKey
     */
    public DynamoDbKey build() {
      return new DynamoDbKey(pk, sk, gsi1Pk, gsi1Sk, gsi2Pk, gsi2Sk);
    }

    /**
     * Sets the primary partition key.
     *
     * @param siteId partition key
     * @param gsi1PkKey the GSI1 partition key
     * @return this Builder instance
     */
    public Builder gsi1Pk(final String siteId, final AttributeValue gsi1PkKey) {
      return gsi1Pk(siteId, gsi1PkKey != null ? gsi1PkKey.s() : null);
    }

    /**
     * Sets the GSI1 partition key.
     *
     * @param siteId partition key
     * @param gsi1PkKey the GSI1 partition key
     * @return this Builder instance
     */
    public Builder gsi1Pk(final String siteId, final String gsi1PkKey) {
      this.gsi1Pk =
          gsi1PkKey != null ? SiteIdKeyGenerator.createDatabaseKey(siteId, gsi1PkKey) : null;
      return this;
    }

    /**
     * Sets the GSI1 sort key.
     *
     * @param gsi1SkKey the GSI1 sort key
     * @return this Builder instance
     */
    public Builder gsi1Sk(final String gsi1SkKey) {
      this.gsi1Sk = gsi1SkKey;
      return this;
    }

    /**
     * Sets the primary partition key.
     *
     * @param siteId partition key
     * @param gsi2PkKey the GSI2 partition key
     * @return this Builder instance
     */
    public Builder gsi2Pk(final String siteId, final AttributeValue gsi2PkKey) {
      return gsi2Pk(siteId, gsi2PkKey != null ? gsi2PkKey.s() : null);
    }

    /**
     * Sets the GSI2 partition key.
     *
     * @param siteId partition key
     * @param gsi2PkKey the GSI2 partition key
     * @return this Builder instance
     */
    public Builder gsi2Pk(final String siteId, final String gsi2PkKey) {
      this.gsi2Pk =
          gsi2PkKey != null ? SiteIdKeyGenerator.createDatabaseKey(siteId, gsi2PkKey) : null;
      return this;
    }

    /**
     * Sets the GSI2 sort key.
     *
     * @param gsi2SkKey the GSI2 sort key
     * @return this Builder instance
     */
    public Builder gsi2Sk(final String gsi2SkKey) {
      this.gsi2Sk = gsi2SkKey;
      return this;
    }

    /**
     * Sets the primary partition key.
     *
     * @param siteId partition key
     * @param pkKey the primary partition key
     * @return this Builder instance
     */
    public Builder pk(final String siteId, final String pkKey) {
      this.pk = SiteIdKeyGenerator.createDatabaseKey(siteId, pkKey);
      return this;
    }

    /**
     * Sets the primary sort key.
     *
     * @param skKey the primary sort key
     * @return this Builder instance
     */
    public Builder sk(final String skKey) {
      this.sk = skKey;
      return this;
    }
  }
}

