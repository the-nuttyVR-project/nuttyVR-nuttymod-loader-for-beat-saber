# nuttyVR website

Site URL: https://the-nuttyvr-project.github.io/nuttyVR-nuttymod-loader-for-beat-saber/

The site presents the Hub download, the five-step installation checklist, the five companion APKs, the demo game, the source project and compatibility status. The primary Hub ZIP and full toolkit ZIP use the verified nightly.link workflow URLs. Direct APK downloads serve the checked-in signed 0.1.0 snapshot; ZIP downloads must be extracted before installation. Beat Saber is explicitly marked as requiring an adapter.

## Local build

Run `python3 website/build.py` from the repository root. The Python builder verifies the APK checksums and Android-source fingerprint from `prebuilt/nightly/build.json`, copies the static files and APKs into `website/dist/`, and packages source and APK archives. It does not require Node.js, Gradle, the Android SDK or a signing key. All browser assets and direct downloads use relative URLs suitable for a GitHub Pages project subpath.

The checklist saves progress only in the current browser's local storage. It remains usable when local storage is disabled. No account, analytics or server-side application is required. Google Fonts is optional; the page uses system fonts if it is unavailable.

## Hosting and updates

`.github/workflows/pages.yml` builds and publishes `website/dist/` on relevant pushes to `main` or a manual **Publish website** workflow run. It uses the `github-pages` environment and GitHub's official Pages upload/deploy actions. The repository's **Settings → Pages → Build and deployment → Source** must be **GitHub Actions**. The workflow does not publish to another hosting provider.

Edit `website/src/` for content, style and checklist changes. The generated `website/dist/` is ignored by Git and is rebuilt for deployment. When replacing APKs, update the complete signed snapshot and its record together. The builder rejects stale or modified snapshots rather than publishing downloads that do not match the Android source.

The source archive excludes signing keys, generated outputs, Git metadata, local machine configuration and APK snapshots. Direct download SHA-256 values are available at `downloads/SHA256SUMS` on the website. `downloads/RELEASE-SHA256SUMS` covers the two ZIP downloads.
