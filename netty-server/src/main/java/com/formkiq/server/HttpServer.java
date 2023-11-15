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

import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
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

  /** Default Server Port. */
  private static final int DEFAULT_PORT = 8080;
  /** {@link Logger}. */
  private static Logger logger = Logger.getLogger(HttpServer.class.getName());

  /**
   * Create Options.
   * 
   * @return {@link Options}
   */
  private static Options createOptions() {

    Options options = new Options();
    Option port = new Option(null, "port", true, "http server port");
    options.addOption(port);

    Option dynamodb = new Option(null, "dynamodb-url", true, "dynamodb url");
    dynamodb.setRequired(true);
    options.addOption(dynamodb);

    Option s3 = new Option(null, "s3-url", true, "s3 url");
    s3.setRequired(true);
    options.addOption(s3);

    Option s3presigner = new Option(null, "s3-presigner-url", true, "s3 presigner url");
    s3presigner.setRequired(true);
    options.addOption(s3presigner);

    Option minioAccessKey = new Option(null, "minio-access-key", true, "Minio Access Key");
    minioAccessKey.setRequired(true);
    options.addOption(minioAccessKey);

    Option minioSecretKey = new Option(null, "minio-secret-key", true, "Minio Secret Key");
    minioSecretKey.setRequired(true);
    options.addOption(minioSecretKey);

    Option apiKey = new Option(null, "api-key", true, "API Key");
    options.addOption(apiKey);

    Option adminUser = new Option(null, "admin-username", true, "Admin Username");
    options.addOption(adminUser);

    Option adminPassword = new Option(null, "admin-password", true, "Admin Password");
    options.addOption(adminPassword);

    Option typesenseHost = new Option(null, "typesense-host", true, "Typesense Host");
    typesenseHost.setRequired(true);
    options.addOption(typesenseHost);

    Option typesenseApiKey = new Option(null, "typesense-api-key", true, "Typesense Api Key");
    typesenseApiKey.setRequired(true);
    options.addOption(typesenseApiKey);

    return options;
  }

  /**
   * Main.
   * 
   * @param args {@link String}
   * @throws ParseException ParseException
   * @throws InterruptedException InterruptedException
   */
  public static void main(final String[] args) throws ParseException, InterruptedException {
    logger.info("Starting FormKiQ server");
    new HttpServer(args).run();
  }

  /** {@link CommandLine}. */
  private CommandLine commandLine;
  /** Server Port. */
  private int port;

  /**
   * constructor.
   * 
   * @param args {@link String}
   * @throws ParseException ParseException
   */
  public HttpServer(final String[] args) throws ParseException {

    Options options = createOptions();
    String[] newargs = loadArgs(options, args);

    CommandLineParser parser = new DefaultParser();
    CommandLine line = parser.parse(options, newargs);

    if (line.hasOption("port")) {
      this.port = Integer.parseInt(line.getOptionValue("port"));
    } else {
      this.port = DEFAULT_PORT;
    }

    this.commandLine = line;
  }

  /**
   * Load Args from Environment and commandline.
   * 
   * @param options {@link Options}
   * @param args {@link String}
   * @return {@link String}
   */
  private String[] loadArgs(final Options options, final String[] args) {
    Set<String> set = new HashSet<>(Set.of(args));

    for (Option o : options.getOptions()) {
      String env = o.getLongOpt().toUpperCase().replaceAll("-", "_");
      if (System.getenv().containsKey(env)) {
        set.add("--" + o.getLongOpt() + "=" + System.getenv(env));
      }
    }

    String[] newargs = set.toArray(new String[0]);
    return newargs;
  }

  /**
   * Run Server.
   * 
   * @return {@link Channel}
   * 
   * @throws InterruptedException InterruptedException
   */
  public Channel run() throws InterruptedException {

    HttpServerInitializer childHandler = null;
    EventLoopGroup bossGroup = new NioEventLoopGroup(1);
    EventLoopGroup workerGroup = new NioEventLoopGroup();

    try {

      ServerBootstrap b = new ServerBootstrap();
      b.group(bossGroup, workerGroup);
      b.channel(NioServerSocketChannel.class);
      b.handler(new LoggingHandler(LogLevel.INFO));

      childHandler = new HttpServerInitializer(this.commandLine);
      b.childHandler(childHandler);

      Channel ch = b.bind(this.port).sync().channel();

      System.err.println(
          "Open your web browser and navigate to " + "http://127.0.0.1:" + this.port + '/');

      ch.closeFuture().sync();

      return ch;

    } finally {

      if (childHandler != null) {
        childHandler.shutdownGracefully();
      }

      bossGroup.shutdownGracefully();
      workerGroup.shutdownGracefully();
    }
  }
}
