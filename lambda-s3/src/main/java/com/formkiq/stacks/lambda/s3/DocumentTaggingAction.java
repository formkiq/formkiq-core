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
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
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

    String tags = action.parameters().get("tags");
    if (Strings.isEmpty(tags)) {
      throw new IOException("missing 'tags' parameter");
    }

    DocumentItem item =
        this.serviceCache.getExtension(DocumentService.class).findDocument(siteId, documentId);

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

  @SuppressWarnings("unchecked")
  private void parseChatGptResponse(final String siteId, final String documentId,
      final HttpResponse<String> response) {

    Map<String, Object> responseMap = this.gson.fromJson(response.body(), Map.class);
    List<Map<String, Object>> choices = (List<Map<String, Object>>) responseMap.get("choices");

    Collection<DocumentTag> tags = new ArrayList<>();

    for (Map<String, Object> choice : choices) {

      String text = choice.get("text").toString();
      Map<String, List<String>> data = this.gson.fromJson(text, Map.class);

      for (Entry<String, List<String>> e : data.entrySet()) {

        if (!e.getValue().isEmpty()) {

          DocumentTag tag = new DocumentTag(documentId, e.getKey(), e.getValue(), new Date(),
              "System", DocumentTagType.USERDEFINED);

          if (e.getValue().size() == 1) {
            tag.setValue(e.getValue().get(0));
            tag.setValues(null);
          }

          tags.add(tag);
        }
      }
    }

    if (!tags.isEmpty()) {
      this.documentService.addTags(siteId, documentId, tags, null);
    }
  }

  @Override
  public void run(final String siteId, final String documentId, final Action action)
      throws IOException {

    String engine = action.parameters().get("engine");
    if (engine != null && "chatgpt".equals(engine.toLowerCase())) {
      runChatGpt(siteId, documentId, action);
    } else {
      throw new IOException("Unknown engine: " + engine);
    }
  }

  /**
   * Run ChatGpt Document Tagging.
   * 
   * @param siteId {@link String}
   * @param documentId {@link String}
   * @param action {@link Action}
   * @throws IOException IOException
   */
  private void runChatGpt(final String siteId, final String documentId, final Action action)
      throws IOException {
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

    if (is2XX(response)) {
      parseChatGptResponse(siteId, documentId, response);
    } else {
      throw new IOException("ChatGpt status " + response.statusCode());
    }
  }

}
