#!/bin/bash

# Cross-platform environment setup for WebHardMon
# Compatible with: Git Bash (Windows), Linux, macOS

# Get the directory where the script is located
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# Load environment variables from .env file if it exists
if [ -f "$SCRIPT_DIR/.env" ]; then
    set -a
    source "$SCRIPT_DIR/.env"
    set +a
    echo "✓ Loaded environment variables from .env"
else
    echo "✗ .env file not found. Please create one using .env.example as a template."
    exit 1
fi

# Verify required variables are set
required_vars=("DB_HOST" "DB_PORT" "DB_NAME" "DB_USER" "DB_PASS" "GRAFANA_URL")
for var in "${required_vars[@]}"; do
    if [ -z "${!var}" ]; then
        echo "✗ Missing required environment variable: $var"
        exit 1
    fi
done

echo "✓ All environment variables loaded successfully"
echo "Starting WebHardMon application..."

# Run the Spring Boot application with Maven
mvn spring-boot:run
