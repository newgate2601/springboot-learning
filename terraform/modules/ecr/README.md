# ECR Module

Creates shared Amazon ECR repositories for application container images.

- Immutable image tags.
- Scan on push.
- AES256 encryption.
- Lifecycle policy to keep only a limited number of images.

Images should be deployed by digest from GitOps, not by the `latest` tag.
