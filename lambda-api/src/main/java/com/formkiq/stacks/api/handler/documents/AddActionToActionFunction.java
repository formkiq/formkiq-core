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
package com.formkiq.stacks.api.handler.documents;

import com.formkiq.aws.dynamodb.ApiAuthorization;
import com.formkiq.aws.dynamodb.documents.DocumentArtifact;
import com.formkiq.module.actions.Action;
import com.formkiq.module.actions.ActionBuilder;
import com.formkiq.module.actions.ActionStatus;

import java.util.function.BiFunction;

/**
 * {@link BiFunction} to convert AddAction to Action.
 */
public class AddActionToActionFunction implements BiFunction<String, AddAction, Action> {

  /** {@link DocumentArtifact}. */
  private final DocumentArtifact document;

  /**
   * constructor.
   * 
   * @param documentArtifact {@link DocumentArtifact}
   */
  public AddActionToActionFunction(final DocumentArtifact documentArtifact) {
    this.document = documentArtifact;
  }

  @Override
  public Action apply(final String siteId, final AddAction a) {
    String username = ApiAuthorization.getAuthorization().getUsername();
    return new ActionBuilder().type(a.type()).queueId(a.queueId())
        .parameters(a.parameters() != null ? a.parameters().toMap() : null)
        .status(ActionStatus.PENDING).userId(username).document(document).indexUlid().build(siteId);
  }
}
