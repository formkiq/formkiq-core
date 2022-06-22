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
package com.formkiq.stacks.websocket.awstest;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

/**
 * Implementation of {@link WebSocketClient}.
 *
 */
public class WebSocketClientImpl extends WebSocketClient {

  /** Is OnOpen. */
  private boolean onOpen = false;
  /** Is OnClose. */
  private boolean onClose = false;
  /** Close Code. */
  private int closeCode = -1;
  /** WebSocket Messages. */
  private List<String> messages = new ArrayList<>();
  /** WebSocket Errors. */
  private List<Exception> errors = new ArrayList<>();

  /**
   * constructor.
   * 
   * @param serverUri {@link URI}
   */
  public WebSocketClientImpl(final URI serverUri) {
    super(serverUri);
  }

  /**
   * Get Close Code.
   * 
   * @return int
   */
  public int getCloseCode() {
    return this.closeCode;
  }

  /**
   * Get Web Socket {@link Exception}.
   * 
   * @return {@link List} {@link Exception}
   */
  public List<Exception> getErrors() {
    return this.errors;
  }

  /**
   * Get Web Socket Messages.
   * 
   * @return {@link List} {@link String}
   */
  public List<String> getMessages() {
    return this.messages;
  }

  /**
   * Is connection closed.
   * 
   * @return boolean
   */
  public boolean isOnClose() {
    return this.onClose;
  }

  /**
   * Is connection open.
   * 
   * @return boolean
   */
  public boolean isOnOpen() {
    return this.onOpen;
  }

  @Override
  public void onClose(final int code, final String reason, final boolean remote) {
    this.closeCode = code;
    this.onClose = true;
  }

  @Override
  public void onError(final Exception ex) {
    this.errors.add(ex);
  }

  @Override
  public void onMessage(final String message) {
    this.messages.add(message);
  }

  @Override
  public void onOpen(final ServerHandshake handshakedata) {
    this.onOpen = true;
  }
}
