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
package com.formkiq.stacks.dynamodb.permissions;

import java.util.Collection;
import java.util.stream.Collectors;
import com.formkiq.module.lambdaservices.AwsServiceCache;
import com.formkiq.module.lambdaservices.DocumentAuthorizationHandler;

/**
 * 
 * Document Permission implementation of {@link DocumentAuthorizationHandler}.
 *
 */
public class DocumentPermissionsAuthorizationHandler implements DocumentAuthorizationHandler {

  @Override
  public boolean isAuthorized(final AwsServiceCache awsservice, final Collection<String> roleNames,
      final String siteId, final String documentId, final String permission) {

    boolean authorized = false;
    DocumentPermissionService permissionService =
        awsservice.getExtension(DocumentPermissionService.class);

    boolean hasDocumentPermissions = permissionService.hasDocumentPermissions(siteId, documentId);

    if (hasDocumentPermissions) {

      Collection<Permission> types =
          permissionService.getPermissions(roleNames, siteId, documentId);
      Collection<String> typesNames =
          types.stream().map(t -> t.name().toLowerCase()).collect(Collectors.toList());

      authorized = typesNames.contains(permission);

    } else {
      authorized = true;
    }

    return authorized;
  }

}
