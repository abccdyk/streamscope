# AWS deployment and maintenance

StreamScope is deployed in Ohio (`us-east-2`). The public entry point is [the CloudFront demo](https://dd1loyi6128h4.cloudfront.net). Local Docker Compose remains the easiest way to reproduce the application.

## Resource layout

| Component | Project resource |
| --- | --- |
| ECS cluster | `streamscope-cluster` |
| Backend service / task family | `streamscope-backend-service` / `streamscope-backend` |
| Frontend service / task family | `streamscope-frontend-service` / `streamscope-frontend` |
| ECR repositories | `streamscope-backend`, `streamscope-frontend` |
| Current image tag | `v2` in each repository |
| Load balancer | `streamscope-alb` |
| Target groups | `streamscope-backend-tg`, `streamscope-frontend-tg` |
| RDS instance / database | `streamscope-db` / `streamscope` |
| CloudFront distribution name | `streamscope-web` |

Resource IDs, database credentials, and the secret origin-header value are not required in this public guide. The task definitions and network resources are currently maintained manually; this repository does not contain a complete infrastructure-as-code deployment.

## Network and request routing

- CloudFront redirects browser HTTP requests to HTTPS and uses its default certificate.
- The origin is the internet-facing ALB, currently over HTTP on port 80.
- CloudFront uses `CachingDisabled` with `AllViewer` so personalized responses are not cached and session cookies, CSRF headers, and query strings reach the backend.
- ALB priority 10 matches `/api/*` AND the secret origin-header condition, then forwards to backend port 8080.
- Priority 20 matches the origin-header condition and forwards to frontend port 80. The default action returns 403.
- ALB ingress permits only the AWS-managed CloudFront origin-facing prefix list.
- Application security groups allow ingress only from the ALB security group. MySQL port 3306 permits only the backend security group.
- Fargate tasks have public IP addresses for outbound access in public subnets. RDS has public access disabled.
- CloudFront origin-header names and values must be treated as credentials and must not be included in screenshots or commits. This control does not make the HTTP origin connection encrypted.

The local Nginx configuration proxies `/api/` to the Compose backend service. In AWS, ALB routes the API directly to the backend, so the Compose DNS name is not used for normal API requests.

## Runtime configuration

Backend non-secret environment variables:

| Variable | Deployment value |
| --- | --- |
| `DB_URL` | `jdbc:mysql://<rds-endpoint>:3306/streamscope?sslMode=REQUIRED` |
| `DB_USERNAME` | `streamscope` |
| `TWITCH_CLIENT_ID` | Twitch application Client ID |
| `SERVER_PORT` | `8080` |
| `SERVER_FORWARD_HEADERS_STRATEGY` | `framework` |
| `SERVER_SERVLET_SESSION_COOKIE_SECURE` | `true` |

The database URL requires encryption, but `REQUIRED` does not verify the server certificate identity. Certificate identity verification for the regular backend connection remains follow-up work.

Secrets are injected from Standard Parameter Store `SecureString` parameters:

| Environment variable | Parameter name |
| --- | --- |
| `DB_PASSWORD` | `/streamscope/db/app-password` |
| `TWITCH_CLIENT_SECRET` | `/streamscope/twitch/client-secret` |

The application execution role has `AmazonECSTaskExecutionRolePolicy` and scoped `ssm:GetParameters` access to these two parameters. With the default `aws/ssm` key, no separate customer-managed KMS key permission was configured.

A separate one-time initialization task used `/streamscope/db/admin-password` and the application password to create the schema-scoped application database user. It finished with exit code 0. It is not part of ordinary application deployments and is not maintained as an ECS Service.

Changing a parameter does not update an already running container. Keep the actual database password and parameter value in sync, then start fresh application tasks.

## Publishing an update

1. Run the relevant local checks and review the CI result.
2. Authenticate the AWS CLI with the project IAM profile, not root.
3. Build for the task's Linux x86-64 architecture.
4. Push using a new version tag; ECR tags are immutable.
5. Register a new revision of the corresponding task definition with the new image tag.
6. Update that ECS Service to the new revision and wait for healthy targets.

Example in Windows CMD, after configuring Docker's ECR credential helper for your registry:

```cmd
set "AWS_PROFILE=streamscope-iam"
set "AWS_ECR_DISABLE_CACHE=true"
set "ECR_REGISTRY=<account-id>.dkr.ecr.us-east-2.amazonaws.com"
docker build --platform linux/amd64 --provenance=false --sbom=false -t %ECR_REGISTRY%/streamscope-backend:v3 ./backend
docker push %ECR_REGISTRY%/streamscope-backend:v3
```

Replace `<account-id>` with your account ID. Ensure `docker-credential-ecr-login.exe` is on PATH. The helper retrieves temporary ECR credentials without a separate `docker login`. The cache setting above disables the helper's local token cache.

Use an unused tag in place of `v3` if necessary. Apply the same process to `frontend` and `streamscope-frontend` for frontend updates. The provenance/SBOM switches avoid the manifest/tag immutability conflict encountered with this project's Windows Docker setup; they do not claim to resolve all ECR push errors.

A forced redeployment alone does not change an image reference from `v2` to `v3`. Keep the previous task definition revision available for rollback. Database migrations must remain backward-compatible with the chosen rollback version.

## Verification and troubleshooting

- Frontend target group: HTTP port 80, health path `/`.
- Backend target group: HTTP port 8080, health path `/actuator/health`.
- Each Service maintains one task. Check task health, target health, and Service events.
- Public basic API check: [`/api/health`](https://dd1loyi6128h4.cloudfront.net/api/health).
- Check the corresponding `/ecs/streamscope-frontend` or `/ecs/streamscope-backend` CloudWatch log group. Retention is seven days.
- For startup failures, inspect the stopped reason, container exit code, and logs before retrying.
- For 503 responses, check target registration and target health.
- For login/CSRF failures, use the HTTPS CloudFront address and check request cookies, CSRF headers, CloudFront request forwarding, and disabled API caching.
- The backend currently stores sessions in memory. Restarting it can require signing in again.
- Do not log passwords, Twitch tokens, or the origin-header secret while debugging.

## Cost controls and shutdown

Running ECS tasks, ALB, RDS, public IPv4 addresses, ECR storage, logs, and CloudFront usage can all contribute to cost. The RDS estimate seen at creation was only for that instance and its storage, not the full deployment.

For a temporary pause, set both ECS Services' desired count to zero and verify there are no standalone running tasks, then temporarily stop RDS. Restore RDS first when resuming, followed by the two Services.

Stopping RDS retains storage and backup charges, and RDS automatically starts after seven consecutive stopped days. Keeping ALB provisioned continues to incur its charges even with no running application tasks.

Permanent teardown requires a separate decision about data and backups. Verify recoverability before deleting RDS, and account for deletion protection, retained snapshots, ECR images, and log storage. Do not delete shared default VPC resources. This document intentionally does not provide an automatic destructive teardown script.

References:
- [ECS ALB integration](https://docs.aws.amazon.com/AmazonECS/latest/developerguide/alb.html)
- [Parameter Store secret injection](https://docs.aws.amazon.com/AmazonECS/latest/developerguide/secrets-envvar-ssm-paramstore.html)
- [Restricting ALB access to CloudFront](https://docs.aws.amazon.com/AmazonCloudFront/latest/DeveloperGuide/restrict-access-to-load-balancer.html)
- [AWS ECR credential helper](https://github.com/awslabs/amazon-ecr-credential-helper)
- [RDS temporary stop behavior and billing](https://docs.aws.amazon.com/AmazonRDS/latest/UserGuide/USER_StopInstance.html)
- [ALB billing and deletion](https://docs.aws.amazon.com/elasticloadbalancing/latest/application/load-balancer-delete.html)

