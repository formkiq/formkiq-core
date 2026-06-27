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
package com.formkiq.stacks.api.handler.users;

import com.formkiq.aws.dynamodb.objects.DateUtil;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AttributeType;
import software.amazon.awssdk.services.cognitoidentityprovider.model.UserType;

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Function to Convert {@link UserType} to {@link Map}.
 */
public class UsersResponseToMap implements Function<UserType, Map<String, Object>> {


  /** {@link SimpleDateFormat}. */
  private final SimpleDateFormat df = DateUtil.getIsoDateFormatter();

  @Override
  public Map<String, Object> apply(final UserType ut) {

    String email = getEmail(ut.attributes());

    Map<String, Object> attributes = new UserAttributesToMap().apply(ut.attributes());

    return Map.of("username", ut.username(), "email", email, "userStatus", ut.userStatus().name(),
        "enabled", ut.enabled(), "insertedDate", toString(ut.userCreateDate()), "lastModifiedDate",
        toString(ut.userLastModifiedDate()), "attributes", attributes);
  }

  /**
   * Get Email from {@link UserType}.
   * 
   * @param attributes {@link List} {@link AttributeType}
   * @return {@link String}
   */
  public String getEmail(final List<AttributeType> attributes) {
    return attributes.stream().filter(u -> "email".equalsIgnoreCase(u.name()))
        .map(AttributeType::value).findAny().orElse("");
  }

  private String toString(final Instant date) {
    String result = "";

    if (date != null) {
      result = this.df.format(Date.from(date));
    }

    return result;
  }
}
