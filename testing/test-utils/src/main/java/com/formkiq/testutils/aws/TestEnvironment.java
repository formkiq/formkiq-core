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
package com.formkiq.testutils.aws;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;

import static com.formkiq.testutils.aws.DynamoDbExtension.DOCUMENTS_TABLE;

import java.util.HashMap;
import java.util.Map;

/**
 * Builder for test environment variables.
 */
public class TestEnvironment {

  /**
   * Create Builder.
   *
   * @return {@link TestEnvironment}
   */
  public static TestEnvironment builder() {
    return new TestEnvironment();
  }

  public static AwsCredentialsProvider createCredentials() {
    AwsCredentials creds = AwsBasicCredentials.create("aaa", "bbb");
    return StaticCredentialsProvider.create(creds);
  }

  /** Environment {@link Map}. */
  private final Map<String, String> environment = new HashMap<>();

  /**
   * constructor.
   */
  private TestEnvironment() {
    environment("AWS_REGION", TestServices.AWS_REGION.toString());
    environment("LOG_LEVEL", "info");
    environment("AWS_LAMBDA_LOG_FORMAT", "TEXT");
    environment("ENABLE_AWS_X_RAY", "false");
    environment("DOCUMENTS_TABLE", DOCUMENTS_TABLE);
    environment("DOCUMENT_VERSIONS_PLUGIN",
        "com.formkiq.stacks.dynamodb.DocumentVersionServiceNoVersioning");
  }

  /**
   * Set AWS Lambda Log Format.
   *
   * @param logFormat {@link String}
   * @return {@link TestEnvironment}
   */
  public TestEnvironment awsLambdaLogFormat(final String logFormat) {
    return environment("AWS_LAMBDA_LOG_FORMAT", logFormat);
  }

  /**
   * Set AWS Region.
   *
   * @param awsRegion {@link String}
   * @return {@link TestEnvironment}
   */
  public TestEnvironment awsRegion(final String awsRegion) {
    return environment("AWS_REGION", awsRegion);
  }

  /**
   * Build.
   *
   * @return {@link Map}
   */
  public Map<String, String> build() {
    return new HashMap<>(this.environment);
  }

  /**
   * Set Enable AWS X-Ray.
   *
   * @param enabled {@link String}
   * @return {@link TestEnvironment}
   */
  public TestEnvironment enableAwsXray(final String enabled) {
    return environment("ENABLE_AWS_X_RAY", enabled);
  }

  /**
   * Set Enable AWS X-Ray.
   *
   * @param enabled boolean
   * @return {@link TestEnvironment}
   */
  public TestEnvironment enableAwsXray(final boolean enabled) {
    return enableAwsXray(Boolean.toString(enabled));
  }

  /**
   * Set Environment variables.
   *
   * @param env {@link Map}
   * @return {@link TestEnvironment}
   */
  public TestEnvironment environment(final Map<String, String> env) {
    this.environment.putAll(env);
    return this;
  }

  /**
   * Set Environment variable.
   *
   * @param key {@link String}
   * @param value {@link String}
   * @return {@link TestEnvironment}
   */
  public TestEnvironment environment(final String key, final String value) {
    this.environment.put(key, value);
    return this;
  }

  /**
   * Set Log Level.
   *
   * @param logLevel {@link String}
   * @return {@link TestEnvironment}
   */
  public TestEnvironment logLevel(final String logLevel) {
    return environment("LOG_LEVEL", logLevel);
  }
}
