# Publish to GitHub

Create an empty GitHub repository named `commerceflow-services`; do not initialize it with another README.

From the unpacked project directory:

```powershell
git init -b main
git add .
git commit -m "SCRUM-14 Initialize CommerceFlow microservice testbed [FR-010 FR-021 FR-030 FR-041]"
git remote add origin https://github.com/sskengine/commerceflow-services.git
git push -u origin main
```

Then update ECA:

```env
GITHUB_ALLOWED_REPOSITORIES=sskengine/commerceflow,sskengine/commerceflow-services
```

Restart ECA after changing `.env`, and add the same GitHub webhook URL and secret to the new repository. Select the `Pull requests` event and keep SSL verification enabled.

Use branch and commit conventions:

```text
branch: SCRUM-15-payment-validation
commit: SCRUM-15 Enforce order and payment currency [FR-021 BRULE-003]
PR title: SCRUM-15 Validate currency across Order and Payment
```

The repository URL above assumes the existing GitHub owner `sskengine`. Change it only if the repository is created under another organization.
