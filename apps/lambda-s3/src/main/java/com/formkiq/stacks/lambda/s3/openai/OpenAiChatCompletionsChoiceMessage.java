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
import com.google.gson.annotations.SerializedName;

/**
 * OpenAi Chat Completions Choice Message.
 */
@Reflectable
public class OpenAiChatCompletionsChoiceMessage {
  /** Content. */
  @Reflectable
  private String content;
  /** Function Call. */
  @Reflectable
  @SerializedName("function_call")
  private OpenAiChatCompletionsChoiceMessageFunctionCall functionCall;
  /** Role. */
  @Reflectable
  private String role;

  /**
   * constructor.
   */
  public OpenAiChatCompletionsChoiceMessage() {

  }

  /**
   * Get Content.
   * 
   * @return {@link String}
   */
  public String content() {
    return this.content;
  }

  /**
   * Set Content.
   * 
   * @param messageContent {@link String}
   * @return {@link OpenAiChatCompletionsChoiceMessage}
   */
  public OpenAiChatCompletionsChoiceMessage content(final String messageContent) {
    this.content = messageContent;
    return this;
  }

  /**
   * Get Function Call.
   * 
   * @return {@link OpenAiChatCompletionsChoiceMessageFunctionCall}
   */
  public OpenAiChatCompletionsChoiceMessageFunctionCall functionCall() {
    return this.functionCall;
  }

  /**
   * Set Function Call.
   * 
   * @param messageFunctionCall {@link OpenAiChatCompletionsChoiceMessageFunctionCall}
   * @return {@link OpenAiChatCompletionsChoiceMessage}
   */
  public OpenAiChatCompletionsChoiceMessage functionCall(
      final OpenAiChatCompletionsChoiceMessageFunctionCall messageFunctionCall) {
    this.functionCall = messageFunctionCall;
    return this;
  }

  /**
   * Get Role.
   * 
   * @return {@link String}
   */
  public String role() {
    return this.role;
  }

  /**
   * Set Role.
   * 
   * @param messageRole {@link String}
   * @return {@link OpenAiChatCompletionsChoiceMessage}
   */
  public OpenAiChatCompletionsChoiceMessage role(final String messageRole) {
    this.role = messageRole;
    return this;
  }
}
