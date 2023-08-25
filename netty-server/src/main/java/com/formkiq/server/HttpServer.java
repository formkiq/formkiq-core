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
package com.formkiq.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

/**
 * FormKiQ Http Server.
 */
public class HttpServer {

  // static final boolean SSL = System.getProperty("ssl") != null;
  // static final int PORT = Integer.parseInt(System.getProperty("port", SSL ? "8443" : "8080"));

  /** Default Server Port. */
  private static final int DEFAULT_PORT = 8080;
  /** Server Port. */
  private int port;

  /**
   * constructor.
   * 
   * @param serverPort int
   */
  public HttpServer(final int serverPort) {
    this.port = serverPort;
  }

  /**
   * Main.
   * 
   * @param args {@link String}
   * @throws Exception Exception
   */
  public static void main(final String[] args) throws Exception {

    int port = args.length > 0 ? Integer.parseInt(args[0]) : DEFAULT_PORT;

    new HttpServer(port).run();
  }

  /**
   * Run Server.
   * 
   * @throws Exception Exception
   */
  public void run() throws Exception {

    EventLoopGroup bossGroup = new NioEventLoopGroup(1);
    EventLoopGroup workerGroup = new NioEventLoopGroup();
    try {
      ServerBootstrap b = new ServerBootstrap();
      b.group(bossGroup, workerGroup);
      b.channel(NioServerSocketChannel.class);
      b.handler(new LoggingHandler(LogLevel.INFO));
      b.childHandler(new HttpServerInitializer());

      Channel ch = b.bind(this.port).sync().channel();

      System.err.println(
          "Open your web browser and navigate to " + "http://127.0.0.1:" + this.port + '/');

      ch.closeFuture().sync();
    } finally {
      bossGroup.shutdownGracefully();
      workerGroup.shutdownGracefully();
    }
  }
}
