package com.formkiq.stacks.api.handler;

import static com.formkiq.aws.dynamodb.objects.Strings.isEmpty;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import com.formkiq.aws.dynamodb.DynamodbRecordToMap;
import com.formkiq.stacks.dynamodb.attributes.DocumentAttributeRecord;

/**
 * {@link Function} to merge {@link DocumentAttributeRecord} with the same keys together.
 */
public class DocumentAttributeRecordToMap
    implements Function<Collection<DocumentAttributeRecord>, Collection<Map<String, Object>>> {

  @Override
  public Collection<Map<String, Object>> apply(
      final Collection<DocumentAttributeRecord> attributes) {

    DocumentAttributeRecord last = null;
    Map<String, Object> lastValues = null;
    Collection<Map<String, Object>> c = new ArrayList<>();

    for (DocumentAttributeRecord a : attributes) {

      if (lastValues != null && last != null && last.getKey().equals(a.getKey())) {

        if (!isEmpty(a.getStringValue())) {

          addStringValues(lastValues, a);

        } else if (a.getNumberValue() != null) {

          addNumberValues(lastValues, a);
        }

      } else {

        lastValues = new DynamodbRecordToMap().apply(a);
        lastValues.remove("documentId");
        lastValues.remove("valueType");

        c.add(lastValues);

        last = a;
      }
    }
    
    return c;
  }

  @SuppressWarnings("unchecked")
  private void addNumberValues(final Map<String, Object> lastValues,
      final DocumentAttributeRecord a) {
    if (lastValues.containsKey("numberValue")) {

      List<Double> s = new ArrayList<>();
      s.add((Double) lastValues.get("numberValue"));
      s.add(a.getNumberValue());

      lastValues.remove("numberValue");
      lastValues.put("numberValues", s);

    } else if (lastValues.containsKey("numberValues")) {
      ((List<Double>) lastValues.get("numberValues")).add(a.getNumberValue());
    }
  }

  @SuppressWarnings("unchecked")
  private void addStringValues(final Map<String, Object> lastValues,
      final DocumentAttributeRecord a) {

    if (lastValues.containsKey("stringValue")) {

      List<String> s = new ArrayList<>();
      s.add((String) lastValues.get("stringValue"));
      s.add(a.getStringValue());

      lastValues.remove("stringValue");
      lastValues.put("stringValues", s);

    } else if (lastValues.containsKey("stringValues")) {
      ((List<String>) lastValues.get("stringValues")).add(a.getStringValue());
    }
  }

}
