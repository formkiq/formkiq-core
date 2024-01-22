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

import static com.formkiq.aws.dynamodb.objects.Strings.removeEndingPunctuation;
import static com.formkiq.aws.dynamodb.objects.Strings.removeQuotes;
import static com.formkiq.module.http.HttpResponseStatus.is2XX;
import static com.formkiq.stacks.dynamodb.ConfigService.CHATGPT_API_KEY;
import java.io.IOException;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Collectors;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.formkiq.aws.dynamodb.DynamicObject;
import com.formkiq.aws.dynamodb.model.DocumentItem;
import com.formkiq.aws.dynamodb.model.DocumentTag;
import com.formkiq.aws.dynamodb.model.DocumentTagType;
import com.formkiq.aws.dynamodb.objects.Strings;
import com.formkiq.module.actions.Action;
import com.formkiq.module.actions.ActionType;
import com.formkiq.module.http.HttpHeaders;
import com.formkiq.module.http.HttpService;
import com.formkiq.module.http.HttpServiceJdk11;
import com.formkiq.module.lambdaservices.AwsServiceCache;
import com.formkiq.stacks.dynamodb.ConfigService;
import com.formkiq.stacks.dynamodb.DocumentService;
import com.formkiq.stacks.lambda.s3.openai.OpenAiChatCompletionsChoice;
import com.formkiq.stacks.lambda.s3.openai.OpenAiChatCompletionsChoiceMessage;
import com.formkiq.stacks.lambda.s3.openai.OpenAiChatCompletionsChoiceMessageFunctionCall;
import com.formkiq.stacks.lambda.s3.openai.OpenAiChatCompletionsResponse;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * 
 * {@link ActionType} Document Tagging implementation of {@link DocumentAction}.
 *
 */
public class DocumentTaggingAction implements DocumentAction {

  /** Maximum document size to send to ChatGpt. */
  private static final int CHAT_GPT_MAX_LENGTH = 2000;
  /** Chat Gpt Temperature. */
  private static final Double CHAT_GPT_TEMPERATURE = Double.valueOf(0.5);
  /** {@link ConfigService}. */
  private ConfigService configsService;
  /** {@link DocumentService}. */
  private DocumentService documentService;
  /** {@link Gson}. */
  private Gson gson = new GsonBuilder().create();
  /** {@link HttpService}. */
  private HttpService http = new HttpServiceJdk11();
  /** {@link AwsServiceCache}. */
  private AwsServiceCache serviceCache;

  /**
   * constructor.
   * 
   * @param services {@link AwsServiceCache}
   */
  public DocumentTaggingAction(final AwsServiceCache services) {
    this.serviceCache = services;
    this.configsService = services.getExtension(ConfigService.class);
    this.documentService = services.getExtension(DocumentService.class);
  }

  private String createChatGptPrompt(final LambdaLogger logger, final String siteId,
      final String documentId, final Action action) throws IOException {

    DocumentItem item = this.documentService.findDocument(siteId, documentId);

    DocumentContentFunction docContentFucn = new DocumentContentFunction(this.serviceCache);
    List<String> contentUrls =
        docContentFucn.getContentUrls(this.serviceCache.debug() ? logger : null, siteId, item);

    if (contentUrls.isEmpty()) {
      throw new IOException("'contentUrls' is empty");
    }

    StringBuilder sb = docContentFucn.getContentUrls(contentUrls);

    String text = sb.toString().trim();
    if (text.length() > CHAT_GPT_MAX_LENGTH) {
      text = text.substring(0, CHAT_GPT_MAX_LENGTH);
    }

    return "Extract the tags from the text below.\n\n" + text;
  }

  private Map<String, Object> generateOpenApiSchema(final Action action) throws IOException {

    List<String> tags = getTagsAsList(action);

    Map<String, Object> schema = new HashMap<>();
    schema.put("type", "object");

    Map<String, Object> properties = new HashMap<>();

    for (String tag : tags) {
      properties.put(tag, Map.of("type", "array", "description", "Get the tag values for " + tag,
          "items", Map.of("type", "string")));
    }

    schema.put("properties", properties);

    return schema;
  }

  private String getTags(final Action action) throws IOException {
    String tags = action.parameters().get("tags");
    if (Strings.isEmpty(tags)) {
      throw new IOException("missing 'tags' parameter");
    }
    return tags;
  }

  private List<String> getTagsAsList(final Action action) throws IOException {
    return Arrays.asList(getTags(action).split(","));
  }

  @SuppressWarnings("unchecked")
  private void parseChatGptResponse(final String siteId, final String documentId,
      final Action action, final HttpResponse<String> httpResponse) throws IOException {

    List<String> paramTags = getTagsAsList(action);

    OpenAiChatCompletionsResponse response =
        this.gson.fromJson(httpResponse.body(), OpenAiChatCompletionsResponse.class);

    List<OpenAiChatCompletionsChoice> choices = response.choices();

    Collection<DocumentTag> tags = new ArrayList<>();

    for (OpenAiChatCompletionsChoice choice : choices) {

      OpenAiChatCompletionsChoiceMessage message = choice.message();
      OpenAiChatCompletionsChoiceMessageFunctionCall functionCall = message.functionCall();
      String arguments = functionCall.arguments();

      Map<String, Object> data = parseGptText(paramTags, arguments);

      for (Entry<String, Object> e : data.entrySet()) {

        List<Object> list = Collections.emptyList();

        Object obj = e.getValue();
        if (obj != null) {
          if (obj instanceof Collection) {
            list = (List<Object>) obj;
          } else {
            list = Arrays.asList(obj.toString());
          }
        }

        if (!list.isEmpty()) {

          DocumentTag tag = new DocumentTag(documentId, e.getKey(), list.get(0).toString(),
              new Date(), "System", DocumentTagType.USERDEFINED);

          if (list.size() > 1) {
            tag.setValue(null);
            tag.setValues(list.stream().map(i -> i.toString()).collect(Collectors.toList()));
          }

          tags.add(tag);
        }
      }
    }

    if (!tags.isEmpty()) {
      this.documentService.addTags(siteId, documentId, tags, null);
      this.documentService.deleteDocumentTag(siteId, documentId, "untagged");
    }
  }

  @SuppressWarnings("unchecked")
  private Map<String, Object> parseGptText(final List<String> tags, final String text) {

    Map<String, String> tagMap =
        tags.stream().collect(Collectors.toMap(String::toLowerCase, s -> s));

    Map<String, Object> data = this.gson.fromJson(text, Map.class);
    data = data.entrySet().stream()
        .filter(e -> tagMap.containsKey(e.getKey().toLowerCase()) && e.getValue() != null)
        .collect(Collectors.toMap(e -> tagMap.get(e.getKey().toLowerCase()),
            e -> removeQuotesFromObject(e.getValue())));

    return data;
  }

  @SuppressWarnings("unchecked")
  private Object removeQuotesFromObject(final Object o) {
    Object oo = o;
    if (oo instanceof String) {
      oo = removeEndingPunctuation(removeQuotes((String) oo));
    } else if (oo instanceof Collection) {

      Collection<Object> list = new ArrayList<>();
      for (Object obj : (Collection<Object>) oo) {
        if (obj instanceof String) {
          list.add(removeEndingPunctuation(removeQuotes((String) obj)));
        }
      }

      oo = list;
    }

    return oo;
  }

  @Override
  public void run(final LambdaLogger logger, final String siteId, final String documentId,
      final Action action) throws IOException {

    String engine = action.parameters().get("engine");
    if (engine != null && "chatgpt".equals(engine.toLowerCase())) {
      runChatGpt(logger, siteId, documentId, action);
    } else {
      throw new IOException("Unknown engine: " + engine);
    }
  }

  /**
   * Run ChatGpt Document Tagging.
   * 
   * @param logger {@link LambdaLogger}
   * @param siteId {@link String}
   * @param documentId {@link String}
   * @param action {@link Action}
   * @throws IOException IOException
   */
  private void runChatGpt(final LambdaLogger logger, final String siteId, final String documentId,
      final Action action) throws IOException {

    DynamicObject configs = this.configsService.get(siteId);
    String chatGptApiKey = configs.getString(CHATGPT_API_KEY);

    if (chatGptApiKey == null) {
      throw new IOException(String.format("missing config '%s'", CHATGPT_API_KEY));
    }

    Map<String, Object> payload = new HashMap<>();

    String prompt = createChatGptPrompt(logger, siteId, documentId, action);

    Map<String, Object> schema = generateOpenApiSchema(action);

    payload.put("model", "gpt-3.5-turbo");
    payload.put("messages", Arrays.asList(Map.of("role", "user", "content", prompt)));
    payload.put("functions", Arrays.asList(Map.of("name", "get_text_data", "parameters", schema)));
    payload.put("function_call", Map.of("name", "get_text_data"));
    payload.put("temperature", CHAT_GPT_TEMPERATURE);

    String url = this.serviceCache.environment("CHATGPT_API_COMPLETIONS_URL");

    Optional<HttpHeaders> headers = Optional.of(new HttpHeaders()
        .add("Content-Type", "application/json").add("Authorization", "Bearer " + chatGptApiKey));

    String body = this.gson.toJson(payload);
    if (this.serviceCache.debug()) {
      logger.log("sending POST request to " + url + " body: " + body);
    }

    HttpResponse<String> response = this.http.post(url, headers, Optional.empty(), body);

    if (this.serviceCache.debug()) {
      logger.log(String.format("{\"engine\":\"%s\",\"statusCode\":\"%s\",\"body\":\"%s\"}",
          "chatgpt", String.valueOf(response.statusCode()), response.body()));
    }

    if (is2XX(response)) {
      parseChatGptResponse(siteId, documentId, action, response);
    } else {
      throw new IOException("ChatGpt status " + response.statusCode() + " " + response.body());
    }
  }
}
