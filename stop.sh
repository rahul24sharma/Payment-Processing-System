#!/bin/bash

echo "🛑 Stopping PulsePay..."
docker compose down

echo "✅ All services stopped"
echo ""
echo "💾 Data volumes preserved. To remove volumes:"
echo "docker compose down -v"