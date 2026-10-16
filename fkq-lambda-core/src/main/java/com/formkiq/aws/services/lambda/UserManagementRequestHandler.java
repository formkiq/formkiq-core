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
package com.formkiq.aws.services.lambda;

import com.formkiq.aws.dynamodb.ApiPermission;

import java.util.Locale;
import java.util.Optional;

/** Required user management permissions for API request methods. */
public interface UserManagementRequestHandler extends ApiGatewayRequestHandler {

  @Override
  default ApiPermissionScope getPermissionScope() {
    return ApiPermissionScope.ANY_SITE;
  }

  @Override
  default Optional<ApiPermission> getRequiredPermission(final String method) {
    return switch (method.toLowerCase(Locale.ROOT)) {
      case "get", "head" -> Optional.of(ApiPermission.READ_USER_MANAGEMENT);
      case "post", "put", "patch" -> Optional.of(ApiPermission.WRITE_USER_MANAGEMENT);
      case "delete" -> Optional.of(ApiPermission.DELETE_USER_MANAGEMENT);
      default -> Optional.empty();
    };
  }
}
