FROM ubuntu/jre:17-22.04_16

LABEL vendor="FormKiQ Inc"
LABEL maintainer="formkiq.com"

EXPOSE 8080

WORKDIR /app

COPY build/libs/*.jar .

# ENV JAVA_TOOL_OPTIONS=-agentlib:jdwp=transport=dt_socket,server=y,address=*:8100,suspend=n
ENTRYPOINT ["/opt/java/bin/java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "/app/netty-server.jar"]