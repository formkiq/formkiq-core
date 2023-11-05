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
package com.formkiq.server;

import java.io.Closeable;
import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import com.amazonaws.services.lambda.runtime.Context;
import com.formkiq.lambda.runtime.graalvm.LambdaContext;
import com.formkiq.module.lambda.typesense.TypesenseProcessor;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.DescribeStreamRequest;
import software.amazon.awssdk.services.dynamodb.model.DescribeStreamResponse;
import software.amazon.awssdk.services.dynamodb.model.GetRecordsRequest;
import software.amazon.awssdk.services.dynamodb.model.GetRecordsResponse;
import software.amazon.awssdk.services.dynamodb.model.GetShardIteratorRequest;
import software.amazon.awssdk.services.dynamodb.model.Shard;
import software.amazon.awssdk.services.dynamodb.model.ShardIteratorType;
import software.amazon.awssdk.services.dynamodb.streams.DynamoDbStreamsAsyncClient;

/**
 * Polls DynamoDb Stream and sends stream to Http Endpoint.
 */
public class DynamoDbStreamToTypesense implements Closeable {

  // /** {@link AwsCredentialsProvider}. */
  // private AwsCredentialsProvider credentialsProvider;
  // /** {@link URI}. */
  // private URI dynamodbUri;
  // /** {@link String}. */
  // private String httpEndpoint;
  // /** {@link Region}. */
  // private Region region;
  /** Shard Map. */
  // private Map<String, String> shardIteratorMap = new HashMap<>();
  /** {@link String}. */
  private String streamArn;
  /** {@link DynamoDbStreamsAsyncClient}. */
  private DynamoDbStreamsAsyncClient streamsClient = null;
  /** {@link Gson}. */
  private Gson gson = new GsonBuilder().create();
  /** {@link TypesenseProcessor}. */
  private TypesenseProcessor processor;

  /**
   * constructor.
   * 
   * @param awsRegion {@link Region}
   * @param awsCredentialsProvider {@link AwsCredentialsProvider}
   * @param dynamoDbStreamArn {@link String}
   * @param dynamodbUri {@link String}
   * @param typesenseProcessor {@link String}
   */
  public DynamoDbStreamToTypesense(final Region awsRegion,
      final AwsCredentialsProvider awsCredentialsProvider, final String dynamoDbStreamArn,
      final URI dynamodbUri, final TypesenseProcessor typesenseProcessor) {

    this.streamArn = dynamoDbStreamArn;
    this.processor = typesenseProcessor;

    this.streamsClient = DynamoDbStreamsAsyncClient.builder().endpointOverride(dynamodbUri)
        .region(awsRegion).credentialsProvider(awsCredentialsProvider).build();

  }

  @Override
  public void close() throws IOException {
    if (this.streamsClient != null) {
      this.streamsClient.close();
    }
  }

  private String getShardIterator(final String shardId, final String iteratorType) {

    GetShardIteratorRequest getShardIteratorRequest = GetShardIteratorRequest.builder()
        .streamArn(this.streamArn).shardId(shardId).shardIteratorType(iteratorType).build();

    return this.streamsClient.getShardIterator(getShardIteratorRequest).join().shardIterator();
  }

  /**
   * Run Stream Poller.
   */
  public void run() {

    try {
      // Describe the stream to get shard information
      DescribeStreamRequest streamRequest =
          DescribeStreamRequest.builder().streamArn(this.streamArn).build();
      DescribeStreamResponse streamResponse =
          this.streamsClient.describeStream(streamRequest).get();

      // Iterate through each shard in the stream
      for (Shard shard : streamResponse.streamDescription().shards()) {

        String shardId = shard.shardId();

        String iteratorType = ShardIteratorType.TRIM_HORIZON.toString();
        GetRecordsRequest getRecordsRequest = GetRecordsRequest.builder()
            .shardIterator(getShardIterator(shardId, iteratorType)).build();

        GetRecordsResponse getRecordsResponse =
            this.streamsClient.getRecords(getRecordsRequest).join();
        List<software.amazon.awssdk.services.dynamodb.model.Record> records =
            getRecordsResponse.records();

        for (software.amazon.awssdk.services.dynamodb.model.Record record : records) {

          Map<String, Object> dynamodb = new HashMap<>();
          dynamodb.put("Keys", record.dynamodb().keys());

          if (!record.dynamodb().newImage().isEmpty()) {
            Map<String, AttributeValue> newImage = record.dynamodb().newImage();
            String json = this.gson.toJson(newImage);
            dynamodb.put("NewImage", this.gson.fromJson(json, Map.class));
          }

          if (!record.dynamodb().newImage().isEmpty()) {
            Map<String, AttributeValue> oldImage = record.dynamodb().oldImage();
            String json = this.gson.toJson(oldImage);
            dynamodb.put("OldImage", this.gson.fromJson(json, Map.class));
          }

          Map<String, Object> map = new HashMap<>();
          map.put("eventID", record.eventID());
          map.put("eventName", record.eventNameAsString());
          map.put("eventVersion", record.eventVersion());
          map.put("eventSource", record.eventSource());
          map.put("awsRegion", record.awsRegion());
          map.put("dynamodb", dynamodb);

          map = Map.of("Records", Arrays.asList(map));

          Context context = new LambdaContext(UUID.randomUUID().toString());

          try {
            this.processor.handleRequest(map, context);
          } catch (Exception e) {
            e.printStackTrace();
          }
        }

        // Update the shard iterator
        getRecordsRequest = getRecordsRequest.toBuilder()
            .shardIterator(getRecordsResponse.nextShardIterator()).build();
      }
    } catch (InterruptedException | ExecutionException e) {
      e.printStackTrace();
    }
  }
}

