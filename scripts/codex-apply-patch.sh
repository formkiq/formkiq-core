#!/usr/bin/env bash
set -euo pipefail

PATCH_FILE="${1:-}"

if [[ -z "$PATCH_FILE" ]]; then
  echo "Usage: $0 <patch-file>"
  exit 1
fi

if [[ ! -f "$PATCH_FILE" ]]; then
  echo "ERROR: Patch file not found: $PATCH_FILE"
  exit 1
fi

# Ensure we are in a git repo
if ! git rev-parse --is-inside-work-tree >/dev/null 2>&1; then
  echo "ERROR: Not inside a git repository"
  exit 1
fi

# Warn if working tree is dirty
if ! git diff --quiet || ! git diff --cached --quiet; then
  echo "⚠️  WARNING: You have staged or unstaged changes."
  echo "It is recommended to commit or stash before applying a patch."
  read -rp "Continue anyway? (y/N): " confirm
  if [[ ! "$confirm" =~ ^[Yy]$ ]]; then
    echo "Aborted."
    exit 1
  fi
fi

echo "🧪 Checking if patch can be applied cleanly..."
if ! git apply --check "$PATCH_FILE"; then
  echo "❌ Patch cannot be applied cleanly."
  echo "Resolve conflicts manually or re-generate the patch."
  exit 1
fi

echo "📄 Patch preview:"
echo "----------------------------------------"
git apply --stat "$PATCH_FILE"
echo "----------------------------------------"

read -rp "Apply this patch? (y/N): " apply_confirm
if [[ ! "$apply_confirm" =~ ^[Yy]$ ]]; then
  echo "Patch not applied."
  exit 0
fi

echo "🚀 Applying patch..."
git apply "$PATCH_FILE"

echo
echo "✅ Patch applied successfully."

echo
echo "Next steps:"
echo "  - Review changes: git diff"
echo "  - Run tests"
echo "  - Re-run Codex review if desired"
