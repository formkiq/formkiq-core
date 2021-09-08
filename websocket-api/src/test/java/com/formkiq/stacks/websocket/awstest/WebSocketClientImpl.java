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
   * @param serverUri {@link URI}
   */
  public WebSocketClientImpl(final URI serverUri) {
    super(serverUri);
  }

  /**
   * Get Close Code.
   * @return int
   */
  public int getCloseCode() {
    return this.closeCode;
  }

  /**
   * Get Web Socket {@link Exception}.
   * @return {@link List} {@link Exception}
   */
  public List<Exception> getErrors() {
    return this.errors;
  }

  /**
   * Get Web Socket Messages.
   * @return {@link List} {@link String}
   */
  public List<String> getMessages() {
    return this.messages;
  }

  /**
   * Is connection closed.
   * @return boolean
   */
  public boolean isOnClose() {
    return this.onClose;
  }

  /**
   * Is connection open.
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
    this.onOpen  = true;
  }
}