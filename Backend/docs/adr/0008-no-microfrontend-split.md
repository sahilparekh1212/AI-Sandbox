# ADR-0008: No microfrontend split — one standalone Angular SPA, not shell + remotes

**Status:** Accepted (deliberately not built — this ADR is the "articulate the decision" outcome,
not a commitment to never split). Parallels [ADR-0005](0005-no-api-gateway-yet.md), which reached
the same "don't add the indirection yet" conclusion on the backend edge.

## Context

The `UI/` TODO proposed a microfrontend (MFE) architecture: a `shell` host plus `auth-mfe` and
`audit-mfe` remotes loaded at runtime (Module Federation / Native Federation), with a `shared`
library for cross-cutting code. The SPA has since been built — but as a **single standalone Angular
19 application**: a shell component (header nav + router-outlet), an auth feature (login, OAuth
callback, profile) wired through an HTTP interceptor + route guard, and an audit feature (paginated
table + stats). Two feature domains, one build, one deployable.

The question this ADR settles: split that single app into federated microfrontends, or not.

## Decision

Keep it as one SPA. Do **not** split into a shell + remotes.

Microfrontends exist to buy things this project doesn't need: **independent deploy cadence** per
feature, **team autonomy** over separate codebases, **tech-stack independence** (one remote on a
different framework or Angular version), and **fault/again isolation** at the remote boundary. All
four presuppose organizational or scale pressures that a solo-built, two-feature portfolio app
deployed as a single artifact simply doesn't have. Auth and Audit ship together, are written by one
person in one Angular 19 workspace, and are released as one nginx-served bundle — so every benefit
of federation would be latent, while every cost is immediate.

And the costs are real: a runtime remote-loading step (a network round-trip to fetch each remote's
entry before its routes resolve), **shared-dependency version skew** to manage across host and
remotes (the classic Federation footgun — Angular/RxJS singletons must line up), a more elaborate
build and CI (per-project builds, federation manifests, a dev-time loader), and harder local
development (multiple dev servers to run one app). That's a lot of machinery to administer a split
the deployment topology doesn't reflect.

Crucially, the *one* concrete benefit people reach MFE for early — **lazy-loading feature code so
the initial bundle stays small** — Angular already provides *within a single app* via lazy-loaded
routes (`loadComponent`/`loadChildren`). We get code-splitting without the distributed-system tax.

## What would change the calculus

Revisit this ADR (and reach for `@angular-architects/native-federation`, the current Angular tool
over the older webpack Module Federation plugin) once any of these appear:

1. **Independent deploy cadence** — auth and audit genuinely need to ship on different schedules
   without redeploying each other.
2. **Separate teams/ownership** — different people own auth vs. audit and want isolated codebases,
   pipelines, and release control.
3. **Tech divergence** — a feature wants a different framework, or to upgrade Angular on its own
   timeline instead of lock-step with the rest.
4. **Many feature domains** — the app grows well past two features, to the point the single
   workspace becomes the coordination bottleneck.
5. **Runtime composition of remotes not built here** — e.g. embedding a third-party or
   separately-owned remote the shell doesn't build.

## Alternatives considered

- **Full Native Federation split (shell + auth-mfe + audit-mfe + shared).** The TODO's target and
  the strongest "I can do MFE" signal. Rejected for now: it's architecture layered on top of
  working features to solve problems (independent deploy, team autonomy) this project doesn't have —
  the same "solving a problem the system doesn't have yet" reasoning as ADR-0005's gateway.
- **A `shared` library without the network split.** If code reuse across features is the actual
  goal, an Angular library (`ng generate library`) gives it with zero runtime federation cost. Not
  built yet because the two features share little beyond the auth core, which already lives in a
  cohesive `core/auth` folder; worth doing the moment a third feature reuses it.
- **Webpack Module Federation.** Even if we split, this would be the wrong tool on Angular 19 —
  Native Federation is the maintained, esbuild-compatible path. Noted so a future revisit doesn't
  default to the older plugin.

## Consequences

- The UI stays a **single deployable** (one nginx container), which keeps the compose/OpenShift
  story and CI simple — no per-remote build, manifest, or version-alignment step.
- This also keeps [ADR-0005](0005-no-api-gateway-yet.md) stable: that ADR named "the microfrontend
  split actually gets built" as one trigger for adding a BFF/gateway. Since the split isn't
  happening, that trigger doesn't fire either — the SPA continues to call Auth and Audit directly.
- If any trigger above shows up, this is an additive migration (host-ify the current app, extract
  remotes) — no business logic changes — so deferring costs little.
