#!/usr/bin/env bash
# FB-23 — Cold-start regression measurement harness.
#
# Runs N iterations of `adb shell am start -W` against the installed debug build
# and prints median + p95 for `TotalTime` and `WaitTime` (the two metrics ADR-003
# uses for the cold-start budget).
#
# Budgets (per ADR-003):
#   - Opt-out path  (diagnosticsOptIn = false): baseline + 100 ms median TotalTime
#   - Opt-in  path  (diagnosticsOptIn = true):  baseline + 300 ms median TotalTime
#
# Typical usage:
#   # 1. Establish baseline (before FB-01/02/06) by checking out 7fb2ac2 and
#   #    running ./gradlew installDebug, then:
#   scripts/measure-cold-start.sh baseline
#
#   # 2. Install current build; opt-out is the default after `pm clear`:
#   ./gradlew installDebug
#   adb shell pm clear dev.tuandoan.tasktracker
#   scripts/measure-cold-start.sh opt-out
#
#   # 3. For the opt-in path, flip the Settings → Privacy toggle on the device,
#   #    force-stop the app, then:
#   scripts/measure-cold-start.sh opt-in
#
# Output is a single line per iteration plus a summary block:
#   run 01: TotalTime=412 WaitTime=420
#   ...
#   === opt-out N=10 TotalTime median=405 p95=440 WaitTime median=413 p95=448 ===
#
# Paste the summary into the FB-23 Notion page.
#
# Dependencies: adb on PATH, a connected device (or single emulator) with the
# debug APK already installed. No network or root access required.
set -euo pipefail

readonly PACKAGE="dev.tuandoan.tasktracker"
readonly ACTIVITY="${PACKAGE}/${PACKAGE}.MainActivity"
readonly ITERATIONS="${ITERATIONS:-10}"
readonly WARMUP_SLEEP_SEC="${WARMUP_SLEEP_SEC:-2}"

label="${1:-run}"

if ! command -v adb >/dev/null 2>&1; then
  echo "error: adb not found on PATH" >&2
  exit 2
fi

device_count=$(adb devices | awk 'NR>1 && $2=="device"' | wc -l | tr -d '[:space:]')
if [[ "$device_count" -eq 0 ]]; then
  echo "error: no adb device attached. Connect a Pixel (or start an emulator) and enable USB debugging." >&2
  exit 2
fi
if [[ "$device_count" -gt 1 ]]; then
  echo "error: $device_count devices attached. Export ANDROID_SERIAL to pick one." >&2
  exit 2
fi

# Confirm the app is installed; `am start` fails silently on a fresh device.
if ! adb shell pm list packages "$PACKAGE" | grep -q "$PACKAGE"; then
  echo "error: $PACKAGE not installed. Run ./gradlew installDebug first." >&2
  exit 2
fi

# One warm-up launch so the device's JIT / resource caches stabilize. Not
# counted in the median. Otherwise the first iteration is consistently 40–80 ms
# slower than the rest and skews p95.
echo "Warming up..."
adb shell am force-stop "$PACKAGE"
sleep "$WARMUP_SLEEP_SEC"
adb shell am start -W -S -n "$ACTIVITY" >/dev/null 2>&1 || true

total_times=()
wait_times=()

for i in $(seq 1 "$ITERATIONS"); do
  adb shell am force-stop "$PACKAGE"
  sleep "$WARMUP_SLEEP_SEC"

  out=$(adb shell am start -W -S -n "$ACTIVITY")
  total=$(echo "$out" | awk -F'[: ]+' '/^TotalTime:/ {print $2}')
  wait=$(echo "$out"  | awk -F'[: ]+' '/^WaitTime:/  {print $2}')

  if [[ -z "$total" || -z "$wait" ]]; then
    echo "error: could not parse am start output on iteration $i:" >&2
    echo "$out" >&2
    exit 3
  fi

  printf "run %02d: TotalTime=%s WaitTime=%s\n" "$i" "$total" "$wait"
  total_times+=("$total")
  wait_times+=("$wait")
done

# Sort + pick median / p95. `sort -n` handles the small sample well; p95 rounds
# up to the next index so N=10 → p95 is the 10th (max) entry, N=20 → 19th.
summarize() {
  local name="$1"; shift
  local arr=("$@")
  local sorted
  sorted=$(printf "%s\n" "${arr[@]}" | sort -n)
  local count=${#arr[@]}
  local mid=$((count / 2))
  local median
  if (( count % 2 == 1 )); then
    median=$(echo "$sorted" | awk -v n=$((mid + 1)) 'NR==n')
  else
    local a b
    a=$(echo "$sorted" | awk -v n="$mid" 'NR==n')
    b=$(echo "$sorted" | awk -v n=$((mid + 1)) 'NR==n')
    median=$(( (a + b) / 2 ))
  fi
  local p95_idx=$(( (count * 95 + 99) / 100 )) # ceil(N * 0.95)
  local p95
  p95=$(echo "$sorted" | awk -v n="$p95_idx" 'NR==n')
  printf "%s median=%s p95=%s" "$name" "$median" "$p95"
}

total_summary=$(summarize "TotalTime" "${total_times[@]}")
wait_summary=$(summarize "WaitTime" "${wait_times[@]}")

echo ""
echo "=== $label N=$ITERATIONS $total_summary $wait_summary ==="
