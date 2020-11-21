#!/bin/bash
export AWS_ACCESS_KEY_ID=none
export AWS_SECRET_ACCESS_KEY=none
until aws --region us-east-1 --endpoint-url=http://localhost:8000 dynamodb list-tables; do
  >&2 echo "DynamoDB is unavailable - sleeping"
  sleep 1
done

echo "DynamoDB is available"
