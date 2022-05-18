package com.formkiq.testutils.aws;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.Map;
import com.formkiq.stacks.client.FormKiqClientV1;
import com.formkiq.stacks.client.requests.GetDocumentUploadRequest;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * 
 * FormKiQ Documents Service.
 *
 */
public class FkqDocumentService {

  /** {@link HttpClient}. */
  private HttpClient http = HttpClient.newHttpClient();
  /** {@link Gson}. */
  private Gson gson = new GsonBuilder().disableHtmlEscaping().create();

  
  /**
   * Add "file" but this just creates DynamoDB record and not the S3 file.
   * 
   * @param client {@link FormKiqClientV1}
   * @param siteId {@link String}
   * @param content byte[]
   * @param contentType {@link String}
   * @return {@link String}
   * @throws IOException IOException
   * @throws URISyntaxException URISyntaxException
   * @throws InterruptedException InterruptedException
   */
  public String addDocument(final FormKiqClientV1 client, final String siteId, final byte[] content,
      final String contentType) throws IOException, URISyntaxException, InterruptedException {
    // given
    final int status = 200;
    GetDocumentUploadRequest request =
        new GetDocumentUploadRequest().siteId(siteId).contentLength(content.length);

    // when
    HttpResponse<String> response = client.getDocumentUploadAsHttpResponse(request);

    // then
    if (response.statusCode() == status) {
      Map<String, Object> map = toMap(response);
      String s3url = map.get("url").toString();
      this.http.send(HttpRequest.newBuilder(new URI(s3url)).header("Content-Type", contentType)
          .method("PUT", BodyPublishers.ofByteArray(content)).build(), BodyHandlers.ofString());
      return map.get("documentId").toString();
    }
    
    throw new IOException("unexpected response " + response.statusCode());
  }
  
  /**
   * Convert {@link HttpResponse} to {@link Map}.
   * 
   * @param response {@link HttpResponse}
   * @return {@link Map}
   * @throws IOException IOException
   */
  @SuppressWarnings("unchecked")
  protected Map<String, Object> toMap(final HttpResponse<String> response) throws IOException {
    Map<String, Object> m = this.gson.fromJson(response.body(), Map.class);
    return m;
  }
}
