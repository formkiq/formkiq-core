FROM amazonlinux:2023 AS builder
ARG LAMBDA_ARCHITECTURE=x86_64
ARG TESSERACT_VERSION=5.5.2
ARG LEPTONICA_VERSION=1.87.0
ARG TESSERACT_SHA256=6235ea0dae45ea137f59c09320406f5888383741924d98855bd2ce0d16b54f21
ARG LEPTONICA_SHA256=c73363397f96eb1295602bf44d708a994ad42046c791bf03ea0505d829bdb6a7

RUN dnf install -y gcc-c++ cmake make tar gzip zip \
    libjpeg-turbo-devel libpng-devel libtiff-devel zlib-devel libwebp-devel \
    && dnf clean all
WORKDIR /tmp
RUN curl --fail --show-error --location --retry 3 \
      "https://github.com/DanBloomberg/leptonica/releases/download/${LEPTONICA_VERSION}/leptonica-${LEPTONICA_VERSION}.tar.gz" -o leptonica.tar.gz \
    && echo "${LEPTONICA_SHA256}  leptonica.tar.gz" | sha256sum --check - \
    && tar -xzf leptonica.tar.gz \
    && cd "leptonica-${LEPTONICA_VERSION}" \
    && ./configure --prefix=/opt --disable-static \
    && make -j2 && make install
RUN curl --fail --show-error --location --retry 3 \
      "https://github.com/tesseract-ocr/tesseract/archive/refs/tags/${TESSERACT_VERSION}.tar.gz" -o tesseract.tar.gz \
    && echo "${TESSERACT_SHA256}  tesseract.tar.gz" | sha256sum --check - \
    && tar -xzf tesseract.tar.gz \
    && cmake -S "tesseract-${TESSERACT_VERSION}" -B tess-build \
      -DCMAKE_BUILD_TYPE=Release -DCMAKE_INSTALL_PREFIX=/opt -DCMAKE_PREFIX_PATH=/opt -DBUILD_SHARED_LIBS=ON \
      -DBUILD_TRAINING_TOOLS=OFF -DBUILD_TESTS=OFF -DDISABLE_ARCHIVE=ON -DDISABLE_CURL=ON \
    && cmake --build tess-build --parallel 2 \
    && cmake --install tess-build

# Preserve the layout used by Tess4J: /opt/lib and /opt/tesseract/share/tessdata.
RUN mkdir -p /layer/bin /layer/lib /layer/tesseract/share/tessdata \
    && cp /opt/bin/tesseract /layer/bin/ \
    && cp -L /opt/lib*/libtesseract.so /layer/lib/libtesseract.so \
    && cp -L /opt/lib*/libtesseract.so.* /layer/lib/ \
    && cp -L /opt/lib*/libleptonica.so.* /layer/lib/ \
    && for binary in /layer/bin/tesseract /layer/lib/libtesseract.so /layer/lib/libleptonica.so.*; do \
      LD_LIBRARY_PATH=/opt/lib:/opt/lib64 ldd "$binary" | awk '$3 ~ /^\// {print $3}'; \
    done | sort -u | while read -r library; do cp -Ln "$library" /layer/lib/; done
ARG TESSDATA_VERSION=4.1.0
RUN for language in eng osd; do \
      curl --fail --show-error --location --retry 3 \
        "https://raw.githubusercontent.com/tesseract-ocr/tessdata_fast/${TESSDATA_VERSION}/${language}.traineddata" \
        -o "/layer/tesseract/share/tessdata/${language}.traineddata"; \
    done
WORKDIR /layer
RUN zip -r "/tmp/layer-tesseract-${TESSERACT_VERSION}-${LAMBDA_ARCHITECTURE}.zip" .

# Smoke-test in the same minimal runtime and filesystem layout as the Java Lambda.
FROM public.ecr.aws/lambda/java:25 AS smoke
COPY --from=builder /layer/ /opt/
ENV TESSDATA_PREFIX=/opt/tesseract/share/tessdata
COPY smoke-test.sh /tmp/smoke-test.sh
RUN sh /tmp/smoke-test.sh
COPY --from=builder /tmp/layer-tesseract-*.zip /layers/
ENTRYPOINT ["/bin/sh"]
