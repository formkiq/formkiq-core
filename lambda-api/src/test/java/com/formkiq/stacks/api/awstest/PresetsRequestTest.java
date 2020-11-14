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
package com.formkiq.stacks.api.awstest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import java.io.IOException;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.junit.Test;
import com.formkiq.stacks.client.FormKiqClientV1;
import com.formkiq.stacks.client.models.AddPresetResponse;
import com.formkiq.stacks.client.models.PresetTagBody;
import com.formkiq.stacks.client.models.PresetTags;
import com.formkiq.stacks.client.models.Presets;
import com.formkiq.stacks.client.models.PresetsBody;
import com.formkiq.stacks.client.requests.AddPresetRequest;
import com.formkiq.stacks.client.requests.DeletePresetRequest;
import com.formkiq.stacks.client.requests.DeletePresetTagRequest;
import com.formkiq.stacks.client.requests.GetPresetTagsRequest;
import com.formkiq.stacks.client.requests.GetPresetsRequest;
import com.formkiq.stacks.client.requests.OptionsPresetRequest;
import com.formkiq.stacks.client.requests.OptionsPresetTagsRequest;
import com.formkiq.stacks.client.requests.PresetTagRequest;

/**
 * Process Urls.
 * <p>
 * GET, OPTIONS, POST /presets tests
 * </p>
 * <p>
 * OPTIONS, DELETE /presets/{presetId} GET, POST, PATCH, OPTIONS /presets/{presetId}/tags DELETE
 * </p>
 * <p>
 * OPTIONS /presets/{presetId}/tags/{tagKey}
 * </p>
 *
 */
public class PresetsRequestTest extends AbstractApiTest {

  /** Http Status OK. */
  private static final int HTTP_STATUS_OK = 200;
  /** JUnit Test Timeout. */
  private static final int TEST_TIMEOUT = 20000;

  /**
   * Test GET, OPTIONS, POST /presets, DELETE /presets/{presetId}.
   * 
   * @throws Exception Exception
   */
  @Test(timeout = TEST_TIMEOUT)
  public void testPresets01() throws Exception {
    // given
    String preset1 = "sample test1";
    String preset2 = "sample test2";

    for (FormKiqClientV1 c : getFormKiqClients()) {

      for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {

        List<AddPresetResponse> list = new ArrayList<>();

        list.add(c.addPreset(new AddPresetRequest().siteId(siteId).body(new PresetsBody()
            .name(preset1).tags(Arrays.asList(new PresetTagBody().key("First Name"))))));

        list.add(c.addPreset(new AddPresetRequest().siteId(siteId).body(new PresetsBody()
            .name(preset2).tags(Arrays.asList(new PresetTagBody().key("Last Name"))))));

        try {
          Presets presets = c.getPresets(new GetPresetsRequest().siteId(siteId));

          assertEquals(2, presets.presets().size());

          assertEquals("sample test1", presets.presets().get(0).name());
          assertNotNull(presets.presets().get(0).insertedDate());
          assertNotNull(presets.presets().get(0).id());
          assertNull(presets.presets().get(0).userId());
          assertEquals(siteId != null ? siteId : "default", presets.presets().get(0).siteId());

          assertEquals("sample test2", presets.presets().get(1).name());
          assertNotNull(presets.presets().get(1).insertedDate());
          assertNotNull(presets.presets().get(1).id());
          assertNull(presets.presets().get(1).userId());
          assertEquals(siteId != null ? siteId : "default", presets.presets().get(1).siteId());

        } finally {

          list.forEach(p -> {
            try {
              c.deletePreset(new DeletePresetRequest().siteId(siteId).presetId(p.id()));
            } catch (IOException | InterruptedException e) {
              e.printStackTrace();
              fail();
            }
          });
        }
      }
    }
  }

  /**
   * Options.
   * 
   * @throws Exception Exception
   */
  @Test(timeout = TEST_TIMEOUT)
  public void testOptions01() throws Exception {
    for (FormKiqClientV1 client : getFormKiqClients()) {
      HttpResponse<String> response = client.optionsPresets();
      assertEquals(HTTP_STATUS_OK, response.statusCode());
      assertPreflightedCorsHeaders(response.headers());

      response = client.optionsPreset(new OptionsPresetRequest().presetId("AAA"));
      assertEquals(HTTP_STATUS_OK, response.statusCode());
      assertPreflightedCorsHeaders(response.headers());

      response = client.optionsPresetTags(new OptionsPresetTagsRequest().presetId("AAA"));
      assertEquals(HTTP_STATUS_OK, response.statusCode());
      assertPreflightedCorsHeaders(response.headers());

      response =
          client.optionsPresetTags(new OptionsPresetTagsRequest().presetId("AAA").tag("BBB"));
      assertEquals(HTTP_STATUS_OK, response.statusCode());
      assertPreflightedCorsHeaders(response.headers());
    }
  }

  /**
   * Test GET, POST, PATCH, DELETE /presets/{presetId}/tags.
   * 
   * @throws Exception Exception
   */
  @Test(timeout = TEST_TIMEOUT)
  public void testPresetTags01() throws Exception {
    // given
    String preset1 = "sample test1";

    for (FormKiqClientV1 c : getFormKiqClients()) {

      for (String siteId : Arrays.asList(UUID.randomUUID().toString())) {

        AddPresetResponse response =
            c.addPreset(new AddPresetRequest().siteId(siteId).body(new PresetsBody().name(preset1)
                .tags(Arrays.asList(new PresetTagBody().key("First Name")))));

        String presetId = response.id();

        c.addPresetTags(new PresetTagRequest().siteId(siteId).presetId(presetId)
            .body(new PresetTagBody().key("First Name")));

        HttpResponse<String> httpResponse = c.addPresetTagsAsHttpResponse(new PresetTagRequest()
            .siteId(siteId).presetId(presetId).body(new PresetTagBody().key("First Name2")));
        assertEquals("{\"message\":\"Added tags\"}", httpResponse.body());

        PresetTags tags =
            c.getPresetTags(new GetPresetTagsRequest().siteId(siteId).presetId(presetId));
        assertEquals(2, tags.tags().size());
        assertEquals("First Name", tags.tags().get(0).key());
        assertEquals("First Name2", tags.tags().get(1).key());

        assertFalse(c.deletePresetTag(
            new DeletePresetTagRequest().presetId(presetId).siteId(siteId).tag("First N23423ame")));

        assertTrue(c.deletePresetTag(
            new DeletePresetTagRequest().presetId(presetId).siteId(siteId).tag("First Name")));
        tags = c.getPresetTags(new GetPresetTagsRequest().siteId(siteId).presetId(presetId));
        assertEquals(1, tags.tags().size());
        assertEquals("First Name2", tags.tags().get(0).key());
      }
    }
  }
}
