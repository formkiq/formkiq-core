# Stage 1: Build the Java installation and create ZIP
FROM amazonlinux:2 AS builder

# Set environment variables for SDKMAN!
ENV SDKMAN_DIR=/root/.sdkman
ENV JAVA_VERSION=17.0.12-graal
ENV BUILD_HOME=/root/build

# Install required packages
RUN yum install -y unzip zip tar gzip gcc zlib-devel fontconfig && \
    curl -s "https://get.sdkman.io" | bash && \
    source "/root/.sdkman/bin/sdkman-init.sh" && \
    sdk install java $JAVA_VERSION

COPY build/libs/ $BUILD_HOME/

# java -Djava.awt.headless=true -agentlib:native-image-agent=config-output-dir=src/main/resources/META-INF/native-image -jar build/libs/formkiq-module-s3-object-1.0.0.jar

RUN $SDKMAN_DIR/candidates/java/$JAVA_VERSION/bin/native-image -J-Djava.awt.headless=true --no-fallback --enable-http --enable-https -Djava.net.preferIPv4Stack=true -H:Name=server -cp "$BUILD_HOME/*" com.formkiq.lambda.runtime.graalvm.LambdaRuntime

RUN cp /usr/lib64/libpng15.so.15 /libpng15.so.15

RUN rm libfontmanager.so
RUN zip graalvm.zip server lib*.so libpng15.so*

### Stage 2: Copy the ZIP file to a lightweight image
FROM alpine:latest

## Copy the ZIP file from the builder stage
COPY --from=builder /graalvm.zip /graalvm.zip

## Optional: Verify the ZIP file
RUN ls -lah /graalvm.zip

# Default command
CMD ["sh"]
