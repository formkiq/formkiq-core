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
package com.formkiq.module.httpsigv4;

import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.auth.signer.params.Aws4SignerParams;
import software.amazon.awssdk.auth.signer.params.Aws4SignerParams.Builder;
import software.amazon.awssdk.regions.Region;

/**
 * 
 * Aws4 Signer Builder.
 *
 */
public class Aws4SignerParamsBuilder {

  /** {@link Aws4SignerParamsBuilder}. */
  private Builder<?> builder;

  /**
   * constructor.
   */
  public Aws4SignerParamsBuilder() {
    this.builder = Aws4SignerParams.builder();
  }

  /**
   * Build {@link Aws4SignerParams}.
   * 
   * @return {@link Aws4SignerParams}
   */
  public Aws4SignerParams build() {
    return this.builder.build();
  }

  /**
   * Init Sts Client.
   */
  public void initAws4SignerParams() {
    build();
  }

  /**
   * Set Region.
   * 
   * @param region {@link Region}
   * @return {@link Aws4SignerParamsBuilder}
   */
  public Aws4SignerParamsBuilder region(final Region region) {
    this.builder = this.builder.signingRegion(region);
    return this;
  }

  /**
   * Set Credentials.
   * 
   * @param credentials {@link AwsCredentials}
   * @return {@link Aws4SignerParamsBuilder}
   */
  public Aws4SignerParamsBuilder setCredentials(final AwsCredentials credentials) {
    return setCredentials(StaticCredentialsProvider.create(credentials));
  }

  /**
   * Set Credentials.
   * 
   * @param cred {@link AwsCredentialsProvider}
   * @return {@link Aws4SignerParamsBuilder}
   */
  public Aws4SignerParamsBuilder setCredentials(final AwsCredentialsProvider cred) {
    this.builder = this.builder.awsCredentials(cred.resolveCredentials());
    return this;
  }

  /**
   * Set Credentials.
   * 
   * @param profileName {@link String}
   * @return {@link Aws4SignerParamsBuilder}
   */
  public Aws4SignerParamsBuilder setCredentials(final String profileName) {
    try (ProfileCredentialsProvider prov =
        ProfileCredentialsProvider.builder().profileName(profileName).build()) {
      return setCredentials(prov);
    }
  }

  /**
   * Set Signing Name.
   * 
   * @param signingName {@link String}
   * @return {@link Aws4SignerParamsBuilder}
   */
  public Aws4SignerParamsBuilder signingName(final String signingName) {
    this.builder = this.builder.signingName(signingName);
    return this;
  }
}
