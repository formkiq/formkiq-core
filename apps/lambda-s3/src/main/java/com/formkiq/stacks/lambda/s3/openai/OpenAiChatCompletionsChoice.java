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
package com.formkiq.stacks.lambda.s3.openai;

import com.formkiq.graalvm.annotations.Reflectable;

/**
 * Open AI Chat Completions Choice.
 */
@Reflectable
public class OpenAiChatCompletionsChoice {

  /** Index. */
  @Reflectable
  private int index;
  /** OpenAiChatCompletionsChoiceMessage. */
  @Reflectable
  private OpenAiChatCompletionsChoiceMessage message;

  /**
   * constructor.
   */
  public OpenAiChatCompletionsChoice() {

  }

  /**
   * Get Index.
   * 
   * @return int
   */
  public int index() {
    return this.index;
  }

  /**
   * Set Index.
   * 
   * @param choiceIndex int
   * @return {@link OpenAiChatCompletionsChoice}
   */
  public OpenAiChatCompletionsChoice index(final int choiceIndex) {
    this.index = choiceIndex;
    return this;
  }

  /**
   * Get {@link OpenAiChatCompletionsChoiceMessage}.
   * 
   * @return {@link OpenAiChatCompletionsChoiceMessage}
   */
  public OpenAiChatCompletionsChoiceMessage message() {
    return this.message;
  }

  /**
   * Set {@link OpenAiChatCompletionsChoiceMessage}.
   * 
   * @param choiceMessage {@link OpenAiChatCompletionsChoiceMessage}
   * @return {@link OpenAiChatCompletionsChoice}
   */
  public OpenAiChatCompletionsChoice message(
      final OpenAiChatCompletionsChoiceMessage choiceMessage) {
    this.message = choiceMessage;
    return this;
  }
}
