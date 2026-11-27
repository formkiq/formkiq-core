#!/usr/bin/env bash
# Package one architecture for one or more regional distribution buckets.
set -euo pipefail

if [[ "$#" -lt 3 ]]; then
  echo "Usage: $0 VERSION ARCHITECTURE REGION [REGION ...]" >&2
  exit 2
fi

version="$1"
architecture="$2"
shift 2
: "${S3_BUCKET:?S3_BUCKET must identify the source distribution bucket}"
if [[ ! "$version" =~ ^[a-zA-Z0-9][a-zA-Z0-9._+-]*$ ]]; then
  echo "Invalid release version" >&2
  exit 2
fi

case "$architecture" in
  arm64)
    release_prefix="$version"
    ;;
  x86_64)
    release_prefix="$version/$architecture"
    ;;
  *)
    echo "Unsupported architecture: $architecture" >&2
    exit 2
    ;;
esac
for region in "$@"; do
  if [[ ! "$region" =~ ^[a-z]{2}(-[a-z]+)+-[0-9]+$ ]]; then
    echo "Invalid AWS region: $region" >&2
    exit 2
  fi
done

package_workdir="$(mktemp -d)"
trap 'rm -rf -- "$package_workdir"' EXIT
archive="formkiq-core-$version-$architecture.zip"
aws s3 cp "s3://$S3_BUCKET/formkiq-core/$architecture/$archive" "$package_workdir/$archive"
unzip -q "$package_workdir/$archive" -d "$package_workdir/unpacked"
cd "$package_workdir/unpacked"

assets=(vpc.yaml vpc-import.yaml)
if [[ "${PUBLISH_OPENSEARCH_BASTION:-false}" == "true" ]]; then
  assets+=(opensearch-bastion.yaml)
fi
for region in "$@"; do
  bucket="formkiq-core-distribution-$region"
  output_template="$package_workdir/template-$region.yaml"
  sam package --s3-prefix "$release_prefix" --s3-bucket "$bucket" \
    --template-file template.yaml --region "$region" --output-template-file "$output_template"
  aws s3 cp "$output_template" "s3://$bucket/$release_prefix/template.yaml" --region "$region"
  for asset in "${assets[@]}"; do
    aws s3 cp "$asset" "s3://$bucket/$release_prefix/$asset" --region "$region"
  done
  echo "Published $architecture template: s3://$bucket/$release_prefix/template.yaml"
done
