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
package com.formkiq.stacks.dynamodb.numbering;

import static com.formkiq.aws.dynamodb.DbKeys.PK;
import static com.formkiq.aws.dynamodb.DbKeys.SK;

import java.time.Clock;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.formkiq.aws.dynamodb.BatchGetConfig;
import com.formkiq.aws.dynamodb.DynamoDbKey;
import com.formkiq.aws.dynamodb.DynamoDbService;
import com.formkiq.aws.dynamodb.QueryResult;
import com.formkiq.aws.dynamodb.base64.Pagination;
import com.formkiq.validation.ValidationBuilder;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ReturnValue;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemResponse;

/** DynamoDB implementation of {@link NumberingSequenceService}. */
public class NumberingSequenceServiceDynamoDb implements NumberingSequenceService {

  /** Calendar year token. */
  private static final String YEAR_TOKEN = "{YEAR}";
  /** Sequence number token. */
  private static final String SEQUENCE_TOKEN = "{SEQUENCE}";
  /** Pattern token matcher. */
  private static final Pattern TOKEN_PATTERN = Pattern.compile("\\{[^{}]+}");
  /** DynamoDB service. */
  private final DynamoDbService db;
  /** Clock. */
  private final Clock clock;

  /**
   * Constructor using the system clock.
   *
   * @param dbService DynamoDB service
   */
  public NumberingSequenceServiceDynamoDb(final DynamoDbService dbService) {
    this(dbService, Clock.systemUTC());
  }

  /**
   * Constructor.
   *
   * @param dbService DynamoDB service
   * @param sequenceClock Clock used for period calculation
   */
  public NumberingSequenceServiceDynamoDb(final DynamoDbService dbService,
      final Clock sequenceClock) {
    this.db = dbService;
    this.clock = sequenceClock;
  }

  private NumberingSequence addState(final NumberingSequenceRecord record, final String period,
      final Map<String, AttributeValue> counter) {
    Long current =
        counter != null && counter.containsKey("Number") ? Long.valueOf(counter.get("Number").n())
            : null;
    String currentPeriod = current != null ? period : null;
    String lastValue = current != null ? format(record, current, period) : null;
    return new NumberingSequence(record.attributeKey(), record.pattern(), record.startAt(),
        record.padding(), record.reset(), record.timezone(), currentPeriod, current, lastValue);
  }

  private NumberingSequence addState(final String siteId, final NumberingSequenceRecord record) {
    String period = period(record, this.clock.instant());
    DynamoDbKey key = NumberingSequenceRecord.counterKey(siteId, record.attributeKey(), period);
    return addState(record, period,
        this.db.get(AttributeValue.fromS(key.pk()), AttributeValue.fromS(key.sk())));
  }

  private String createParsingExpression(final NumberingSequenceRecord record) {
    StringBuilder expression = new StringBuilder("^");
    int position = 0;
    Matcher matcher = TOKEN_PATTERN.matcher(record.pattern());
    while (matcher.find()) {
      expression.append(Pattern.quote(record.pattern().substring(position, matcher.start())));
      if (YEAR_TOKEN.equals(matcher.group())) {
        expression.append("(?<year>\\d{4})");
      } else if (SEQUENCE_TOKEN.equals(matcher.group())) {
        expression.append("(?<sequence>\\d+)");
      }
      position = matcher.end();
    }
    expression.append(Pattern.quote(record.pattern().substring(position))).append('$');
    return expression.toString();
  }

  @Override
  public NumberingSequence find(final String siteId, final String attributeKey) {
    DynamoDbKey key = NumberingSequenceRecord.builder().attributeKey(attributeKey).buildKey(siteId);
    Map<String, AttributeValue> attributes =
        this.db.get(AttributeValue.fromS(key.pk()), AttributeValue.fromS(key.sk()));
    NumberingSequenceRecord record = NumberingSequenceRecord.fromAttributeMap(attributes);
    return record != null ? addState(siteId, record) : null;
  }

  @Override
  public Pagination<NumberingSequence> findAll(final String siteId, final String nextToken,
      final int limit) {
    QueryResult result = new GetNumberingSequencesQuery().query(this.db, siteId, nextToken, limit);
    List<NumberingSequenceRecord> records =
        result.items().stream().map(NumberingSequenceRecord::fromAttributeMap).toList();

    Map<String, String> periods = new HashMap<>();
    List<Map<String, AttributeValue>> counterKeys = records.stream().map(record -> {
      String period = period(record, this.clock.instant());
      periods.put(record.attributeKey(), period);
      return NumberingSequenceRecord.counterKey(siteId, record.attributeKey(), period).toMap();
    }).toList();

    Map<String, Map<String, AttributeValue>> counters = new HashMap<>();
    if (!counterKeys.isEmpty()) {
      this.db.getBatch(new BatchGetConfig(), counterKeys).forEach(
          counter -> counters.put(counter.get(PK).s() + "#" + counter.get(SK).s(), counter));
    }

    List<NumberingSequence> sequences = records.stream().map(record -> {
      String period = periods.get(record.attributeKey());
      DynamoDbKey key = NumberingSequenceRecord.counterKey(siteId, record.attributeKey(), period);
      Map<String, AttributeValue> counter =
          counters.getOrDefault(key.pk() + "#" + key.sk(), Map.of());
      return addState(record, period, counter);
    }).toList();

    return new Pagination<>(sequences, result.lastEvaluatedKey());
  }

  private NumberingSequenceRecord findRecord(final String siteId, final String attributeKey) {
    DynamoDbKey key = NumberingSequenceRecord.builder().attributeKey(attributeKey).buildKey(siteId);
    Map<String, AttributeValue> attributes =
        this.db.get(AttributeValue.fromS(key.pk()), AttributeValue.fromS(key.sk()));
    return NumberingSequenceRecord.fromAttributeMap(attributes);
  }

  private String format(final NumberingSequenceRecord record, final long sequence,
      final String period) {
    String sequenceValue = String.format(Locale.ROOT, "%0" + record.padding() + "d", sequence);
    return record.pattern().replace(YEAR_TOKEN, period).replace(SEQUENCE_TOKEN, sequenceValue);
  }

  @Override
  public NumberingSequenceValue next(final String siteId, final String attributeKey) {
    NumberingSequenceRecord record = findRecord(siteId, attributeKey);
    NumberingSequenceValue value = null;
    if (record != null) {
      String period = period(record, this.clock.instant());
      DynamoDbKey key = NumberingSequenceRecord.counterKey(siteId, attributeKey, period);
      long base = record.startAt() - 1;
      UpdateItemRequest request =
          UpdateItemRequest.builder().tableName(this.db.getTableName()).key(key.toMap())
              .updateExpression("SET #number = if_not_exists(#number, :base) + :increment")
              .expressionAttributeNames(Map.of("#number", "Number"))
              .expressionAttributeValues(Map.of(":base", AttributeValue.fromN(String.valueOf(base)),
                  ":increment", AttributeValue.fromN("1")))
              .returnValues(ReturnValue.UPDATED_NEW).build();
      UpdateItemResponse response = this.db.updateItem(request);
      long sequence = Long.parseLong(response.attributes().get("Number").n());
      value = new NumberingSequenceValue(format(record, sequence, period), sequence, period);
    }
    return value;
  }

  @Override
  public NumberingSequenceValue parse(final String siteId, final String attributeKey,
      final String value) {
    NumberingSequenceRecord record = findRecord(siteId, attributeKey);
    NumberingSequenceValue parsed = new NumberingSequenceValue(value, null, null);
    if (record != null && value != null) {
      Matcher matcher = Pattern.compile(createParsingExpression(record)).matcher(value);
      if (matcher.matches()) {
        String period =
            NumberingSequenceReset.YEARLY.equals(record.reset()) ? matcher.group("year") : "NONE";
        parsed = new NumberingSequenceValue(value, Long.valueOf(matcher.group("sequence")), period);
      }
    }
    return parsed;
  }

  private String period(final NumberingSequenceRecord record, final Instant instant) {
    if (NumberingSequenceReset.NONE.equals(record.reset())) {
      return "NONE";
    }
    ZoneId zoneId = ZoneId.of(record.timezone());
    return String.valueOf(ZonedDateTime.ofInstant(instant, zoneId).getYear());
  }

  @Override
  public NumberingSequence save(final String siteId, final NumberingSequenceRecord record) {
    validate(record);
    this.db.putItem(record.getAttributes());
    return addState(siteId, record);
  }

  private void validate(final NumberingSequenceRecord record) {
    ValidationBuilder validation = new ValidationBuilder();
    validation.isRequired("numberingSequence", record);
    if (record == null) {
      validation.check();
      return;
    }

    validation.isRequired("attributeKey", record.attributeKey());
    validation.isRequired("pattern", record.pattern());
    validation.isRequired("startAt", record.startAt() != null && record.startAt() > 0,
        "'startAt' must be greater than 0");
    validation.isRequired("padding", record.padding() != null && record.padding() > 0,
        "'padding' must be greater than 0");
    validation.isRequired("reset", record.reset());
    validation.isRequired("timezone", record.timezone());

    validateTimeZone(record, validation);

    validatePattern(record, validation);

    validation.check();
  }

  private void validatePattern(final NumberingSequenceRecord record,
      final ValidationBuilder validation) {
    if (record.pattern() != null) {
      List<String> tokens = new ArrayList<>();
      Matcher matcher = TOKEN_PATTERN.matcher(record.pattern());
      while (matcher.find()) {
        tokens.add(matcher.group());
      }
      if (tokens.stream().filter(SEQUENCE_TOKEN::equals).count() != 1) {
        validation.addError("pattern", "'pattern' must contain {SEQUENCE} exactly once");
      }
      tokens.stream().filter(t -> !YEAR_TOKEN.equals(t) && !SEQUENCE_TOKEN.equals(t)).distinct()
          .forEach(t -> validation.addError("pattern", "unsupported pattern token '" + t + "'"));

      long yearTokens = tokens.stream().filter(YEAR_TOKEN::equals).count();
      if (NumberingSequenceReset.YEARLY.equals(record.reset()) && yearTokens != 1) {
        validation.addError("pattern", "YEARLY sequences must contain {YEAR} exactly once");
      } else if (NumberingSequenceReset.NONE.equals(record.reset()) && yearTokens > 0) {
        validation.addError("pattern", "NONE sequences cannot contain {YEAR}");
      }
    }
  }

  private void validateTimeZone(final NumberingSequenceRecord record,
      final ValidationBuilder validation) {
    if (record.timezone() != null && !record.timezone().isEmpty()) {
      try {
        ZoneId.of(record.timezone());
      } catch (DateTimeException e) {
        validation.addError("timezone", "invalid IANA timezone '" + record.timezone() + "'");
      }
    }
  }
}
