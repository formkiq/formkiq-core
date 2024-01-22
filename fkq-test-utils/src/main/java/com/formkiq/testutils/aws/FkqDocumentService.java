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
package com.formkiq.testutils.aws;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import com.formkiq.client.api.AdvancedDocumentSearchApi;
import com.formkiq.client.api.DocumentActionsApi;
import com.formkiq.client.api.DocumentTagsApi;
import com.formkiq.client.api.DocumentsApi;
import com.formkiq.client.invoker.ApiClient;
import com.formkiq.client.invoker.ApiException;
import com.formkiq.client.model.AddAction;
import com.formkiq.client.model.AddDocumentResponse;
import com.formkiq.client.model.AddDocumentTagsRequest;
import com.formkiq.client.model.GetDocumentActionsResponse;
import com.formkiq.client.model.GetDocumentContentResponse;
import com.formkiq.client.model.GetDocumentFulltextResponse;
import com.formkiq.client.model.GetDocumentResponse;
import com.formkiq.client.model.GetDocumentTagResponse;
import com.formkiq.client.model.GetDocumentUrlResponse;
import com.formkiq.stacks.client.FormKiqClient;
import com.formkiq.stacks.client.FormKiqClientV1;
import com.formkiq.stacks.client.models.AddDocument;
import com.formkiq.stacks.client.models.AddDocumentAction;
import com.formkiq.stacks.client.models.AddDocumentTag;
import com.formkiq.stacks.client.models.DocumentAction;
import com.formkiq.stacks.client.models.DocumentActionType;
import com.formkiq.stacks.client.models.DocumentActions;
import com.formkiq.stacks.client.models.DocumentTag;
import com.formkiq.stacks.client.requests.AddDocumentRequest;
import com.formkiq.stacks.client.requests.GetDocumentActionsRequest;
import com.formkiq.stacks.client.requests.GetDocumentContentRequest;
import com.formkiq.stacks.client.requests.GetDocumentRequest;
import com.formkiq.stacks.client.requests.GetDocumentTagsKeyRequest;
import com.formkiq.stacks.client.requests.GetDocumentUploadRequest;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import software.amazon.awssdk.utils.StringUtils;

/**
 * 
 * FormKiQ Documents Service.
 *
 */
public class FkqDocumentService {

  /** {@link Gson}. */
  private static Gson gson = new GsonBuilder().disableHtmlEscaping().create();
  /** {@link HttpClient}. */
  private static HttpClient http = HttpClient.newHttpClient();
  /** 200 OK. */
  private static final int STATUS_OK = 200;

  /**
   * Add Document.
   * 
   * @param apiClient {@link ApiClient}
   * @param siteId {@link String}
   * @param path {@link String}
   * @param data byte[]
   * @param contentType {@link String}
   * @param actions {@link List} {@link AddAction}
   * @param tags {@link com.formkiq.client.model.AddDocumentTag}
   * @return {@link String}
   * @throws ApiException ApiException
   */
  public static String addDocument(final ApiClient apiClient, final String siteId,
      final String path, final byte[] data, final String contentType, final List<AddAction> actions,
      final List<com.formkiq.client.model.AddDocumentTag> tags) throws ApiException {

    DocumentsApi api = new DocumentsApi(apiClient);
    String content = Base64.getEncoder().encodeToString(data);
    com.formkiq.client.model.AddDocumentRequest req =
        new com.formkiq.client.model.AddDocumentRequest().content(content).contentType(contentType)
            .isBase64(Boolean.TRUE).path(path).actions(actions).tags(tags);
    AddDocumentResponse response = api.addDocument(req, siteId, null);
    return response.getDocumentId();
  }

  /**
   * Add "file" but this just creates DynamoDB record and the S3 file.
   * 
   * @param apiClient {@link ApiClient}
   * @param siteId {@link String}
   * @param path {@link String}
   * @param content byte[]
   * @param contentType {@link String}
   * @param shareKey {@link String}
   * @return {@link String}
   * @throws IOException IOException
   * @throws InterruptedException InterruptedException
   * @throws URISyntaxException URISyntaxException
   * @throws ApiException ApiException
   */
  public static String addDocument(final ApiClient apiClient, final String siteId,
      final String path, final byte[] content, final String contentType, final String shareKey)
      throws IOException, InterruptedException, URISyntaxException, ApiException {

    DocumentsApi api = new DocumentsApi(apiClient);
    GetDocumentUrlResponse response =
        api.getDocumentUpload(path, siteId, Integer.valueOf(content.length), null, shareKey);
    String s3url = response.getUrl();

    if (content.length > 0) {
      http.send(HttpRequest.newBuilder(new URI(s3url)).header("Content-Type", contentType)
          .method("PUT", BodyPublishers.ofByteArray(content)).build(), BodyHandlers.ofString());
    }

    return response.getDocumentId();
  }

  /**
   * Add Document.
   * 
   * @param apiClient {@link ApiClient}
   * @param siteId {@link String}
   * @param path {@link String}
   * @param content {@link String}
   * @param contentType {@link String}
   * @param actions {@link List} {@link AddAction}
   * @return {@link String}
   * @throws ApiException ApiException
   */
  public static String addDocument(final ApiClient apiClient, final String siteId,
      final String path, final String content, final String contentType,
      final List<AddAction> actions) throws ApiException {

    DocumentsApi api = new DocumentsApi(apiClient);
    com.formkiq.client.model.AddDocumentRequest req =
        new com.formkiq.client.model.AddDocumentRequest().content(content).contentType(contentType)
            .path(path).actions(actions);
    AddDocumentResponse response = api.addDocument(req, siteId, null);
    return response.getDocumentId();
  }

  /**
   * Add "file" but this just creates DynamoDB record and not the S3 file.
   * 
   * @param client {@link FormKiqClientV1}
   * @param siteId {@link String}
   * @param path {@link String}
   * @param content byte[]
   * @param contentType {@link String}
   * @return {@link String}
   * @throws IOException IOException
   * @throws URISyntaxException URISyntaxException
   * @throws InterruptedException InterruptedException
   * @deprecated do no used
   */
  @Deprecated
  public static String addDocument(final FormKiqClientV1 client, final String siteId,
      final String path, final byte[] content, final String contentType)
      throws IOException, URISyntaxException, InterruptedException {
    // given
    final int status = 200;
    GetDocumentUploadRequest request =
        new GetDocumentUploadRequest().siteId(siteId).path(path).contentLength(content.length);

    // when
    HttpResponse<String> response = client.getDocumentUploadAsHttpResponse(request);

    // then
    if (response.statusCode() == status) {
      Map<String, Object> map = toMap(response);
      String s3url = map.get("url").toString();
      http.send(HttpRequest.newBuilder(new URI(s3url)).header("Content-Type", contentType)
          .method("PUT", BodyPublishers.ofByteArray(content)).build(), BodyHandlers.ofString());
      return map.get("documentId").toString();
    }

    throw new IOException("unexpected response " + response.statusCode());
  }

  /**
   * Add Document Tag.
   * 
   * @param apiClient {@link ApiClient}
   * @param siteId {@link String}
   * @param documentId {@link String}
   * @param key {@link String}
   * @param value {@link String}
   * @throws ApiException ApiException
   */
  public static void addDocumentTag(final ApiClient apiClient, final String siteId,
      final String documentId, final String key, final String value) throws ApiException {

    DocumentTagsApi api = new DocumentTagsApi(apiClient);
    AddDocumentTagsRequest req = new AddDocumentTagsRequest()
        .addTagsItem(new com.formkiq.client.model.AddDocumentTag().key(key).value(value));
    api.addDocumentTags(documentId, req, siteId, "true");
  }

  /**
   * Add Document with Actions.
   * 
   * @param client {@link FormKiqClient}
   * @param siteId {@link String}
   * @param path {@link String}
   * @param content {@link String}
   * @param contentType {@link String}
   * @param actions {@link List} {@link AddDocumentAction}
   * @param tags {@link List} {@link DocumentTag}
   * @return {@link String}
   * @throws IOException IOException
   * @throws InterruptedException InterruptedException
   */
  public static String addDocumentWithActions(final FormKiqClient client, final String siteId,
      final String path, final byte[] content, final String contentType,
      final List<AddDocumentAction> actions, final List<AddDocumentTag> tags)
      throws IOException, InterruptedException {

    String base64 = Base64.getEncoder().encodeToString(content);

    return client
        .addDocument(new AddDocumentRequest().siteId(siteId).document(new AddDocument().path(path)
            .contentAsBase64(base64).contentType(contentType).tags(tags).actions(actions)))
        .documentId();
  }

  /**
   * Add Document with Actions.
   * 
   * @param client {@link FormKiqClient}
   * @param siteId {@link String}
   * @param path {@link String}
   * @param content {@link String}
   * @param contentType {@link String}
   * @param actions {@link List} {@link AddDocumentAction}
   * @param tags {@link List} {@link DocumentTag}
   * @return {@link String}
   * @throws IOException IOException
   * @throws InterruptedException InterruptedException
   */
  public static String addDocumentWithActions(final FormKiqClient client, final String siteId,
      final String path, final String content, final String contentType,
      final List<AddDocumentAction> actions, final List<AddDocumentTag> tags)
      throws IOException, InterruptedException {
    return client.addDocument(new AddDocumentRequest().siteId(siteId).document(new AddDocument()
        .path(path).content(content).contentType(contentType).tags(tags).actions(actions)))
        .documentId();
  }

  /**
   * Add Document.
   * 
   * @param apiClient {@link ApiClient}
   * @param siteId {@link String}
   * @param path {@link String}
   * @param content {@link String}
   * @param contentType {@link String}
   * @param tags {@link List} {@link com.formkiq.client.model.AddDocumentTag}
   * @return {@link String}
   * @throws ApiException ApiException
   */
  public static String addDocumentWithTags(final ApiClient apiClient, final String siteId,
      final String path, final String content, final String contentType,
      final List<com.formkiq.client.model.AddDocumentTag> tags) throws ApiException {

    DocumentsApi api = new DocumentsApi(apiClient);
    com.formkiq.client.model.AddDocumentRequest req =
        new com.formkiq.client.model.AddDocumentRequest().content(content).contentType(contentType)
            .path(path).tags(tags);
    AddDocumentResponse response = api.addDocument(req, siteId, null);
    return response.getDocumentId();
  }

  /**
   * Convert {@link HttpResponse} to {@link Map}.
   * 
   * @param response {@link HttpResponse}
   * @return {@link Map}
   * @throws IOException IOException
   */
  @SuppressWarnings("unchecked")
  public static Map<String, Object> toMap(final HttpResponse<String> response) throws IOException {
    Map<String, Object> m = gson.fromJson(response.body(), Map.class);
    return m;
  }

  /**
   * Wait for Actions to Complete.
   * 
   * @param client {@link ApiClient}
   * @param siteId {@link String}
   * @param documentId {@link String}
   * @param actionStatus {@link String}
   * @return {@link GetDocumentActionsResponse}
   * @throws ApiException ApiException
   * @throws InterruptedException InterruptedException
   */
  public static GetDocumentActionsResponse waitForAction(final ApiClient client,
      final String siteId, final String documentId, final String actionStatus)
      throws ApiException, InterruptedException {

    GetDocumentActionsResponse response = null;
    DocumentActionsApi api = new DocumentActionsApi(client);

    List<com.formkiq.client.model.DocumentAction> o = Collections.emptyList();

    while (o.isEmpty()) {

      try {
        response = api.getDocumentActions(documentId, siteId, null);

        o = response.getActions().stream()
            .filter(a -> a.getStatus().name().equalsIgnoreCase(actionStatus))
            .collect(Collectors.toList());

      } catch (ApiException e) {
        // ignore
      }

      if (o.isEmpty()) {
        TimeUnit.SECONDS.sleep(1);
      }
    }

    return response;
  }

  /**
   * Wait for Actions to Complete.
   * 
   * @param client {@link ApiClient}
   * @param siteId {@link String}
   * @param documentId {@link String}
   * @param actionStatus {@link String}
   * @return {@link GetDocumentActionsResponse}
   * @throws ApiException ApiException
   * @throws InterruptedException InterruptedException
   */
  public static GetDocumentActionsResponse waitForActions(final ApiClient client,
      final String siteId, final String documentId, final String actionStatus)
      throws ApiException, InterruptedException {

    GetDocumentActionsResponse response = null;
    DocumentActionsApi api = new DocumentActionsApi(client);

    List<com.formkiq.client.model.DocumentAction> o = Collections.emptyList();

    while (o.isEmpty()) {

      try {
        response = api.getDocumentActions(documentId, siteId, null);

        o = response.getActions().stream()
            .filter(a -> a.getStatus().name().equalsIgnoreCase(actionStatus))
            .collect(Collectors.toList());

        if (response.getActions().size() != o.size()) {
          o = Collections.emptyList();
        }

      } catch (ApiException e) {
        // ignore
      }

      if (o.isEmpty()) {
        TimeUnit.SECONDS.sleep(1);
      }
    }

    return response;
  }

  /**
   * Wait for Actions to Complete.
   * 
   * 
   * @param client {@link ApiClient}
   * @param siteId {@link String}
   * @param documentId {@link String}
   * @return {@link GetDocumentActionsResponse}
   * @throws ApiException ApiException
   * @throws InterruptedException InterruptedException
   */
  public static GetDocumentActionsResponse waitForActionsComplete(final ApiClient client,
      final String siteId, final String documentId) throws ApiException, InterruptedException {
    return waitForActions(client, siteId, documentId, "COMPLETE");
  }

  /**
   * Wait for Actions to Complete.
   * 
   * @param client {@link FormKiqClientV1}
   * @param siteId {@link String}
   * @param documentId {@link String}
   * @param actionType {@link DocumentActionType}
   * @throws InterruptedException InterruptedException
   * @throws IOException IOException
   * @deprecated do no used
   */
  @Deprecated
  public static void waitForActionsComplete(final FormKiqClientV1 client, final String siteId,
      final String documentId, final DocumentActionType actionType)
      throws IOException, InterruptedException {

    Optional<DocumentAction> o = Optional.empty();

    while (o.isEmpty()) {

      DocumentActions response = client.getDocumentActions(
          new GetDocumentActionsRequest().siteId(siteId).documentId(documentId));

      o = response.actions().stream().filter(a -> actionType.name().equalsIgnoreCase(a.type()))
          .filter(a -> a.status().equalsIgnoreCase("COMPLETE")).findAny();

      if (o.isEmpty()) {
        TimeUnit.SECONDS.sleep(1);
      }
    }
  }

  /**
   * Wait For Document Content Length.
   * 
   * @param client {@link FormKiqClientV1}
   * @param siteId {@link String}
   * @param documentId {@link String}
   * @return {@link GetDocumentResponse}
   * @throws InterruptedException InterruptedException
   */
  public static GetDocumentResponse waitForDocument(final ApiClient client, final String siteId,
      final String documentId) throws InterruptedException {

    DocumentsApi api = new DocumentsApi(client);

    while (true) {

      try {
        return api.getDocument(documentId, siteId, null);

      } catch (ApiException e) {
        // ignore error
      }

      TimeUnit.SECONDS.sleep(1);
    }
  }

  /**
   * Wait For Document Content.
   * 
   * @param client {@link ApiClient}
   * @param siteId {@link String}
   * @param documentId {@link String}
   * @return {@link GetDocumentContentResponse}
   * @throws InterruptedException InterruptedException
   */
  public static GetDocumentContentResponse waitForDocumentContent(final ApiClient client,
      final String siteId, final String documentId) throws InterruptedException {
    return waitForDocumentContent(client, siteId, documentId, null);
  }

  /**
   * Wait For Document Content.
   * 
   * @param client {@link ApiClient}
   * @param siteId {@link String}
   * @param documentId {@link String}
   * @param content {@link String}
   * @return {@link GetDocumentContentResponse}
   * @throws InterruptedException InterruptedException
   */
  public static GetDocumentContentResponse waitForDocumentContent(final ApiClient client,
      final String siteId, final String documentId, final String content)
      throws InterruptedException {

    DocumentsApi api = new DocumentsApi(client);

    while (true) {

      try {
        GetDocumentContentResponse response =
            api.getDocumentContent(documentId, siteId, null, null);
        if (content == null || (content.equals(response.getContent()))) {
          return response;
        }

      } catch (ApiException e) {
        // ignore error
      }

      TimeUnit.SECONDS.sleep(1);
    }
  }

  /**
   * Wait For Document Content.
   * 
   * @param client {@link FormKiqClientV1}
   * @param siteId {@link String}
   * @param documentId {@link String}
   * @throws IOException IOException
   * @throws InterruptedException InterruptedException
   * @throws URISyntaxException URISyntaxException
   * @deprecated do no used
   */
  @Deprecated
  public static void waitForDocumentContent(final FormKiqClientV1 client, final String siteId,
      final String documentId) throws IOException, InterruptedException, URISyntaxException {

    GetDocumentContentRequest request =
        new GetDocumentContentRequest().siteId(siteId).documentId(documentId);

    while (true) {

      HttpResponse<String> response = client.getDocumentContentAsHttpResponse(request);
      if (STATUS_OK == response.statusCode()) {
        break;
      }

      TimeUnit.SECONDS.sleep(1);
    }
  }

  /**
   * Wait For Document Content.
   * 
   * @param client {@link FormKiqClientV1}
   * @param siteId {@link String}
   * @param documentId {@link String}
   * @param content {@link String}
   * @throws IOException IOException
   * @throws InterruptedException InterruptedException
   * @throws URISyntaxException URISyntaxException
   * @deprecated do no used
   */
  @Deprecated
  public static void waitForDocumentContent(final FormKiqClientV1 client, final String siteId,
      final String documentId, final String content)
      throws IOException, InterruptedException, URISyntaxException {

    GetDocumentContentRequest request =
        new GetDocumentContentRequest().siteId(siteId).documentId(documentId);

    while (true) {

      HttpResponse<String> response = client.getDocumentContentAsHttpResponse(request);
      if (STATUS_OK == response.statusCode() && response.body().contains(content)) {
        break;
      }

      TimeUnit.SECONDS.sleep(1);
    }
  }

  /**
   * Wait For Document Content Length.
   * 
   * @param client {@link FormKiqClientV1}
   * @param siteId {@link String}
   * @param documentId {@link String}
   * @return {@link GetDocumentResponse}
   * @throws InterruptedException InterruptedException
   */
  public static GetDocumentResponse waitForDocumentContentLength(final ApiClient client,
      final String siteId, final String documentId) throws InterruptedException {

    DocumentsApi api = new DocumentsApi(client);

    while (true) {

      try {
        GetDocumentResponse response = api.getDocument(documentId, siteId, null);
        if (response.getContentLength() != null) {
          return response;
        }

      } catch (ApiException e) {
        // ignore error
      }

      TimeUnit.SECONDS.sleep(1);
    }
  }

  /**
   * Wait For Document Content.
   * 
   * @param client {@link ApiClient}
   * @param siteId {@link String}
   * @param documentId {@link String}
   * @return {@link GetDocumentContentResponse}
   * @throws InterruptedException InterruptedException
   */
  public static GetDocumentFulltextResponse waitForDocumentFulltext(final ApiClient client,
      final String siteId, final String documentId) throws InterruptedException {

    AdvancedDocumentSearchApi api = new AdvancedDocumentSearchApi(client);

    while (true) {

      try {
        return api.getDocumentFulltext(documentId, siteId, null);

      } catch (ApiException e) {
        // ignore error
      }

      TimeUnit.SECONDS.sleep(1);
    }
  }

  /**
   * Wait For Document Content.
   * 
   * @param client {@link ApiClient}
   * @param siteId {@link String}
   * @param documentId {@link String}
   * @param content {@link String}
   * @return {@link GetDocumentContentResponse}
   * @throws InterruptedException InterruptedException
   */
  public static GetDocumentFulltextResponse waitForDocumentFulltext(final ApiClient client,
      final String siteId, final String documentId, final String content)
      throws InterruptedException {

    GetDocumentFulltextResponse response = null;
    AdvancedDocumentSearchApi api = new AdvancedDocumentSearchApi(client);

    while (true) {

      try {
        response = api.getDocumentFulltext(documentId, siteId, null);
        if (content.equals(response.getContent())) {
          break;
        }

      } catch (ApiException e) {
        // ignore error
      }

      TimeUnit.SECONDS.sleep(1);
    }

    return response;
  }

  /**
   * Wait for Document Tag.
   * 
   * @param apiClient {@link ApiClient}
   * @param siteId {@link String}
   * @param documentId {@link String}
   * @param tagKey {@link String}
   * @return {@link GetDocumentTagResponse}
   * @throws InterruptedException InterruptedException
   */
  public static GetDocumentTagResponse waitForDocumentTag(final ApiClient apiClient,
      final String siteId, final String documentId, final String tagKey)
      throws InterruptedException {

    DocumentTagsApi api = new DocumentTagsApi(apiClient);

    while (true) {

      try {
        return api.getDocumentTag(documentId, tagKey, siteId, null);
      } catch (ApiException e) {
        // tag not found
      }

      TimeUnit.SECONDS.sleep(1);
    }
  }

  /**
   * Wait For Document Content.
   * 
   * @param client {@link FormKiqClientV1}
   * @param siteId {@link String}
   * @param documentId {@link String}
   * @param tagKey {@link String}
   * @return {@link DocumentTag}
   * @throws IOException IOException
   * @throws InterruptedException InterruptedException
   * @throws URISyntaxException URISyntaxException
   */
  public static DocumentTag waitForDocumentTag(final FormKiqClientV1 client, final String siteId,
      final String documentId, final String tagKey)
      throws IOException, InterruptedException, URISyntaxException {

    DocumentTag tags = null;
    GetDocumentTagsKeyRequest tagReq =
        new GetDocumentTagsKeyRequest().siteId(siteId).documentId(documentId).tagKey(tagKey);

    while (true) {

      try {
        tags = client.getDocumentTag(tagReq);
        break;
      } catch (IOException e) {
        // tag not found
      }

      TimeUnit.SECONDS.sleep(1);
    }

    return tags;
  }

  /**
   * Fetch Document Content Type.
   * 
   * @param client {@link FormKiqClientV1}
   * @param siteId {@link String}
   * @param documentId {@link String}
   * @throws IOException IOException
   * @throws InterruptedException InterruptedException
   * @throws URISyntaxException URISyntaxException
   * @deprecated do no used
   */
  @SuppressWarnings("unchecked")
  @Deprecated
  public void waitForDocumentContentType(final FormKiqClientV1 client, final String siteId,
      final String documentId) throws IOException, InterruptedException, URISyntaxException {

    GetDocumentRequest request = new GetDocumentRequest().siteId(siteId).documentId(documentId);

    while (true) {

      HttpResponse<String> response = client.getDocumentAsHttpResponse(request);
      if (STATUS_OK == response.statusCode()) {
        Map<String, Object> map = gson.fromJson(response.body(), Map.class);
        String contentType = (String) map.get("contentType");
        if (!StringUtils.isEmpty(contentType)) {
          break;
        }
      }

      TimeUnit.SECONDS.sleep(1);
    }
  }
}
