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
package com.formkiq.stacks.dynamodb.config;

import com.formkiq.graalvm.annotations.Reflectable;

/**
 * Site Configuration Google.
 */
@Reflectable
public class SiteConfigurationGoogle {
  /** workloadIdentityAudience. */
  private String workloadIdentityAudience;
  /** workloadIdentityServiceAccount. */
  private String workloadIdentityServiceAccount;

  /**
   * constructor.
   */
  public SiteConfigurationGoogle() {}

  /**
   * Get WorkloadIdentityAudience.
   * 
   * @return String
   */
  public String getWorkloadIdentityAudience() {
    return this.workloadIdentityAudience;
  }

  /**
   * Get WorkloadIdentityServiceAccount.
   * 
   * @return String
   */
  public String getWorkloadIdentityServiceAccount() {
    return this.workloadIdentityServiceAccount;
  }

  /**
   * Set WorkloadIdentityAudience.
   *
   * @param identityAudience {@link String}
   * @return SiteConfigurationGoogle
   */
  public SiteConfigurationGoogle setWorkloadIdentityAudience(final String identityAudience) {
    this.workloadIdentityAudience = identityAudience;
    return this;
  }

  /**
   * Set WorkloadIdentityServiceAccount.
   *
   * @param serviceAccount {@link String}
   * @return SiteConfigurationGoogle
   */
  public SiteConfigurationGoogle setWorkloadIdentityServiceAccount(final String serviceAccount) {
    this.workloadIdentityServiceAccount = serviceAccount;
    return this;
  }
}
