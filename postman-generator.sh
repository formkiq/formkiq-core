#!/bin/bash

# URL of the OpenAPI spec file
input_url1="https://raw.githubusercontent.com/formkiq/formkiq-core/refs/heads/master/docs/openapi/openapi-iam.yaml"
input_url2="https://raw.githubusercontent.com/formkiq/formkiq-core/refs/heads/master/docs/openapi/openapi-jwt.yaml"
input_url3="https://raw.githubusercontent.com/formkiq/formkiq-core/refs/heads/master/docs/openapi/openapi-key.yaml"

mkdir -p docs/postman

# Temporary files
temp1_yaml=$(mktemp)
temp1_json=$(mktemp)
temp2_yaml=$(mktemp)
temp2_json=$(mktemp)
temp3_yaml=$(mktemp)
temp3_json=$(mktemp)

output_file1="docs/postman/openapi-iam.json"
output_file2="docs/postman/openapi-jwt.json"
output_file3="docs/postman/openapi-key.json"

# Step 1: Fetch the OpenAPI YAML file from the URL
echo "Fetching OpenAPI spec from $input_url1..."
curl -s "$input_url1" -o "$temp1_yaml"

echo "Fetching OpenAPI spec from $input_url2..."
curl -s "$input_url2" -o "$temp2_yaml"

echo "Fetching OpenAPI spec from $input_url3..."
curl -s "$input_url3" -o "$temp3_yaml"

# Step 2: Convert YAML to JSON using openapi2postmanv2
echo "Converting OpenAPI spec to Postman collection JSON..."
npx openapi2postmanv2 -s "$temp1_yaml" -o "$temp1_json" -p

if [ $? -ne 0 ]; then
    echo "Error: Failed to convert OpenAPI spec to Postman collection."
    rm "$temp1_yaml"
    exit 1
fi

npx openapi2postmanv2 -s "$temp2_yaml" -o "$temp2_json" -p

if [ $? -ne 0 ]; then
    echo "Error: Failed to convert OpenAPI spec to Postman collection."
    rm "$temp2_yaml"
    exit 1
fi

npx openapi2postmanv2 -s "$temp3_yaml" -o "$temp3_json" -p

if [ $? -ne 0 ]; then
    echo "Error: Failed to convert OpenAPI spec to Postman collection."
    rm "$temp3_yaml"
    exit 1
fi

echo "Replacing 'auth' and 'variable' blocks in JSON..."
jq 'walk(
  if type == "object" then
    if has("auth") then
      .auth = {
        "type": "awsv4",
        "awsv4": [
          {"key": "sessionToken", "value": "{{sessionToken}}", "type": "string"},
          {"key": "region", "value": "{{awsRegion}}", "type": "string"},
          {"key": "secretKey", "value": "{{secretKey}}", "type": "string"},
          {"key": "accessKey", "value": "{{accessKey}}", "type": "string"},
          {"key": "service", "value": "execute-api", "type": "string"}
        ]
      }
    elif has("variable") then
      .variable = [
        {"key": "baseUrl", "value": "http://localhost"},
        {"key": "awsRegion", "value": "us-east-1"},
        {"key": "sessionToken", "value": ""},
        {"key": "secretKey", "value": ""},
        {"key": "accessKey", "value": ""}
      ]
    else
      .
    end
  else
    .
  end
)' "$temp1_json" > "$output_file1"

if [ $? -ne 0 ]; then
    echo "Error: JSON replacement failed."
    rm "$temp1_yaml" "$temp1_json"
    exit 1
fi

jq 'walk(
  if type == "object" then
    if has("auth") then
      .auth = {
        "type": "bearer",
        "bearer": [
          {"key": "token", "value": "{{token}}", "type": "string"}
        ]
      }
    elif has("variable") then
      .variable = [
        {"key": "baseUrl", "value": "http://localhost"},
        {"key": "token", "value": ""}
      ]
    else
      .
    end
  else
    .
  end
)' "$temp2_json" > "$output_file2"

if [ $? -ne 0 ]; then
    echo "Error: JSON replacement failed."
    rm "$temp2_yaml" "$temp2_json"
    exit 1
fi

jq 'walk(
  if type == "object" then
    if has("variable") then
      .variable = [
        {"key": "baseUrl", "value": "http://localhost"},
        {"key": "apiKey", "value": ""}
      ]
    else
      .
    end
  else
    .
  end
)' "$temp3_json" > "$output_file3"

if [ $? -ne 0 ]; then
    echo "Error: JSON replacement failed."
    rm "$temp3_yaml" "$temp3_json"
    exit 1
fi

# Step 4: Cleanup and output result
rm "$temp1_yaml" "$temp1_json" "$temp2_yaml" "$temp2_json" "$temp3_yaml" "$temp3_json"
echo "Replacement completed. Modified JSON saved to $output_file1."
echo "Replacement completed. Modified JSON saved to $output_file2."
echo "Replacement completed. Modified JSON saved to $output_file3."