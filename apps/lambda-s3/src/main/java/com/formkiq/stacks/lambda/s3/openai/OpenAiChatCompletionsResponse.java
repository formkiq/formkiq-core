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

import java.util.List;
import com.formkiq.graalvm.annotations.Reflectable;

/**
 * Open AI Chat Completions Response.
 */
@Reflectable
public class OpenAiChatCompletionsResponse {

  /** Response Choices. */
  @Reflectable
  private List<OpenAiChatCompletionsChoice> choices;
  /** Response Created Timestamp. */
  @Reflectable
  private long created;
  /** Response Id. */
  @Reflectable
  private String id;
  /** Response Model. */
  @Reflectable
  private String model;
  /** Response Object Type. */
  @Reflectable
  private String object;

  /**
   * constructor.
   */
  public OpenAiChatCompletionsResponse() {

  }

  /**
   * Get Choices.
   * 
   * @return {@link List} {@link OpenAiChatCompletionsChoice}
   */
  public List<OpenAiChatCompletionsChoice> choices() {
    return this.choices;
  }

  /**
   * Set Choices.
   * 
   * @param list {@link List} {@link OpenAiChatCompletionsChoice}
   * @return {@link OpenAiChatCompletionsResponse}
   */
  public OpenAiChatCompletionsResponse choices(final List<OpenAiChatCompletionsChoice> list) {
    this.choices = list;
    return this;
  }

  /**
   * Get Created Timestamp.
   * 
   * @return long
   */
  public long created() {
    return this.created;
  }

  /**
   * Set Created Timestamp.
   * 
   * @param timestamp long
   * @return {@link OpenAiChatCompletionsResponse}
   */
  public OpenAiChatCompletionsResponse created(final long timestamp) {
    this.created = timestamp;
    return this;
  }

  /**
   * Get Id.
   * 
   * @return {@link String}
   */
  public String id() {
    return this.id;
  }

  /**
   * Set Id.
   * 
   * @param identifier {@link String}
   * @return {@link OpenAiChatCompletionsResponse}
   */
  public OpenAiChatCompletionsResponse id(final String identifier) {
    this.id = identifier;
    return this;
  }

  /**
   * Get Model.
   * 
   * @return {@link String}
   */
  public String model() {
    return this.model;
  }

  /**
   * Set Model.
   * 
   * @param responseModel {@link String}
   * @return {@link OpenAiChatCompletionsResponse}
   */
  public OpenAiChatCompletionsResponse model(final String responseModel) {
    this.model = responseModel;
    return this;
  }

  /**
   * Get Object Response.
   * 
   * @return {@link String}
   */
  public String object() {
    return this.object;
  }

  /**
   * Set Object Response.
   * 
   * @param responseObject {@link String}
   * @return {@link OpenAiChatCompletionsResponse}
   */
  public OpenAiChatCompletionsResponse object(final String responseObject) {
    this.object = responseObject;
    return this;
  }
}
