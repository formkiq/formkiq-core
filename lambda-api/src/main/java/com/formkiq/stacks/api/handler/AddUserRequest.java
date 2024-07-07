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
 * Add User Request.
 */
@Reflectable
public class AddUserRequest {
  /** {@link AddUser}. */
  private AddUser user;

  /**
   * Get User.
   * 
   * @return AddUser
   */
  public AddUser getUser() throws BadException {
    if (this.user == null) {
      throw new BadException("invalid request body");
    }
    return this.user;
  }

  /**
   * Set User.
   * 
   * @param addUser {@link AddUser}
   * @return AddUserRequest
   */
  public AddUserRequest setUser(final AddUser addUser) {
    this.user = addUser;
    return this;
  }

  /**
   * Get Username.
   * 
   * @return String
   * @throws BadException BadException
   */
  public String getUsername() throws BadException {
    String username = getUser().getUsername();
    if (isEmpty(username)) {
      throw new BadException("invalid 'username''");
    }

    return username;
  }
}
