# AI-Sandbox UI

The Angular SPA fronting the AI-Sandbox backend: sign in (demo login or Google OAuth), browse
the audit dashboard, chat with the guarded LLM assistant, and generate flashcard study decks.
Standalone components, signals, no UI framework — the dark theme is a single hand-rolled
design-token layer in [`src/styles.scss`](src/styles.scss).

One deliberately boring architecture decision up front: this is **one SPA, not a
microfrontend federation** — see
[ADR-0008](../Backend/docs/adr/0008-no-microfrontend-split.md) for why, and what would have
to change to revisit that.

## What's inside

| Route                       | What it does                                                           | Auth    |
| --------------------------- | ---------------------------------------------------------------------- | ------- |
| `/`                         | About — tech stack, design decisions, feature tour                     | public  |
| `/login`, `/login/callback` | Demo login form, Google OAuth fragment handoff                         | public  |
| `/audit`                    | Server-side paginated/sorted/filtered audit table + CSS-only stat bars | guarded |
| `/assistant`                | Chat over the server-side Claude proxy                                 | guarded |
| `/flashcards`               | Generated Q&A study deck with flip/next/shuffle                        | guarded |
| `/profile`                  | `/auth/me` profile view (avatar icon, top-right)                       | guarded |

The auth plumbing lives in [`src/app/core/auth/`](src/app/core/auth/): a functional HTTP
interceptor that attaches `Authorization: Bearer` (our APIs only) and does a single silent
refresh-and-retry on 401, a route guard with `returnUrl`, the OAuth `#access_token` fragment
consumption (followed by `history.replaceState` so tokens don't linger in history), and a
localStorage token store — an explicit tradeoff vs httpOnly cookies, documented where it's
implemented.

## Run it

**Against the full Docker stack (no Node needed):** from `Backend/`,
`docker compose up --build` serves the production bundle through nginx at
<http://localhost:4200> (demo login: `demo` / `demo`).

**Dev server with live reload:**

```bash
npm ci
npm start            # ng serve on http://localhost:4200
```

The dev build ([`environment.development.ts`](src/environments/environment.development.ts))
calls Auth on `:8085` and Audit on `:8083` directly — both must be running (the compose stack
works, or `gradlew bootRun` per the backend README) and both already allow CORS from
`http://localhost:4200`. The production build instead uses same-origin `/auth-api` and
`/audit-api` paths that nginx proxies inside the compose network, so the deployed SPA never
hardcodes a backend host and needs no CORS at all — see [`nginx.conf`](nginx.conf) for the
proxy rules (including the `X-Forwarded-*` headers Google OAuth needs to survive the proxy).

## Scripts

```bash
npm start            # dev server
npm run build        # production build → dist/ai-sandbox-ui/browser
npm test             # Karma/Jasmine, watch mode
npm run test:ci      # headless single run (what Frontend CI runs)
npm run lint         # angular-eslint
npm run format       # prettier --write
npm run format:check # what CI enforces
```

`Frontend CI` (`.github/workflows/frontend-ci.yml`) runs format check, lint, prod build, and
the headless unit tests on every PR touching `UI/**`.

## Testing

Unit/component specs sit next to their sources (`*.spec.ts`) and run under Karma/Jasmine with
a CI-safe `ChromeHeadlessNoSandbox` launcher. Browser end-to-end tests are deliberately **not**
here — the Playwright suite in the top-level [`e2e/`](../e2e/) package runs against the real
compose stack (login through nginx, Kafka-produced audit rows appearing in the table), because
the interesting failures live in the seams between the SPA and the system, not inside the SPA.

## Container

[`Dockerfile`](Dockerfile) is a two-stage build: `npm ci` + prod build on Node 22 (matching
Frontend CI), then `nginx:1.28-alpine` serving the bundle with SPA fallback, immutable cache
headers on the content-hashed bundles, `no-cache` on `index.html`, and the two API reverse
proxies described above. The compose `ui` service publishes it on host `:4200`.
