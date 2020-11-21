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
package com.formkiq.aws.iam;

import software.amazon.awssdk.services.iam.IamClient;
import software.amazon.awssdk.services.iam.model.AddUserToGroupRequest;
import software.amazon.awssdk.services.iam.model.AddUserToGroupResponse;

/**
 * 
 * Iam Services.
 *
 */
public class IamService {

  /** {@link IamConnectionBuilder}. */
  private IamConnectionBuilder builder;

  /**
   * Constructor.
   * 
   * @param connectionBuilder {@link IamConnectionBuilder}
   */
  public IamService(final IamConnectionBuilder connectionBuilder) {
    this.builder = connectionBuilder;
  }

  /**
   * Build {@link IamClient}.
   * 
   * @return {@link IamClient}
   */
  public IamClient buildClient() {
    return this.builder.build();
  }

  /**
   * Add IAM User To IAM Group.
   * 
   * @param iam {@link IamClient}
   * @param userName {@link String}
   * @param groupName {@link String}
   * @return {@link AddUserToGroupResponse}
   */
  public AddUserToGroupResponse addUserToGroup(final IamClient iam, final String userName,
      final String groupName) {
    return iam.addUserToGroup(
        AddUserToGroupRequest.builder().groupName(groupName).userName(userName).build());
  }
}
