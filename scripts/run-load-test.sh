#!/bin/bash

# Run Gatling load test

echo "Starting Gatling load test..."

cd "$(dirname "$0")/.." || exit 1

# Ensure service is running
if ! curl -s http://localhost:8080/actuator/health > /dev/null; then
    echo "❌ Decision service is not running at localhost:8080"
    echo "Start it with: ./mvnw spring-boot:run"
    exit 1
fi

echo "✓ Service health check passed"

# Run Gatling
./mvnw gatling:test

# Open results
LATEST_REPORT=$(ls -t target/gatling/*/index.html 2>/dev/null | head -1)

if [ -n "$LATEST_REPORT" ]; then
    echo ""
    echo "✓ Load test completed"
    echo "Report: $LATEST_REPORT"
    
    if command -v open > /dev/null; then
        open "$LATEST_REPORT"
    elif command -v xdg-open > /dev/null; then
        xdg-open "$LATEST_REPORT"
    else
        echo "Open the report manually: $LATEST_REPORT"
    fi
else
    echo "⚠ No Gatling report found"
fi
