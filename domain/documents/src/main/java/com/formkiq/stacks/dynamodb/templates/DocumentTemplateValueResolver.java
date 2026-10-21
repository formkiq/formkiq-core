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
package com.formkiq.stacks.dynamodb.templates;

import com.formkiq.aws.dynamodb.ApiAuthorization;
import com.formkiq.aws.dynamodb.DynamoDbService;
import com.formkiq.aws.dynamodb.QueryResult;
import com.formkiq.aws.dynamodb.documentattributes.DocumentAttributeRecord;
import com.formkiq.aws.dynamodb.documentattributes.QueryDocumentAttributesByKey;
import com.formkiq.aws.dynamodb.documents.DocumentArtifact;
import com.formkiq.aws.dynamodb.documents.DocumentMetadata;
import com.formkiq.aws.dynamodb.documents.DocumentRecord;
import com.formkiq.aws.dynamodb.documents.FindDocumentById;
import com.formkiq.strings.ObjectPath;
import com.formkiq.strings.TemplateSubstitutor;
import com.formkiq.strings.TemplateValueResolver;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

/**
 * Resolves document template paths for watermarks, notifications, and other document features.
 * Supported namespaces are document, attributes, metadata, and auth. Every attribute is a list; for
 * example {@code attributes.Contract[0].items[2].description}. Custom user claims come from the
 * supplied authorization, such as {@code auth.claims["custom:department"]}.
 *
 * <p>
 * Current document fields and metadata are loaded on demand with {@link FindDocumentById}.
 * Attributes use {@link QueryDocumentAttributesByKey}, following all pages. The document and each
 * referenced attribute key are cached, including missing results. Attribute values are ordered by
 * their DynamoDB sort keys; JSON arrays retain their order. Database errors propagate unchanged and
 * failed lookups are not cached. Create one resolver per operation; instances are not thread-safe
 * and must not be shared across users.
 *
 * <p>
 * {@link #resolveValue(String)} returns typed values. {@link #resolve(String)} formats scalars,
 * including ISO 8601 dates, and rejects whole maps/lists unless a custom formatter is provided.
 * Missing values return null so {@link TemplateSubstitutor} preserves their placeholders. The
 * resolver does not define marker syntax, aliases, or a global missing-value policy.
 *
 * <pre>{@code
 * var resolver = new DocumentTemplateValueResolver(database, siteId, document, authorization);
 * String text = new TemplateSubstitutor("${", "}").substituteWithResolver(template, resolver);
 * }</pre>
 */
public final class DocumentTemplateValueResolver
    implements TemplateValueResolver<RuntimeException> {

  /** Number of attribute records requested per database page. */
  private static final int PAGE_SIZE = 100;

  /**
   * Format a scalar using locale-independent text. Dates use ISO 8601; structured values require a
   * custom formatter or a path that selects a scalar.
   *
   * @param value the selected value
   * @return scalar text, or null for a missing value
   * @throws IllegalArgumentException if the selected value is not a supported scalar
   */
  public static String formatScalar(final Object value) {
    if (value instanceof Date date) {
      return date.toInstant().toString();
    }
    if (value instanceof Map<?, ?> || value instanceof List<?>) {
      throw new IllegalArgumentException("Template expression must select a scalar value");
    }
    return Objects.toString(value, null);
  }

  /** Document database. */
  private final DynamoDbService database;
  /** Site for all document and attribute lookups. */
  private final String siteId;
  /** Exact document or artifact being resolved. */
  private final DocumentArtifact document;
  /** Requesting username and custom user claims. */
  private final Map<String, Object> auth;
  /** Final value rendering. */
  private final Function<Object, String> formatter;
  /** Parsed paths reused within this operation. */
  private final Map<String, ObjectPath> paths = new HashMap<>();
  /** Loaded attribute values, including empty lists for missing keys. */
  private final Map<String, List<Object>> attributes = new HashMap<>();
  /** Loaded document fields. */
  private Map<String, Object> documentFields = Map.of();
  /** Loaded custom metadata. */
  private Map<String, Object> metadata = Map.of();

  /** Whether the document lookup completed, including a missing result. */
  private boolean documentLoaded;

  /**
   * Creates a resolver for an explicit site, which may differ from the authorization's default
   * site. The caller is responsible for authorizing access to that site and document.
   *
   * @param db document database
   * @param documentSiteId the document site, or null for the default site
   * @param documentArtifact the document or artifact being resolved
   * @param authorization the requesting user's authorization
   */
  public DocumentTemplateValueResolver(final DynamoDbService db, final String documentSiteId,
      final DocumentArtifact documentArtifact, final ApiAuthorization authorization) {
    this(db, documentSiteId, documentArtifact, authorization,
        DocumentTemplateValueResolver::formatScalar);
  }

  /**
   * Creates a resolver with caller-defined final rendering, such as joining lists or serializing
   * objects to JSON. The formatter is invoked only for non-null selected values.
   *
   * @param db document database
   * @param documentSiteId the document site, or null for the default site
   * @param documentArtifact the document or artifact being resolved
   * @param authorization the requesting user's authorization
   * @param valueFormatter final rendering policy
   */
  public DocumentTemplateValueResolver(final DynamoDbService db, final String documentSiteId,
      final DocumentArtifact documentArtifact, final ApiAuthorization authorization,
      final Function<Object, String> valueFormatter) {

    this.database = Objects.requireNonNull(db, "db");
    this.siteId = documentSiteId;
    this.document = Objects.requireNonNull(documentArtifact, "documentArtifact");
    this.formatter = Objects.requireNonNull(valueFormatter, "valueFormatter");
    Objects.requireNonNull(authorization, "authorization");

    Map<String, Object> values = new HashMap<>();
    values.put("username", authorization.getUsername());
    values.put("claims", authorization.getUserClaims());
    this.auth = values;
  }

  private Object attributeValue(final DocumentAttributeRecord record) {
    return switch (record.getValueType()) {
      case STRING, COMPOSITE_STRING, CLASSIFICATION, RELATIONSHIPS, ENTITY ->
        record.getStringValue();
      case BOOLEAN -> record.getBooleanValue();
      case NUMBER -> record.getNumberValue();
      case DATE -> record.getDateValueAsString();
      case JSON -> record.getJsonValue();
      case KEY_ONLY, PUBLICATION, WATERMARK -> null;
    };
  }

  private List<Object> attributeValues(final String key) {

    if (!this.attributes.containsKey(key)) {
      List<DocumentAttributeRecord> records = loadAttributes(key);
      List<Object> values = records.stream().map(this::attributeValue).toList();
      this.attributes.put(key, values);
    }

    return this.attributes.get(key);
  }

  private Map<String, Object> fields(final DocumentRecord record) {
    Map<String, Object> values = new HashMap<>();
    values.put("documentId", record.documentId());
    values.put("artifactId", record.artifactId());
    values.put("artifactCategory", record.artifactCategory());
    values.put("belongsToDocumentId", record.belongsToDocumentId());
    values.put("path", record.path());
    values.put("deepLinkPath", record.deepLinkPath());
    values.put("resourceType", record.resourceType() != null ? record.resourceType().name() : null);
    values.put("contentType", record.contentType());
    values.put("contentLength", record.contentLength());
    values.put("checksum", record.checksum());
    values.put("checksumType", record.checksumType());
    values.put("s3version", record.s3version());
    values.put("userId", record.userId());
    values.put("version", record.version());
    values.put("width", record.width());
    values.put("height", record.height());
    values.put("timeToLive", record.timeToLive());
    values.put("insertedDate", record.insertedDate());
    values.put("lastModifiedDate", record.lastModifiedDate());
    values.put("hasArtifacts", record.hasArtifacts());
    values.put("promotedArtifactId", record.promotedArtifactId());
    values.put("deletedDate", record.deletedDate());
    return values;
  }

  private List<DocumentAttributeRecord> loadAttributes(final String key) {
    QueryDocumentAttributesByKey query = new QueryDocumentAttributesByKey(this.document, key);
    QueryResult result = query.query(this.database, this.siteId, null, PAGE_SIZE);
    return result.items().stream()
        .map(item -> new DocumentAttributeRecord().getFromAttributes(this.siteId, item)).toList();
  }

  private void loadDocument() {
    if (!this.documentLoaded) {
      DocumentRecord record =
          new FindDocumentById().find(this.database, this.siteId, this.document);

      if (record != null) {
        Map<String, Object> values = fields(record);
        Map<String, Object> customMetadata = metadataValues(record);
        this.documentFields = values;
        this.metadata = customMetadata;
      }
      this.documentLoaded = true;
    }
  }

  private Map<String, Object> metadataValues(final DocumentRecord record) {
    Map<String, Object> values = new HashMap<>();
    if (record.metadata() != null) {
      for (DocumentMetadata entry : record.metadata()) {
        values.put(entry.key(), entry.value() != null ? entry.value() : entry.values());
      }
    }
    return values;
  }

  private Object namespace(final List<Object> segments) {
    String name = (String) segments.getFirst();
    return switch (name) {
      case "document", "metadata" -> {
        loadDocument();
        yield "document".equals(name) ? this.documentFields : this.metadata;
      }
      case "attributes" -> {
        if (segments.size() < 2) {
          throw new IllegalArgumentException("Attribute template paths require an attribute key");
        }
        yield segments.get(1) instanceof String key
            ? Collections.singletonMap(key, attributeValues(key))
            : null;
      }
      case "auth" -> this.auth;
      default -> null;
    };
  }

  @Override
  public String resolve(final String expression) {
    Object value = resolveValue(expression);
    return value != null ? this.formatter.apply(value) : null;
  }

  /**
   * Resolve a path while retaining its native scalar, map, or list type. This does not apply the
   * final formatter. Maps, lists, and dates are returned directly. The attributes namespace
   * requires a literal attribute key before indexing.
   *
   * @param expression the rooted or relative path, with surrounding whitespace ignored
   * @return the selected value, or null when missing or incompatible with the path
   * @throws IllegalArgumentException if the path is malformed or lacks an attribute key
   */
  public Object resolveValue(final String expression) {
    String source = expression != null ? expression.trim() : null;
    ObjectPath path = this.paths.computeIfAbsent(source, ObjectPath::parse);
    List<Object> segments = path.segments();
    if (segments.isEmpty() || !(segments.getFirst() instanceof String name)) {
      return null;
    }
    return path.resolve(Collections.singletonMap(name, namespace(segments)));
  }

}
