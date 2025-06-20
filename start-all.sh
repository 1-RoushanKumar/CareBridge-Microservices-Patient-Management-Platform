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
)

# Start each container
for container in "${containers[@]}"; do
  echo "🚀 Starting $container..."
  docker start "$container"
done

echo "✅ All containers started."


#./start-all.sh to run.
