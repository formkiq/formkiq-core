FROM ubuntu/jre:17-22.04_16

LABEL vendor="FormKiQ Inc"
LABEL maintainer="formkiq.com"

EXPOSE 8080

WORKDIR /app

COPY *.jar .

ENTRYPOINT ["/opt/java/bin/java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "/app/netty-server-<%=version%>.jar"]