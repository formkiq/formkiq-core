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

import static com.formkiq.stacks.dynamodb.DocumentService.MAX_RESULTS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import com.formkiq.lambda.apigateway.ApiGatewayRequestEvent;
import com.formkiq.lambda.apigateway.util.GsonUtil;
import com.formkiq.stacks.api.handler.PresetTypes;

/** Unit Tests for request GET / POS /presets. */
public class ApiDocumentsPresetsRequestTest extends AbstractRequestHandler {

  /**
   * before.
   * 
   * @throws Exception Exception
   */
  @Override
  @Before
  public void before() throws Exception {
    super.before();
    getDocumentService().deletePresets(null, PresetTypes.TAGGING.name());
  }

  /**
   * Get /presets.
   *
   * @throws Exception an error has occurred
   */
  @SuppressWarnings("unchecked")
  @Test
  public void testGetPresets01() throws Exception {
    // given
    final int count = 15;

    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {

      newOutstream();
      for (int i = 0; i < count; i++) {
        testSavePreset(siteId);
      }

      ApiGatewayRequestEvent event = toRequestEvent("get", "/presets", null);

      // when
      String response = handleRequest(event);

      // then
      Map<String, String> m = GsonUtil.getInstance().fromJson(response, Map.class);

      final int mapsize = 3;
      assertEquals(mapsize, m.size());
      assertEquals("200.0", String.valueOf(m.get("statusCode")));
      assertEquals(getHeaders(), "\"headers\":" + GsonUtil.getInstance().toJson(m.get("headers")));

      Map<String, Object> resp = GsonUtil.getInstance().fromJson(m.get("body"), Map.class);

      assertNotNull(resp.get("next"));
      assertNull(resp.get("previous"));

      List<Map<String, Object>> presets = (List<Map<String, Object>>) resp.get("presets");
      assertEquals(MAX_RESULTS, presets.size());

      Map<String, Object> a = presets.get(0);
      assertNotNull(a.get("id"));
      assertNotNull(a.get("insertedDate"));
      assertEquals("test", a.get("name"));
      assertEquals("default", a.get("siteId"));
      assertEquals("TAGGING", a.get("type"));
      assertNull(a.get("userId"));

      // given
      newOutstream();
      addParameter(event, "next", resp.get("next").toString());

      // when
      response = handleRequest(event);

      // then
      m = GsonUtil.getInstance().fromJson(response, Map.class);
      resp = GsonUtil.getInstance().fromJson(m.get("body"), Map.class);
      assertNull(resp.get("next"));
      assertNotNull(resp.get("previous"));

      presets = (List<Map<String, Object>>) resp.get("presets");
      assertEquals(count - MAX_RESULTS, presets.size());
    }
  }

  /**
   * Get Presets.
   * 
   * @param siteId {@link String}
   * @return {@link LinkageError} {@link Map}.
   * @throws IOException IOException
   */
  @SuppressWarnings("unchecked")
  private List<Map<String, Object>> getPresets(final String siteId) throws IOException {
    newOutstream();

    ApiGatewayRequestEvent event = toRequestEvent("get", "/presets", null);
    addParameter(event, "siteId", siteId);

    String response = handleRequest(event);
    Map<String, String> m = GsonUtil.getInstance().fromJson(response, Map.class);
    Map<String, Object> resp = GsonUtil.getInstance().fromJson(m.get("body"), Map.class);
    List<Map<String, Object>> presets = (List<Map<String, Object>>) resp.get("presets");

    newOutstream();

    return presets;
  }

  /**
   * Delete /presets.
   *
   * @throws Exception an error has occurred
   */
  @SuppressWarnings("unchecked")
  @Test
  public void testDeletePresets01() throws Exception {
    // given
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {

      newOutstream();

      String id = testSavePreset(siteId).get("id");
      assertEquals(1, getPresets(siteId).size());

      ApiGatewayRequestEvent event = toRequestEvent("delete", "/presets/{presetId}", null);
      setPathParameter(event, "presetId", id);
      addParameter(event, "siteId", siteId);

      // when
      String response = handleRequest(event);

      // then
      assertEquals(0, getPresets(siteId).size());


      Map<String, String> m = GsonUtil.getInstance().fromJson(response, Map.class);

      final int mapsize = 3;
      assertEquals(mapsize, m.size());
      assertEquals("200.0", String.valueOf(m.get("statusCode")));
      assertEquals(getHeaders(), "\"headers\":" + GsonUtil.getInstance().toJson(m.get("headers")));

      Map<String, Object> resp = GsonUtil.getInstance().fromJson(m.get("body"), Map.class);
      assertEquals("{message=Removed preset '" + id + "'}", resp.toString());
    }
  }

  /**
   * Post /presets, bad request.
   *
   * @throws Exception an error has occurred
   */
  @SuppressWarnings("unchecked")
  @Test
  public void testPostPresets01() throws Exception {
    // given
    ApiGatewayRequestEvent event = toRequestEvent("post", "/presets", "asd");

    // when
    String response = handleRequest(event);

    // then
    Map<String, String> m = GsonUtil.getInstance().fromJson(response, Map.class);

    final int mapsize = 3;
    assertEquals(mapsize, m.size());
    assertEquals("400.0", String.valueOf(m.get("statusCode")));
    assertEquals(getHeaders(), "\"headers\":" + GsonUtil.getInstance().toJson(m.get("headers")));

    assertEquals("{\"message\":\"invalid JSON body\"}", m.get("body").toString());
  }

  /**
   * Save Preset.
   * 
   * @param siteId {@link String}
   * @return {@link Map}
   * @throws IOException IOException
   */
  @SuppressWarnings("unchecked")
  private Map<String, String> testSavePreset(final String siteId) throws IOException {
    // given
    Map<String, Object> map = Map.of("name", "test", "type", PresetTypes.TAGGING.name(), "tags",
        Arrays.asList(Map.of("key", "foobar")));

    ApiGatewayRequestEvent event = toRequestEvent("post", "/presets", map);
    addParameter(event, "siteId", siteId);

    // when
    String response = handleRequest(event);

    // then
    Map<String, String> m = GsonUtil.getInstance().fromJson(response, Map.class);

    final int mapsize = 3;
    assertEquals(mapsize, m.size());
    assertEquals("201.0", String.valueOf(m.get("statusCode")));
    assertEquals(getHeaders(), "\"headers\":" + GsonUtil.getInstance().toJson(m.get("headers")));
    Map<String, String> resp = GsonUtil.getInstance().fromJson(m.get("body"), Map.class);
    assertEquals(siteId != null ? siteId : "default", resp.get("siteId"));
    assertNotNull(resp.get("id"));

    newOutstream();

    return resp;
  }

  /**
   * Post Preset Tag.
   * 
   * @param siteId {@link String}
   * @param id {@link String}
   * @param tagKey {@link String}
   * @throws IOException IOException
   */
  @SuppressWarnings("unchecked")
  private void postPresetTag(final String siteId, final String id, final String tagKey)
      throws IOException {
    // given
    newOutstream();
    Map<String, Object> map = Map.of("key", tagKey);

    ApiGatewayRequestEvent event = toRequestEvent("post", "/presets/{presetId}/tags", map);
    setPathParameter(event, "presetId", id);
    addParameter(event, "siteId", siteId);

    // when
    String response = handleRequest(event);

    // then
    Map<String, String> m = GsonUtil.getInstance().fromJson(response, Map.class);

    final int mapsize = 3;
    assertEquals(mapsize, m.size());
    assertEquals("201.0", String.valueOf(m.get("statusCode")));
    assertEquals(getHeaders(), "\"headers\":" + GsonUtil.getInstance().toJson(m.get("headers")));

    newOutstream();
  }

  /**
   * Delete Preset Tag.
   * 
   * @param siteId {@link String}
   * @param id {@link String}
   * @param tagKey {@link String}
   * @throws IOException IOException
   */
  @SuppressWarnings("unchecked")
  private void deletePresetTag(final String siteId, final String id, final String tagKey)
      throws IOException {
    // given
    newOutstream();
    Map<String, Object> map = Map.of("tags", Arrays.asList(Map.of("key", tagKey)));

    ApiGatewayRequestEvent event =
        toRequestEvent("delete", "/presets/{presetId}/tags/{tagKey}", map);
    setPathParameter(event, "presetId", id);
    setPathParameter(event, "tagKey", tagKey);
    addParameter(event, "siteId", siteId);

    // when
    String response = handleRequest(event);

    // then
    Map<String, String> m = GsonUtil.getInstance().fromJson(response, Map.class);

    final int mapsize = 3;
    assertEquals(mapsize, m.size());
    assertEquals("200.0", String.valueOf(m.get("statusCode")));
    assertEquals(getHeaders(), "\"headers\":" + GsonUtil.getInstance().toJson(m.get("headers")));
  }

  /**
   * Get /presets/{presetId}/tags.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testGetPresetTags01() throws Exception {
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
      // given
      newOutstream();
      String id = testSavePreset(siteId).get("id");
      postPresetTag(siteId, id, "First Name");

      List<Map<String, Object>> tags = getPresetTags(siteId, id);
      assertEquals(1, tags.size());
      assertEquals("First Name", tags.get(0).get("key"));

      postPresetTag(siteId, id, "Last Name!");
      tags = getPresetTags(siteId, id);
      assertEquals(2, tags.size());
      assertEquals("First Name", tags.get(0).get("key"));
      assertEquals("Last Name!", tags.get(1).get("key"));

      deletePresetTag(siteId, id, "First Name");
      tags = getPresetTags(siteId, id);
      assertEquals(1, tags.size());
      assertEquals("Last Name!", tags.get(0).get("key"));
    }
  }

  /**
   * Get Preset Tags.
   * 
   * @param siteId {@link String}
   * @param id {@link String}
   * @return {@link List} {@link Map}
   * @throws IOException IOException
   */
  @SuppressWarnings("unchecked")
  private List<Map<String, Object>> getPresetTags(final String siteId, final String id)
      throws IOException {
    newOutstream();

    ApiGatewayRequestEvent event = toRequestEvent("get", "/presets/{presetId}/tags", null);
    setPathParameter(event, "presetId", id);
    addParameter(event, "siteId", siteId);

    // when
    String response = handleRequest(event);

    // then
    Map<String, String> m = GsonUtil.getInstance().fromJson(response, Map.class);
    Map<String, Object> resp = GsonUtil.getInstance().fromJson(m.get("body"), Map.class);

    List<Map<String, Object>> tags = (List<Map<String, Object>>) resp.get("tags");
    return tags;
  }
}
