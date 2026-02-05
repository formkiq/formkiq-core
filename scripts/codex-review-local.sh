#!/usr/bin/env bash
set -euo pipefail

# Configuration
RULES_FILE=".codex/code-review-strict.md"
OUTPUT_DIR="codex-reviews"
TIMESTAMP="$(date +"%Y%m%d-%H%M%S")"
OUTPUT_FILE="${OUTPUT_DIR}/review-${TIMESTAMP}.md"

# Ensure rules file exists
if [[ ! -f "$RULES_FILE" ]]; then
  echo "ERROR: Rules file not found at $RULES_FILE"
  exit 1
fi

# Create output directory
mkdir -p "$OUTPUT_DIR"

# Build prompt
TMP="$(mktemp)"

cat "$RULES_FILE" > "$TMP"

echo -e "\n\n=== STAGED DIFF (INDEX) ===\n" >> "$TMP"
git diff --cached --patch --unified=3 >> "$TMP"

echo -e "\n\n=== UNSTAGED DIFF (WORKING TREE) ===\n" >> "$TMP"
git diff --patch --unified=3 >> "$TMP"

# Check if there are any changes
if ! grep -qE '^\+|\-' "$TMP"; then
  echo "No staged or unstaged changes to review."
  rm -f "$TMP"
  exit 0
fi

echo "Running Codex strict review..."
echo "Output will be saved to: $OUTPUT_FILE"
echo

# Run Codex and store output
codex exec "$(cat "$TMP")" | tee "$OUTPUT_FILE"

# Cleanup
rm -f "$TMP"

echo
echo "✅ Code review completed."
echo "📄 Saved to: $OUTPUT_FILE"
