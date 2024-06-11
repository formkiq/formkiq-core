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

import static com.formkiq.aws.dynamodb.objects.Objects.notNull;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import com.formkiq.aws.dynamodb.objects.Objects;
import com.formkiq.stacks.dynamodb.attributes.DocumentAttributeRecord;
import com.formkiq.stacks.dynamodb.attributes.DocumentAttributeValueType;
import com.formkiq.stacks.dynamodb.schemas.Schema;
import com.formkiq.stacks.dynamodb.schemas.SchemaAttributesCompositeKey;

/**
 * {@link Function} to create {@link DocumentAttributeRecord} from a {@link Schema}.
 */
public class DocumentAttributeSchema
    implements Function<Collection<DocumentAttributeRecord>, Collection<DocumentAttributeRecord>> {

  /** Document Id. */
  private final String docId;
  /** Schema. */
  private final Schema schema;

  /**
   * constructor.
   * 
   * @param documentSchema {@link Schema}
   * @param documentId {@link String}
   */
  public DocumentAttributeSchema(final Schema documentSchema, final String documentId) {
    this.docId = documentId;
    this.schema = documentSchema;
  }

  @Override
  public Collection<DocumentAttributeRecord> apply(final Collection<DocumentAttributeRecord> c) {

    Collection<DocumentAttributeRecord> compositeKeys = Collections.emptyList();

    if (this.schema != null) {
      compositeKeys = createCompositeKeys(c);
    }

    return compositeKeys;
  }

  /**
   * Create Composite Keys from {@link Collection} {@link DocumentAttributeRecord}.
   * 
   * @param list {@link Collection} {@link DocumentAttributeRecord}
   * @return {@link Collection} {@link DocumentAttributeRecord}
   */
  private Collection<DocumentAttributeRecord> createCompositeKeys(
      final Collection<DocumentAttributeRecord> list) {

    Map<String, List<DocumentAttributeRecord>> documentAttributeKeys =
        list.stream().collect(Collectors.groupingBy(DocumentAttributeRecord::getKey));

    Collection<DocumentAttributeRecord> compositeKeys = new ArrayList<>();

    notNull(this.schema.getAttributes().getCompositeKeys()).forEach(a -> {
      Collection<DocumentAttributeRecord> keys = createCompositeKeys(a, documentAttributeKeys);
      compositeKeys.addAll(keys);
    });

    return compositeKeys;
  }

  /**
   * Create Composite Keys from Keys and Values.
   * 
   * @param compositeKeys {@link List} {@link String}
   * @param compositeValues {@link List} {@link String}
   * @return {@link List} {@link DocumentAttributeRecord}
   */
  private List<DocumentAttributeRecord> createCompositeKeys(final List<List<String>> compositeKeys,
      final List<List<String>> compositeValues) {

    List<DocumentAttributeRecord> records = new ArrayList<>();

    for (int i = 0; i < compositeKeys.size(); i++) {

      String compositeKey = String.join("#", compositeKeys.get(i));
      String stringValue = String.join("#", compositeValues.get(i));

      DocumentAttributeRecord r = new DocumentAttributeRecord();
      r.setKey(compositeKey);
      r.setDocumentId(this.docId);
      r.setValueType(DocumentAttributeValueType.COMPOSITE_STRING);
      r.setStringValue(stringValue);
      records.add(r);
    }

    return records;
  }


  /**
   * Create Composite Keys from {@link SchemaAttributesCompositeKey}.
   * 
   * @param schemaAttributes {@link SchemaAttributesCompositeKey}
   * @param documentAttributeKeys {@link Map}
   * @return {@link Collection} {@link DocumentAttributeRecord}
   */
  @SuppressWarnings("boxing")
  private Collection<DocumentAttributeRecord> createCompositeKeys(
      final SchemaAttributesCompositeKey schemaAttributes,
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

        compositeKeys.addAll(createCompositeKeys(newCompositeKeys, newCompositeValues));
      }
    }

    return compositeKeys;
  }
}
