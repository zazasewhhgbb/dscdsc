# Price Tracker

A personal Android app that watches a product page's price and notifies you the moment it
drops to (or below) a price you set — no account, no server, everything stored on-device.

Paste a product URL → the app reads the page's own price/currency → you pick a target →
you get a notification when it's hit.

## What the interface looks like

Home screen is a **grid of your tracked products**, each tile showing its photo, current
price, target price, and status. The first tile in the grid is always **+ Add product** —
tapping it opens a bottom sheet where you paste a URL, hit **TEST LINK**, review what the app
found, set a target price (drag the slider or type an exact number — it's always in the
currency the page itself uses, never a different one), and save.

Tapping a product's photo opens it in your browser. Each tile has its own refresh icon and a
small colored status dot (green once the target is reached, red if the last check failed).

A bottom nav bar switches between **Home** and **Settings** (toggle automatic checking and
notifications on/off).

## How price extraction works

`ProductParser` tries four strategies in order, and never invents a price it isn't confident
about:

1. **JSON-LD** (`<script type="application/ld+json">`) — the most reliable source, since it's
   structured data sites publish for search engines/Google Shopping. Handles single objects,
   arrays, `@graph` wrappers, multiple `<script>` blocks, and multiple `offers` (it picks the
   lowest currently-available offer, i.e. the sale price, not a crossed-out original).
2. **Open Graph / product meta tags** (`og:title`, `product:price:amount`, etc.)
3. **Standard HTML metadata** (`<title>`, Twitter Card tags)
4. **Common ecommerce HTML patterns** (`itemprop=price`, `.product-price`, `[data-price]`, …) —
   deliberately a *small* set of generic patterns, not a giant per-website selector library.

If none of these find a confidently-parseable price, the app tells you plainly that it
couldn't read the page — it never guesses or displays a fake price.

### Price number parsing

`PriceParser` handles the fact that "1.499" means 1499 in Norway but could mean 1.499 (about
one and a half) elsewhere, and "1,499" is 1499 in the US but a decimal in parts of Europe. It
resolves this from the digit pattern rather than guessing from currency alone: whitespace is
always a thousands separator; a lone separator followed by 3 digits is treated as thousands
grouping, 1–2 digits as a decimal point; if both `.` and `,` appear, whichever comes last is
the decimal separator. Genuinely ambiguous formats are rejected rather than guessed at. It also
recognizes the Nordic "kr, no øre" notation used by sites like XXL.no, where a trailing `,-` or
`.-` (e.g. `1799,-`) means "and zero øre," not a minus sign. See `PriceParserTest.kt` for every
case this covers.

### Which websites are likely to work

Most modern ecommerce platforms (Shopify, WooCommerce, Magento, and most storefronts built for
SEO/Google Shopping) publish JSON-LD Product data, so those tend to work well out of the box.

### Why some websites won't work

- Sites that render the price entirely with JavaScript after page load — this app makes a
  plain HTTP GET request and parses the returned HTML; it does not run a browser engine.
- Sites protected by Cloudflare/bot-challenge pages, CAPTCHAs, or that require login — the app
  deliberately does **not** attempt to bypass any of these (see "Limitations" below).
- Sites whose `robots.txt` disallows automated access to the page. `RobotsTxtChecker` fetches
  and parses the site's `robots.txt` (the generic `*` group — this app identifies as a normal
  browser, not a named bot) before every request and simply refuses to fetch a disallowed page,
  telling the user why. If `robots.txt` itself can't be fetched or parsed, the page is treated
  as allowed rather than blocked, since many sites don't publish one at all — but an explicit
  `Disallow` that matches is always honored. See `RobotsTxtChecker.kt` /
  `RobotsTxtCheckerTest.kt`.
- Sites with a genuinely non-standard, unparseable price format.

In every one of these cases you'll see a clear message rather than a wrong or fake price.

## Automatic checking — and its real limitation

Tracked products are checked automatically around **09:00, 19:00 and 01:00**, in your phone's
current local timezone. This uses `WorkManager`, scheduled as a chain of one-off requests that
each reschedule themselves for the next occurrence of their time slot.

**Android does not guarantee exact-time execution for this kind of background work.** Doze
mode, battery optimization, and OS-level batching can delay a check by anywhere from a few
minutes to a few hours if the phone is idle/asleep right at the scheduled moment. Treat these
as *scheduled check windows*, not a promise of second-accurate timing. The app deliberately
does **not** run a persistent foreground service to force exact timing — that would keep the
CPU/radio active continuously and drain the battery. See `WorkScheduler.kt` for the full
reasoning, and `docs/BACKEND.md` for how a future server-side scheduler could offer exact
timing instead.

## Notifications

One notification per product; it won't repeat every check while the price stays below target.
If the price rises back above your target and later dips again, you'll get a fresh alert. See
`PriceComparator.kt` / `PriceComparatorTest.kt` for the exact re-arm logic.

## Architecture

```
ui/            Jetpack Compose screens, ViewModels (no networking/DB code here)
domain/        PriceChecker interface + LocalPriceChecker, URL normalization, price comparison
data/
  parser/      JSON-LD / Open Graph / HTML-fallback / price-number parsing (pure Kotlin, no
               Android dependencies — easy to unit test, easy to port to a backend later)
  network/     OkHttp-based page fetching with full error classification + robots.txt check
  database/    Room entities, DAOs
  repository/  Ties parsing + network + database together; rate-limits itself
notifications/ Notification channel + the single price-alert notification
workers/       WorkManager scheduling for the three daily check windows
```

`PriceChecker` is an interface specifically so the whole app doesn't depend on checking
happening on-device — see `docs/BACKEND.md` for how a future backend implementation could
slot in without touching the UI, database, or ViewModels at all.

## Known limitations

- No currency conversion — the target price is always in the same currency the page reported;
  there's no way to enter a target in a different currency (this is intentional, not a bug).
- No price history chart yet (the data is being recorded in the `price_history` table already,
  ready for this).
- JavaScript-rendered prices aren't supported (would require a headless browser — out of scope
  for an on-device app; a backend could add this later, see `docs/BACKEND.md`).
- The app does not, and will not, attempt to bypass CAPTCHAs, Cloudflare challenges, logins,
  or rate limits. If a site blocks it, it fails gracefully and says so.
- A handful of user-facing error strings live directly in `LocalPriceChecker.kt` rather than
  `strings.xml`, because that class is deliberately plain Kotlin with zero Android framework
  dependency (no `Context`), which is what keeps it trivially unit-testable and portable to a
  future backend. This is a deliberate trade-off, not an oversight.

## Building

### Locally (Android Studio — recommended)

1. Open this folder in Android Studio (Jellyfish or newer). It will generate the Gradle
   wrapper (`gradlew`, `gradle-wrapper.jar`) for you automatically on first sync.
2. Run the app on a device/emulator, or **Build > Build Bundle(s) / APK(s) > Build APK(s)**.

### Locally (command line)

This repository does not include a pre-built `gradle-wrapper.jar` binary. Generate one once
with any local Gradle install (or Android Studio's bundled Gradle):

```bash
gradle wrapper --gradle-version 8.7
```

Then build as usual:

```bash
./gradlew assembleDebug
./gradlew testDebugUnitTest   # run the unit tests
```

The debug APK will be at `app/build/outputs/apk/debug/app-debug.apk`.

### GitHub Actions

Push this repository to GitHub, then open the **Actions** tab — `.github/workflows/build-apk.yml`
runs automatically on every push to `main` (and can be triggered manually via
"Run workflow"). It runs the unit tests, builds the debug APK, and uploads it as a workflow
artifact you can download directly from the run's summary page. (The workflow installs Gradle
itself rather than relying on a wrapper jar, for the same reason noted above.)

## Installing the APK

Download the APK (from a local build or a GitHub Actions artifact) onto your phone, then open
it — Android will prompt you to allow installs from that source if you haven't already. On
first launch you'll be asked to allow notifications (Android 13+); that's the only runtime
permission the app requests, alongside plain internet access.

## Testing a product

1. Paste a product URL into the **Add product** sheet and tap **TEST LINK**.
2. If found, you'll see the photo, name, and current price/currency the page reported.
3. Drag the slider or type an exact target price, then **SAVE PRODUCT**.
4. Use the refresh icon on the tile any time to check it immediately rather than waiting for
   the next scheduled window.

## Testing the code

Unit tests cover URL validation/normalization/dedup, price-string parsing (every format called
out above, including deliberately-ambiguous ones that must be rejected), currency detection,
JSON-LD parsing (single object, arrays, `@graph`, multiple blocks, multiple offers), the
Open Graph and HTML-fallback strategies against 9 sample HTML fixtures, target-price
comparison, and the notification re-arm logic. Run them with:

```bash
./gradlew testDebugUnitTest
```

## Future improvements

- Backend-based price checking with exact scheduling (see `docs/BACKEND.md`)
- Push notifications via Firebase Cloud Messaging once a backend exists
- Broader ecommerce site support, JavaScript rendering for sites that need it
- Price history charts (the underlying data is already being recorded)
- Currency conversion
- Multiple target prices / percentage-discount alerts / stock & availability alerts
- Cloud sync, optional account system, import/export (CSV)
- Dark mode (the app already follows system dynamic color on Android 12+; a full custom dark
  palette is the remaining piece)
