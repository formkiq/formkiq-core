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
package com.formkiq.aws.secretsmanager;

import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.CreateSecretRequest;
import software.amazon.awssdk.services.secretsmanager.model.CreateSecretResponse;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;

/**
 * {@link SecretsManagerService} implementation.
 */
public class SecretsManagerServiceImpl implements SecretsManagerService {

  /** {@link SecretsManagerClient}. */
  private final SecretsManagerClient client;

  /**
   * constructor.
   * 
   * @param connection {@link SecretsManagerConnectionBuilder}
   */
  public SecretsManagerServiceImpl(final SecretsManagerConnectionBuilder connection) {
    client = connection.build();
  }

  @Override
  public String createSecret(final String name, final String value) {
    CreateSecretResponse resp =
        client.createSecret(CreateSecretRequest.builder().name(name).secretString(value).build());
    return resp.arn();
  }

  @Override
  public String createSecret(final String name, final byte[] value) {
    CreateSecretResponse resp = client.createSecret(CreateSecretRequest.builder().name(name)
        .secretBinary(SdkBytes.fromByteArray(value)).build());
    return resp.arn();
  }

  @Override
  public byte[] loadSecretBytesByArn(final String arn) {

    GetSecretValueResponse resp =
        client.getSecretValue(GetSecretValueRequest.builder().secretId(arn).build());

    if (resp.secretBinary() != null) {
      return resp.secretBinary().asByteArray();
    }

    return null;
  }

  @Override
  public String loadSecretStringByArn(final String arn) {
    GetSecretValueResponse resp =
        client.getSecretValue(GetSecretValueRequest.builder().secretId(arn).build());

    return resp.secretString();
  }
}
