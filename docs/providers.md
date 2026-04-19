# Providers

Use this document when you want to control the backend default provider and understand how runtime provider switching works in the UI.

## Overview

The frontend always talks to the Spring Boot backend.

- the backend still has a configured default provider
- the UI can now switch provider per request without restarting the backend
- the helper scripts below set the backend default provider for a local session

Supported providers:

- `ollama`: default local provider
- `bedrock`: optional AWS-managed provider
- `huggingface`: optional hosted provider with a curated backend-side model list

## Ollama

Default local workflow:

```bash
cd scripts
./run-backend-ollama.sh
```

Defaults:

- provider: `ollama`
- `MCP_ENABLED=true`

Requirements:

- Ollama running locally at `http://localhost:11434`
- at least one local model installed, for example:

```bash
ollama pull llama3:8b
```

## Bedrock

Preferred local Bedrock workflow:

```bash
cd scripts
./run-backend-bedrock.sh
```

Defaults:

- provider: `bedrock`
- region: `us-east-2`
- model: `us.amazon.nova-pro-v1:0`
- `MCP_ENABLED=true`

Requirements:

- valid AWS credentials available to the backend process
- Bedrock model access enabled in your AWS account
- the selected region supports the configured model
- for Nova Pro, use an inference profile id such as `us.amazon.nova-pro-v1:0` rather than the base model id `amazon.nova-pro-v1:0`

Common credential paths:

- default AWS SDK resolution from `~/.aws/credentials`
- `AWS_PROFILE`
- environment credentials

Example with an explicit AWS profile:

```bash
cd scripts
AWS_PROFILE=personal ./run-backend-bedrock.sh
```

Override the region or model when needed:

```bash
cd scripts
BEDROCK_REGION=us-east-1 BEDROCK_MODEL_ID=amazon.nova-lite-v1:0 ./run-backend-bedrock.sh
```

## Hugging Face

Preferred hosted Hugging Face workflow:

```bash
cd scripts
HUGGINGFACE_API_TOKEN=hf_xxx ./run-backend-huggingface.sh
```

Defaults:

- provider: `huggingface`
- base URL: `https://router.huggingface.co/v1/chat/completions`
- default model: `meta-llama/Llama-3.1-8B-Instruct`
- curated model list: defaults to the configured default model unless overridden
- `MCP_ENABLED=true`

Requirements:

- a valid Hugging Face API token available to the backend process
- a configured default model that you want the backend to offer in the UI

Override the curated model list when needed:

```bash
cd scripts
HUGGINGFACE_API_TOKEN=hf_xxx \
HUGGINGFACE_DEFAULT_MODEL=Qwen/Qwen2.5-72B-Instruct \
HUGGINGFACE_MODELS=Qwen/Qwen2.5-72B-Instruct,meta-llama/Llama-3.1-8B-Instruct \
./run-backend-huggingface.sh
```

## Verification

After the backend starts, verify the default provider:

- health: `http://localhost:8080/actuator/health`
- info: `http://localhost:8080/actuator/info`

You can also run the local smoke check:

```bash
cd scripts
./check-app.sh
```

## Notes

- the frontend model selector is provider-aware
- the frontend provider selector can switch between supported providers at runtime without restarting the backend
- for `ollama`, the UI only offers models installed locally
- for `bedrock`, the backend tries to list available inference profiles in the configured region and falls back to the configured model id if discovery is unavailable
- for `huggingface`, the backend exposes a curated configured model list instead of browsing the full Hugging Face catalog at runtime
- successful MCP/tool execution still enriches prompts before the backend calls the active provider
