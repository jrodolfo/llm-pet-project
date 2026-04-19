#!/usr/bin/env bash
# Starts the Spring Boot backend in Hugging Face mode with curated hosted-model defaults.
#
# Override behavior with environment variables such as:
#   HUGGINGFACE_API_TOKEN=hf_xxx
#   HUGGINGFACE_DEFAULT_MODEL=meta-llama/Llama-3.1-8B-Instruct
#   HUGGINGFACE_MODELS=meta-llama/Llama-3.1-8B-Instruct,Qwen/Qwen2.5-72B-Instruct
#   MCP_ENABLED=false

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
BACKEND_DIR="${REPO_ROOT}/backend"

HUGGINGFACE_BASE_URL="${HUGGINGFACE_BASE_URL:-https://router.huggingface.co/v1/chat/completions}"
HUGGINGFACE_DEFAULT_MODEL="${HUGGINGFACE_DEFAULT_MODEL:-meta-llama/Llama-3.1-8B-Instruct}"
HUGGINGFACE_MODELS="${HUGGINGFACE_MODELS:-${HUGGINGFACE_DEFAULT_MODEL}}"
HUGGINGFACE_API_TOKEN="${HUGGINGFACE_API_TOKEN:-}"
MCP_ENABLED="${MCP_ENABLED:-true}"

if [ -z "${HUGGINGFACE_API_TOKEN}" ]; then
  printf '%s\n' 'Error: HUGGINGFACE_API_TOKEN is required for the Hugging Face provider.' >&2
  exit 1
fi

printf '%s\n' \
  "Starting backend with provider=huggingface" \
  "  base_url=${HUGGINGFACE_BASE_URL}" \
  "  default_model=${HUGGINGFACE_DEFAULT_MODEL}" \
  "  models=${HUGGINGFACE_MODELS}" \
  "  mcp_enabled=${MCP_ENABLED}"

cd "${BACKEND_DIR}"

mvn spring-boot:run \
  -Dspring-boot.run.jvmArguments="-Dapp.model.provider=huggingface -Dhuggingface.base-url=${HUGGINGFACE_BASE_URL} -Dhuggingface.api-token=${HUGGINGFACE_API_TOKEN} -Dhuggingface.default-model=${HUGGINGFACE_DEFAULT_MODEL} -Dhuggingface.models=${HUGGINGFACE_MODELS} -Dmcp.enabled=${MCP_ENABLED}"
