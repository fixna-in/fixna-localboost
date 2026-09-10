#!/usr/bin/env bash
set -euo pipefail
cp -n .env.example .env || true
docker compose up -d
echo "Fixna infrastructure started."
