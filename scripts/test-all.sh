#!/usr/bin/env bash
set -euo pipefail
mvn -f backend/pom.xml test
npm test --prefix frontend
