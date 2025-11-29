<div align="center">
<img src="https://github.com/XDdevv/Github-Store/blob/main/composeApp/src/commonMain/composeResources/drawable/app-icon.png" width="200" alt="Project logo"/>
</div>

<h1 align="center">Github Store</h1>

<p align="center">
  <a href="https://opensource.org/licenses/Apache-2.0"><img alt="License" src="https://img.shields.io/badge/License-Apache%202.0-blue.svg"/></a>
  <a href="https://kotlinlang.org"><img alt="Kotlin" src="https://img.shields.io/badge/Kotlin-Multiplatform-7F52FF.svg"/></a>
  <a href="#"><img alt="Platforms" src="https://img.shields.io/badge/Platforms-Android%20%7C%20Desktop-brightgreen"/></a>
</p>

<p align="center">
GithubStore is a cross‑platform “play store” for GitHub releases.  
It discovers repositories that ship real installable binaries and lets you install the latest release in one click.
</p>

<p align="center">
  <img src="screenshots/banner.png" />
</p>

---

### All screenshots can be found in [screenshots/](screenshots/) folder.

<img src="/screenshots/preview.gif" align="right" width="320"/>


## ✨ What is GithubStore?

GithubStore is a Kotlin Multiplatform app (Android + Desktop) that turns GitHub releases into a clean, app‑store style experience:

- Only shows repositories that actually provide installable assets (APK, EXE, DMG, etc.).
- Detects your platform and surfaces the correct installer.
- Always installs from the **latest published release** and highlights its changelog.
- Presents a polished details screen with stats, README, and developer info.

The repository is currently private while the core is being stabilized, but it will be public and fully open source under the Apache 2.0 license very soon.

---

## 🔃 Download
Go to the [Releases](https://github.com/rainxchzed/Github-Store/releases) to download the latest Installer.

## 🚀 Features

- **Smart discovery**
  - Home sections for “Popular”, “Recently Updated”, and “New” projects.
  - Only repos with valid installable assets are shown.
  - Platform‑aware topic scoring so Android/desktop users see relevant apps first.

- **Latest‑release installs**
  - Fetches `/releases/latest` for each repo.
  - Shows only assets from the latest release.
  - Single “Install latest” action, plus an expandable list of all installers for that release.

- **Rich details screen**
  - App name, version, “Install latest” button.
  - Stars, forks, open issues, contributors.
  - Rendered README content (“About this app”).
  - Latest release notes (body) with markdown formatting.
  - List of installers with platform labels and file sizes.

- **Cross‑platform UX**
  - Android: opens APK downloads and hands off to the package installer.
  - Desktop (Windows/macOS/Linux): downloads to a configurable location and opens the file.

- **GitHub login & rate‑limit friendly**
  - Sign‑in with GitHub to use the user’s own 5,000 req/hour quota.
  
---

## 🔍 How does my app appear in GithubStore?

GithubStore does not use any private indexing or manual curation rules.  
Your project can appear automatically if it follows these conditions:

1. **Public repository on GitHub**
   - Visibility must be `public`.

2. **At least one published release**
   - Created via GitHub Releases (not only tags).
   - The latest release must not be a draft or prerelease.

3. **Installable assets in the latest release**
   - The latest release must contain at least one asset file with a supported extension:
     - Android: `.apk`
     - Windows: `.exe`, `.msi`
     - macOS: `.dmg`, `.pkg`
     - Linux: `.deb`, `.rpm`
   - GithubStore ignores GitHub’s auto‑generated source artifacts (`Source code (zip)` / `Source code (tar.gz)`).

4. **Discoverable by search / topics**
   - Repositories are fetched via the public GitHub Search API.
   - Topic, language, and description help the ranking:
     - Android apps: topics like `android`, `mobile`, `apk`.
     - Desktop apps: topics like `desktop`, `windows`, `linux`, `macos`, `compose-desktop`, `electron`.
   - Having at least a few stars makes it more likely to appear under Popular/Updated/New sections.

If your repo meets these conditions, GithubStore can find it through search and show it automatically—no manual submission required.

---

## 🧭 How GithubStore works (high‑level)

1. **Search**
   - Uses GitHub’s `/search/repositories` endpoint with platform‑aware queries.
   - Applies simple scoring based on topics, language, and description.
   - Filters out archived repos and those with too few signals.

2. **Release + asset check**
   - For candidate repos, calls `/repos/{owner}/{repo}/releases/latest`.
   - Checks the `assets` array for platform‑specific file extensions.
   - If no suitable asset is found, the repo is excluded from results.

3. **Details screen**
   - Repository info: name, owner, description, stars, forks, issues, contributors.
   - Latest release: tag, published date, body (changelog), assets.
   - README: loaded from the default branch and rendered as “About this app”.

4. **Install flow**
   - When the user taps “Install latest”:
     - Picks the best matching asset for the current platform.
     - Streams the download.
     - Delegates to the OS installer (APK installer on Android, default handler on desktop).

---

## 🧱 Tech stack

- **Language & Platform**
  - Kotlin Multiplatform (Android + JVM Desktop)
  - Compose Multiplatform UI

- **Networking & Data**
  - Ktor `HttpClient` for GitHub REST API.
  - kotlinx.serialization for JSON models.
  - Kotlinx.coroutines + Flow for async + streaming search results.

- **Architecture**
  - Clean modular design with `core` (domain/models) and feature modules.
  - Repository pattern for data access.
  - ViewModel/state holder per screen (platform‑specific wrapper around shared logic).

- **Auth & Deep‑links**
  - GitHub OAuth (Device Code flow).

---

## ✅ Pros / Why use GithubStore?

- **No more hunting through GitHub releases**  
  See only repos that actually ship binaries for your platform.

- **Always the latest release**  
  Installs are guaranteed to come from the latest published release; the changelog you see is exactly what you’re installing.

- **Uniform experience across platforms**  
  Same UI and logic for Android and desktop, with platform‑native install behavior.

- **Open source & extensible**  
  Written in KMP with a clear separation between networking, domain logic, and UI—easy to fork, extend, or adapt.

---

## 🔑 Configuration

GithubStore uses a GitHub OAuth app for authentication and API rate‑limit isolation.

1. Create a GitHub OAuth app at **GitHub → Settings → Developer settings → OAuth Apps**.
2. Set the callback URL to `githubstore://callback` (_Not quite important_).
3. Copy the **Client ID** from the OAuth app.
4. In your project’s `local.properties`, add:

---

## ⚠️ Disclaimer

GithubStore only helps you discover and download release assets that are already published on GitHub by third‑party developers.  
The contents, safety, and behavior of those downloads are entirely the responsibility of their respective authors and distributors, not this project.  

By using GithubStore, you understand and agree that you install and run any downloaded software at your own risk.  
This project does not review, validate, or guarantee that any installer is safe, free of malware, or fit for any particular purpose.


## 📄 License

GithubStore will be released under the **Apache License, Version 2.0**.
```
Copyright 2025 rainxchzed

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this project except in compliance with the License.
You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
