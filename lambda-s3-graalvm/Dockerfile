# Stage 1: Build the Java installation and create ZIP
FROM amazonlinux:2 AS builder

# Set environment variables for SDKMAN!
ENV SDKMAN_DIR=/root/.sdkman
ENV JAVA_VERSION=17.0.12-graal
ENV BUILD_HOME=/root/build

# Install required packages
RUN yum install -y curl tar gzip unzip zip gcc zlib-devel fontconfig && \
    ARCH="$(uname -m)" && \
    case "$ARCH" in \
      x86_64) GRAAL_ARCH="x64" ;; \
      aarch64) GRAAL_ARCH="aarch64" ;; \
      *) echo "Unsupported architecture: $ARCH" >&2; exit 1 ;; \
    esac && \
    GRAAL_URL="https://download.oracle.com/graalvm/17/archive/graalvm-jdk-17.0.12_linux-${GRAAL_ARCH}_bin.tar.gz" && \
    mkdir -p /opt/graalvm && \
    curl -fL "$GRAAL_URL" -o /tmp/graalvm.tar.gz && \
    tar -xzf /tmp/graalvm.tar.gz -C /opt/graalvm --strip-components=1 && \
    rm -f /tmp/graalvm.tar.gz && \
    export JAVA_HOME=/opt/graalvm && \
    export PATH=/opt/graalvm/bin:$PATH && \
    java -version

COPY build/libs/ $BUILD_HOME/

# java -Djava.awt.headless=true -agentlib:native-image-agent=config-output-dir=src/main/resources/META-INF/native-image -jar build/libs/formkiq-module-s3-object-1.0.0.jar

RUN /opt/graalvm/bin/native-image -J-Djava.awt.headless=true --no-fallback --enable-http --enable-https -Djava.net.preferIPv4Stack=true -H:Name=server -cp "$BUILD_HOME/*" com.formkiq.lambda.runtime.graalvm.LambdaRuntime

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
CMD ["sh", "-c", "cp /graalvm.zip /output/graalvm.zip"]
