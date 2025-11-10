#!/bin/bash

# Quick setup and run script

set -e

echo "🚀 Setting up Fraud Decision Engine..."

# Check prerequisites
command -v docker >/dev/null 2>&1 || { echo "❌ Docker required but not installed"; exit 1; }
command -v docker-compose >/dev/null 2>&1 || { echo "❌ docker-compose required but not installed"; exit 1; }
command -v mvn >/dev/null 2>&1 || { echo "❌ Maven required but not installed"; exit 1; }

echo "✓ Prerequisites check passed"

# Start infrastructure
echo ""
echo "📦 Starting infrastructure (Kafka, Redis, Postgres)..."
docker-compose up -d

# Wait for services
echo ""
echo "⏳ Waiting for services to be ready..."
sleep 20

# Check health
echo ""
echo "🏥 Checking service health..."
docker-compose ps

# Build application
echo ""
echo "🔨 Building application..."
./mvnw clean package -DskipTests

# Run application
echo ""
echo "▶️  Starting decision service..."
./mvnw spring-boot:run &
APP_PID=$!

# Wait for app to start
echo ""
echo "⏳ Waiting for application startup..."
for i in {1..30}; do
    if curl -s http://localhost:8080/actuator/health > /dev/null; then
        echo "✓ Application is ready!"
        break
    fi
    sleep 2
done

echo ""
echo "✅ Setup complete!"
echo ""
echo "📊 Useful commands:"
echo "  - View metrics: curl http://localhost:8080/actuator/metrics"
echo "  - Generate traffic: ./scripts/generate-traffic.sh"
echo "  - Run load test: ./scripts/run-load-test.sh"
echo "  - View logs: docker-compose logs -f"
echo "  - Stop all: docker-compose down && kill $APP_PID"
echo ""
echo "🌐 Endpoints:"
echo "  - API: http://localhost:8080/api/v1/decisions"
echo "  - Actuator: http://localhost:8080/actuator"
echo "  - Prometheus: http://localhost:9090 (if monitoring profile active)"
