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
package com.formkiq.testutils.api.documents;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.formkiq.aws.dynamodb.actions.Action;
import com.formkiq.aws.dynamodb.actions.ActionStatus;
import com.formkiq.aws.dynamodb.actions.ActionType;
import com.formkiq.aws.dynamodb.documents.DocumentArtifact;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/** Fluent assertions for an {@link Action}. */
public class AssertAction {

  /** Expected action parameters. */
  private final Map<String, Object> parameters = new LinkedHashMap<>();
  /** Expected document. */
  private DocumentArtifact document;
  /** Expected action sort key. */
  private String sk;
  /** Whether the action sort key should be asserted. */
  private boolean skSet;
  /** Expected action status. */
  private ActionStatus status;
  /** Whether status should be asserted. */
  private boolean statusSet;
  /** Expected action type. */
  private ActionType type;
  /** Whether type should be asserted. */
  private boolean typeSet;
  /** Expected user id. */
  private String userId;
  /** Whether user id should be asserted. */
  private boolean userIdSet;

  /**
   * Assert the configured fields against an action.
   *
   * @param action action to verify
   * @return the verified action
   */
  public Action assertAction(final Action action) {
    assertNotNull(action, "Action is required");

    if (this.document != null) {
      assertEquals(this.document.documentId(), action.documentId(), "Action documentId");
      assertEquals(this.document.artifactId(), action.artifactId(), "Action artifactId");
    }
    if (this.skSet) {
      assertNotNull(action.key(), "Action key");
      assertEquals(this.sk, action.key().sk(), "Action sk");
    }
    if (this.typeSet) {
      assertEquals(this.type, action.type(), "Action type");
    }
    if (this.statusSet) {
      assertEquals(this.status, action.status(), "Action status");
    }
    if (this.userIdSet) {
      assertEquals(this.userId, action.userId(), "Action userId");
    }
    if (!this.parameters.isEmpty()) {
      assertNotNull(action.parameters(), "Action parameters");
      this.parameters.forEach((key, value) -> assertEquals(value, action.parameters().get(key),
          "Action parameter '" + key + "'"));
    }

    return action;
  }

  /**
   * Assert that a collection contains exactly one action and verify it.
   *
   * @param actions actions to verify
   * @return the verified action
   */
  public Action assertOne(final Collection<Action> actions) {
    assertNotNull(actions, "Actions are required");
    assertEquals(1, actions.size(), "Expected exactly one action");
    return assertAction(actions.iterator().next());
  }

  /**
   * Assert the document.
   *
   * @param value expected document
   * @return this assertion
   */
  public AssertAction document(final DocumentArtifact value) {
    this.document = Objects.requireNonNull(value, "'document' is required");
    return this;
  }

  /**
   * Assert an action parameter.
   *
   * @param key parameter key
   * @param value expected parameter value
   * @return this assertion
   */
  public AssertAction parameter(final String key, final Object value) {
    this.parameters.put(key, value);
    return this;
  }

  /**
   * Assert the action sort key.
   *
   * @param value expected sort key
   * @return this assertion
   */
  public AssertAction sk(final String value) {
    this.sk = value;
    this.skSet = true;
    return this;
  }

  /**
   * Assert the action status.
   *
   * @param value expected status
   * @return this assertion
   */
  public AssertAction status(final ActionStatus value) {
    this.status = value;
    this.statusSet = true;
    return this;
  }

  /**
   * Assert the action type.
   *
   * @param value expected type
   * @return this assertion
   */
  public AssertAction type(final ActionType value) {
    this.type = value;
    this.typeSet = true;
    return this;
  }

  /**
   * Assert the action user id.
   *
   * @param value expected user id
   * @return this assertion
   */
  public AssertAction userId(final String value) {
    this.userId = value;
    this.userIdSet = true;
    return this;
  }
}
