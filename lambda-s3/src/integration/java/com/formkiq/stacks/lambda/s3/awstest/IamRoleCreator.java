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
package com.formkiq.stacks.lambda.s3.awstest;

import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.services.iam.IamClient;
import software.amazon.awssdk.services.iam.model.CreateRoleRequest;
import software.amazon.awssdk.services.iam.model.CreateRoleResponse;
import software.amazon.awssdk.services.iam.model.DeleteRolePolicyRequest;
import software.amazon.awssdk.services.iam.model.DeleteRoleRequest;
import software.amazon.awssdk.services.iam.model.NoSuchEntityException;
import software.amazon.awssdk.services.iam.model.PutRolePolicyRequest;

public class IamRoleCreator {
  public static class Builder {
    /** {@link String}. */
    public String profileName;
    /** {@link String}. */
    private String roleName;
    /** {@link String}. */
    private String queueArn;

    public IamRoleCreator build() {
      if (roleName == null || queueArn == null) {
        throw new IllegalStateException("roleName, queueArn must be provided");
      }
      return new IamRoleCreator(this);
    }

    public Builder profileName(final String iamProfileName) {
      this.profileName = iamProfileName;
      return this;
    }

    public Builder queueArn(final String iamQueueArn) {
      this.queueArn = iamQueueArn;
      return this;
    }

    public Builder roleName(final String iamRoleName) {
      this.roleName = iamRoleName;
      return this;
    }
  }

  public static Builder builder() {
    return new Builder();
  }

  /** {@link IamClient}. */
  private final IamClient iam;

  /** {@link String}. */
  private final String roleName;

  /** {@link String}. */
  private final String queueArn;

  private IamRoleCreator(final Builder builder) {
    this.roleName = builder.roleName;
    this.queueArn = builder.queueArn;
    this.iam = IamClient.builder()
        .credentialsProvider(ProfileCredentialsProvider.create(builder.profileName)).build();
  }

  /**
   * Executes the creation of the IAM role with trust policy and attaches the inline permissions
   * policy.
   *
   * @return String
   */
  public String create() {
    String assumeRolePolicy = String.format("{\"Version\":\"2012-10-17\",\"Statement\":["
        + "{\"Sid\":\"TrustEventBridgeService\",\"Effect\":\"Allow\",\"Principal\":"
        + "{\"Service\":\"events.amazonaws.com\"},\"Action\":\"sts:AssumeRole\"," + "\"Condition\":"
        + "{\"StringEquals\":{\"aws:SourceAccount\":\"%s\",\"aws:SourceArn\":\"%s\"}}}]}",
        "513422444723",
        "arn:aws:events:us-east-2:513422444723:rule/FormKiQ-DocumentEvents-dev172/sqs");

    try {
      iam.deleteRolePolicy(DeleteRolePolicyRequest.builder().roleName(roleName)
          .policyName("AllowEventBridgeSendMessage").build());

      iam.deleteRole(DeleteRoleRequest.builder().roleName(roleName).build());
    } catch (NoSuchEntityException e) {
      // ignore
    }

    // Create the role
    CreateRoleResponse createRes = iam.createRole(
        CreateRoleRequest.builder().roleName(roleName).assumeRolePolicyDocument(assumeRolePolicy)
            .description("Allows EventBridge to send messages to SQS").build());

    // 2) Build the inline permissions policy JSON
    String inlinePolicy =
        String.format("{\"Version\":\"2012-10-17\",\"Statement\":[{\"Effect\":\"Allow\","
            + "\"Action\":[\"sqs:SendMessage\"],\"Resource\":[\"%s\"]}]}", queueArn);

    // Attach the inline policy
    iam.putRolePolicy(PutRolePolicyRequest.builder().roleName(roleName)
        .policyName("AllowEventBridgeSendMessage").policyDocument(inlinePolicy).build());

    iam.close();

    return createRes.role().arn();
  }
}
