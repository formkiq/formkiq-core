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
package com.formkiq.stacks.lambda.s3.actions;

import com.formkiq.aws.dynamodb.model.DocumentItem;
import com.formkiq.aws.dynamodb.model.MappingRecord;
import com.formkiq.module.actions.Action;
import com.formkiq.module.actions.ActionStatus;
import com.formkiq.module.lambdaservices.AwsServiceCache;
import com.formkiq.module.lambdaservices.logger.Logger;
import com.formkiq.stacks.dynamodb.DocumentService;
import com.formkiq.stacks.dynamodb.GsonUtil;
import com.formkiq.stacks.dynamodb.attributes.AttributeDataType;
import com.formkiq.stacks.dynamodb.attributes.AttributeRecord;
import com.formkiq.stacks.dynamodb.attributes.AttributeService;
import com.formkiq.stacks.dynamodb.attributes.AttributeValidationAccess;
import com.formkiq.stacks.dynamodb.attributes.AttributeValidationType;
import com.formkiq.stacks.dynamodb.attributes.DocumentAttributeRecord;
import com.formkiq.stacks.dynamodb.attributes.DocumentAttributeValueType;
import com.formkiq.stacks.dynamodb.mappings.MappingAttribute;
import com.formkiq.stacks.dynamodb.mappings.MappingAttributeLabelMatchingType;
import com.formkiq.stacks.dynamodb.mappings.MappingAttributeMetadataField;
import com.formkiq.stacks.dynamodb.mappings.MappingAttributeSourceType;
import com.formkiq.stacks.dynamodb.mappings.MappingService;
import com.formkiq.stacks.lambda.s3.DocumentAction;
import com.formkiq.stacks.lambda.s3.DocumentContentFunction;
import com.formkiq.stacks.lambda.s3.ProcessActionStatus;
import com.formkiq.stacks.lambda.s3.text.BeginsWithMatcher;
import com.formkiq.stacks.lambda.s3.text.ContainsMatcher;
import com.formkiq.stacks.lambda.s3.text.ExactMatcher;
import com.formkiq.stacks.lambda.s3.text.FuzzyMatcher;
import com.formkiq.stacks.lambda.s3.text.IdpTextMatcher;
import com.formkiq.stacks.lambda.s3.text.TextMatch;
import com.formkiq.stacks.lambda.s3.text.TextMatchAlgorithm;
import com.formkiq.stacks.lambda.s3.text.TokenGeneratorRegex;
import com.formkiq.stacks.lambda.s3.text.TokenGeneratorKeyValue;
import com.formkiq.strings.StringFormatter;
import com.formkiq.strings.StringFormatterAlphaNumeric;
import com.formkiq.validation.ValidationException;
import com.google.gson.Gson;

import java.io.IOException;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.formkiq.aws.dynamodb.objects.Objects.notNull;
import static com.formkiq.aws.dynamodb.objects.Strings.isEmpty;

/**
 * Intelligent Document Processor implementation of {@link DocumentAction}.
 */
public class IdpAction implements DocumentAction {

  /** IDP Regex. */
  public static final String REGEX = "\\s+";
  /** Idp Action {@link StringFormatter}. */
  public static final StringFormatter FORMATTER =
      text -> new StringFormatterAlphaNumeric().format(text.toLowerCase());
  /** {@link MappingService}. */
  private final MappingService mappingService;
  /** {@link DocumentContentFunction}. */
  private final DocumentContentFunction documentContentFunc;
  /** {@link DocumentService}. */
  private final DocumentService documentService;
  /** {@link AttributeService}. */
  private final AttributeService attributeService;
  /** {@link IdpTextMatcher}. */
  private final IdpTextMatcher matcher = new IdpTextMatcher();
  /** {@link SendHttpRequest}. */
  private final SendHttpRequest http;
  /** {@link Gson}. */
  private final Gson gson = GsonUtil.getInstance();

  /**
   * constructor.
   * 
   * @param serviceCache {@link AwsServiceCache}
   */
  public IdpAction(final AwsServiceCache serviceCache) {
    this.mappingService = serviceCache.getExtension(MappingService.class);
    this.attributeService = serviceCache.getExtension(AttributeService.class);
    this.documentService = serviceCache.getExtension(DocumentService.class);
    this.documentContentFunc = new DocumentContentFunction(serviceCache);
    this.http = new SendHttpRequest(serviceCache);
  }


  @Override
  public ProcessActionStatus run(final Logger logger, final String siteId, final String documentId,
      final List<Action> actions, final Action action) throws IOException, ValidationException {

    String mappingId = (String) action.parameters().get("mappingId");

    MappingRecord mapping = getMapping(siteId, mappingId);

    List<MappingAttribute> mappingAttributes = this.mappingService.getAttributes(mapping);
    Map<String, String> dataClassificationMap =
        createDataClassificationMap(siteId, documentId, mappingAttributes);

    Collection<DocumentAttributeRecord> records = new ArrayList<>();

    for (MappingAttribute mappingAttribute : mappingAttributes) {

      MappingAttributeSourceType sourceType = mappingAttribute.getSourceType();

      switch (sourceType) {
        case CONTENT ->
          records.addAll(processContent(logger, siteId, documentId, mappingAttribute));
        case CONTENT_KEY_VALUE ->
          records.addAll(processContentKeyValue(logger, siteId, documentId, mappingAttribute));
        case METADATA -> records.addAll(processMetaData(siteId, documentId, mappingAttribute));
        case MANUAL -> records.addAll(createDocumentAttribute(siteId, documentId, mappingAttribute,
            createValues(mappingAttribute)));
        case DATA_CLASSIFICATION -> {
          String value = dataClassificationMap.get(mappingAttribute.getAttributeKey());
          if (!isEmpty(value)) {
            records.addAll(
                createDocumentAttribute(siteId, documentId, mappingAttribute, List.of(value)));
          }
        }
        default -> throw new IllegalArgumentException("Unsupported source type: " + sourceType);
      }
    }

    if (!records.isEmpty()) {
      this.documentService.saveDocumentAttributes(siteId, documentId, records,
          AttributeValidationType.FULL, AttributeValidationAccess.ADMIN_UPDATE);
    }

    return new ProcessActionStatus(ActionStatus.COMPLETE);
  }

  private Map<String, String> createDataClassificationMap(final String siteId,
      final String documentId, final List<MappingAttribute> mappingAttributes) throws IOException {
    boolean hasDataClassification = mappingAttributes.stream()
        .anyMatch(a -> MappingAttributeSourceType.DATA_CLASSIFICATION.equals(a.getSourceType()));

    return hasDataClassification ? createDataClassificationAttributes(siteId, documentId)
        : Collections.emptyMap();
  }

  private Map<String, String> createDataClassificationAttributes(final String siteId,
      final String documentId) throws IOException {
    HttpResponse<String> response = this.http.sendRequest(siteId, "get",
        "/documents/" + documentId + "/dataClassification?limit=100", "");
    String body = response.body();
    DataClassificationsResponse data = gson.fromJson(body, DataClassificationsResponse.class);
    List<DataClassification> dataClassifications = notNull(data.dataClassifications());

    if (dataClassifications.isEmpty()) {
      throw new IllegalArgumentException("No Data Classifications found");
    }

    Map<String, String> attributeMap = new HashMap<>();

    for (int i = dataClassifications.size() - 1; i >= 0; i--) {
      DataClassification dataClassification = dataClassifications.get(i);
      notNull(dataClassification.attributes()).forEach(attribute -> {
        attributeMap.put(attribute.key(), attribute.value());
      });
    }

    return attributeMap;
  }

  private List<String> createValues(final MappingAttribute mappingAttribute) {
    List<String> values = new ArrayList<>();

    if (!isEmpty(mappingAttribute.getDefaultValue())) {
      values.add(mappingAttribute.getDefaultValue());
    }

    values.addAll(notNull(mappingAttribute.getDefaultValues()));

    return values;
  }

  private List<DocumentAttributeRecord> processMetaData(final String siteId,
      final String documentId, final MappingAttribute mappingAttribute) throws ValidationException {
    String text = getMetadataText(mappingAttribute, siteId, documentId);

    TextMatchAlgorithm alg = getTextMatchAlgorithm(mappingAttribute);
    List<String> matchValues = findMappingAttributeValue(mappingAttribute, alg, text);
    return createDocumentAttribute(siteId, documentId, mappingAttribute, matchValues);
  }

  private List<DocumentAttributeRecord> processContentKeyValue(final Logger logger,
      final String siteId, final String documentId, final MappingAttribute mappingAttribute)
      throws IOException, ValidationException {

    DocumentItem item = this.documentService.findDocument(siteId, documentId);

    List<Map<String, Object>> contentKeyValues =
        this.documentContentFunc.findContentKeyValues(logger, siteId, item);

    List<String> labelTexts = mappingAttribute.getLabelTexts();
    TextMatchAlgorithm alg = getTextMatchAlgorithm(mappingAttribute);
    TextMatch match = matcher.findMatch(null, labelTexts,
        new TokenGeneratorKeyValue(contentKeyValues), alg, null, text -> text);

    List<DocumentAttributeRecord> records = Collections.emptyList();

    if (match != null) {
      Optional<List<String>> o = contentKeyValues.stream()
          .filter(m -> m.get("key").toString().contains(match.getToken().getOriginal()))
          .map(v -> (List<String>) v.get("values")).findFirst();

      if (o.isPresent()) {
        records = createDocumentAttribute(siteId, documentId, mappingAttribute, o.get());
      }
    }

    return records;
  }

  private List<DocumentAttributeRecord> processContent(final Logger logger, final String siteId,
      final String documentId, final MappingAttribute mappingAttribute)
      throws IOException, ValidationException {
    String text = getDocumentContent(logger, siteId, documentId);

    TextMatchAlgorithm alg = getTextMatchAlgorithm(mappingAttribute);
    List<String> matchValues = findMappingAttributeValue(mappingAttribute, alg, text);
    return createDocumentAttribute(siteId, documentId, mappingAttribute, matchValues);
  }

  private String getMetadataText(final MappingAttribute mappingAttribute, final String siteId,
      final String documentId) {
    String text;

    DocumentItem document = this.documentService.findDocument(siteId, documentId);

    MappingAttributeMetadataField metadataField = mappingAttribute.getMetadataField();
    switch (metadataField) {
      case PATH -> text = document.getPath();
      case USERNAME -> text = document.getUserId();
      case CONTENT_TYPE -> text = document.getContentType();
      default -> throw new IllegalArgumentException("Unsupported metadata field: " + metadataField);
    }

    return text;
  }

  private List<DocumentAttributeRecord> createDocumentAttribute(final String siteId,
      final String documentId, final MappingAttribute mappingAttribute,
      final List<String> matchValues) throws ValidationException {

    String attributeKey = mappingAttribute.getAttributeKey();
    AttributeRecord attribute = this.attributeService.getAttribute(siteId, attributeKey);

    List<DocumentAttributeRecord> records = notNull(matchValues).stream()
        .map(val -> createDocumentAttribute(documentId, attribute, attributeKey, val)).toList();

    if (records.isEmpty() && AttributeDataType.KEY_ONLY.equals(attribute.getDataType())) {
      records = List.of(createDocumentAttribute(documentId, attribute, attributeKey, null));
    }

    return records;
  }

  private DocumentAttributeRecord createDocumentAttribute(final String documentId,
      final AttributeRecord attribute, final String attributeKey, final String matchValue) {

    DocumentAttributeRecord r = new DocumentAttributeRecord().setDocumentId(documentId)
        .setKey(attributeKey).setUserId("System");

    switch (attribute.getDataType()) {
      case STRING -> r.setValueType(DocumentAttributeValueType.STRING).setStringValue(matchValue);
      case NUMBER -> r.setValueType(DocumentAttributeValueType.NUMBER)
          .setNumberValue(Double.valueOf(matchValue));
      case BOOLEAN -> r.setValueType(DocumentAttributeValueType.BOOLEAN)
          .setBooleanValue(Boolean.valueOf(matchValue));
      case KEY_ONLY -> r.setValueType(DocumentAttributeValueType.KEY_ONLY);
      default ->
        throw new IllegalArgumentException("Unsupported data type: " + attribute.getDataType());
    }
    return r;
  }

  private List<String> findMappingAttributeValue(final MappingAttribute mappingAttribute,
      final TextMatchAlgorithm alg, final String text) {

    List<String> labelTexts = mappingAttribute.getLabelTexts();

    TextMatch match = matcher.findMatch(text, labelTexts, new TokenGeneratorRegex(REGEX, FORMATTER),
        alg, REGEX, FORMATTER);

    String value;

    String validationRegex = mappingAttribute.getValidationRegex();
    MappingAttributeSourceType sourceType = mappingAttribute.getSourceType();

    switch (sourceType) {
      case CONTENT -> value = matcher.findMatchValue(text, match, validationRegex);
      case METADATA -> value = match != null ? match.getToken().getOriginal() : null;
      default -> throw new IllegalArgumentException("Unsupported source type: " + sourceType);
    }

    List<String> values = Collections.emptyList();

    if (!isEmpty(value)) {
      values = List.of(value);
    } else if (!notNull(mappingAttribute.getDefaultValues()).isEmpty()) {
      values = mappingAttribute.getDefaultValues();
    } else if (!isEmpty(mappingAttribute.getDefaultValue())) {
      value = mappingAttribute.getDefaultValue();
      values = List.of(value);
    }

    return values;
  }

  private TextMatchAlgorithm getTextMatchAlgorithm(final MappingAttribute mappingAttribute) {

    TextMatchAlgorithm textMatchAlgorithm;
    MappingAttributeLabelMatchingType labelMatchingType = mappingAttribute.getLabelMatchingType();

    switch (labelMatchingType) {
      case EXACT -> textMatchAlgorithm = new ExactMatcher();
      case FUZZY -> textMatchAlgorithm = new FuzzyMatcher();
      case CONTAINS -> textMatchAlgorithm = new ContainsMatcher();
      case BEGINS_WITH -> textMatchAlgorithm = new BeginsWithMatcher();
      default ->
        throw new IllegalArgumentException("Unsupported label matching type: " + labelMatchingType);
    }

    return textMatchAlgorithm;
  }

  private String getDocumentContent(final Logger logger, final String siteId,
      final String documentId) throws IOException {

    DocumentItem item = this.documentService.findDocument(siteId, documentId);

    List<String> contentUrls = this.documentContentFunc.getContentUrls(logger, siteId, item);
    StringBuilder sb = this.documentContentFunc.getContentUrls(contentUrls);
    return sb.toString();
  }

  private MappingRecord getMapping(final String siteId, final String mappingId) throws IOException {
    MappingRecord mapping = this.mappingService.getMapping(siteId, mappingId);
    if (mapping == null) {
      throw new IOException("Mapping '" + mappingId + "' not found");
    }

    return mapping;
  }
}
