
package com.formkiq.stacks.lambda.s3;

import com.formkiq.aws.ssm.SsmService;
import com.formkiq.module.lambdaservices.AwsServiceCache;
import com.formkiq.module.lambdaservices.AwsServiceExtension;
import com.formkiq.stacks.client.FormKiqClient;
import com.formkiq.stacks.client.FormKiqClientConnection;
import com.formkiq.stacks.client.FormKiqClientV1;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.regions.Region;

/**
 * 
 * {@link FormKiqClient} implementation of {@link AwsServiceExtension}.
 *
 */
public class FormKiQClientV1Extension implements AwsServiceExtension<FormKiqClientV1> {

  /** {@link FormKiqClientV1}. */
  private FormKiqClientV1 formkiqClient = null;
  /** {@link Region}. */
  private Region region;
  /** {@link AwsCredentials}. */
  private AwsCredentials credentials;

  /**
   * constructor.
   * 
   * @param awsRegion {@link Region}
   * @param awsCredentials {@link AwsCredentials}
   */
  public FormKiQClientV1Extension(final Region awsRegion, final AwsCredentials awsCredentials) {
    this.region = awsRegion;
    this.credentials = awsCredentials;
  }

  @Override
  public FormKiqClientV1 loadService(final AwsServiceCache awsServiceCache) {

    if (this.formkiqClient == null) {
      initFormKiQClient(awsServiceCache);
    }

    return this.formkiqClient;
  }

  private void initFormKiQClient(final AwsServiceCache awsServiceCache) {

    String appEnvironment = awsServiceCache.environment("APP_ENVIRONMENT");
    SsmService ssm = awsServiceCache.getExtension(SsmService.class);
    String documentsIamUrl =
        ssm.getParameterValue("/formkiq/" + appEnvironment + "/api/DocumentsIamUrl");

    FormKiqClientConnection fkqConnection =
        new FormKiqClientConnection(documentsIamUrl).region(this.region);

    if (this.credentials != null) {
      fkqConnection = fkqConnection.credentials(this.credentials);
    }

    this.formkiqClient = new FormKiqClientV1(fkqConnection);
  }
}
