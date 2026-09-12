# Tizi integration

## Unified project boundary

ai-digital-marketplace is the single development repository. Its Java backend owns the commerce flow and the node subscription domain under backend/src/main/java/com/aidigital/marketplace/node.

The imported infra/ assets provide node-side deployment, configuration rendering, health checks, and systemd integration. The standalone Python subscription manager from the former tizi/subscription-manager is intentionally not wired in as a second source of truth, because the marketplace already contains the 2S-UI provisioning adapter and subscription persistence.

## Delivery flow

1. A customer purchases a node product.
2. The marketplace confirms payment and creates an idempotent provisioning job.
3. The backend 2S-UI adapter provisions the subscription.
4. The customer receives the subscription URL through the marketplace delivery APIs.
5. Node-side health and deployment operations use infra/la02 and infra/systemd.

## Sensitive data

Keep real keys, tokens, passwords, and .env files outside Git. Commit only example files with placeholders.
