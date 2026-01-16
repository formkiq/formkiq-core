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
package com.formkiq.stacks.lambda.s3.actions;

import com.formkiq.module.actions.Action;
import com.formkiq.module.lambdaservices.AwsServiceCache;

import java.util.Map;

/**
 * DocumentAction for DataClassification {@link Action}.
 */
public class AddMetadataExtractionAction extends AbstractIntelligentDocumentProcessingAction {

  /**
   * constructor.
   *
   * @param serviceCache {@link AwsServiceCache}
   */
  public AddMetadataExtractionAction(final AwsServiceCache serviceCache) {
    super(serviceCache);
  }

  @Override
  protected Map<String, Object> buildPayload(final Action action) {
    return Map.of();
  }

  @Override
  protected String getMethod() {
    return "POST";
  }

  @Override
  protected String getUrl(final String documentId, final Action action) {
    String llmPromptEntityName = (String) action.parameters().get("llmPromptEntityName");
    return String.format("/documents/%s/metadataExtractionResults/%s", documentId,
        llmPromptEntityName);
  }
}
