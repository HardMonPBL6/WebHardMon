#!/bin/bash

set -e

# Cross-platform environment setup for WebHardMon
# Compatible with: Git Bash (Windows), Linux, macOS

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

ENV_FILE="$SCRIPT_DIR/.env"

if [ ! -f "$ENV_FILE" ]; then
    echo "ERROR: .env file not found. Please create one using .env.example as a template."
    exit 1
fi

# Load .env variables safely
set -a
source "$ENV_FILE"
set +a

echo "OK: Loaded environment variables from .env"

# Required environment variables
required_vars=(
    "DB_HOST"
    "DB_PORT"
    "DB_NAME"
    "DB_USER"
    "DB_PASS"
    "GRAFANA_DASHBOARD_URL"
)

for var in "${required_vars[@]}"; do
    if [ -z "${!var}" ]; then
        echo "ERROR: Missing required environment variable: $var"
        exit 1
    fi
done

echo "OK: All required environment variables loaded"

# Check Maven
if command -v mvn >/dev/null 2>&1; then
    MAVEN_CMD="mvn"
elif [ -f "$SCRIPT_DIR/mvnw" ]; then
    chmod +x "$SCRIPT_DIR/mvnw"
    MAVEN_CMD="$SCRIPT_DIR/mvnw"
else
    echo "ERROR: Maven is not installed and mvnw was not found."
    echo "Install Maven or add the Maven Wrapper to the project."
    exit 1
fi

echo "Starting WebHardMon application..."
echo "Using Grafana dashboard: $GRAFANA_DASHBOARD_URL"

"$MAVEN_CMD" spring-boot:run