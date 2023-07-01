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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import com.formkiq.aws.dynamodb.DbKeys;
import com.formkiq.aws.dynamodb.DynamoDbConnectionBuilder;
import com.formkiq.aws.dynamodb.DynamoDbService;
import com.formkiq.aws.dynamodb.DynamoDbServiceImpl;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/**
 * 
 * DynamoDb implementation of the {@link DocumentPermissionService}.
 *
 */
public class DocumentPermissionServiceDynamoDb implements DocumentPermissionService, DbKeys {

  /** {@link DynamoDbService}. */
  private DynamoDbService db;

  /**
   * constructor.
   * 
   * @param connection {@link DynamoDbConnectionBuilder}
   * @param documentsTable {@link String}
   */
  public DocumentPermissionServiceDynamoDb(final DynamoDbConnectionBuilder connection,
      final String documentsTable) {
    this.db = new DynamoDbServiceImpl(connection, documentsTable);
  }


  /**
   * Create {@link DocumentPermission} Default.
   * 
   * @param documentId {@link String}
   * @return {@link DocumentPermission}
   */
  private DocumentPermission getDefaultDocumentPermissions(final String documentId) {
    return new DocumentPermission().documentId(documentId).name("").type(PermissionType.DEFAULT)
        .permission(Permission.DENY).userId("");
  }

  @Override
  public Collection<Permission> getPermissions(final Collection<String> groups, final String siteId,
      final String documentId) {

    PermissionType permissionType = PermissionType.GROUP;
    Collection<Map<String, AttributeValue>> batch = new ArrayList<>();

    for (String group : groups) {
      for (Permission permission : Permission.values()) {
        DocumentPermission p = new DocumentPermission().documentId(documentId).name(group)
            .type(permissionType).permission(permission);
        batch.add(Map.of(PK, AttributeValue.fromS(p.pk(siteId)), SK, AttributeValue.fromS(p.sk())));
      }
    }

    List<Map<String, AttributeValue>> response = this.db.getBatch(batch);

    return response.stream().map(m -> Permission.valueOf(m.get("permission").s()))
        .collect(Collectors.toList());
  }

  @Override
  public boolean hasDocumentPermissions(final String siteId, final String documentId) {
    DocumentPermission permissions = getDefaultDocumentPermissions(documentId);
    String pk = permissions.pk(siteId);
    String sk = permissions.sk();

    return this.db.exists(AttributeValue.fromS(pk), AttributeValue.fromS(sk));
  }


  @Override
  public void save(final String siteId, final Collection<DocumentPermission> permissions) {

    Collection<String> documentIds =
        permissions.stream().map(d -> d.documentId()).collect(Collectors.toSet());

    List<Map<String, AttributeValue>> rootAttributes =
        documentIds.stream().map(id -> getDefaultDocumentPermissions(id))
            .map(p -> p.getAttributes(siteId)).collect(Collectors.toList());

    List<Map<String, AttributeValue>> attributes =
        permissions.stream().map(p -> p.getAttributes(siteId)).collect(Collectors.toList());

    List<Map<String, AttributeValue>> toSave =
        Stream.concat(rootAttributes.stream(), attributes.stream()).collect(Collectors.toList());

    this.db.putItems(toSave);
  }
}
