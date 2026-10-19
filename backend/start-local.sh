#!/usr/bin/env bash
set -euo pipefail
backend_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
project_dir="$(dirname -- "$backend_dir")"
cd "$project_dir"
# Build the standalone server distribution; no Gradle process is needed to keep it alive.
./gradlew :backend:installDist --console=plain
cd "$backend_dir"
if ! curl --silent --fail --max-time 2 http://127.0.0.1:8080/health >/dev/null; then
    if command -v systemd-run >/dev/null && systemctl --user show-environment >/dev/null 2>&1; then
        systemd-run --user --unit=dishroulette-backend --collect \
            --property=Restart=on-failure --property=RestartSec=3 \
            --working-directory="$backend_dir" "$backend_dir/build/install/backend/bin/backend"
    else
        echo 'Start the backend in another terminal and keep it open:' >&2
        echo "cd '$backend_dir' && ./build/install/backend/bin/backend" >&2
        exit 1
    fi
    ready=false
    for attempt in {1..30}; do
        if curl --silent --fail --max-time 1 http://127.0.0.1:8080/health >/dev/null; then
            ready=true
            break
        fi
        sleep 0.2
    done
    if [ "$ready" != true ]; then
        echo 'Backend did not start. Run: journalctl --user -u dishroulette-backend -n 30' >&2
        exit 1
    fi
fi
adb_path="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-$HOME/Android/Sdk}}/platform-tools/adb"
if [ ! -x "$adb_path" ]; then adb_path="$(command -v adb || true)"; fi
if [ -n "$adb_path" ] && [ -x "$adb_path" ]; then
    connected=0
    while read -r serial state rest; do
        if [ "$state" = device ]; then
            "$adb_path" -s "$serial" reverse tcp:8080 tcp:8080
            connected=$((connected + 1))
        fi
    done < <("$adb_path" devices | tail -n +2)
    if command -v systemd-run >/dev/null && systemctl --user show-environment >/dev/null 2>&1; then
        if ! systemctl --user is-active --quiet dishroulette-devices; then
            systemd-run --user --unit=dishroulette-devices --collect \
                --property=Restart=on-failure --property=RestartSec=3 \
                /bin/bash "$backend_dir/keep-devices-connected.sh" "$adb_path"
        fi
        echo 'Automatic USB reconnection is active for this login session.'
    fi
    echo "Recipe service ready; connected $connected Android device(s)."
    if [ "$connected" -eq 0 ]; then echo 'Start your emulator or connect and authorize your USB phone.'; fi
else
    echo 'Backend ready. Add adb to PATH, then run: adb reverse tcp:8080 tcp:8080'
fi
