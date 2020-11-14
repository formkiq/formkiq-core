/**
 *
 * FormKiQ License
 *
 * Copyright (c) 2018 FormKiQ, INC
 * 
 * This code is the property of FormKiQ, INC. In the Software Development Agreement signed by both
 * FormKiQ and your company, FormKiQ grants you a limited license to use, modify, and create
 * derivative works of this code. Please consult the Software Development Agreement for the complete
 * terms under which you may use this code.
 *
 */
package com.formkiq.stacks.dynamodb;

import static com.formkiq.stacks.dynamodb.SiteIdKeyGenerator.createDatabaseKey;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/**
 * 
 * DynamoDB Keys.
 *
 */
public interface DbKeys {

  /** Partition Key of Table. */
  String PK = "PK";

  /** Sort Key of Table. */
  String SK = "SK";

  /** Global Secondary Index 1. */
  String GSI1 = "GSI1";

  /** Global Secondary Index 1 Primary Key. */
  String GSI1_PK = GSI1 + PK;

  /** Global Secondary Index 1 Sort Key. */
  String GSI1_SK = GSI1 + SK;

  /** Global Secondary Index 2. */
  String GSI2 = "GSI2";

  /** Global Secondary Index 2 Primary Key. */
  String GSI2_PK = GSI2 + PK;

  /** Global Secondary Index 2 Sort Key. */
  String GSI2_SK = GSI2 + SK;

  /** TAGS Partition Key Prefix. */
  String PREFIX_TAGS = "tags_";

  /** FORMATS Partition Key Prefix. */
  String PREFIX_FORMATS = "formats_";

  /** Preset Partition Key Prefix. */
  String PREFIX_PRESETS = "pre";

  /** Preset Tag Partition Key Prefix. */
  String PREFIX_PRESETTAGS = "pretag";

  /** Composite Tag Key Deliminator. */
  String TAG_DELIMINATOR = "\t";

  /**
   * Add Number to {@link Map} {@link AttributeValue}.
   * 
   * @param map {@link Map} {@link AttributeValue}
   * @param key {@link String}
   * @param value {@link String}
   */
  default void addN(final Map<String, AttributeValue> map, final String key, final String value) {
    if (value != null) {
      map.put(key, AttributeValue.builder().n(value).build());
    }
  }

  /**
   * Add {@link String} to {@link Map} {@link AttributeValue}.
   * 
   * @param map {@link Map} {@link AttributeValue}
   * @param key {@link String}
   * @param value {@link String}
   */
  default void addS(final Map<String, AttributeValue> map, final String key, final String value) {
    if (value != null) {
      map.put(key, AttributeValue.builder().s(value).build());
    }
  }

  /**
   * Get Db Index.
   * 
   * @param pk {@link String}
   * @return {@link String}
   */
  default String getIndexName(String pk) {
    String index = null;

    if (pk.startsWith(GSI1)) {
      index = GSI1;
    } else if (pk.startsWith(GSI2)) {
      index = GSI2;
    }

    return index;
  }

  /**
   * Document Key {@link AttributeValue}.
   * 
   * @param siteId {@link String}
   * @param documentId {@link String}
   * @return {@link Map}
   */
  default Map<String, AttributeValue> keysDocument(String siteId, String documentId) {
    return keysDocument(siteId, documentId, Optional.empty());
  }

  /**
   * Document Key {@link AttributeValue}.
   * 
   * @param siteId {@link String}
   * @param documentId {@link String}
   * @param childdocument {@link Optional} {@link String}
   * @return {@link Map}
   */
  default Map<String, AttributeValue> keysDocument(String siteId, String documentId,
      Optional<String> childdocument) {
    return childdocument.isPresent()
        ? keysGeneric(siteId, documentId, "document_" + childdocument.get())
        : keysGeneric(siteId, documentId, "document");
  }

  /**
   * Document Formats Key {@link AttributeValue}.
   * 
   * @param siteId {@link String}
   * @param documentId {@link String}
   * @param contentType {@link String}
   * @return {@link Map}
   */
  default Map<String, AttributeValue> keysDocumentFormats(String siteId, String documentId,
      final String contentType) {
    String sk = contentType != null ? "format_" + contentType : "format_";
    return keysGeneric(siteId, PREFIX_FORMATS + documentId, sk);
  }

  /**
   * Document Tag Key {@link AttributeValue}.
   * 
   * @param siteId {@link String}
   * @param documentId {@link String}
   * @param tagKey {@link String}
   * @return {@link Map}
   */
  default Map<String, AttributeValue> keysDocumentTag(String siteId, String documentId,
      final String tagKey) {
    return keysGeneric(siteId, PREFIX_TAGS + documentId, tagKey);
  }

  /**
   * Generic Key {@link AttributeValue}.
   * 
   * @param pk {@link String}
   * @param sk {@link String}
   * @return {@link Map}
   */
  default Map<String, AttributeValue> keysGeneric(String pk, String sk) {
    Map<String, AttributeValue> key = new HashMap<String, AttributeValue>();
    key.put(PK, AttributeValue.builder().s(pk).build());

    if (sk != null) {
      key.put(SK, AttributeValue.builder().s(sk).build());
    }

    return key;
  }

  /**
   * Generic Key {@link AttributeValue}.
   * 
   * @param siteId {@link String}
   * @param pk {@link String}
   * @param sk {@link String}
   * @return {@link Map}
   */
  default Map<String, AttributeValue> keysGeneric(String siteId, String pk, String sk) {
    return keysGeneric(siteId, PK, pk, SK, sk);
  }

  /**
   * Generic Key {@link AttributeValue}.
   * 
   * @param siteId {@link String}
   * @param pkKey {@link String}
   * @param pk {@link String}
   * @param skKey {@link String}
   * @param sk {@link String}
   * @return {@link Map}
   */
  default Map<String, AttributeValue> keysGeneric(String siteId, String pkKey, String pk,
      String skKey, String sk) {
    Map<String, AttributeValue> key = new HashMap<String, AttributeValue>();
    key.put(pkKey, AttributeValue.builder().s(createDatabaseKey(siteId, pk)).build());

    if (sk != null) {
      key.put(skKey, AttributeValue.builder().s(sk).build());
    }

    return key;
  }

  /**
   * Preset Key {@link AttributeValue}.
   * 
   * @param siteId {@link String}
   * @param id {@link String}
   * @return {@link Map}
   */
  default Map<String, AttributeValue> keysPreset(String siteId, String id) {
    String sk = "preset";
    if (id == null) {
      throw new IllegalArgumentException("'id' required");
    }
    return keysGeneric(siteId, PREFIX_PRESETS + "_" + id, sk);
  }

  /**
   * Preset Key {@link AttributeValue}.
   * 
   * @param siteId {@link String}
   * @param id {@link String}
   * @param type {@link String}
   * @param name {@link String}s
   * @return {@link Map}
   */
  default Map<String, AttributeValue> keysPresetGsi2(String siteId, String id, final String type,
      final String name) {
    if (type == null) {
      throw new IllegalArgumentException("'type' required");
    }
    String pk = PREFIX_PRESETS + "_name";
    String sk = MessageFormat.format("{0}" + TAG_DELIMINATOR, type);

    if (name != null && id != null) {
      sk = MessageFormat.format("{0}" + TAG_DELIMINATOR + "{1}" + TAG_DELIMINATOR + "{2}", type,
          name, id);
    } else if (name != null) {
      sk = MessageFormat.format("{0}" + TAG_DELIMINATOR + "{1}" + TAG_DELIMINATOR, type, name);
    }

    return keysGeneric(siteId, GSI2_PK, pk, GSI2_SK, sk);
  }

  /**
   * Preset Key Tag {@link AttributeValue}.
   * 
   * @param siteId {@link String}
   * @param id {@link String}
   * @param tag {@link String}
   * @return {@link Map}
   */
  default Map<String, AttributeValue> keysPresetTag(String siteId, String id, final String tag) {
    if (id == null) {
      throw new IllegalArgumentException("'id' required");
    }
    return keysGeneric(siteId, PREFIX_PRESETTAGS + "_" + id, tag);
  }

  /**
   * Convert PK / SK values to Query {@link AttributeValue} map.
   * 
   * @param keys {@link Map}
   * @return {@link Map}
   */
  default Map<String, AttributeValue> queryKeys(final Map<String, AttributeValue> keys) {

    Map<String, AttributeValue> map = new HashMap<>(2);

    if (keys.containsKey(PK)) {
      map.put(":" + PK.toLowerCase(), keys.get(PK));
    }

    if (keys.containsKey(SK)) {
      map.put(":" + SK.toLowerCase(), keys.get(SK));
    }

    return map;
  }

}
