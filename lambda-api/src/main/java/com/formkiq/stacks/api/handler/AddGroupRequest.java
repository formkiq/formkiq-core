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
package com.formkiq.stacks.api.handler;

import com.formkiq.aws.services.lambda.exceptions.BadException;
import com.formkiq.graalvm.annotations.Reflectable;

import static com.formkiq.aws.dynamodb.objects.Strings.isEmpty;

/**
 * Add Group Request.
 */
@Reflectable
public class AddGroupRequest {
  /** {@link AddGroup}. */
  private AddGroup group;

  /**
   * Get Group.
   * 
   * @return AddGroup
   */
  public AddGroup getGroup() throws BadException {
    if (this.group == null) {
      throw new BadException("invalid request body");
    }
    return this.group;
  }

  /**
   * Set Group.
   * 
   * @param addGroup {@link AddGroup}
   * @return AddGroupRequest
   */
  public AddGroupRequest setGroup(final AddGroup addGroup) {
    this.group = addGroup;
    return this;
  }

  /**
   * Get Group Name.
   * 
   * @return String
   * @throws BadException BadException
   */
  public String getGroupName() throws BadException {
    String groupName = getGroup().getGroupName();
    if (isEmpty(groupName)) {
      throw new BadException("invalid 'groupName''");
    }

    return groupName;
  }
}
