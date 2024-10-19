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
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import com.formkiq.client.api.AdvancedDocumentSearchApi;
import com.formkiq.client.api.DocumentActionsApi;
import com.formkiq.client.api.DocumentAttributesApi;
import com.formkiq.client.api.DocumentTagsApi;
import com.formkiq.client.api.DocumentVersionsApi;
import com.formkiq.client.api.DocumentsApi;
import com.formkiq.client.invoker.ApiClient;
import com.formkiq.client.invoker.ApiException;
import com.formkiq.client.model.AddAction;
import com.formkiq.client.model.AddDocumentAttribute;
import com.formkiq.client.model.AddDocumentResponse;
import com.formkiq.client.model.AddDocumentTagsRequest;
import com.formkiq.client.model.DocumentAction;
import com.formkiq.client.model.DocumentActionStatus;
import com.formkiq.client.model.DocumentAttribute;
import com.formkiq.client.model.GetDocumentActionsResponse;
import com.formkiq.client.model.GetDocumentContentResponse;
import com.formkiq.client.model.GetDocumentFulltextResponse;
import com.formkiq.client.model.GetDocumentResponse;
import com.formkiq.client.model.GetDocumentTagResponse;
import com.formkiq.client.model.GetDocumentUrlResponse;
import com.formkiq.client.model.GetDocumentVersionsResponse;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import static com.formkiq.aws.dynamodb.objects.Strings.isEmpty;

/**
 * 
 * FormKiQ Documents Service.
 *
 */
public class FkqDocumentService {

  /** {@link Gson}. */
  private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();
  /** {@link HttpClient}. */
  private static final HttpClient HTTP = HttpClient.newHttpClient();

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
        api.getDocumentUpload(path, siteId, null, null, content.length, null, shareKey);
    String s3url = response.getUrl();

    if (content.length > 0) {
      HTTP.send(HttpRequest.newBuilder(new URI(s3url)).header("Content-Type", contentType)
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
   * Add Document.
   * 
   * @param apiClient {@link ApiClient}
   * @param siteId {@link String}
   * @param path {@link String}
   * @param data byte[]
   * @param contentType {@link String}
   * @param actions {@link List} {@link AddAction}
   * @param attributes {@link List} {@link AddDocumentAttribute}
   * @return {@link String}
   * @throws ApiException ApiException
   */
  public static String addDocumentWithAttributes(final ApiClient apiClient, final String siteId,
      final String path, final byte[] data, final String contentType,
      final List<AddDocumentAttribute> attributes, final List<AddAction> actions)
      throws ApiException {

    DocumentsApi api = new DocumentsApi(apiClient);
    String content = Base64.getEncoder().encodeToString(data);
    com.formkiq.client.model.AddDocumentRequest req =
        new com.formkiq.client.model.AddDocumentRequest().content(content).contentType(contentType)
            .isBase64(Boolean.TRUE).path(path).actions(actions).attributes(attributes);
    AddDocumentResponse response = api.addDocument(req, siteId, null);
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
   */
  @SuppressWarnings("unchecked")
  public static Map<String, Object> toMap(final HttpResponse<String> response) {
    return GSON.fromJson(response.body(), Map.class);
  }

  /**
   * Wait for Actions to Complete.
   * 
   * @param client {@link ApiClient}
   * @param siteId {@link String}
   * @param documentId {@link String}
   * @param actionStatus {@link Collection} {@link DocumentActionStatus}
   * @return {@link GetDocumentActionsResponse}
   * @throws InterruptedException InterruptedException
   */
  public static GetDocumentActionsResponse waitForAction(final ApiClient client,
      final String siteId, final String documentId,
      final Collection<DocumentActionStatus> actionStatus) throws InterruptedException {

    GetDocumentActionsResponse response = null;
    DocumentActionsApi api = new DocumentActionsApi(client);

    List<com.formkiq.client.model.DocumentAction> o = Collections.emptyList();

    while (o.isEmpty()) {

      try {
        response = api.getDocumentActions(documentId, siteId, null, null, null);

        o = response.getActions().stream().filter(a -> actionStatus.contains(a.getStatus()))
            .toList();

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
   * @throws InterruptedException InterruptedException
   */
  public static GetDocumentActionsResponse waitForActionsComplete(final ApiClient client,
      final String siteId, final String documentId) throws InterruptedException {
    return waitForActions(client, siteId, documentId, "COMPLETE");
  }

  /**
   * Wait for Actions to Complete.
   * 
   * @param client {@link ApiClient}
   * @param siteId {@link String}
   * @param documentId {@link String}
   * @param actionStatus {@link String}
   * @return {@link GetDocumentActionsResponse}
   * @throws InterruptedException InterruptedException
   * @deprecated use with {@link DocumentActionStatus}
   */
  @Deprecated
  public static GetDocumentActionsResponse waitForActions(final ApiClient client,
      final String siteId, final String documentId, final String actionStatus)
      throws InterruptedException {
    return waitForActions(client, siteId, documentId,
        List.of(DocumentActionStatus.valueOf(actionStatus.toUpperCase())));
  }

  /**
   * Wait for Actions to Complete.
   *
   * @param client {@link ApiClient}
   * @param siteId {@link String}
   * @param documentId {@link String}
   * @param actionStatus {@link String}
   * @return {@link GetDocumentActionsResponse}
   * @throws InterruptedException InterruptedException
   */
  public static GetDocumentActionsResponse waitForActions(final ApiClient client,
      final String siteId, final String documentId,
      final Collection<DocumentActionStatus> actionStatus) throws InterruptedException {

    GetDocumentActionsResponse response = null;
    DocumentActionsApi api = new DocumentActionsApi(client);

    List<com.formkiq.client.model.DocumentAction> o = Collections.emptyList();

    while (o.isEmpty()) {

      try {
        response = api.getDocumentActions(documentId, siteId, null, null, null);

        List<DocumentAction> actions = response.getActions();
        o = actions.stream().filter(a -> actionStatus.contains(a.getStatus()))
            .collect(Collectors.toList());

        if (actions.size() != o.size()) {
          o = Collections.emptyList();

          if (actions.stream().anyMatch(a -> DocumentActionStatus.FAILED.equals(a.getStatus()))) {
            throw new InterruptedException(
                "Found " + DocumentActionStatus.FAILED.name() + " action");
          }
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
   * @param client {@link ApiClient}
   * @param siteId {@link String}
   * @param documentId {@link String}
   * @param actionStatus {@link String}
   * @return {@link GetDocumentActionsResponse}
   * @throws ApiException ApiException
   * @throws InterruptedException InterruptedException
   */
  public static GetDocumentActionsResponse waitForActionsWithRetry(final ApiClient client,
      final String siteId, final String documentId,
      final Collection<DocumentActionStatus> actionStatus)
      throws ApiException, InterruptedException {
    GetDocumentActionsResponse response;

    try {
      response = waitForActions(client, siteId, documentId, actionStatus);
    } catch (InterruptedException e) {

      if (e.getMessage().contains(DocumentActionStatus.FAILED.name())) {

        DocumentActionsApi api = new DocumentActionsApi(client);
        api.addDocumentRetryAction(documentId, siteId);
        TimeUnit.SECONDS.sleep(1);
        response = waitForActions(client, siteId, documentId, actionStatus);

      } else {
        throw e;
      }
    }

    return response;
  }

  /**
   * Wait For Document Content Length.
   * 
   * @param client {@link ApiClient}
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
    return waitForDocumentContent(client, siteId, documentId, null, content);
  }

  /**
   * Wait For Document Content.
   *
   * @param client {@link ApiClient}
   * @param siteId {@link String}
   * @param documentId {@link String}
   * @param content {@link String}
   * @param versionKey {@link String}
   * @return {@link GetDocumentContentResponse}
   * @throws InterruptedException InterruptedException
   */
  public static GetDocumentContentResponse waitForDocumentContent(final ApiClient client,
      final String siteId, final String documentId, final String versionKey, final String content)
      throws InterruptedException {

    DocumentsApi api = new DocumentsApi(client);

    while (true) {

      try {
        GetDocumentContentResponse response =
            api.getDocumentContent(documentId, siteId, versionKey, null);

        if (content == null || content.equals(response.getContent())
            || !isEmpty(response.getContentUrl())) {
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
   * @param contentType {@link String}
   * @return {@link GetDocumentContentResponse}
   * @throws InterruptedException InterruptedException
   */
  public static GetDocumentContentResponse waitForDocumentContentType(final ApiClient client,
      final String siteId, final String documentId, final String contentType)
      throws InterruptedException {

    DocumentsApi api = new DocumentsApi(client);

    while (true) {

      try {
        GetDocumentContentResponse response =
            api.getDocumentContent(documentId, siteId, null, null);
        if ((contentType.equals(response.getContentType()))) {
          return response;
        }

      } catch (ApiException e) {
        // ignore error
      }

      TimeUnit.SECONDS.sleep(1);
    }
  }

  /**
   * Wait For Document Content Length.
   * 
   * @param client {@link ApiClient}
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

    GetDocumentFulltextResponse response;
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
   * Get Document Attribute.
   * 
   * @param client {@link ApiClient}
   * @param siteId {@link String}
   * @param documentId {@link String}
   * @param attributeKey {@link String}
   * @return DocumentAttribute
   * @throws ApiException ApiException
   */
  public static DocumentAttribute getDocumentAttribute(final ApiClient client, final String siteId,
      final String documentId, final String attributeKey) throws ApiException {
    DocumentAttributesApi documentAttributesApi = new DocumentAttributesApi(client);
    return documentAttributesApi.getDocumentAttribute(documentId, attributeKey, siteId)
        .getAttribute();
  }

  /**
   * Get Document Attribute.
   *
   * @param client {@link ApiClient}
   * @param siteId {@link String}
   * @param documentId {@link String}
   * @return {@link List} {@link DocumentAttribute}
   * @throws ApiException ApiException
   */
  public static List<DocumentAttribute> getDocumentAttributes(final ApiClient client,
      final String siteId, final String documentId) throws ApiException {
    DocumentAttributesApi documentAttributesApi = new DocumentAttributesApi(client);
    return documentAttributesApi.getDocumentAttributes(documentId, siteId, null, null)
        .getAttributes();
  }

  /**
   * Wait For Document Versions.
   *
   * @param client {@link ApiClient}
   * @param siteId {@link String}
   * @param documentId {@link String}
   * @param expectedNumbeOfVersions int
   * @return {@link GetDocumentContentResponse}
   * @throws InterruptedException InterruptedException
   */
  public static GetDocumentVersionsResponse waitForDocumentVersions(final ApiClient client,
      final String siteId, final String documentId, final int expectedNumbeOfVersions)
      throws InterruptedException {

    DocumentVersionsApi api = new DocumentVersionsApi(client);

    while (true) {

      try {
        GetDocumentVersionsResponse response =
            api.getDocumentVersions(documentId, siteId, "100", null, null);
        if (response.getDocuments().size() == expectedNumbeOfVersions) {
          return response;
        }

      } catch (ApiException e) {
        // ignore error
      }

      TimeUnit.SECONDS.sleep(1);
    }
  }

  /**
   * Wait for Document Attribute.
   *
   * @param client {@link ApiClient}
   * @param siteId {@link String}
   * @param documentId {@link String}
   * @param attributeKey {@link String}
   * @return DocumentAttribute
   * @throws InterruptedException InterruptedException
   */
  public static DocumentAttribute waitForDocumentAtrribute(final ApiClient client,
      final String siteId, final String documentId, final String attributeKey)
      throws InterruptedException {
    DocumentAttributesApi documentAttributesApi = new DocumentAttributesApi(client);

    while (true) {

      try {
        return documentAttributesApi.getDocumentAttribute(documentId, attributeKey, siteId)
            .getAttribute();
      } catch (ApiException e) {
        // ignore
      }

      TimeUnit.SECONDS.sleep(1);
    }
  }
}
