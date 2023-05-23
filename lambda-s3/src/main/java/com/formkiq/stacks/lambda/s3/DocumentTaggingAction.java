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
import java.util.stream.Collectors;
import java.util.Optional;
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
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;

/**
 * 
 * {@link ActionType} Document Tagging implementation of {@link DocumentAction}.
 *
 */
public class DocumentTaggingAction implements DocumentAction {

  /** Chat Gpt Frequency Penalty. */
  private static final Double CHAT_GPT_FREQ_PENALTY = Double.valueOf(0.8);
  /** Maximum document size to send to ChatGpt. */
  private static final int CHAT_GPT_MAX_LENGTH = 2000;
  /** Chat GPT Max Tokens. */
  private static final Integer CHAT_GPT_MAX_TOKENS = Integer.valueOf(1000);
  /** Chat Gpt Presence Penalty. */
  private static final Double CHAT_GPT_PRESENCE_PENALTY = Double.valueOf(0.0);
  /** Chat Gpt Temperature. */
  private static final Double CHAT_GPT_TEMPERATURE = Double.valueOf(0.5);
  /** Chat Gpt Top P. */
  private static final Double CHAT_GPT_TOP_P = Double.valueOf(1.0);
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

  private String createChatGptPrompt(final String siteId, final String documentId,
      final Action action) throws IOException {

    String tags = getTags(action);

    DocumentItem item = this.documentService.findDocument(siteId, documentId);

    DocumentContentFunction docContentFucn = new DocumentContentFunction(this.serviceCache);
    List<String> contentUrls = docContentFucn.getContentUrls(siteId, item);

    if (contentUrls.isEmpty()) {
      throw new IOException("'contentUrls' is empty");
    }

    StringBuilder sb = docContentFucn.getContentUrls(contentUrls);

    String text = sb.toString().trim();
    if (text.length() > CHAT_GPT_MAX_LENGTH) {
      text = text.substring(0, CHAT_GPT_MAX_LENGTH);
    }

    String prompt = "Extract the tags " + tags
        + " from the text below and return in a JSON key/list.\n\n" + text;

    return prompt;
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
      final Action action, final HttpResponse<String> response) throws IOException {

    List<String> paramTags = getTagsAsList(action);

    Map<String, Object> responseMap = this.gson.fromJson(response.body(), Map.class);
    List<Map<String, Object>> choices = (List<Map<String, Object>>) responseMap.get("choices");

    Collection<DocumentTag> tags = new ArrayList<>();

    for (Map<String, Object> choice : choices) {

      String text = choice.get("text").toString();
      Map<String, Object> data = parseGptText(paramTags, text);

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

    Map<String, String> tagMap = tags.stream().filter(t -> !t.toLowerCase().equals(t))
        .collect(Collectors.toMap(String::toLowerCase, s -> s));

    Map<String, Object> data = new HashMap<>();

    try {
      data = this.gson.fromJson(text, Map.class);
    } catch (JsonSyntaxException e) {

      String[] strs = text.split("\n");

      for (String s : strs) {

        int pos = s.indexOf(":");

        if (pos > -1) {
          String key = s.substring(0, pos).trim().toLowerCase();
          String value = s.substring(pos + 1).trim();

          if (!key.isEmpty() && !value.isEmpty()) {
            data.put(key, Arrays.asList(value));
          }
        }
      }
    }

    for (Map.Entry<String, String> e : tagMap.entrySet()) {
      Object object = data.get(e.getKey());
      data.put(e.getValue(), object);
      data.remove(e.getKey());
    }

    return data;
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

    Optional<HttpHeaders> headers = Optional.of(new HttpHeaders()
        .add("Content-Type", "application/json").add("Authorization", "Bearer " + chatGptApiKey));

    Map<String, Object> payload = new HashMap<>();

    payload.put("model", "text-davinci-003");
    payload.put("max_tokens", CHAT_GPT_MAX_TOKENS);
    payload.put("temperature", CHAT_GPT_TEMPERATURE);
    payload.put("top_p", CHAT_GPT_TOP_P);
    payload.put("frequency_penalty", CHAT_GPT_FREQ_PENALTY);
    payload.put("presence_penalty", CHAT_GPT_PRESENCE_PENALTY);
    payload.put("prompt", createChatGptPrompt(siteId, documentId, action));

    String url = this.serviceCache.environment("CHATGPT_API_COMPLETIONS_URL");
    HttpResponse<String> response = this.http.post(url, headers, this.gson.toJson(payload));
    logger.log(String.format("{\"engine\":\"%s\",\"statusCode\":\"%s\",\"body\":\"%s\"}", "chatgpt",
        String.valueOf(response.statusCode()), response.body()));

    if (is2XX(response)) {
      parseChatGptResponse(siteId, documentId, action, response);
    } else {
      throw new IOException("ChatGpt status " + response.statusCode());
    }
  }

}
