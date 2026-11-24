#!/bin/bash
set -euo pipefail
cd "$(dirname "$0")"
architecture="${1:-${LAMBDA_ARCHITECTURE:-x86_64}}"
case "$architecture" in
  x86_64) platform=linux/amd64 ;;
  arm64) platform=linux/arm64 ;;
  *) echo "Unsupported architecture: $architecture" >&2; exit 2 ;;
esac
image="formkiq/layer-tesseract:$architecture"
archive="layer-tesseract-5.5.2-$architecture.zip"
docker build --platform "$platform" --build-arg LAMBDA_ARCHITECTURE="$architecture" -t "$image" .
container=$(docker create --platform "$platform" "$image")
trap 'docker rm "$container" >/dev/null' EXIT
mkdir -p ../build/distributions/api
docker cp "$container:/layers/$archive" "../build/distributions/api/$archive"
