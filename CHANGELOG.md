# FormKiQ-Core Releases #

### Version 1.2.5
- Added GET/POST /webhooks/{webhookId}/tags

### Version 1.2.4
- Added 'path' to DocumentEvent

### Version 1.2.3
- Added PATCH /webhooks/{webhookId}

### Version 1.2.2
- Added ability to set TimeToLive on records created through a webhook

### Version 1.2.1
- Added SnsDocumentEventArn as CloudFormation Output

### Version 1.2.0
- Added /public/webhooks/{webhooks+} API
- Added GET / POST /webhooks API
- Added DELETE /webhooks/{webhookId} API
- Updated GraalVM version to 21.0.0
- Updated Console to 1.3.5
- Added Document's Content to SNS message if size < 256KB and Content is plain text
- Added userId as System Defined tag
- Added AppEnvironment as CloudFormation Output

### Version 1.1.0
- Added Email Notify Module
- Fixed /documents to not return belongsToDocumentId
- Changed a NULL tag value to store as a ""
- Updated FormKiQ Console to v1.3.3
- Merged Create/Update/Delete DocumentEvents SNS Topic into 1.
- Changed /documents/{documentId} to return all data for child documents
- Added SiteId as Document Event Message Attribute
- Setup SAM / SAR deployment

### Version 1.0.0 (Nov 25, 2020)
- Initial Release
