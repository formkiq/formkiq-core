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
package com.formkiq.stacks.dynamodb;

import static com.formkiq.aws.dynamodb.DbKeys.COMPOSITE_KEY_DELIM;
import static com.formkiq.aws.dynamodb.objects.Objects.notNull;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.formkiq.aws.dynamodb.ApiAuthorization;
import com.formkiq.aws.dynamodb.objects.Objects;
import com.formkiq.aws.dynamodb.documentattributes.DocumentAttributeRecord;
import com.formkiq.aws.dynamodb.documentattributes.DocumentAttributeValueType;
import com.formkiq.stacks.dynamodb.schemas.Schema;
import com.formkiq.stacks.dynamodb.schemas.SchemaAttributes;
import com.formkiq.stacks.dynamodb.schemas.SchemaAttributesCompositeKey;

/**
 * {@link Function} to create {@link DocumentAttributeRecord} from a {@link Schema}.
 */
public class SchemaCompositeKeyGenerator {

  /** {@link Date}. */
  private final Date now = new Date();

  /**
   * constructor.
   */
  public SchemaCompositeKeyGenerator() {}

  /**
   * Generate a list of {@link DocumentAttributeRecord} from a {@link SchemaAttributes} and
   * CompositeKeys.
   * 
   * @param schemaAttributes {@link Collection} {@link SchemaAttributes}
   * @param documentId {@link String}
   * @param allDocumentAttributeRecords {@link Collection} {@link DocumentAttributeRecord}
   * @return {@link Collection} {@link DocumentAttributeRecord}
   */
  public Collection<DocumentAttributeRecord> apply(
      final Collection<SchemaAttributes> schemaAttributes, final String documentId,
      final Collection<DocumentAttributeRecord> allDocumentAttributeRecords) {

    return notNull(schemaAttributes).stream()
        .flatMap(s -> createCompositeKeys(s, documentId, allDocumentAttributeRecords).stream())
        .toList();
  }

  /**
   * Create Composite Keys from {@link Collection} {@link DocumentAttributeRecord}.
   *
   * @param schemaAttributes {@link SchemaAttributes}
   * @param documentId {@link String}
   * @param allDocumentAttributes {@link Collection} {@link DocumentAttributeRecord}
   * @return {@link Collection} {@link DocumentAttributeRecord}
   */
  private Collection<DocumentAttributeRecord> createCompositeKeys(
      final SchemaAttributes schemaAttributes, final String documentId,
      final Collection<DocumentAttributeRecord> allDocumentAttributes) {

    Map<String, List<DocumentAttributeRecord>> documentAttributeKeys = allDocumentAttributes
        .stream().filter(a -> !DocumentAttributeValueType.CLASSIFICATION.equals(a.getValueType()))
        .collect(Collectors.groupingBy(DocumentAttributeRecord::getKey));

    Collection<DocumentAttributeRecord> compositeKeys = new ArrayList<>();

    notNull(schemaAttributes.getCompositeKeys()).forEach(a -> {

      Collection<DocumentAttributeRecord> keys =
          createCompositeKeys(a, documentId, documentAttributeKeys);
      compositeKeys.addAll(keys);
    });

    return compositeKeys;
  }

  /**
   * Create Composite Keys from Keys and Values.
   *
   * @param documentId {@link String}
   * @param compositeKeys {@link List} {@link String}
   * @param compositeValues {@link List} {@link String}
   * @return {@link List} {@link DocumentAttributeRecord}
   */
  private List<DocumentAttributeRecord> createCompositeKeys(final String documentId,
      final List<List<String>> compositeKeys, final List<List<String>> compositeValues) {

    String username = getUsername();
    List<DocumentAttributeRecord> records = new ArrayList<>();

    for (int i = 0; i < compositeKeys.size(); i++) {

      String compositeKey = String.join(COMPOSITE_KEY_DELIM, compositeKeys.get(i));
      String stringValue = String.join(COMPOSITE_KEY_DELIM, compositeValues.get(i));

      DocumentAttributeRecord r = new DocumentAttributeRecord();
      r.setKey(compositeKey);
      r.setDocumentId(documentId);
      r.setValueType(DocumentAttributeValueType.COMPOSITE_STRING);
      r.setStringValue(stringValue);
      r.setInsertedDate(this.now);
      r.setUserId(username);
      records.add(r);
    }

    return records;
  }

  /**
   * Create Composite Keys from {@link SchemaAttributesCompositeKey}.
   *
   * @param schemaAttributes {@link SchemaAttributesCompositeKey}
   * @param documentId {@link String}
   * @param documentAttributeKeys {@link Map}
   * @return {@link Collection} {@link DocumentAttributeRecord}
   */
  @SuppressWarnings("boxing")
  private Collection<DocumentAttributeRecord> createCompositeKeys(
      final SchemaAttributesCompositeKey schemaAttributes, final String documentId,
      final Map<String, List<DocumentAttributeRecord>> documentAttributeKeys) {

    Collection<DocumentAttributeRecord> compositeKeys = new ArrayList<>();

    List<List<DocumentAttributeRecord>> compositeRecords =
        schemaAttributes.getAttributeKeys().stream().filter(documentAttributeKeys::containsKey)
            .map(documentAttributeKeys::get).toList();

    if (schemaAttributes.getAttributeKeys().size() == compositeRecords.size()) {

      int listSize = compositeRecords.stream().map(List::size).reduce(1, (aa, bb) -> aa * bb);

      if (listSize > 0) {

        List<List<String>> newCompositeKeys = new ArrayList<>();
        List<List<String>> newCompositeValues = new ArrayList<>();

        for (int i = 0; i < listSize; i++) {
          newCompositeKeys.add(new ArrayList<>());
          newCompositeValues.add(new ArrayList<>());
        }

        for (List<DocumentAttributeRecord> cr : compositeRecords) {

          int index = 0;
          int loop = listSize / cr.size();

          for (DocumentAttributeRecord c : cr) {
            for (int i = 0; i < loop; i++) {

              newCompositeKeys.get(index).add(c.getKey());

              switch (c.getValueType()) {
                case STRING:
                  newCompositeValues.get(index).add(c.getStringValue());
                  break;
                case NUMBER:
                  String s = Objects.formatDouble(c.getNumberValue(), Objects.DOUBLE_FORMAT);
                  newCompositeValues.get(index).add(s);
                  break;
                default:
                  throw new IllegalArgumentException("Unexpected value: " + c.getValueType());
              }

              index++;
            }
          }
        }

        compositeKeys.addAll(createCompositeKeys(documentId, newCompositeKeys, newCompositeValues));
      }
    }

    return compositeKeys;
  }

  private String getUsername() {
    return ApiAuthorization.getAuthorization().getUsername();
  }
}
