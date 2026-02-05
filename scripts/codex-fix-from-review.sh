#!/usr/bin/env bash
set -euo pipefail

RULES_FILE=".codex/code-review-strict.md"
OUTPUT_DIR="codex-reviews"
PATCH_DIR="codex-patches"
TIMESTAMP="$(date +"%Y%m%d-%H%M%S")"

REVIEW_FILE="${1:-}"
if [[ -z "$REVIEW_FILE" ]]; then
  echo "Usage: $0 <path-to-review.md>"
  exit 1
fi

if [[ ! -f "$RULES_FILE" ]]; then
  echo "ERROR: Rules file not found at $RULES_FILE"
  exit 1
fi

if [[ ! -f "$REVIEW_FILE" ]]; then
  echo "ERROR: Review file not found: $REVIEW_FILE"
  exit 1
fi

mkdir -p "$PATCH_DIR"

TMP="$(mktemp)"
cat "$RULES_FILE" > "$TMP"

echo -e "\n\n=== TASK ===\n" >> "$TMP"
cat >> "$TMP" <<'EOF'
Based on the code review findings below, produce concrete code changes.

Hard requirements:
- Output ONLY a unified diff patch that can be applied with: git apply
- Modify ONLY files that appear in the provided diffs (staged/unstaged)
- Do NOT introduce unrelated refactors
- Keep changes minimal and production-safe
- If something cannot be fixed safely from the diff alone, do not guess; instead add a TODO comment in the code at the relevant location.

EOF

echo -e "\n\n=== CODE REVIEW FINDINGS ===\n" >> "$TMP"
cat "$REVIEW_FILE" >> "$TMP"

echo -e "\n\n=== STAGED DIFF (INDEX) ===\n" >> "$TMP"
git diff --cached --patch --unified=3 >> "$TMP"

echo -e "\n\n=== UNSTAGED DIFF (WORKING TREE) ===\n" >> "$TMP"
git diff --patch --unified=3 >> "$TMP"

PATCH_FILE="${PATCH_DIR}/codex-fix-${TIMESTAMP}.patch"

echo "Generating patch from review..."
codex exec "$(cat "$TMP")" | tee "$PATCH_FILE"

rm -f "$TMP"

echo
echo "✅ Patch saved to: $PATCH_FILE"
echo "To apply:"
echo "  git apply \"$PATCH_FILE\""
