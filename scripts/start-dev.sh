#!/usr/bin/env bash
set -euo pipefail
docker compose up -d
echo "Start backend with: mvn -f backend/pom.xml spring-boot:run"
echo "Start frontend with: npm run dev --prefix frontend"
