#!/usr/bin/env bash
# Generates both services' OpenAPI specs by booting each one and fetching springdoc's
# /v3/api-docs — the spec is derived from the running code, not a hand-maintained file, so it
# can't drift from what the API actually serves.
#
# Usage: generate-openapi-specs.sh <backend-dir> <out-dir>
#
# The API-contract workflow runs this twice: once against the PR head's Backend/ and once
# against the base branch's checkout. It always invokes the *head's* copy of this script for
# both, so the base branch doesn't need to contain it (it didn't, on the PR that added it).
set -euo pipefail

backend_dir=$(cd "$1" && pwd)
mkdir -p "$2"
out_dir=$(cd "$2" && pwd)

(cd "$backend_dir" && ./gradlew --quiet :Auth:bootJar :Audit:bootJar)

# Boots a jar, polls /v3/api-docs until the app serves it, writes the spec, stops the app.
fetch_spec() {
	local jar=$1 port=$2 out=$3
	java -jar "$jar" >"$out.boot.log" 2>&1 &
	local pid=$!
	for _ in $(seq 1 60); do
		if curl -sf "http://localhost:$port/v3/api-docs" -o "$out"; then
			kill "$pid" 2>/dev/null || true
			wait "$pid" 2>/dev/null || true
			echo "wrote $out"
			return 0
		fi
		if ! kill -0 "$pid" 2>/dev/null; then
			echo "service on :$port exited before serving its spec" >&2
			cat "$out.boot.log" >&2
			return 1
		fi
		sleep 2
	done
	echo "timed out waiting for :$port" >&2
	kill "$pid" 2>/dev/null || true
	return 1
}

auth_jar=$(ls "$backend_dir"/Auth/build/libs/*.jar | grep -v plain | head -1)
audit_jar=$(ls "$backend_dir"/Audit/build/libs/*.jar | grep -v plain | head -1)

# Both boot on the LOCAL default profile (Auth: in-memory stores + placeholder OAuth creds;
# Audit: in-memory H2). No broker in this job, so Audit's Kafka listener must not start.
export SPRING_KAFKA_LISTENER_AUTO_STARTUP=false

# openapi-diff's parser reads OpenAPI 3.0, not the 3.1 springdoc emits by default. Downgrade
# only here, for the generated comparison specs — the running services keep serving 3.1.
export SPRINGDOC_API_DOCS_VERSION=openapi_3_0

fetch_spec "$audit_jar" 8083 "$out_dir/audit.json"
fetch_spec "$auth_jar" 8085 "$out_dir/auth.json"
