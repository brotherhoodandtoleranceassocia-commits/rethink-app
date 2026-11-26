# HARES security layer

HARES is the branded Android build of this fork of Rethink DNS & Firewall.

## Android compatibility

- The current upstream Android minimum is API 23 (Android 6.0); this fork keeps that minimum.
- The HARES Android application ID is `com.hares.security`; the internal Kotlin namespace remains the upstream namespace to preserve integration with the Rethink source.

## Native components

- The underlying Rethink engine remains responsible for Android's local VPN, DNS filtering, firewall rules, per-app network controls, and connection tracking.
- The HARES screen adds offline URL heuristics, a local incident history, Android security notifications, and an Android share target for text links.
- Users can open the HARES screen from the About page or share a link from another app to HARES.

## Important security boundaries

- A local URL heuristic is not a threat-intelligence lookup and cannot certify a link as safe.
- The HARES share target analyzes a link after the user shares it; it does not silently intercept every link opened by every app.
- Network protection depends on the user granting Android VPN permission and enabling the Rethink DNS/firewall protection features.
- Android restrictions, OEM battery management, always-on VPN settings, and app-specific exemptions can affect protection behavior.
- No software can guarantee that every banking app, payment flow, or phishing attempt is protected on every Android device.

## Upstream attribution

This project is based on the upstream Rethink DNS & Firewall project by Celzero and retains its Apache-2.0 licensing and notices. The HARES-specific code is an additional layer; it does not replace or claim authorship of the upstream VPN/firewall engine.

## Build the installable APK

The repository includes an Android CI workflow that runs lint, HARES URL scanner regression tests, and builds a universal debug APK artifact named `HARES-ANDROID-UNIVERSAL-APK`. GitHub may require the repository owner to enable Actions for a newly created fork before the first workflow run. The artifact is intended for device testing; a stable production signing key must be configured before distributing upgradeable release builds.
