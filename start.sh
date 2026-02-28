#!/bin/bash

echo "🚀 Starting PulsePay..."
echo ""

# Check if Docker is running
if ! docker info > /dev/null 2>&1; then
    echo "❌ Docker is not running. Please start Docker first."
    exit 1
fi

echo "✅ Docker is running"
echo ""

# Stop any existing containers
echo "🛑 Stopping existing containers..."
docker compose down

echo ""
echo "🏗️  Building services..."
docker compose build

echo ""
echo "🚀 Starting infrastructure (Postgres, Redis, Kafka)..."
docker compose up -d postgres redis zookeeper kafka

echo "⏳ Waiting for infrastructure to be ready..."
sleep 20

echo ""
echo "🚀 Starting monitoring stack..."
docker compose up -d prometheus grafana jaeger elasticsearch kibana

echo ""
echo "🚀 Starting Eureka Server..."
docker compose up -d eureka-server

echo "⏳ Waiting for Eureka..."
sleep 15

echo ""
echo "🚀 Starting microservices..."
docker compose up -d merchant-service fraud-service payment-service ledger-service settlement-service notification-service

echo "⏳ Waiting for services to register..."
sleep 20

echo ""
echo "🚀 Starting API Gateway..."
docker compose up -d api-gateway

echo ""
echo "✅ All services started!"
echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "📊 Service URLs:"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "🌐 API Gateway:        http://localhost:8080"
echo "🔍 Eureka Dashboard:   http://localhost:8761"
echo "📊 Grafana:            http://localhost:3001 (admin/admin)"
echo "📈 Prometheus:         http://localhost:9090"
echo "🔎 Jaeger Tracing:     http://localhost:16686"
echo "📋 Kibana Logs:        http://localhost:5601"
echo "📨 Kafka UI:           http://localhost:8090"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""
echo "🧪 Test the API:"
echo "curl http://localhost:8080/api/v1/health"
echo ""
echo "📝 View logs:"
echo "docker compose logs -f payment-service"
echo ""
echo "🛑 Stop all services:"
echo "./stop.sh"
echo ""