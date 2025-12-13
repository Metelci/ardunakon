# GitHub Actions CI/CD Setup Guide

This document explains how to configure the GitHub Actions workflows for the Ardunakon project.

## 📋 Overview

The project includes three CI/CD workflows:

| Workflow | Trigger | Purpose |
|----------|---------|---------|
| **PR Checks** | PRs, push to main | Unit tests, lint, ktlint, build |
| **Release** | Tags (v*) | Signed APK, changelog, GitHub release |
| **Code Quality** | Push to main, PRs | SonarCloud, dependency scanning |

---

## 🔐 Required GitHub Secrets

Navigate to: **Repository → Settings → Secrets and variables → Actions → New repository secret**

### Release Workflow Secrets

| Secret Name | Description |
|-------------|-------------|
| `KEYSTORE_BASE64` | Base64-encoded release keystore |
| `KEYSTORE_PASSWORD` | Keystore password |
| `KEY_ALIAS` | Signing key alias |
| `KEY_PASSWORD` | Signing key password |

### Code Quality Secrets

| Secret Name | Description |
|-------------|-------------|
| `SONAR_TOKEN` | SonarCloud authentication token |

---

## 🔑 Creating a Release Keystore

If you don't have a release keystore, create one:

### Step 1: Generate the Keystore

```bash
keytool -genkey -v -keystore release-keystore.jks -keyalg RSA -keysize 2048 -validity 10000 -alias ardunakon
```

You'll be prompted for:
- **Keystore password**: Choose a strong password
- **Key password**: Can be same as keystore password
- **Distinguished Name**: Your name, organization, etc.

### Step 2: Convert to Base64

**On Linux/macOS:**
```bash
base64 -i release-keystore.jks | tr -d '\n' > keystore-base64.txt
```

**On Windows (PowerShell):**
```powershell
[Convert]::ToBase64String([IO.File]::ReadAllBytes("release-keystore.jks")) | Out-File -Encoding ASCII keystore-base64.txt
```

### Step 3: Add to GitHub Secrets

1. Copy the contents of `keystore-base64.txt`
2. Go to Repository → Settings → Secrets → New repository secret
3. Name: `KEYSTORE_BASE64`, Value: (paste the base64 string)
4. Add `KEYSTORE_PASSWORD`, `KEY_ALIAS`, and `KEY_PASSWORD` secrets

### Step 4: Secure Your Keystore

> ⚠️ **IMPORTANT**: Never commit your keystore to the repository!

- Store the original `.jks` file securely offline
- Add `*.jks` to `.gitignore` (already included)
- Delete `keystore-base64.txt` after adding to GitHub secrets

---

## ☁️ SonarCloud Setup

### Step 1: Create SonarCloud Account

1. Go to [sonarcloud.io](https://sonarcloud.io)
2. Sign up with GitHub
3. Import your repository

### Step 2: Get Your Token

1. Go to **My Account → Security**
2. Generate a new token
3. Add it as GitHub secret `SONAR_TOKEN`

### Step 3: Configure Project

The workflow automatically configures:
- Project Key: `{owner}_{repo}`
- Organization: `{owner}`

If you need different values, update `.github/workflows/code-quality.yml`.

---

## 📦 Dependabot

Dependabot is automatically enabled by the `dependabot.yml` configuration:

- **Gradle dependencies**: Checked weekly on Mondays
- **GitHub Actions**: Checked weekly on Mondays
- **Grouped updates**: AndroidX, Compose, and Kotlin dependencies are grouped

PRs are labeled with `dependencies` for easy filtering.

---

## 🚀 Creating a Release

### Step 1: Update Version

In `app/build.gradle`:
```groovy
versionCode 24  // Increment this
versionName "0.2.7-alpha"  // Update version name
```

### Step 2: Update Changelog

Add a new section at the top of `play_store_changelog.txt`:
```
What's New in v0.2.7-alpha:

🆕 **New Features**:
    • Feature description here

---
```

### Step 3: Commit and Tag

```bash
git add .
git commit -m "Release v0.2.7-alpha"
git push origin main

git tag v0.2.7-alpha
git push origin v0.2.7-alpha
```

### Step 4: Monitor Workflow

1. Go to **Actions** tab in GitHub
2. Watch the "Release" workflow
3. Once complete, find the release under **Releases**

---

## 🔧 Local Development

### Run ktlint Check
```bash
./gradlew ktlintCheck
```

### Auto-fix ktlint Issues
```bash
./gradlew ktlintFormat
```

### Run Unit Tests
```bash
./gradlew testDebugUnitTest
```

### Generate Coverage Report
```bash
./gradlew jacocoTestReport
```
Report location: `app/build/reports/jacoco/jacocoTestReport/html/index.html`

### Run Dependency Check
```bash
./gradlew dependencyCheckAnalyze
```
Report location: `app/build/reports/dependency-check-report.html`

---

## 📊 Badge Examples

Add these to your README.md:

```markdown
[![PR Checks](https://github.com/Metelci/ardunakon/actions/workflows/pr-checks.yml/badge.svg)](https://github.com/Metelci/ardunakon/actions/workflows/pr-checks.yml)
[![Release](https://github.com/Metelci/ardunakon/actions/workflows/release.yml/badge.svg)](https://github.com/Metelci/ardunakon/actions/workflows/release.yml)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=Metelci_ardunakon&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=Metelci_ardunakon)
```

---

## ❓ Troubleshooting

### "ktlint failed"

Run locally to see issues:
```bash
./gradlew ktlintCheck
```

Auto-fix most issues:
```bash
./gradlew ktlintFormat
```

### "Keystore decode failed"

Ensure the base64 string has no line breaks:
```bash
# Verify by decoding
echo "$KEYSTORE_BASE64" | base64 -d > test.jks
keytool -list -keystore test.jks
```

### "SonarCloud analysis failed"

1. Verify `SONAR_TOKEN` secret is set
2. Check organization name matches your GitHub username
3. Ensure the project exists in SonarCloud

### "Release build failed"

Check all four secrets are set correctly:
- `KEYSTORE_BASE64`
- `KEYSTORE_PASSWORD`
- `KEY_ALIAS`
- `KEY_PASSWORD`
