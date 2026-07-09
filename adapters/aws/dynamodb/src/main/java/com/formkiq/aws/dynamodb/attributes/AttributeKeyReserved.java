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
package com.formkiq.aws.dynamodb.attributes;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Reserved Attribute Keys.
 */
public enum AttributeKeyReserved {
  /** Publication. */
  PUBLICATION("Publication"),
  /** Classification. */
  CLASSIFICATION("Classification"),
  /** Sensitivity. */
  SENSITIVITY("Sensitivity", AttributeType.GOVERNANCE, AttributeDataType.STRING, null),
  /** Malware Scan Result. */
  MALWARE_SCAN_RESULT("MalwareScanResult"),
  /** Relationships. */
  RELATIONSHIPS("Relationships"),
  /** Artifact Status. */
  ARTIFACT_STATUS("ArtifactStatus"),
  /** Checkout. */
  CHECKOUT("Checkout"),
  /** CheckoutForLegalHold. */
  CHECKOUT_FOR_LEGAL_HOLD("CheckoutForLegalHold"),
  /** Checkout. */
  LOCKED_BY("LockedBy"),
  /** Checkout. */
  LOCKED_DATE("LockedDate"),
  /** Esignature Docusign Envelope id. */
  ESIGNATURE_DOCUSIGN_ENVELOPE_ID("EsignatureDocusignEnvelopeId"),
  /** Esignature Docusign Status. */
  ESIGNATURE_DOCUSIGN_STATUS("EsignatureDocusignStatus"),
  /** Agreement Name. */
  AGREEMENT_NAME("AgreementName"),
  /** Agreement Status. */
  AGREEMENT_STATUS("AgreementStatus"),
  /** Agreement Lifecycle State. */
  AGREEMENT_LIFECYCLE_STATE("AgreementLifecycleState"),
  /** Agreement Type. */
  AGREEMENT_TYPE("AgreementType"),
  /** Agreement Value. */
  AGREEMENT_VALUE("AgreementValue", AttributeType.STANDARD, AttributeDataType.NUMBER, null),
  /** Agreement Currency. */
  AGREEMENT_CURRENCY("AgreementCurrency"),
  /** Agreement Effective Date. */
  AGREEMENT_EFFECTIVE_DATE("AgreementEffectiveDate"),
  /** Agreement Expiration Date. */
  AGREEMENT_EXPIRATION_DATE("AgreementExpirationDate"),
  /** Agreement Execution Date. */
  AGREEMENT_EXECUTION_DATE("AgreementExecutionDate"),
  /** Agreement Auto Renew. */
  AGREEMENT_AUTO_RENEW("AgreementAutoRenew", AttributeType.STANDARD, AttributeDataType.BOOLEAN,
      null),
  /** Agreement Governing Law. */
  AGREEMENT_GOVERNING_LAW("AgreementGoverningLaw"),
  /** Agreement Jurisdiction. */
  AGREEMENT_JURISDICTION("AgreementJurisdiction"),
  /** Agreement Owner. */
  AGREEMENT_OWNER("AgreementOwner"),
  /** Agreement Department. */
  AGREEMENT_DEPARTMENT("AgreementDepartment"),
  /** Agreement Parties. */
  AGREEMENT_PARTIES("AgreementParties", AttributeType.STANDARD, AttributeDataType.ENTITY, null),
  /** Agreement Working Documents. */
  AGREEMENT_WORKING_DOCUMENTS("AgreementWorkingDocuments", AttributeType.STANDARD,
      AttributeDataType.ENTITY, null),
  /** Agreement Executed Documents. */
  AGREEMENT_EXECUTED_DOCUMENTS("AgreementExecutedDocuments", AttributeType.STANDARD,
      AttributeDataType.ENTITY, null),
  /** Agreement Clauses. */
  AGREEMENT_CLAUSES("AgreementClauses", AttributeType.STANDARD, AttributeDataType.ENTITY, null),
  /** Agreement Obligations. */
  AGREEMENT_OBLIGATIONS("AgreementObligations", AttributeType.STANDARD, AttributeDataType.ENTITY,
      null),
  /** Agreement Milestones. */
  AGREEMENT_MILESTONES("AgreementMilestones", AttributeType.STANDARD, AttributeDataType.ENTITY,
      null),
  /** Agreement Notes. */
  AGREEMENT_NOTES("AgreementNotes"),
  /** Agreement Party Agreement. */
  AGREEMENT_PARTY_AGREEMENT("AgreementPartyAgreement", AttributeType.STANDARD,
      AttributeDataType.ENTITY, null),
  /** Agreement Party Company. */
  AGREEMENT_PARTY_COMPANY("AgreementPartyCompany", AttributeType.STANDARD, AttributeDataType.ENTITY,
      null),
  /** Agreement Party Contact. */
  AGREEMENT_PARTY_CONTACT("AgreementPartyContact", AttributeType.STANDARD, AttributeDataType.ENTITY,
      null),
  /** Agreement Party Role. */
  AGREEMENT_PARTY_ROLE("AgreementPartyRole"),
  /** Agreement Party Is Primary. */
  AGREEMENT_PARTY_IS_PRIMARY("AgreementPartyIsPrimary", AttributeType.STANDARD,
      AttributeDataType.BOOLEAN, null),
  /** Agreement Party Signing Authority. */
  AGREEMENT_PARTY_SIGNING_AUTHORITY("AgreementPartySigningAuthority", AttributeType.STANDARD,
      AttributeDataType.BOOLEAN, null),
  /** Agreement Party Billing Party. */
  AGREEMENT_PARTY_BILLING_PARTY("AgreementPartyBillingParty", AttributeType.STANDARD,
      AttributeDataType.BOOLEAN, null),
  /** Agreement Party Notices Party. */
  AGREEMENT_PARTY_NOTICES_PARTY("AgreementPartyNoticesParty", AttributeType.STANDARD,
      AttributeDataType.BOOLEAN, null),
  /** Agreement Party Notes. */
  AGREEMENT_PARTY_NOTES("AgreementPartyNotes"),
  /** Company Name. */
  COMPANY_NAME("CompanyName"),
  /** Company Status. */
  COMPANY_STATUS("CompanyStatus"),
  /** Company Legal Name. */
  COMPANY_LEGAL_NAME("CompanyLegalName"),
  /** Company Type. */
  COMPANY_TYPE("CompanyType"),
  /** Company Country. */
  COMPANY_COUNTRY("CompanyCountry"),
  /** Company Region. */
  COMPANY_REGION("CompanyRegion"),
  /** Company Registration Number. */
  COMPANY_REGISTRATION_NUMBER("CompanyRegistrationNumber"),
  /** Company Tax Identifier. */
  COMPANY_TAX_IDENTIFIER("CompanyTaxIdentifier"),
  /** Company Website. */
  COMPANY_WEBSITE("CompanyWebsite"),
  /** Company Notes. */
  COMPANY_NOTES("CompanyNotes"),
  /** Contact Name. */
  CONTACT_NAME("ContactName"),
  /** Contact Status. */
  CONTACT_STATUS("ContactStatus"),
  /** Contact Email. */
  CONTACT_EMAIL("ContactEmail"),
  /** Contact Company. */
  CONTACT_COMPANY("ContactCompany", AttributeType.STANDARD, AttributeDataType.ENTITY, null),
  /** Contact Title. */
  CONTACT_TITLE("ContactTitle"),
  /** Contact Phone. */
  CONTACT_PHONE("ContactPhone"),
  /** Contact Role. */
  CONTACT_ROLE("ContactRole"),
  /** Contact Notes. */
  CONTACT_NOTES("ContactNotes"),
  /** Agreement Working Document Agreement. */
  AGREEMENT_WORKING_DOCUMENT_AGREEMENT("AgreementWorkingDocumentAgreement", AttributeType.STANDARD,
      AttributeDataType.ENTITY, null),
  /** Agreement Working Document Name. */
  AGREEMENT_WORKING_DOCUMENT_NAME("AgreementWorkingDocumentName"),
  /** Agreement Working Document Status. */
  AGREEMENT_WORKING_DOCUMENT_STATUS("AgreementWorkingDocumentStatus"),
  /** Agreement Working Document Document Role. */
  AGREEMENT_WORKING_DOCUMENT_DOCUMENT_ROLE("AgreementWorkingDocumentDocumentRole"),
  /** Agreement Working Document Is Primary. */
  AGREEMENT_WORKING_DOCUMENT_IS_PRIMARY("AgreementWorkingDocumentIsPrimary", AttributeType.STANDARD,
      AttributeDataType.BOOLEAN, null),
  /** Agreement Working Document Artifact Id. */
  AGREEMENT_WORKING_DOCUMENT_ARTIFACT_ID("AgreementWorkingDocumentArtifactId"),
  /** Agreement Working Document Document Id. */
  AGREEMENT_WORKING_DOCUMENT_DOCUMENT_ID("AgreementWorkingDocumentDocumentId"),
  /** Agreement Working Document Current Version Key. */
  AGREEMENT_WORKING_DOCUMENT_CURRENT_VERSION_KEY("AgreementWorkingDocumentCurrentVersionKey"),
  /** Agreement Working Document Source. */
  AGREEMENT_WORKING_DOCUMENT_SOURCE("AgreementWorkingDocumentSource"),
  /** Agreement Working Document Notes. */
  AGREEMENT_WORKING_DOCUMENT_NOTES("AgreementWorkingDocumentNotes"),
  /** Agreement Executed Document Agreement. */
  AGREEMENT_EXECUTED_DOCUMENT_AGREEMENT("AgreementExecutedDocumentAgreement",
      AttributeType.STANDARD, AttributeDataType.ENTITY, null),
  /** Agreement Executed Document Name. */
  AGREEMENT_EXECUTED_DOCUMENT_NAME("AgreementExecutedDocumentName"),
  /** Agreement Executed Document Status. */
  AGREEMENT_EXECUTED_DOCUMENT_STATUS("AgreementExecutedDocumentStatus"),
  /** Agreement Executed Document Executed Document Role. */
  AGREEMENT_EXECUTED_DOCUMENT_EXECUTED_DOCUMENT_ROLE(
      "AgreementExecutedDocumentExecutedDocumentRole"),
  /** Agreement Executed Document Is Primary Executed Copy. */
  AGREEMENT_EXECUTED_DOCUMENT_IS_PRIMARY_EXECUTED_COPY(
      "AgreementExecutedDocumentIsPrimaryExecutedCopy", AttributeType.STANDARD,
      AttributeDataType.BOOLEAN, null),
  /** Agreement Executed Document Artifact Id. */
  AGREEMENT_EXECUTED_DOCUMENT_ARTIFACT_ID("AgreementExecutedDocumentArtifactId"),
  /** Agreement Executed Document Document Id. */
  AGREEMENT_EXECUTED_DOCUMENT_DOCUMENT_ID("AgreementExecutedDocumentDocumentId"),
  /** Agreement Executed Document Current Version Key. */
  AGREEMENT_EXECUTED_DOCUMENT_CURRENT_VERSION_KEY("AgreementExecutedDocumentCurrentVersionKey"),
  /** Agreement Executed Document Execution Date. */
  AGREEMENT_EXECUTED_DOCUMENT_EXECUTION_DATE("AgreementExecutedDocumentExecutionDate"),
  /** Agreement Executed Document Signature Provider. */
  AGREEMENT_EXECUTED_DOCUMENT_SIGNATURE_PROVIDER("AgreementExecutedDocumentSignatureProvider"),
  /** Agreement Executed Document Provider Envelope Id. */
  AGREEMENT_EXECUTED_DOCUMENT_PROVIDER_ENVELOPE_ID("AgreementExecutedDocumentProviderEnvelopeId"),
  /** Agreement Executed Document Notes. */
  AGREEMENT_EXECUTED_DOCUMENT_NOTES("AgreementExecutedDocumentNotes"),
  /** Agreement Clause Agreement. */
  AGREEMENT_CLAUSE_AGREEMENT("AgreementClauseAgreement", AttributeType.STANDARD,
      AttributeDataType.ENTITY, null),
  /** Agreement Clause Source Document. */
  AGREEMENT_CLAUSE_SOURCE_DOCUMENT("AgreementClauseSourceDocument", AttributeType.STANDARD,
      AttributeDataType.ENTITY, null),
  /** Agreement Clause Extraction Run. */
  AGREEMENT_CLAUSE_EXTRACTION_RUN("AgreementClauseExtractionRun", AttributeType.STANDARD,
      AttributeDataType.ENTITY, null),
  /** Agreement Clause Status. */
  AGREEMENT_CLAUSE_STATUS("AgreementClauseStatus"),
  /** Agreement Clause Clause Type. */
  AGREEMENT_CLAUSE_CLAUSE_TYPE("AgreementClauseClauseType"),
  /** Agreement Clause Clause Title. */
  AGREEMENT_CLAUSE_CLAUSE_TITLE("AgreementClauseClauseTitle"),
  /** Agreement Clause Clause Text. */
  AGREEMENT_CLAUSE_CLAUSE_TEXT("AgreementClauseClauseText"),
  /** Agreement Clause Sequence Number. */
  AGREEMENT_CLAUSE_SEQUENCE_NUMBER("AgreementClauseSequenceNumber", AttributeType.STANDARD,
      AttributeDataType.NUMBER, null),
  /** Agreement Clause Risk Level. */
  AGREEMENT_CLAUSE_RISK_LEVEL("AgreementClauseRiskLevel"),
  /** Agreement Clause Source Reference. */
  AGREEMENT_CLAUSE_SOURCE_REFERENCE("AgreementClauseSourceReference"),
  /** Agreement Clause Source Text. */
  AGREEMENT_CLAUSE_SOURCE_TEXT("AgreementClauseSourceText"),
  /** Agreement Clause Confidence Score. */
  AGREEMENT_CLAUSE_CONFIDENCE_SCORE("AgreementClauseConfidenceScore", AttributeType.STANDARD,
      AttributeDataType.NUMBER, null),
  /** Agreement Clause Reviewed By. */
  AGREEMENT_CLAUSE_REVIEWED_BY("AgreementClauseReviewedBy"),
  /** Agreement Clause Reviewed At. */
  AGREEMENT_CLAUSE_REVIEWED_AT("AgreementClauseReviewedAt"),
  /** Agreement Clause Notes. */
  AGREEMENT_CLAUSE_NOTES("AgreementClauseNotes"),
  /** Agreement Obligation Agreement. */
  AGREEMENT_OBLIGATION_AGREEMENT("AgreementObligationAgreement", AttributeType.STANDARD,
      AttributeDataType.ENTITY, null),
  /** Agreement Obligation Source Clause. */
  AGREEMENT_OBLIGATION_SOURCE_CLAUSE("AgreementObligationSourceClause", AttributeType.STANDARD,
      AttributeDataType.ENTITY, null),
  /** Agreement Obligation Extraction Run. */
  AGREEMENT_OBLIGATION_EXTRACTION_RUN("AgreementObligationExtractionRun", AttributeType.STANDARD,
      AttributeDataType.ENTITY, null),
  /** Agreement Obligation Status. */
  AGREEMENT_OBLIGATION_STATUS("AgreementObligationStatus"),
  /** Agreement Obligation Title. */
  AGREEMENT_OBLIGATION_TITLE("AgreementObligationTitle"),
  /** Agreement Obligation Description. */
  AGREEMENT_OBLIGATION_DESCRIPTION("AgreementObligationDescription"),
  /** Agreement Obligation Obligation Type. */
  AGREEMENT_OBLIGATION_OBLIGATION_TYPE("AgreementObligationObligationType"),
  /** Agreement Obligation Responsible Party. */
  AGREEMENT_OBLIGATION_RESPONSIBLE_PARTY("AgreementObligationResponsibleParty",
      AttributeType.STANDARD, AttributeDataType.ENTITY, null),
  /** Agreement Obligation Responsible Contact. */
  AGREEMENT_OBLIGATION_RESPONSIBLE_CONTACT("AgreementObligationResponsibleContact",
      AttributeType.STANDARD, AttributeDataType.ENTITY, null),
  /** Agreement Obligation Due Date. */
  AGREEMENT_OBLIGATION_DUE_DATE("AgreementObligationDueDate"),
  /** Agreement Obligation Start Date. */
  AGREEMENT_OBLIGATION_START_DATE("AgreementObligationStartDate"),
  /** Agreement Obligation End Date. */
  AGREEMENT_OBLIGATION_END_DATE("AgreementObligationEndDate"),
  /** Agreement Obligation Is Recurring. */
  AGREEMENT_OBLIGATION_IS_RECURRING("AgreementObligationIsRecurring", AttributeType.STANDARD,
      AttributeDataType.BOOLEAN, null),
  /** Agreement Obligation Recurrence Rule. */
  AGREEMENT_OBLIGATION_RECURRENCE_RULE("AgreementObligationRecurrenceRule"),
  /** Agreement Obligation Triggers Payment. */
  AGREEMENT_OBLIGATION_TRIGGERS_PAYMENT("AgreementObligationTriggersPayment",
      AttributeType.STANDARD, AttributeDataType.BOOLEAN, null),
  /** Agreement Obligation Linked Milestone. */
  AGREEMENT_OBLIGATION_LINKED_MILESTONE("AgreementObligationLinkedMilestone",
      AttributeType.STANDARD, AttributeDataType.ENTITY, null),
  /** Agreement Obligation Source Reference. */
  AGREEMENT_OBLIGATION_SOURCE_REFERENCE("AgreementObligationSourceReference"),
  /** Agreement Obligation Source Text. */
  AGREEMENT_OBLIGATION_SOURCE_TEXT("AgreementObligationSourceText"),
  /** Agreement Obligation Confidence Score. */
  AGREEMENT_OBLIGATION_CONFIDENCE_SCORE("AgreementObligationConfidenceScore",
      AttributeType.STANDARD, AttributeDataType.NUMBER, null),
  /** Agreement Obligation Reviewed By. */
  AGREEMENT_OBLIGATION_REVIEWED_BY("AgreementObligationReviewedBy"),
  /** Agreement Obligation Reviewed At. */
  AGREEMENT_OBLIGATION_REVIEWED_AT("AgreementObligationReviewedAt"),
  /** Agreement Obligation Notes. */
  AGREEMENT_OBLIGATION_NOTES("AgreementObligationNotes"),
  /** Agreement Milestone Agreement. */
  AGREEMENT_MILESTONE_AGREEMENT("AgreementMilestoneAgreement", AttributeType.STANDARD,
      AttributeDataType.ENTITY, null),
  /** Agreement Milestone Source Clause. */
  AGREEMENT_MILESTONE_SOURCE_CLAUSE("AgreementMilestoneSourceClause", AttributeType.STANDARD,
      AttributeDataType.ENTITY, null),
  /** Agreement Milestone Extraction Run. */
  AGREEMENT_MILESTONE_EXTRACTION_RUN("AgreementMilestoneExtractionRun", AttributeType.STANDARD,
      AttributeDataType.ENTITY, null),
  /** Agreement Milestone Status. */
  AGREEMENT_MILESTONE_STATUS("AgreementMilestoneStatus"),
  /** Agreement Milestone Title. */
  AGREEMENT_MILESTONE_TITLE("AgreementMilestoneTitle"),
  /** Agreement Milestone Description. */
  AGREEMENT_MILESTONE_DESCRIPTION("AgreementMilestoneDescription"),
  /** Agreement Milestone Milestone Type. */
  AGREEMENT_MILESTONE_MILESTONE_TYPE("AgreementMilestoneMilestoneType"),
  /** Agreement Milestone Due Date. */
  AGREEMENT_MILESTONE_DUE_DATE("AgreementMilestoneDueDate"),
  /** Agreement Milestone Completed Date. */
  AGREEMENT_MILESTONE_COMPLETED_DATE("AgreementMilestoneCompletedDate"),
  /** Agreement Milestone Amount. */
  AGREEMENT_MILESTONE_AMOUNT("AgreementMilestoneAmount", AttributeType.STANDARD,
      AttributeDataType.NUMBER, null),
  /** Agreement Milestone Currency. */
  AGREEMENT_MILESTONE_CURRENCY("AgreementMilestoneCurrency"),
  /** Agreement Milestone Percentage. */
  AGREEMENT_MILESTONE_PERCENTAGE("AgreementMilestonePercentage", AttributeType.STANDARD,
      AttributeDataType.NUMBER, null),
  /** Agreement Milestone Condition. */
  AGREEMENT_MILESTONE_CONDITION("AgreementMilestoneCondition"),
  /** Agreement Milestone Dependency. */
  AGREEMENT_MILESTONE_DEPENDENCY("AgreementMilestoneDependency"),
  /** Agreement Milestone Responsible Party. */
  AGREEMENT_MILESTONE_RESPONSIBLE_PARTY("AgreementMilestoneResponsibleParty",
      AttributeType.STANDARD, AttributeDataType.ENTITY, null),
  /** Agreement Milestone Responsible Contact. */
  AGREEMENT_MILESTONE_RESPONSIBLE_CONTACT("AgreementMilestoneResponsibleContact",
      AttributeType.STANDARD, AttributeDataType.ENTITY, null),
  /** Agreement Milestone Source Reference. */
  AGREEMENT_MILESTONE_SOURCE_REFERENCE("AgreementMilestoneSourceReference"),
  /** Agreement Milestone Source Text. */
  AGREEMENT_MILESTONE_SOURCE_TEXT("AgreementMilestoneSourceText"),
  /** Agreement Milestone Confidence Score. */
  AGREEMENT_MILESTONE_CONFIDENCE_SCORE("AgreementMilestoneConfidenceScore", AttributeType.STANDARD,
      AttributeDataType.NUMBER, null),
  /** Agreement Milestone Reviewed By. */
  AGREEMENT_MILESTONE_REVIEWED_BY("AgreementMilestoneReviewedBy"),
  /** Agreement Milestone Reviewed At. */
  AGREEMENT_MILESTONE_REVIEWED_AT("AgreementMilestoneReviewedAt"),
  /** Agreement Milestone Notes. */
  AGREEMENT_MILESTONE_NOTES("AgreementMilestoneNotes"),
  /** Retention Policy. */
  RETENTION_POLICY("RetentionPolicy", AttributeType.GOVERNANCE, AttributeDataType.ENTITY, null),
  /** Retention Period In Days. */
  RETENTION_PERIOD_IN_DAYS("RetentionPeriodInDays", AttributeType.STANDARD,
      AttributeDataType.NUMBER, null),
  /** Retention Disposition Date. */
  DISPOSITION_DATE("DispositionDate", AttributeType.STANDARD, AttributeDataType.STRING,
      AttributeDerivedType.STORED_DERIVED),
  /** Retention Disposition Period In Days. */
  DISPOSITION_DATE_IN_DAYS("DispositionPeriodInDays", AttributeType.STANDARD,
      AttributeDataType.NUMBER, null),
  /** Retention Start Date Source Type. */
  RETENTION_START_DATE_SOURCE_TYPE("RetentionStartDateSourceType"),
  /** Retention Effective Start Date. */
  RETENTION_EFFECTIVE_START_DATE("RetentionEffectiveStartDate", AttributeType.STANDARD,
      AttributeDataType.STRING, AttributeDerivedType.STANDARD),
  /** Retention Effective End Date. */
  RETENTION_EFFECTIVE_END_DATE("RetentionEffectiveEndDate", AttributeType.STANDARD,
      AttributeDataType.STRING, AttributeDerivedType.STANDARD),
  /** Retention Effective Status. */
  RETENTION_EFFECTIVE_STATUS("RetentionEffectiveStatus", AttributeType.STANDARD,
      AttributeDataType.STRING, AttributeDerivedType.STANDARD),
  /** LLM User Prompt. */
  LLM_USER_PROMPT("UserPrompt"),
  /** LLM System Prompt. */
  LLM_SYSTEM_PROMPT("LlmSystemPrompt"),
  /** LLM System Prompt Document Id. */
  LLM_SYSTEM_PROMPT_DOCUMENT_ID("LlmSystemPromptDocumentId"),
  /** LLM System Prompt Artifact Id. */
  LLM_SYSTEM_PROMPT_ARTIFACT_ID("LlmSystemPromptArtifactId"),
  /** LLM Response Preset Entity Types. */
  LLM_RESPONSE_PRESET_ENTITY_TYPES("LlmResponsePresetEntityTypes"),
  /** LLM Response Field Key. */
  LLM_RESPONSE_FIELD_KEYS("LlmResponseFieldKeys"),
  /** LLM Model Id. */
  LLM_MODEL_ID("LlmModelId"),
  /** LLM Max Token. */
  LLM_MAX_TOKENS("LlmMaxTokens", AttributeType.STANDARD, AttributeDataType.NUMBER, null),
  /** LLM Analysis Category. */
  LLM_ANALYSIS_CATEGORY("LlmAnalysisCategory");

  /**
   * Find {@link AttributeKeyReserved}.
   * 
   * @param key {@link AttributeKeyReserved}
   * @return {@link AttributeKeyReserved}
   */
  public static AttributeKeyReserved find(final String key) {
    Optional<AttributeKeyReserved> a =
        Arrays.stream(values()).filter(v -> v.getKey().equalsIgnoreCase(key)).findFirst();
    return a.orElse(null);
  }

  /**
   * Get Derived Attributes.
   * 
   * @return {@link List} {@link AttributeKeyReserved}
   */
  public static List<AttributeKeyReserved> getDerivedAttributes() {
    return Arrays.stream(values()).filter(a -> a.getDerivedType() != null).toList();
  }

  /** {@link AttributeType}. */
  private final AttributeType type;

  /** Is Attribute Derived. */
  private final AttributeDerivedType derivedType;

  /** Attribute Data Type. */
  private final AttributeDataType dataType;

  /** Key Name. */
  private final String key;

  AttributeKeyReserved(final String reservedKey) {
    this(reservedKey, AttributeType.STANDARD, AttributeDataType.STRING, null);
  }

  AttributeKeyReserved(final String reservedKey, final AttributeType attributeType,
      final AttributeDataType attributeDataType, final AttributeDerivedType derivedAttributeType) {
    this.key = reservedKey;
    this.dataType = attributeDataType;
    this.derivedType = derivedAttributeType;
    this.type = attributeType;
  }

  /**
   * Get Data Type.
   *
   * @return {@link AttributeDataType}
   */
  public AttributeDataType getDataType() {
    return this.dataType;
  }

  /**
   * Is Attribute Key Derived.
   * 
   * @return boolean
   */
  public AttributeDerivedType getDerivedType() {
    return derivedType;
  }

  /**
   * Get Key Name.
   * 
   * @return {@link String}
   */
  public String getKey() {
    return this.key;
  }

  /**
   * Get Attribute Type.
   * 
   * @return {@link AttributeType}
   */
  public AttributeType getType() {
    return type;
  }

  public boolean isDerived() {
    return derivedType != null;
  }
}
