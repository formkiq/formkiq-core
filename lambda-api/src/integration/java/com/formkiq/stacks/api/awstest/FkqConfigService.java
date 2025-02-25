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
package com.formkiq.stacks.api.awstest;

import com.formkiq.aws.dynamodb.DynamoDbConnectionBuilder;
import com.formkiq.aws.ssm.SsmService;
import com.formkiq.stacks.dynamodb.config.ConfigService;
import com.formkiq.stacks.dynamodb.config.ConfigServiceDynamoDb;
import com.formkiq.stacks.dynamodb.config.SiteConfiguration;
import com.formkiq.testutils.aws.FkqSsmService;
import software.amazon.awssdk.regions.Region;

import java.util.Map;

/**
 * 
 * {@link ConfigService} configured specifically for FormKiQ.
 *
 */
public class FkqConfigService implements ConfigService {

  /** {@link ConfigService}. */
  private final ConfigService service;

  /**
   * constructor.
   * 
   * @param awsProfile {@link String}
   * @param awsRegion {@link Region}
   * @param appenvironment {@link String}
   */
  public FkqConfigService(final String awsProfile, final Region awsRegion,
      final String appenvironment) {

    SsmService ssm = new FkqSsmService(awsProfile, awsRegion);
    String documentsTable =
        ssm.getParameterValue("/formkiq/" + appenvironment + "/dynamodb/DocumentsTableName");

    DynamoDbConnectionBuilder db =
        new DynamoDbConnectionBuilder(false).setCredentials(awsProfile).setRegion(awsRegion);
    this.service = new ConfigServiceDynamoDb(db, documentsTable);
  }

  @Override
  public void delete(final String siteId) {
    this.service.delete(siteId);
  }

  @Override
  public SiteConfiguration get(final String siteId) {
    return this.service.get(siteId);
  }

  @Override
  public boolean save(final String siteId, final SiteConfiguration obj) {
    return this.service.save(siteId, obj);
  }

  @Override
  public long increment(final String siteId, final String key) {
    return this.service.increment(siteId, key);
  }

  @Override
  public long getIncrement(final String siteId, final String key) {
    return this.service.getIncrement(siteId, key);
  }

  @Override
  public Map<String, Long> getIncrements(final String siteId) {
    return this.service.getIncrements(siteId);
  }
}
