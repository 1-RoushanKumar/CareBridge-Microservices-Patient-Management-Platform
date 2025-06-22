#!/bin/bash

# List of your container names
containers=(
  analytics-service
  api-gateway
  auth-service-db
  billing-service
  auth-service
  kafka
  patient-service-db
  patient-service
  appointment-service
  appointment-service-db
)

# Start database containers first (names ending with -db)
echo "🗄️ Starting database containers..."
for container in "${containers[@]}"; do
  if [[ "$container" == *-db ]]; then
    echo "🚀 Starting DB: $container..."
    docker start "$container"
  fi
done

# Wait for databases to be fully initialized
echo "⏳ Waiting 15 seconds for databases to initialize..."
sleep 15

# Start other (non-database) containers
echo "🛠️ Starting service containers..."
for container in "${containers[@]}"; do
  if [[ "$container" != *-db ]]; then
    echo "🚀 Starting Service: $container..."
    docker start "$container"
  fi
done

echo "✅ All containers started successfully."

# Usage: ./start-all.sh
