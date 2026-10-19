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
package com.formkiq.aws.dynamodb;

/**
 * API permissions with READ, WRITE, DELETE and GOVERN umbrella grants and scoped grants grouped by
 * FormKiQ client operation tags. For example, READ implies READ_DOCUMENTS and
 * READ_DOCUMENT_ATTRIBUTES, while READ_DOCUMENTS does not imply READ_DOCUMENT_ATTRIBUTES.
 */
public enum ApiPermission {
  /**
   * Admin permission.
   *
   */
  ADMIN(0, "Grant all API permissions."),
  /**
   * Delete permission.
   *
   */
  DELETE(1, "Delete resources covered by DELETE permissions."),
  /**
   * Read permission.
   *
   */
  READ(2, "Read resources covered by READ permissions."),
  /**
   * Write permission.
   *
   */
  WRITE(3, "Write resources covered by WRITE permissions."),
  /**
   * Govern permission.
   *
   */
  GOVERN(4, "Govern resources covered by GOVERN permissions."),

  /** Govern permission for Access Control. */
  GOVERN_ACCESS_CONTROL(5, ApiPermissionRoleType.GOVERN, ApiPermissionGroup.ACCESS_CONTROL,
      "Read and manage access-control policies."),
  /** Read permission for Attributes. */
  READ_ATTRIBUTES(6, ApiPermissionRoleType.READ, ApiPermissionGroup.ATTRIBUTES,
      "Read attribute definitions and allowed values."),
  /** Write permission for Attributes. */
  WRITE_ATTRIBUTES(7, ApiPermissionRoleType.WRITE, ApiPermissionGroup.ATTRIBUTES,
      "Create and update attribute definitions and allowed values."),
  /** Delete permission for Attributes. */
  DELETE_ATTRIBUTES(8, ApiPermissionRoleType.DELETE, ApiPermissionGroup.ATTRIBUTES,
      "Delete attribute definitions and allowed values."),
  /** Read permission for Document Actions. */
  READ_DOCUMENT_ACTIONS(9, ApiPermissionRoleType.READ, ApiPermissionGroup.DOCUMENT_ACTIONS,
      "Read document actions and their status."),
  /** Write permission for Document Actions. */
  WRITE_DOCUMENT_ACTIONS(10, ApiPermissionRoleType.WRITE, ApiPermissionGroup.DOCUMENT_ACTIONS,
      "Create, update, and retry document actions."),
  /** Read permission for Document Attributes. */
  READ_DOCUMENT_ATTRIBUTES(11, ApiPermissionRoleType.READ, ApiPermissionGroup.DOCUMENT_ATTRIBUTES,
      "Read attributes assigned to documents."),
  /** Write permission for Document Attributes. */
  WRITE_DOCUMENT_ATTRIBUTES(12, ApiPermissionRoleType.WRITE, ApiPermissionGroup.DOCUMENT_ATTRIBUTES,
      "Add, update, and generate attributes assigned to documents."),
  /** Delete permission for Document Attributes. */
  DELETE_DOCUMENT_ATTRIBUTES(13, ApiPermissionRoleType.DELETE,
      ApiPermissionGroup.DOCUMENT_ATTRIBUTES,
      "Remove attributes and attribute values from documents."),
  /** Govern permission for Document Folders. */
  GOVERN_DOCUMENT_FOLDERS(14, ApiPermissionRoleType.GOVERN, ApiPermissionGroup.DOCUMENT_FOLDERS,
      "Read and manage folder access permissions."),
  /** Write permission for Document Generation. */
  WRITE_DOCUMENT_GENERATION(15, ApiPermissionRoleType.WRITE, ApiPermissionGroup.DOCUMENT_GENERATION,
      "Generate documents from templates and data."),
  /** Write permission for Document Shares. */
  WRITE_DOCUMENT_SHARES(16, ApiPermissionRoleType.WRITE, ApiPermissionGroup.DOCUMENT_SHARES,
      "Create shares for folders and their documents."),
  /** Delete permission for Document Shares. */
  DELETE_DOCUMENT_SHARES(17, ApiPermissionRoleType.DELETE, ApiPermissionGroup.DOCUMENT_SHARES,
      "Delete document shares."),
  /** Read permission for Document Tags. */
  READ_DOCUMENT_TAGS(18, ApiPermissionRoleType.READ, ApiPermissionGroup.DOCUMENT_TAGS,
      "Read tags assigned to documents."),
  /** Write permission for Document Tags. */
  WRITE_DOCUMENT_TAGS(19, ApiPermissionRoleType.WRITE, ApiPermissionGroup.DOCUMENT_TAGS,
      "Add and update tags assigned to documents."),
  /** Delete permission for Document Tags. */
  DELETE_DOCUMENT_TAGS(20, ApiPermissionRoleType.DELETE, ApiPermissionGroup.DOCUMENT_TAGS,
      "Remove tags and tag values from documents."),
  /** Read permission for Document Versions. */
  READ_DOCUMENT_VERSIONS(21, ApiPermissionRoleType.READ, ApiPermissionGroup.DOCUMENT_VERSIONS,
      "Read document versions."),
  /** Write permission for Document Versions. */
  WRITE_DOCUMENT_VERSIONS(22, ApiPermissionRoleType.WRITE, ApiPermissionGroup.DOCUMENT_VERSIONS,
      "Set the active version of a document."),
  /** Govern permission for Document Versions. */
  GOVERN_DOCUMENT_VERSIONS(23, ApiPermissionRoleType.GOVERN, ApiPermissionGroup.DOCUMENT_VERSIONS,
      "Delete historical document versions."),
  /** Read permission for Document Workflows. */
  READ_DOCUMENT_WORKFLOWS(24, ApiPermissionRoleType.READ, ApiPermissionGroup.DOCUMENT_WORKFLOWS,
      "Read workflows, queues, and document workflow status."),
  /** Write permission for Document Workflows. */
  WRITE_DOCUMENT_WORKFLOWS(25, ApiPermissionRoleType.WRITE, ApiPermissionGroup.DOCUMENT_WORKFLOWS,
      "Create and update workflows and queues, and submit document workflow decisions."),
  /** Delete permission for Document Workflows. */
  DELETE_DOCUMENT_WORKFLOWS(26, ApiPermissionRoleType.DELETE, ApiPermissionGroup.DOCUMENT_WORKFLOWS,
      "Delete workflows and queues."),
  /** Read permission for Documents. */
  READ_DOCUMENTS(27, ApiPermissionRoleType.READ, ApiPermissionGroup.DOCUMENTS,
      "Read and search documents, their content, and related document data."),
  /** Write permission for Documents. */
  WRITE_DOCUMENTS(28, ApiPermissionRoleType.WRITE, ApiPermissionGroup.DOCUMENTS,
      "Create, upload, and update documents, their content, and related document data."),
  /** Delete permission for Documents. */
  DELETE_DOCUMENTS(29, ApiPermissionRoleType.DELETE, ApiPermissionGroup.DOCUMENTS,
      "Delete documents and related document data."),
  /** Govern permission for Documents. */
  GOVERN_DOCUMENTS(30, ApiPermissionRoleType.GOVERN, ApiPermissionGroup.DOCUMENTS,
      "Permanently delete documents and manage document governance, including legal holds."),
  /** Read permission for E-Signature. */
  READ_E_SIGNATURE(31, ApiPermissionRoleType.READ, ApiPermissionGroup.E_SIGNATURE,
      "Read electronic signature requests, envelopes, and recipients."),
  /** Write permission for E-Signature. */
  WRITE_E_SIGNATURE(32, ApiPermissionRoleType.WRITE, ApiPermissionGroup.E_SIGNATURE,
      "Create and manage electronic signature envelopes, signing views, and reminders."),
  /** Read permission for Entity. */
  READ_ENTITY(33, ApiPermissionRoleType.READ, ApiPermissionGroup.ENTITY,
      "Read entities and their attributes."),
  /** Write permission for Entity. */
  WRITE_ENTITY(34, ApiPermissionRoleType.WRITE, ApiPermissionGroup.ENTITY,
      "Create and update entities and their attributes."),
  /** Delete permission for Entity. */
  DELETE_ENTITY(35, ApiPermissionRoleType.DELETE, ApiPermissionGroup.ENTITY,
      "Delete entities and their attributes."),
  /** Read permission for Examine Objects. */
  READ_EXAMINE_OBJECTS(36, ApiPermissionRoleType.READ, ApiPermissionGroup.EXAMINE_OBJECTS,
      "Upload PDFs for examination and read their details."),
  /** Write permission for Google Integration. */
  WRITE_GOOGLE_INTEGRATION(37, ApiPermissionRoleType.WRITE, ApiPermissionGroup.GOOGLE_INTEGRATION,
      "Export documents through the Google Drive integration."),
  /** Read permission for Intelligent Document Processing. */
  READ_INTELLIGENT_DOCUMENT_PROCESSING(38, ApiPermissionRoleType.READ,
      ApiPermissionGroup.INTELLIGENT_DOCUMENT_PROCESSING,
      "Read document classifications and AI processing results."),
  /** Write permission for Intelligent Document Processing. */
  WRITE_INTELLIGENT_DOCUMENT_PROCESSING(39, ApiPermissionRoleType.WRITE,
      ApiPermissionGroup.INTELLIGENT_DOCUMENT_PROCESSING,
      "Classify documents and run AI prompts against document content."),
  /** Read permission for Mappings. */
  READ_MAPPINGS(40, ApiPermissionRoleType.READ, ApiPermissionGroup.MAPPINGS,
      "Read document data mappings."),
  /** Write permission for Mappings. */
  WRITE_MAPPINGS(41, ApiPermissionRoleType.WRITE, ApiPermissionGroup.MAPPINGS,
      "Create and update document data mappings."),
  /** Delete permission for Mappings. */
  DELETE_MAPPINGS(42, ApiPermissionRoleType.DELETE, ApiPermissionGroup.MAPPINGS,
      "Delete document data mappings."),
  /** Read permission for Rulesets. */
  READ_RULESETS(43, ApiPermissionRoleType.READ, ApiPermissionGroup.RULESETS,
      "Read rulesets and their rules."),
  /** Write permission for Rulesets. */
  WRITE_RULESETS(44, ApiPermissionRoleType.WRITE, ApiPermissionGroup.RULESETS,
      "Create and update rulesets and their rules."),
  /** Delete permission for Rulesets. */
  DELETE_RULESETS(45, ApiPermissionRoleType.DELETE, ApiPermissionGroup.RULESETS,
      "Delete rulesets and their rules."),
  /** Read permission for Schemas. */
  READ_SCHEMAS(46, ApiPermissionRoleType.READ, ApiPermissionGroup.SCHEMAS,
      "Read schemas and their attribute definitions."),
  /** Write permission for Schemas. */
  WRITE_SCHEMAS(47, ApiPermissionRoleType.WRITE, ApiPermissionGroup.SCHEMAS,
      "Create and update schemas and their attribute definitions."),
  /** Delete permission for Schemas. */
  DELETE_SCHEMAS(48, ApiPermissionRoleType.DELETE, ApiPermissionGroup.SCHEMAS,
      "Delete schemas and their attribute definitions."),
  /** Write permission for Shortlinks. */
  WRITE_SHORTLINKS(49, ApiPermissionRoleType.WRITE, ApiPermissionGroup.SHORTLINKS,
      "Create short links."),
  /** Read permission for System Management. */
  READ_SYSTEM_MANAGEMENT(50, ApiPermissionRoleType.SYSTEM_MANAGEMENT,
      ApiPermissionGroup.SYSTEM_MANAGEMENT,
      "Read system and site configuration, API keys, and search settings."),
  /** Write permission for System Management. */
  WRITE_SYSTEM_MANAGEMENT(51, ApiPermissionRoleType.SYSTEM_MANAGEMENT,
      ApiPermissionGroup.SYSTEM_MANAGEMENT,
      "Manage system and site configuration, API keys, and search settings."),
  /** Delete permission for System Management. */
  DELETE_SYSTEM_MANAGEMENT(52, ApiPermissionRoleType.SYSTEM_MANAGEMENT,
      ApiPermissionGroup.SYSTEM_MANAGEMENT,
      "Delete site groups, API keys, locales, and search resources."),
  /** Govern permission for User Activities. */
  GOVERN_USER_ACTIVITIES(53, ApiPermissionRoleType.GOVERN, ApiPermissionGroup.USER_ACTIVITIES,
      "Read user activity and audit records."),
  /** Read permission for User Management. */
  READ_USER_MANAGEMENT(54, ApiPermissionRoleType.USER_MANAGEMENT,
      ApiPermissionGroup.USER_MANAGEMENT, "Read users, groups, and their memberships."),
  /** Write permission for User Management. */
  WRITE_USER_MANAGEMENT(55, ApiPermissionRoleType.USER_MANAGEMENT,
      ApiPermissionGroup.USER_MANAGEMENT,
      "Create and update users, groups, and their memberships."),
  /** Delete permission for User Management. */
  DELETE_USER_MANAGEMENT(56, ApiPermissionRoleType.USER_MANAGEMENT,
      ApiPermissionGroup.USER_MANAGEMENT, "Delete users, groups, and their memberships."),
  /** Govern permission for User Management. */
  GOVERN_USER_MANAGEMENT(57, ApiPermissionRoleType.USER_MANAGEMENT,
      ApiPermissionGroup.USER_MANAGEMENT, "Govern access to users and groups."),
  /** Read permission for Webhooks. */
  READ_WEBHOOKS(58, ApiPermissionRoleType.READ, ApiPermissionGroup.WEBHOOKS,
      "Read webhooks and their configuration."),
  /** Write permission for Webhooks. */
  WRITE_WEBHOOKS(59, ApiPermissionRoleType.WRITE, ApiPermissionGroup.WEBHOOKS,
      "Create and update webhooks and their configuration."),
  /** Delete permission for Webhooks. */
  DELETE_WEBHOOKS(60, ApiPermissionRoleType.DELETE, ApiPermissionGroup.WEBHOOKS,
      "Delete webhooks."),
  /** Read permission for Reminder Policy. */
  READ_REMINDER_POLICY(61, ApiPermissionRoleType.READ, ApiPermissionGroup.REMINDER_POLICY,
      "Read reminder policy preset entities and their entity type definitions."),
  /** Write permission for Reminder Policy. */
  WRITE_REMINDER_POLICY(62, ApiPermissionRoleType.WRITE, ApiPermissionGroup.REMINDER_POLICY,
      "Create and update reminder policy preset entities and their entity type definitions."),
  /** Delete permission for Reminder Policy. */
  DELETE_REMINDER_POLICY(63, ApiPermissionRoleType.DELETE, ApiPermissionGroup.REMINDER_POLICY,
      "Delete reminder policy preset entities and their entity type definitions."),
  /** Read permission for Retention Policy. */
  READ_RETENTION_POLICY(64, ApiPermissionRoleType.READ, ApiPermissionGroup.RETENTION_POLICY,
      "Read retention policy preset entities and their entity type definitions."),
  /** Write permission for Retention Policy. */
  WRITE_RETENTION_POLICY(65, ApiPermissionRoleType.WRITE, ApiPermissionGroup.RETENTION_POLICY,
      "Create and update retention policy preset entities and their entity type definitions."),
  /** Delete permission for Retention Policy. */
  DELETE_RETENTION_POLICY(66, ApiPermissionRoleType.DELETE, ApiPermissionGroup.RETENTION_POLICY,
      "Delete retention policy preset entities and their entity type definitions.");

  /** Permanent bitmap index, independent of enum declaration order. */
  private final int bitmapIndex;
  /** Human-readable description of the permission. */
  private final String description;
  /** Permission Group. */
  private final ApiPermissionGroup group;
  /** Permission type. */
  private final ApiPermissionRoleType type;

  /**
   * Constructor for a scoped permission.
   *
   * @param permissionBitmapIndex permanent bitmap index
   * @param permissionType permission type
   * @param permissionGroup FormKiQ client tag group
   * @param permissionDescription permission description
   */
  ApiPermission(final int permissionBitmapIndex, final ApiPermissionRoleType permissionType,
      final ApiPermissionGroup permissionGroup, final String permissionDescription) {
    this.bitmapIndex = permissionBitmapIndex;
    this.description = permissionDescription;
    this.type = permissionType;
    this.group = permissionGroup;
  }

  /**
   * Constructor for a top-level permission.
   *
   * @param permissionBitmapIndex permanent bitmap index
   * @param permissionDescription permission description
   */
  ApiPermission(final int permissionBitmapIndex, final String permissionDescription) {
    this.bitmapIndex = permissionBitmapIndex;
    this.description = permissionDescription;
    this.type = ApiPermissionRoleType.valueOf(name());
    this.group = null;
  }

  /**
   * Get the permanent bitmap index. Published indices must never be changed or reused.
   *
   * @return bitmap index
   */
  public int getBitmapIndex() {
    return this.bitmapIndex;
  }

  /**
   * Get the human-readable description of the permission.
   *
   * @return permission description
   */
  public String getDescription() {
    return this.description;
  }

  /**
   * Get the FormKiQ client tag group.
   *
   * @return group, or null for a top-level permission
   */
  public ApiPermissionGroup getGroup() {
    return this.group;
  }

  /**
   * Get the permission type.
   *
   * @return permission role type
   */
  public ApiPermissionRoleType getType() {
    return this.type;
  }

}
