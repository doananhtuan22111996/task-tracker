#!/usr/bin/env bash
# Guardrail: the only place that should write the `tag` field on a Task is
# TaskManager (the normalization chokepoint since v1.9.0 — see TCN-13).
#
# This script greps production sources for `Task(... tag = ...)` or
# `.copy(... tag = ...)` outside a small allowlist. Adding a new site means
# either routing through TaskManager.createTask/updateTask/updateTaskContent
# (which normalize) or adding the file to the allowlist with justification.
#
# Runs as part of `./gradlew check`. Fails with non-zero exit on violations.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
SEARCH_ROOT="$ROOT/app/src/main"

# Files allowed to write the tag field directly. Keep in sync with the
# architectural invariant documented in v1.9.0 release notes.
#   - TaskManager:           the normalization chokepoint itself
#   - TagNormalizer:         pure helper (no task writes, but kept for clarity)
#   - TaskBackupDto:         import path — normalizes before mapping to Task
#   - Task (entity):         @Entity default-param declarations
#   - TaskEditorViewModel:   normalizes `tag` before the Task.copy call it makes
ALLOWLIST=(
  "app/src/main/java/dev/tuandoan/tasktracker/domain/TaskManager.kt"
  "app/src/main/java/dev/tuandoan/tasktracker/domain/service/TagNormalizer.kt"
  "app/src/main/java/dev/tuandoan/tasktracker/data/backup/dto/TaskBackupDto.kt"
  "app/src/main/java/dev/tuandoan/tasktracker/data/database/Task.kt"
  "app/src/main/java/dev/tuandoan/tasktracker/ui/viewmodel/TaskEditorViewModel.kt"
)

# Collect every .kt file under app/src/main and scan each for multi-line
# Task(...) or x.copy(...) blocks that set `tag = ...`. Portable: only POSIX
# tools (find, awk, grep). No ripgrep dependency — CI-safe.
violations=""
while IFS= read -r file; do
  rel="${file#$ROOT/}"
  allowed=false
  for entry in "${ALLOWLIST[@]}"; do
    if [[ "$rel" == "$entry" ]]; then
      allowed=true
      break
    fi
  done
  [[ "$allowed" == true ]] && continue

  # awk walks the file once, tracks when we're inside a `Task(` or `.copy(`
  # block (by counting parens), and reports any `tag =` line encountered
  # inside such a block.
  hit=$(awk '
    BEGIN { depth = 0 }
    {
      line = $0
      # Detect start of a Task( or .copy( block on this line.
      # Require Task to be a word: preceded by start-of-line or non-identifier.
      if (depth == 0) {
        if (match(line, /(^|[^A-Za-z0-9_])Task[[:space:]]*\(|\.copy[[:space:]]*\(/)) {
          depth = 1
          # Strip everything up to the opening paren on this line so the
          # paren counter below starts from the block body.
          line = substr(line, RSTART + RLENGTH)
        }
      }

      if (depth > 0) {
        # Update paren depth for this trimmed line.
        for (i = 1; i <= length(line); i++) {
          c = substr(line, i, 1)
          if (c == "(") depth++
          else if (c == ")") {
            depth--
            if (depth == 0) break
          }
        }
        # Check the ORIGINAL line for a tag = assignment.
        if ($0 ~ /[[:space:],(]tag[[:space:]]*=/) {
          printf("%d: %s\n", NR, $0)
        }
      }
    }
  ' "$file")

  if [[ -n "$hit" ]]; then
    while IFS= read -r match; do
      violations+="$rel:$match"$'\n'
    done <<< "$hit"
  fi
done < <(find "$SEARCH_ROOT" -type f -name "*.kt")

if [[ -n "$violations" ]]; then
  echo "check-tag-invariant: disallowed Task.tag writes found." >&2
  echo "All tag writes must go through TaskManager (which normalizes via TagNormalizer)." >&2
  echo "" >&2
  printf "%s" "$violations" >&2
  echo "" >&2
  echo "If this write is intentional and already normalized, add the file to" >&2
  echo "the ALLOWLIST in scripts/check-tag-invariant.sh with justification." >&2
  exit 1
fi

exit 0
