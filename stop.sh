#!/bin/bash

echo "🛑 Stopping Payment Processing System..."
docker compose down

echo "✅ All services stopped"
echo ""
echo "💾 Data volumes preserved. To remove volumes:"
echo "docker compose down -v"