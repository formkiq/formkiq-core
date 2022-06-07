package com.formkiq.aws.services.lambda;

/**
 * 
 * Extension to {@link AwsServiceCache} to allow custom creation of service classes.
 * @param <T> Type of Class.
 *
 */
public interface AwsServiceExtension<T> {

  /**
   * Load Service.
   * @param awsServiceCache {@link AwsServiceCache}
   * @return T Type of class
   */
  T loadService(AwsServiceCache awsServiceCache);
}
