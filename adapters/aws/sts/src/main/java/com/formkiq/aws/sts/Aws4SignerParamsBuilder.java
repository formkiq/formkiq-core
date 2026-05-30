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
package com.formkiq.aws.sts;

import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.auth.signer.params.Aws4SignerParams;
import software.amazon.awssdk.auth.signer.params.Aws4SignerParams.Builder;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sts.model.Credentials;

/**
 * 
 * Aws4 Signer Builder.
 *
 */
public class Aws4SignerParamsBuilder {

  /** {@link AwsCredentials}. */
  private AwsCredentials credentials;
  /** S3 Region. */
  private Region region;
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
   * Get {@link AwsCredentials}.
   * 
   * @return {@link AwsCredentials}
   */
  public AwsCredentials getCredentials() {
    return this.credentials;
  }

  /**
   * Get Region.
   * 
   * @return {@link Region}
   */
  public Region getRegion() {
    return this.region;
  }

  /**
   * Init Sts Client.
   */
  public void initAws4SignerParams() {
    build();
  }

  /**
   * Set Credentials.
   * 
   * @param cred {@link AwsCredentialsProvider}
   * @return {@link Aws4SignerParamsBuilder}
   */
  public Aws4SignerParamsBuilder setCredentials(final AwsCredentialsProvider cred) {
    this.credentials = cred.resolveCredentials();
    this.builder = this.builder.awsCredentials(cred.resolveCredentials());
    return this;
  }

  /**
   * Set Credentials.
   * 
   * @param cred {@link Credentials}
   * @return {@link Aws4SignerParamsBuilder}
   */
  public Aws4SignerParamsBuilder setCredentials(final Credentials cred) {
    AwsCredentials basic = AwsSessionCredentials.create(cred.accessKeyId(), cred.secretAccessKey(),
        cred.sessionToken());
    return setCredentials(StaticCredentialsProvider.create(basic));
  }

  /**
   * Set Credentials.
   * 
   * @param credentialName {@link String}
   * @return {@link Aws4SignerParamsBuilder}
   */
  public Aws4SignerParamsBuilder setCredentials(final String credentialName) {
    try (ProfileCredentialsProvider prov =
        ProfileCredentialsProvider.builder().profileName(credentialName).build()) {
      return setCredentials(prov);
    }
  }

  /**
   * Set Region.
   * 
   * @param r {@link Region}
   * @return {@link Aws4SignerParamsBuilder}
   */
  public Aws4SignerParamsBuilder setRegion(final Region r) {
    this.region = r;
    this.builder = this.builder.signingRegion(this.region);
    return this;
  }

  /**
   * Set Signing Name.
   * 
   * @param signingName {@link String}
   * @return {@link Aws4SignerParamsBuilder}
   */
  public Aws4SignerParamsBuilder setSigningName(final String signingName) {
    this.builder = this.builder.signingName(signingName);
    return this;
  }
}
