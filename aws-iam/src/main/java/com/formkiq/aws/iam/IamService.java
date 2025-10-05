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

  /**
   * Build {@link IamClient}.
   * 
   * @return {@link IamClient}
   */
  public IamClient buildClient() {
    return this.builder.build();
  }
}
