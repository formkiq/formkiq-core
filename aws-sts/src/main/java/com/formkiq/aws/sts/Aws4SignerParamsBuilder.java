/**
 *
 * FormKiQ License
 *
 * Copyright (c) 2018 FormKiQ, INC
 * 
 * This code is the property of FormKiQ, INC. In the Software Development Agreement signed by both
 * FormKiQ and your company, FormKiQ grants you a limited license to use, modify, and create
 * derivative works of this code. Please consult the Software Development Agreement for the complete
 * terms under which you may use this code.
 *
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
 * S3 Connection Builder.
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
