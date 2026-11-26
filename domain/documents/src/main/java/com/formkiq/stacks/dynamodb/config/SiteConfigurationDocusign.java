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
package com.formkiq.stacks.dynamodb.config;

import static com.formkiq.stacks.dynamodb.config.ConfigService.DOCUSIGN_ENVIRONMENT;
import static com.formkiq.stacks.dynamodb.config.ConfigService.KEY_DOCUSIGN_ACCOUNT_ID;
import static com.formkiq.stacks.dynamodb.config.ConfigService.KEY_DOCUSIGN_CONNECT_URL;
import static com.formkiq.stacks.dynamodb.config.ConfigService.KEY_DOCUSIGN_HMAC_SIGNATURE;
import static com.formkiq.stacks.dynamodb.config.ConfigService.KEY_DOCUSIGN_INTEGRATION_KEY;
import static com.formkiq.stacks.dynamodb.config.ConfigService.KEY_DOCUSIGN_RSA_PRIVATE_KEY;
import static com.formkiq.stacks.dynamodb.config.ConfigService.KEY_DOCUSIGN_USER_ID;
import static com.formkiq.strings.Strings.trim;

import java.util.Arrays;
import java.util.Map;

import com.formkiq.aws.dynamodb.builder.DynamoDbAttributeMapBuilder;
import com.formkiq.aws.dynamodb.builder.DynamoDbTypes;
import com.formkiq.aws.dynamodb.objects.Strings;
import com.formkiq.graalvm.annotations.Reflectable;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/**
 * Site Configuration Docusign.
 */
@Reflectable
public record SiteConfigurationDocusign(String environment, String userId, String integrationKey,
    String rsaPrivateKey, String hmacSignature, String connectUrl, String accountId) {

  /**
   * Create a configuration without explicit account selection.
   *
   * @param docusignEnvironment DocuSign environment
   * @param docusignUserId service user ID
   * @param docusignIntegrationKey integration key
   * @param docusignRsaPrivateKey private key
   * @param docusignHmacSignature callback HMAC secret
   * @param docusignConnectUrl callback URL
   */
  public SiteConfigurationDocusign(final String docusignEnvironment, final String docusignUserId,
      final String docusignIntegrationKey, final String docusignRsaPrivateKey,
      final String docusignHmacSignature, final String docusignConnectUrl) {
    this(docusignEnvironment, docusignUserId, docusignIntegrationKey, docusignRsaPrivateKey,
        docusignHmacSignature, docusignConnectUrl, null);
  }

  static SiteConfigurationDocusign fromAttributeMap(final Map<String, AttributeValue> attributes) {
    String environment = DynamoDbTypes.toString(attributes.get(DOCUSIGN_ENVIRONMENT));
    String userId = DynamoDbTypes.toString(attributes.get(KEY_DOCUSIGN_USER_ID));
    String integrationKey = DynamoDbTypes.toString(attributes.get(KEY_DOCUSIGN_INTEGRATION_KEY));
    String privateKey = DynamoDbTypes.toString(attributes.get(KEY_DOCUSIGN_RSA_PRIVATE_KEY));
    String hmac = DynamoDbTypes.toString(attributes.get(KEY_DOCUSIGN_HMAC_SIGNATURE));
    String connectUrl = DynamoDbTypes.toString(attributes.get(KEY_DOCUSIGN_CONNECT_URL));
    String accountId = DynamoDbTypes.toString(attributes.get(KEY_DOCUSIGN_ACCOUNT_ID));
    String[] values =
        {environment, userId, integrationKey, privateKey, hmac, connectUrl, accountId};
    return Arrays.stream(values).allMatch(Strings::isEmpty) ? null
        : new SiteConfigurationDocusign(environment, userId, integrationKey, privateKey, hmac,
            connectUrl, accountId);
  }

  Map<String, AttributeValue> getAttributes() {
    return DynamoDbAttributeMapBuilder.builder().withString(DOCUSIGN_ENVIRONMENT, environment)
        .withString(KEY_DOCUSIGN_USER_ID, trim(userId))
        .withString(KEY_DOCUSIGN_INTEGRATION_KEY, trim(integrationKey))
        .withString(KEY_DOCUSIGN_RSA_PRIVATE_KEY, trim(rsaPrivateKey))
        .withString(KEY_DOCUSIGN_HMAC_SIGNATURE, trim(hmacSignature))
        .withString(KEY_DOCUSIGN_CONNECT_URL, trim(connectUrl))
        .withString(KEY_DOCUSIGN_ACCOUNT_ID, trim(accountId)).build();
  }
}
