# Future Backend Architecture

Version 1 of Price Tracker checks prices **on-device**, on a schedule, via
`LocalPriceChecker` (implements the `PriceChecker` interface in
`domain/PriceChecker.kt`). This document explains how a backend could later
take over that job without requiring a rewrite of the Android app.

## Why move checking to a backend eventually?

- The device does not need to be online/awake at exactly 09:00/19:00/01:00.
- A server can run *actually* scheduled jobs (cron, Cloud Scheduler, etc.)
  instead of Android's best-effort WorkManager windows.
- Centralised checking is more efficient if you ever track the same product
  URL across multiple users/devices.

## How the app is already structured for this

```
Android App (UI, Room DB)
        |
        v
  PriceChecker  <-- interface, this is the seam
   /         \
LocalPriceChecker      RemotePriceChecker (future)
(fetch + parse          (calls a backend API,
 on-device)               backend does the work)
```

Nothing in the repository, ViewModels, or UI depends on `LocalPriceChecker`
directly — they depend on the `PriceChecker` interface. Swapping the
implementation wired up in `PriceTrackerApp.onCreate()` is the only change
needed to point the app at a backend instead.

## Sketch of a possible backend

1. Android app registers each tracked product (URL + target price) with a
   backend API instead of (or in addition to) checking it locally.
2. A scheduled backend job (Cloud Scheduler, GitHub Actions cron, a Cloud Run
   job, etc.) runs at 09:00 / 19:00 / 01:00 **server time per user**, or in
   UTC with per-user offset handling.
3. The backend re-uses the same extraction priority (JSON-LD → Open Graph →
   HTML fallback) — that logic is pure and has no Android dependencies, so it
   could be ported to a Node/Python/Kotlin-server service largely as-is.
4. The backend compares the new price to the stored target price.
5. If reached, the backend sends a push notification via Firebase Cloud
   Messaging (FCM) to the user's device(s).
6. The device receives the push and shows a local notification exactly like
   `NotificationHelper.sendPriceAlert` does today.

## Suggested technologies (not implemented in v1)

- **Firebase** (Auth optional, Firestore for shared product state, Cloud
  Messaging for push) — fastest to stand up.
- **Cloud Run / Cloud Functions** — for the actual scraping job, since it
  needs a real HTTP client and can scale to zero between the three daily runs.
- **Supabase** — an open-source alternative to Firebase if you'd rather not
  depend on Google.
- **GitHub Actions on a schedule** — surprisingly viable for a personal-scale
  version: a scheduled workflow could run the checker script and write
  results to a small database, with no server to maintain at all.

## What v1 deliberately does NOT do

- No account system, no server, no data leaves the device (see project rule
  32/33 in the original spec).
- No currency conversion.
- No JavaScript rendering (a backend running a headless browser could add
  this later for sites that require it — see "Known Limitations" in the
  main README).
