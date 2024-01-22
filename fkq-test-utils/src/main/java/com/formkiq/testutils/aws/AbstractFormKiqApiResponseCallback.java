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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;
import org.mockserver.mock.action.ExpectationResponseCallback;
import org.mockserver.model.Header;
import org.mockserver.model.Headers;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * 
 * FormKiqApi implementation of {@link ExpectationResponseCallback}.
 *
 */
public abstract class AbstractFormKiqApiResponseCallback implements ExpectationResponseCallback {

  /** {@link Random}. */
  private static final Random NUM_RAND = new Random();
  /** {@link Context}. */
  private Context context = new LambdaContextRecorder();
  /** {@link Gson}. */
  private Gson gson = new GsonBuilder().disableHtmlEscaping().create();
  /** Port to run Test server. */
  private int port = -1;

  /**
   * constructor.
   */
  public AbstractFormKiqApiResponseCallback() {
    this.port = generatePort();
  }

  /**
   * Create {@link HttpResponse} from {@link String} response.
   * 
   * @param response {@link String}
   * @return {@link HttpResponse}
   */
  @SuppressWarnings("unchecked")
  private HttpResponse createResponse(final String response) {

    Map<String, Object> map = this.gson.fromJson(response, Map.class);
    Map<String, String> headerList = (Map<String, String>) map.get("headers");
    List<Header> headers = headerList.entrySet().stream()
        .map(e -> new Header(e.getKey(), Arrays.asList(e.getValue()))).collect(Collectors.toList());

    int statusCode = ((Double) map.get("statusCode")).intValue();
    return HttpResponse.response().withHeaders(new Headers(headers))
        .withStatusCode(Integer.valueOf(statusCode)).withBody(map.get("body").toString());
  }

  private int generatePort() {
    final int topPort = 8000;
    final int bottomPort = 7000;
    return NUM_RAND.nextInt(topPort - bottomPort) + bottomPort;
  }

  /**
   * Get {@link RequestStreamHandler}.
   * 
   * @return {@link RequestStreamHandler}
   */
  public abstract RequestStreamHandler getHandler();

  /**
   * Get Lambda {@link Map} environment.
   * 
   * @return {@link Map}
   * @throws URISyntaxException URISyntaxException
   */
  public abstract Map<String, String> getMapEnvironment() throws URISyntaxException;

  /**
   * Get Resource Urls.
   * 
   * @return {@link Collection} {@link String}
   */
  public abstract Collection<String> getResourceUrls();

  /**
   * Get Server Port.
   * 
   * @return int
   */
  public int getServerPort() {
    return this.port;
  }

  /**
   * Handle transforming {@link HttpRequest} to {@link HttpResponse}.
   */
  @Override
  public HttpResponse handle(final HttpRequest httpRequest) throws Exception {

    initHandler();

    ApiHttpRequest event = new HttpRequestToApiHttpRequest(getResourceUrls()).apply(httpRequest);

    String s = this.gson.toJson(event);

    InputStream is = new ByteArrayInputStream(s.getBytes(StandardCharsets.UTF_8));
    ByteArrayOutputStream outstream = new ByteArrayOutputStream();
    getHandler().handleRequest(is, outstream, this.context);

    String response = new String(outstream.toByteArray(), "UTF-8");
    return createResponse(response);
  }

  /**
   * Initialize Handler.
   */
  public abstract void initHandler();
}
