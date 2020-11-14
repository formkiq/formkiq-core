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
package com.formkiq.stacks.lambda.s3.awstest;

import java.io.Serializable;
import java.util.Comparator;
import software.amazon.awssdk.services.s3.model.LambdaFunctionConfiguration;

/**
 * 
 * {@link Comparator} for {@link LambdaFunctionConfiguration}.
 *
 */
public class LambdaFunctionConfigurationComparator
    implements Comparator<LambdaFunctionConfiguration>, Serializable {

  /** serialVersionUID. */
  private static final long serialVersionUID = -2237347781986343373L;

  @Override
  public int compare(final LambdaFunctionConfiguration o1, final LambdaFunctionConfiguration o2) {
    int c = o1.lambdaFunctionArn().compareTo(o2.lambdaFunctionArn());

    if (c == 0) {
      c = o1.events().get(0).toString().compareTo(o2.events().get(0).toString());
    }

    return c;
  }

}
