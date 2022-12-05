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

import static com.formkiq.aws.dynamodb.DbKeys.GLOBAL_FOLDER_TAGS;
import static com.formkiq.aws.dynamodb.DbKeys.PK;
import static com.formkiq.aws.dynamodb.DbKeys.SK;
import static com.formkiq.aws.dynamodb.DbKeys.TAG_DELIMINATOR;
import static com.formkiq.aws.dynamodb.SiteIdKeyGenerator.createDatabaseKey;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.stream.Collectors;
import com.formkiq.aws.dynamodb.ReadRequestBuilder;
import com.formkiq.aws.dynamodb.WriteRequestBuilder;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/**
 * 
 * Global Index Writer.
 *
 */
public class GlobalIndexWriter {

  /** Cache Size. */
  private static final int CACHE_SIZE = 500;
  /** Cache Queue. */
  private Map<String, ArrayBlockingQueue<String>> tagCache = new HashMap<>();// new

  /**
   * Add Keys {@link String} to {@link ArrayBlockingQueue}.
   * 
   * @param cache {@link ArrayBlockingQueue} {@link String}
   * @param keys {@link Collection} {@link String}
   */
  private void addToCache(final ArrayBlockingQueue<String> cache, final Collection<String> keys) {
    for (String key : keys) {

      if (cache.remainingCapacity() == 0) {
        cache.remove();
      }

      cache.add(key);
    }
  }

  private ArrayBlockingQueue<String> getCache(final String siteId) {
    ArrayBlockingQueue<String> cache = this.tagCache.get(siteId);
    if (cache == null) {
      cache = new ArrayBlockingQueue<String>(CACHE_SIZE);
      this.tagCache.put(siteId, cache);
    }
    return cache;
  }

  private String getCacheKey(final Map<String, AttributeValue> r) {
    return r.get(PK).s() + "_" + r.get(SK).s();
  }

  /**
   * Write Tag Index.
   * 
   * @param documentTableName {@link String}
   * @param dbClient {@link DynamoDbClient}
   * @param siteId {@link String}
   * @param tagKeys {@link Collection} {@link String}
   */
  public void writeTagIndex(final String documentTableName, final DynamoDbClient dbClient,
      final String siteId, final Collection<String> tagKeys) {

    ArrayBlockingQueue<String> cache = getCache(siteId);

    tagKeys.removeIf(e -> cache.contains(e));
    addToCache(cache, tagKeys);

    List<Map<String, AttributeValue>> valueList = tagKeys.stream().map(tagKey -> {

      String pk = createDatabaseKey(siteId, GLOBAL_FOLDER_TAGS);
      String sk = "key" + TAG_DELIMINATOR + tagKey.toLowerCase();

      Map<String, AttributeValue> values = new HashMap<>(Map.of(PK, AttributeValue.fromS(pk), SK,
          AttributeValue.fromS(sk), "tagKey", AttributeValue.fromS(tagKey)));

      return values;
    }).collect(Collectors.toList());

    ReadRequestBuilder readBuilder = new ReadRequestBuilder();
    List<Map<String, AttributeValue>> keys = valueList.stream()
        .map(v -> Map.of(PK, v.get(PK), SK, v.get(SK))).collect(Collectors.toList());
    readBuilder.append(documentTableName, keys);

    Map<String, List<Map<String, AttributeValue>>> batchReadItems =
        readBuilder.batchReadItems(dbClient);

    List<Map<String, AttributeValue>> batchReads = batchReadItems.get(documentTableName);

    Set<String> existingKeys =
        batchReads.stream().map(r -> getCacheKey(r)).collect(Collectors.toSet());

    valueList.removeIf(v -> existingKeys.contains(getCacheKey(v)));

    if (!valueList.isEmpty()) {
      WriteRequestBuilder builder = new WriteRequestBuilder().appends(documentTableName, valueList);
      builder.batchWriteItem(dbClient);
    }
  }
}
