package com.formkiq.testutils.aws;

import com.formkiq.aws.ssm.SsmConnectionBuilder;
import com.formkiq.aws.ssm.SsmService;
import com.formkiq.aws.ssm.SsmServiceImpl;
import software.amazon.awssdk.regions.Region;

/**
 * 
 * {@link SsmService} configured specifically for FormKiQ.
 *
 */
public class FkqSsmService implements SsmService {

  /** {@link SsmService}. */
  private SsmService service;
  
  /**
   * constructor.
   * @param awsProfile {@link String}
   * @param awsRegion {@link Region}
   */
  public FkqSsmService(final String awsProfile, final Region awsRegion) {
    SsmConnectionBuilder ssmBuilder =
        new SsmConnectionBuilder().setCredentials(awsProfile).setRegion(awsRegion);
    this.service = new SsmServiceImpl(ssmBuilder);
  }

  @Override
  public String getParameterValue(final String key) {
    return this.service.getParameterValue(key);
  }

  @Override
  public void putParameter(final String key, final String value) {
    this.service.putParameter(key, value);
  }

  @Override
  public void removeParameter(final String key) {
    this.service.removeParameter(key);
  }
}