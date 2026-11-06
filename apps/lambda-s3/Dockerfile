# Stage 1: Build native image and create ZIP
FROM ghcr.io/graalvm/native-image-community:25 AS builder

ENV BUILD_HOME=/root/build

WORKDIR /workspace

RUN microdnf install -y zip gcc fontconfig libpng && \
    microdnf clean all && \
    java -version && \
    native-image --version

COPY build/libs/ $BUILD_HOME/

# java -Djava.awt.headless=true -agentlib:native-image-agent=config-output-dir=src/main/resources/META-INF/native-image -jar build/libs/formkiq-module-s3-object-1.0.0.jar

RUN native-image -J-Djava.awt.headless=true --no-fallback --enable-http --enable-https -Djava.net.preferIPv4Stack=true -H:Name=server -cp "$BUILD_HOME/*" com.formkiq.lambda.runtime.graalvm.LambdaRuntime

RUN find /usr/lib64 -maxdepth 1 -name 'libpng*.so*' -exec cp {} /workspace/ \;

RUN rm -f libfontmanager.so
RUN zip /graalvm.zip server lib*.so libpng*.so*

### Stage 2: Copy the ZIP file to a lightweight image
FROM alpine:3.24.2@sha256:294b683cb724975bec92580e1e685676bd4b50bda910ddb8c51d4cabeaec77e6

## Copy the ZIP file from the builder stage
COPY --from=builder /graalvm.zip /graalvm.zip

## Optional: Verify the ZIP file
RUN ls -lah /graalvm.zip

# Default command
CMD ["sh", "-c", "cp /graalvm.zip /output/graalvm.zip"]
