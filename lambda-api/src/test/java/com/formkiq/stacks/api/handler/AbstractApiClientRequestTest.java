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
package com.formkiq.stacks.api.handler;

import static com.formkiq.aws.dynamodb.SiteIdKeyGenerator.DEFAULT_SITE_ID;
import org.junit.jupiter.api.extension.RegisterExtension;
import com.formkiq.client.api.DocumentTagsApi;
import com.formkiq.client.api.DocumentsApi;
import com.formkiq.client.api.FoldersApi;
import com.formkiq.client.api.SystemManagementApi;
import com.formkiq.client.invoker.ApiClient;
import com.formkiq.client.invoker.Configuration;
import com.formkiq.lambda.apigateway.util.GsonUtil;
import com.formkiq.module.lambdaservices.AwsServiceCache;
import com.formkiq.stacks.dynamodb.ConfigService;
import com.formkiq.stacks.dynamodb.ConfigServiceExtension;
import com.formkiq.testutils.aws.FormKiqApiExtension;
import com.formkiq.testutils.aws.JwtTokenEncoder;
import com.formkiq.testutils.aws.LocalStackExtension;

/**
 * Abstract Request Test using {@link ApiClient}.
 */
public abstract class AbstractApiClientRequestTest {

  /** {@link FormKiQResponseCallback}. */
  private static final FormKiQResponseCallback CALLBACK = new FormKiQResponseCallback();

  /** FormKiQ Server. */
  @RegisterExtension
  static FormKiqApiExtension server = new FormKiqApiExtension(CALLBACK);

  /** Time out. */
  private static final int TIMEOUT = 30000;

  /**
   * Get {@link AwsServiceCache}.
   * 
   * @return {@link AwsServiceCache}
   */
  public static AwsServiceCache getAwsServices() {
    AwsServiceCache awsServiceCache = LocalStackExtension.getAwsServiceCache();
    awsServiceCache.register(ConfigService.class, new ConfigServiceExtension());
    return awsServiceCache;
  }

  /** {@link ApiClient}. */
  protected ApiClient client =
      Configuration.getDefaultApiClient().setReadTimeout(TIMEOUT).setBasePath(server.getBasePath());
  /** {@link DocumentsApi}. */
  protected DocumentsApi documentsApi = new DocumentsApi(this.client);
  /** {@link FoldersApi}. */
  protected FoldersApi foldersApi = new FoldersApi(this.client);
  /** {@link SystemManagementApi}. */
  protected SystemManagementApi systemApi = new SystemManagementApi(this.client);
  /** {@link DocumentTagsApi}. */
  protected DocumentTagsApi tagsApi = new DocumentTagsApi(this.client);

  /**
   * Convert JSON to Object.
   * 
   * @param <T> Class Type
   * @param json {@link String}
   * @param clazz {@link Class}
   * @return {@link Object}
   */
  protected <T> T fromJson(final String json, final Class<T> clazz) {
    return GsonUtil.getInstance().fromJson(json, clazz);
  }

  /**
   * Set BearerToken.
   * 
   * @param siteId {@link String}
   */
  public void setBearerToken(final String siteId) {
    String jwt = JwtTokenEncoder
        .encodeCognito(new String[] {siteId != null ? siteId : DEFAULT_SITE_ID}, "joesmith");
    this.client.addDefaultHeader("Authorization", jwt);
  }

  /**
   * Set BearerToken.
   * 
   * @param groups {@link String}
   */
  public void setBearerToken(final String[] groups) {
    String jwt = JwtTokenEncoder.encodeCognito(groups, "joesmith");
    this.client.addDefaultHeader("Authorization", jwt);
  }
}
