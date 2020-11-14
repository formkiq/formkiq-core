/**
 *
 * FormKiQ License
 *
 * Copyright (c) 2018 FormKiQ, INC
 * 
 * This code is the property of FormKiQ, INC. In the Software Development Agreement signed by both
 * FormKiQ and your company, FormKiQ grants you a limited license to use, modify, and create
 * derivative works of this code. Please consult the Software Development Agreement for the complete
 * terms under which you may use this code.
 *
 */
package com.formkiq.stacks.api;

import java.util.HashMap;
import java.util.Map;
import com.formkiq.stacks.api.handler.DocumentIdContentGetRequestHandler;
import com.formkiq.stacks.api.handler.DocumentIdFormatsPostRequestHandler;
import com.formkiq.stacks.api.handler.DocumentIdGetRequestHandler;
import com.formkiq.stacks.api.handler.DocumentIdPostRequestHandler;
import com.formkiq.stacks.api.handler.DocumentIdUrlGetRequestHandler;
import com.formkiq.stacks.api.handler.DocumentSignedUrlRequestHandler;
import com.formkiq.stacks.api.handler.DocumentTagDeleteRequestHandler;
import com.formkiq.stacks.api.handler.DocumentTagGetRequestHandler;
import com.formkiq.stacks.api.handler.DocumentTagPostRequestHandler;
import com.formkiq.stacks.api.handler.DocumentTagPutRequestHandler;
import com.formkiq.stacks.api.handler.DocumentTagsGetRequestHandler;
import com.formkiq.stacks.api.handler.DocumentVersionsRequestHandler;
import com.formkiq.stacks.api.handler.DocumentsDeleteRequestHandler;
import com.formkiq.stacks.api.handler.DocumentsGetRequestHandler;
import com.formkiq.stacks.api.handler.DocumentsOptionsRequestHandler;
import com.formkiq.stacks.api.handler.DocumentsRestrictionsMaxContentLength;
import com.formkiq.stacks.api.handler.DocumentsRestrictionsMaxDocuments;
import com.formkiq.stacks.api.handler.PresetsGetRequestHandler;
import com.formkiq.stacks.api.handler.PresetsPostRequestHandler;
import com.formkiq.stacks.api.handler.PresetsTagsRequestHandler;
import com.formkiq.stacks.api.handler.RequestHandler;
import com.formkiq.stacks.api.handler.SearchRequestHandler;
import com.formkiq.stacks.api.handler.SitesRequestHandler;
import com.formkiq.stacks.api.handler.VersionRequestHandler;

/**
 * 
 * Maps Method/Url to Class.
 *
 */
public class ApiRequestMapper {

  /** {@link Map} of {@link RequestHandler}. */
  private static Map<String, RequestHandler> requestHandlers;
  /** {@link DocumentsRestrictionsMaxDocuments}. */
  private static final DocumentsRestrictionsMaxDocuments RESTRICTIONS_MAX_DOCUMENTS =
      new DocumentsRestrictionsMaxDocuments();
  /** {@link DocumentsRestrictionsMaxContentLength}. */
  private static final DocumentsRestrictionsMaxContentLength RESTRICTIONS_MAX_CONTENT_LENGTH =
      new DocumentsRestrictionsMaxContentLength();
  /** {@link DocumentIdPostRequestHandler}. */
  private static final DocumentIdPostRequestHandler POST_HANDLER =
      new DocumentIdPostRequestHandler(RESTRICTIONS_MAX_DOCUMENTS);
  /** {@link DocumentIdUrlGetRequestHandler}. */
  private static final DocumentIdUrlGetRequestHandler CONTENT_HANDLER =
      new DocumentIdUrlGetRequestHandler();
  /** {@link DocumentIdContentGetRequestHandler}. */
  private static final DocumentIdContentGetRequestHandler CONTENT_GET_HANDLER =
      new DocumentIdContentGetRequestHandler();

  /**
   * Find {@link RequestHandler} from {@link ApiGatewayRequestEvent}.
   * 
   * @param event {@link ApiGatewayRequestEvent}
   * @return {@link RequestHandler}
   */
  public static RequestHandler findHandler(final ApiGatewayRequestEvent event) {

    String method = event.getHttpMethod().toLowerCase();
    String resource = event.getResource();

    RequestHandler handler = requestHandlers.containsKey(method + "_" + resource)
        ? requestHandlers.get(method + "_" + resource)
        : requestHandlers.get(method);

    return handler;
  }

  /**
   * Init Api Request Mapper.
   * 
   * @param map Environment variables {@link Map}
   * @return {@link Map}
   * 
   */
  public static Map<String, RequestHandler> init(final Map<String, String> map) {

    requestHandlers = new HashMap<>();
    requestHandlers.put("options", new DocumentsOptionsRequestHandler());
    requestHandlers.put("get_/version", new VersionRequestHandler());

    requestHandlers.put("get_/sites", new SitesRequestHandler());
    requestHandlers.put("post_/documents", POST_HANDLER);
    requestHandlers.put("patch_/documents/{documentId}", POST_HANDLER);

    requestHandlers.put("get_/documents/{documentId}/versions",
        new DocumentVersionsRequestHandler());

    requestHandlers.put("post_/documents/{documentId}/formats",
        new DocumentIdFormatsPostRequestHandler(CONTENT_HANDLER));

    requestHandlers.put("post_/documents/{documentId}/tags", new DocumentTagPostRequestHandler());

    requestHandlers.put("get_/documents/{documentId}/tags/{tagKey}",
        new DocumentTagGetRequestHandler());

    requestHandlers.put("put_/documents/{documentId}/tags/{tagKey}",
        new DocumentTagPutRequestHandler());

    requestHandlers.put("delete_/documents/{documentId}/tags/{tagKey}",
        new DocumentTagDeleteRequestHandler());

    requestHandlers.put("get_/presets", new PresetsGetRequestHandler());
    PresetsPostRequestHandler presetsPostRequestHandler = new PresetsPostRequestHandler();
    requestHandlers.put("post_/presets", presetsPostRequestHandler);
    requestHandlers.put("delete_/presets/{presetId}", presetsPostRequestHandler);

    PresetsTagsRequestHandler presetsTagsRequestHander = new PresetsTagsRequestHandler();
    requestHandlers.put("get_/presets/{presetId}/tags", presetsTagsRequestHander);
    requestHandlers.put("post_/presets/{presetId}/tags", presetsTagsRequestHander);
    requestHandlers.put("patch_/presets/{presetId}/tags", presetsTagsRequestHander);
    requestHandlers.put("delete_/presets/{presetId}/tags/{tagKey}", presetsTagsRequestHander);

    requestHandlers.put("get_/documents", new DocumentsGetRequestHandler());
    requestHandlers.put("get_/documents/{documentId}", new DocumentIdGetRequestHandler());

    requestHandlers.put("delete_/documents/{documentId}", new DocumentsDeleteRequestHandler());

    requestHandlers.put("get_/documents/{documentId}/tags", new DocumentTagsGetRequestHandler());

    requestHandlers.put("get_/documents/{documentId}/url", CONTENT_HANDLER);
    requestHandlers.put("get_/documents/{documentId}/content", CONTENT_GET_HANDLER);

    requestHandlers.put("post_/search", new SearchRequestHandler());

    RequestHandler signedHandler = new DocumentSignedUrlRequestHandler(RESTRICTIONS_MAX_DOCUMENTS,
        RESTRICTIONS_MAX_CONTENT_LENGTH);
    requestHandlers.put("get_/documents/upload", signedHandler);
    requestHandlers.put("get_/documents/{documentId}/upload", signedHandler);

    if (isEnablePublicUrls(map)) {
      requestHandlers.put("post_/public/documents", POST_HANDLER);
    }

    return requestHandlers;
  }

  /**
   * Whether to enable public urls.
   * 
   * @param map {@link Map}
   * @return boolean
   */
  protected static boolean isEnablePublicUrls(final Map<String, String> map) {
    return "true".equals(map.getOrDefault("ENABLE_PUBLIC_URLS", "false"));
  }

  /**
   * private constructor.
   */
  private ApiRequestMapper() {}
}
