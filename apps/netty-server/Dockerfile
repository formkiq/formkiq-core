FROM eclipse-temurin:25-jre

LABEL vendor="FormKiQ Inc"
LABEL maintainer="formkiq.com"

RUN apt-get update \
    && apt-get install -y --no-install-recommends tesseract-ocr tesseract-ocr-eng \
    && mkdir -p /opt/tesseract/share \
    && ln -s /usr/share/tesseract-ocr/5/tessdata /opt/tesseract/share/tessdata \
    && rm -rf /var/lib/apt/lists/*

EXPOSE 8080

WORKDIR /app

COPY build/libs/*.jar .

# Mitigate CVE-2026-114938 until the Temurin base image includes the fix.
ENV JDK_JAVA_OPTIONS="-XX:-UseBASE64Intrinsics"

# ENV JAVA_TOOL_OPTIONS=-agentlib:jdwp=transport=dt_socket,server=y,address=*:8100,suspend=n
ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "/app/netty-server.jar"]
