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

import static com.formkiq.aws.dynamodb.SiteIdKeyGenerator.createDatabaseKey;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
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

  /** Composite Tag Key Deliminator. */
  String TAG_DELIMINATOR = "#";

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

  /** Config Partition Key Prefix. */
  String PREFIX_CONFIG = "configs" + TAG_DELIMINATOR;

  /** API Keys Partition Key Prefix. */
  String PREFIX_API_KEYS = "apikeys" + TAG_DELIMINATOR;

  /** API Key Partition Key Prefix. */
  String PREFIX_API_KEY = "apikey" + TAG_DELIMINATOR;

  /** Documents Partition Key Prefix. */
  String PREFIX_DOCS = "docs" + TAG_DELIMINATOR;

  /** Document Date Partition Keys Prefix. */
  String PREFIX_DOCUMENT_DATE = "docdate";

  /** Document Date Time Series Partition Keys Prefix. */
  String PREFIX_DOCUMENT_DATE_TS = "docts" + TAG_DELIMINATOR;
  /** Document Format Prefix. */
  String PREFIX_DOCUMENT_FORMAT = "format" + TAG_DELIMINATOR;
  /** FORMATS Partition Key Prefix. */
  String PREFIX_FORMATS = "formats" + TAG_DELIMINATOR;
  /** Preset Partition Key Prefix. */
  String PREFIX_PRESETS = "pre";
  /** Preset Tag Partition Key Prefix. */
  String PREFIX_PRESETTAGS = "pretag";
  /** TAGS Partition Key Prefix. */
  String PREFIX_TAG = "tag" + TAG_DELIMINATOR;
  /** TAGS Partition Keys Prefix. */
  String PREFIX_TAGS = "tags" + TAG_DELIMINATOR;
  /** Webhooks Partition Key Prefix. */
  String PREFIX_WEBHOOK = "webhook" + TAG_DELIMINATOR;
  /** Webhooks Partition Key Prefix. */
  String PREFIX_WEBHOOKS = "webhooks" + TAG_DELIMINATOR;
  /** Global Meta Data Key Prefix. */
  String GLOBAL_FOLDER_METADATA = "global" + TAG_DELIMINATOR + "folders";
  /** Global Meta Data Tags Prefix. */
  String GLOBAL_FOLDER_TAGS = "global" + TAG_DELIMINATOR + "tags" + TAG_DELIMINATOR;
  /** Metadata Prefix. */
  String PREFIX_DOCUMENT_METADATA = "md" + TAG_DELIMINATOR;

  /**
   * Add {@link String} to {@link Map} {@link AttributeValue}.
   * 
   * @param map {@link Map} {@link AttributeValue}
   * @param key {@link String}
   * @param values {@link Collection} {@link String}
   */
  default void addL(final Map<String, AttributeValue> map, final String key,
      final Collection<String> values) {
    if (values != null) {
      List<AttributeValue> list =
          values.stream().map(v -> AttributeValue.fromS(v)).collect(Collectors.toList());
      map.put(key, AttributeValue.builder().l(list).build());
    }
  }

  /**
   * Add {@link Map} to {@link Map} {@link AttributeValue}.
   * 
   * @param map {@link Map} {@link AttributeValue}
   * @param key {@link String}
   * @param value {@link Map}
   */
  default void addM(final Map<String, AttributeValue> map, final String key,
      final Map<String, String> value) {
    if (value != null) {

      Map<String, AttributeValue> attr = new HashMap<>();
      for (Map.Entry<String, String> e : value.entrySet()) {
        attr.put(e.getKey(), AttributeValue.builder().s(e.getValue()).build());
      }

      map.put(key, AttributeValue.builder().m(attr).build());
    }
  }

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
        ? keysGeneric(siteId, PREFIX_DOCS + documentId,
            "document" + TAG_DELIMINATOR + childdocument.get())
        : keysGeneric(siteId, PREFIX_DOCS + documentId, "document");
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
    String sk = contentType != null ? PREFIX_DOCUMENT_FORMAT + contentType : PREFIX_DOCUMENT_FORMAT;
    return keysGeneric(siteId, PREFIX_DOCS + documentId, sk);
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
    return keysGeneric(siteId, PREFIX_DOCS + documentId,
        tagKey != null ? PREFIX_TAGS + tagKey : PREFIX_TAGS);
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
    return keysGeneric(siteId, PREFIX_PRESETS + TAG_DELIMINATOR + id, sk);
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
    return keysGeneric(siteId, PREFIX_PRESETTAGS + TAG_DELIMINATOR + id, tag);
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
