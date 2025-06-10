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
package com.formkiq.stacks.lambda.s3;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.Map;

/**
 * Builder for constructing an S3 event notification payload as a Map using GSON.
 */
public class S3EventJsonBuilder {
  /** {@link Gson}. */
  private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
  /** {@link Type}. */
  private static final Type MAP_TYPE = new TypeToken<Map<String, Object>>() {}.getType();
  /** {@link JsonObject}. */
  private final JsonObject root;
  /** {@link JsonArray}. */
  private final JsonArray records;

  /**
   * constructor.
   */
  public S3EventJsonBuilder() {
    root = new JsonObject();
    records = new JsonArray();
    root.add("Records", records);
  }

  /**
   * Adds a record to the Records list.
   * 
   * @param recordBuilder {@link RecordBuilder}
   * 
   * @return S3EventJsonBuilder
   */
  public S3EventJsonBuilder addRecord(final RecordBuilder recordBuilder) {
    records.add(recordBuilder.build());
    return this;
  }

  /**
   * Builds the payload and returns it as a nested Map.
   * 
   * @return Map
   */
  public Map<String, Object> build() {
    return GSON.fromJson(root, MAP_TYPE);
  }

  /**
   * Builder for individual Records entries.
   */
  public static class RecordBuilder {
    /** {@link JsonObject}. */
    private final JsonObject record;

    public RecordBuilder() {
      record = new JsonObject();
    }

    public RecordBuilder withEventName(final String name) {
      record.addProperty("eventName", name);
      return this;
    }

    public RecordBuilder withS3(final S3Builder s3Builder) {
      record.add("s3", s3Builder.build());
      return this;
    }

    private JsonObject build() {
      return record;
    }
  }

  /**
   * Builder for the nested S3 object in each record.
   */
  public static class S3Builder {
    /** {@link JsonObject}. */
    private final JsonObject s3;

    public S3Builder() {
      s3 = new JsonObject();
    }

    public S3Builder withBucket(final String name) {
      JsonObject bucket = new JsonObject();
      bucket.addProperty("name", name);
      s3.add("bucket", bucket);
      return this;
    }

    public S3Builder withObject(final String key) {
      JsonObject objectNode = new JsonObject();
      objectNode.addProperty("key", key);
      s3.add("object", objectNode);
      return this;
    }

    private JsonObject build() {
      return s3;
    }
  }
}
