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
package com.formkiq.testutils.api.folders;

import com.formkiq.client.api.DocumentFoldersApi;
import com.formkiq.client.invoker.ApiClient;
import com.formkiq.client.model.AddFolderPermission;
import com.formkiq.client.model.FolderPermissionType;
import com.formkiq.client.model.SetFolderPermissionsRequest;
import com.formkiq.client.model.SetResponse;
import com.formkiq.testutils.api.ApiHttpResponse;
import com.formkiq.testutils.api.HttpRequestBuilder;

import java.util.List;

/**
 * Builder for Get Document Folders.
 */
public class SetFolderPermissionsRequestBuilder implements HttpRequestBuilder<SetResponse> {

  /** {@link SetFolderPermissionsRequest}. */
  private final SetFolderPermissionsRequest req;

  /**
   * constructor.
   */
  public SetFolderPermissionsRequestBuilder() {
    req = new SetFolderPermissionsRequest();
  }

  /**
   * Add DELETE Role.
   * 
   * @param roleName {@link String}
   * @return SetFolderPermissionsRequestBuilder
   */
  public SetFolderPermissionsRequestBuilder addDeleteRole(final String roleName) {
    return addRole(roleName, FolderPermissionType.DELETE);
  }

  /**
   * Add Read Role.
   * 
   * @param roleName {@link String}
   * @return SetFolderPermissionsRequestBuilder
   */
  public SetFolderPermissionsRequestBuilder addReadRole(final String roleName) {
    return addRole(roleName, FolderPermissionType.READ);
  }

  /**
   * Add Role.
   *
   * @param roleName {@link String}
   * @param permission {@link FolderPermissionType}
   * @return this builder
   */
  public SetFolderPermissionsRequestBuilder addRole(final String roleName,
      final FolderPermissionType permission) {
    this.req.addRolesItem(new AddFolderPermission().roleName(roleName)
        .permissions(permission != null ? List.of(permission) : null));
    return this;
  }

  /**
   * Add Role.
   *
   * @param roleName {@link String}
   * @param permissions {@link List} {@link FolderPermissionType}
   * @return this builder
   */
  public SetFolderPermissionsRequestBuilder addRole(final String roleName,
      final List<FolderPermissionType> permissions) {
    this.req.addRolesItem(new AddFolderPermission().roleName(roleName).permissions(permissions));
    return this;
  }

  /**
   * Add Write Role.
   * 
   * @param roleName {@link String}
   * @return SetFolderPermissionsRequestBuilder
   */
  public SetFolderPermissionsRequestBuilder addWriteRole(final String roleName) {
    return addRole(roleName, FolderPermissionType.WRITE);
  }

  /**
   * Set the folder folderPath to retrieve documents from.
   * 
   * @param folderPath {@link String}
   * @return this builder
   */
  public SetFolderPermissionsRequestBuilder path(final String folderPath) {
    this.req.path(folderPath);
    return this;
  }

  @Override
  public ApiHttpResponse<SetResponse> submit(final ApiClient apiClient, final String siteId) {
    return executeApiCall(
        () -> new DocumentFoldersApi(apiClient).setFolderPermissions(req, siteId));
  }
}
