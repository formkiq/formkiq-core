until aws --region us-east-1 --no-sign-request --endpoint-url=http://localhost:4566 s3 ls; do
  >&2 echo "S3 is unavailable - sleeping"
  sleep 1
done

echo "S3 is available"
