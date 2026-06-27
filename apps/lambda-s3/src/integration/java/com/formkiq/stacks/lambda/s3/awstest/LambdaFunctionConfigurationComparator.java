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
