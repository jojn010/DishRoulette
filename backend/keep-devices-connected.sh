#!/usr/bin/env bash
# Local development helper: adb removes reverse mappings on USB/ADB reconnects.
# Restore only this app's mapping on devices already authorized by the user.
set -u
adb_path="${1:?Pass the adb executable path}"
while true; do
    while read -r serial state rest; do
        [ "$state" = device ] || continue
        mappings="$("$adb_path" -s "$serial" reverse --list 2>/dev/null)" || continue
        if ! awk '$2 == "tcp:8080" && $3 == "tcp:8080" { found=1 } END { exit !found }' <<< "$mappings"; then
            "$adb_path" -s "$serial" reverse tcp:8080 tcp:8080 >/dev/null 2>&1 || true
        fi
    done < <("$adb_path" devices 2>/dev/null)
    sleep 3
done
