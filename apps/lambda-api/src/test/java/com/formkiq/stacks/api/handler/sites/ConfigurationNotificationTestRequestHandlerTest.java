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
package com.formkiq.stacks.api.handler.sites;

import static com.formkiq.aws.services.lambda.ApiResponseStatus.SC_ACCEPTED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.formkiq.aws.dynamodb.ApiAuthorization;
import com.formkiq.aws.dynamodb.ApiPermission;
import com.formkiq.aws.services.lambda.ApiGatewayRequestEvent;
import com.formkiq.module.events.EventService;
import com.formkiq.module.events.EventServiceMock;
import com.formkiq.module.events.notification.NotificationTestEvent;
import com.formkiq.module.lambdaservices.AwsServiceCache;
import com.formkiq.module.lambdaservices.ClassServiceExtension;
import com.formkiq.stacks.dynamodb.config.ConfigService;
import com.formkiq.stacks.dynamodb.config.SiteConfiguration;
import com.formkiq.stacks.dynamodb.config.SiteConfigurationNotification;
import com.formkiq.stacks.dynamodb.config.SiteConfigurationNotificationProvider;
import com.formkiq.validation.ValidationException;
import com.google.gson.Gson;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Tests for {@link ConfigurationNotificationTestRequestHandler}. */
class ConfigurationNotificationTestRequestHandlerTest {

  /** Fixed configuration service. */
  private static final class TestConfigService implements ConfigService {
    /** Configuration. */
    private final SiteConfiguration configuration;

    private TestConfigService(final SiteConfiguration config) {
      this.configuration = config;
    }

    @Override
    public void delete(final String siteId) {
      throw new UnsupportedOperationException();
    }

    @Override
    public SiteConfiguration get(final String siteId) {
      return configuration;
    }

    @Override
    public long getIncrement(final String siteId, final String key) {
      throw new UnsupportedOperationException();
    }

    @Override
    public Map<String, Long> getIncrements(final String siteId) {
      throw new UnsupportedOperationException();
    }

    @Override
    public long increment(final String siteId, final String key) {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean save(final String siteId, final SiteConfiguration config) {
      throw new UnsupportedOperationException();
    }
  }

  /** Site identifier used by tests. */
  private static final String SITE_ID = "site123";

  private ApiAuthorization authorization() {
    return new ApiAuthorization().username("admin@example.com").addPermission(SITE_ID,
        List.of(ApiPermission.ADMIN));
  }

  private AwsServiceCache cache(final EventServiceMock events,
      final SiteConfiguration configuration) {
    AwsServiceCache cache = new AwsServiceCache();
    cache.register(Gson.class, new ClassServiceExtension<>(new Gson()));
    cache.register(EventService.class, new ClassServiceExtension<>(events));
    cache.register(ConfigService.class,
        new ClassServiceExtension<>(new TestConfigService(configuration)));
    return cache;
  }

  private ApiGatewayRequestEvent request(final String body) {
    ApiGatewayRequestEvent event = new ApiGatewayRequestEvent();
    event.setBody(body);
    event.setPathParameters(Map.of("siteId", SITE_ID));
    return event;
  }

  /** A valid request publishes a typed test-notification event. */
  @Test
  void testPostPublishesNotificationTest() throws Exception {
    EventServiceMock events = new EventServiceMock();
    SiteConfigurationNotification notification = new SiteConfigurationNotification(
        "from@example.com", SiteConfigurationNotificationProvider.SES, null);
    SiteConfiguration configuration =
        SiteConfiguration.builder().notification(notification).build(SITE_ID);
    AwsServiceCache cache = cache(events, configuration);
    ApiAuthorization authorization = authorization();
    ConfigurationNotificationTestRequestHandler handler =
        new ConfigurationNotificationTestRequestHandler();
    ApiGatewayRequestEvent request = request("{\"to\":\" recipient@example.com \"}");

    handler.beforePost(request, authorization, cache);
    var response = handler.post(request, authorization, cache);

    assertEquals(SC_ACCEPTED.getStatusCode(), response.statusCode());
    Map<?, ?> body = (Map<?, ?>) response.body();
    assertEquals("Test notification queued", body.get("message"));
    assertFalse(body.containsKey("requestId"));
    NotificationTestEvent published = events.getNotificationTestEvents().getFirst();
    assertEquals(SITE_ID, published.siteId());
    assertEquals("recipient@example.com", published.to());
    assertEquals("admin@example.com", published.userId());
  }

  /** Invalid recipient addresses are rejected before publishing. */
  @Test
  void testPostRejectsInvalidRecipient() {
    EventServiceMock events = new EventServiceMock();
    SiteConfiguration configuration =
        SiteConfiguration.builder().notificationEmail("from@example.com").build(SITE_ID);
    AwsServiceCache cache = cache(events, configuration);
    ConfigurationNotificationTestRequestHandler handler =
        new ConfigurationNotificationTestRequestHandler();

    assertThrows(ValidationException.class,
        () -> handler.post(request("{\"to\":\"not-an-email\"}"), authorization(), cache));
    assertEquals(0, events.getNotificationTestEvents().size());
  }
}
