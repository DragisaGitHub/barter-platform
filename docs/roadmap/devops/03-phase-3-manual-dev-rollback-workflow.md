# DevOps Roadmap 03 — Phase 3 Manual DEV Rollback Workflow

> Outcome target: a separate manually triggered GitHub Actions workflow performs DEV rollback by invoking the existing rollback script on the server.

# Goal

- Provide a fast, explicit, operator-triggered rollback path for DEV.
- Keep `deployment/scripts/rollback-dev.sh` as the source of rollback behavior.
- Make rollback available without requiring an engineer to SSH manually during an incident.

# Scope

## In scope for this phase

- A future dedicated `workflow_dispatch` rollback workflow for DEV.
- Remote SSH execution on the DEV server.
- Default rollback using the latest captured deployment state.
- Optional operator inputs for explicit backend and frontend image refs when needed.
- Logging of who triggered the rollback, when it ran, and which image refs were selected.

## Recommended workflow behavior

1. Manual trigger only.
2. Present a default mode that runs `./deployment/scripts/rollback-dev.sh` with no image overrides.
3. Optionally allow advanced inputs for:
   - state file path
   - backend image ref override
   - frontend image ref override
4. SSH to the DEV server.
5. Refresh the repo checkout if rollback should use the latest script version.
6. Execute the rollback script.
7. Capture post-rollback health outcome in the workflow log.

## Rollback operating model

- Primary rollback path should use the latest state captured before deploy.
- The workflow should not attempt database restore automatically.
- The workflow should not delete Docker volumes, prune images, or run destructive cleanup.
- Rollback stays image-based for backend and frontend only.

# Required files and secrets

## Required repository files

- `deployment/scripts/rollback-dev.sh`
- `deployment/scripts/capture-deployment-state.sh`
- `deployment/docs/DEV_DEPLOYMENT.md`
- `deployment/env/dev.env.example`

## Required GitHub secrets or variables for later implementation

- all DEV SSH secrets from Phase 1
- optional workflow inputs for explicit rollback image refs

## Required server-side files and state

- `deployment/env/dev.env`
- `deployment/state/dev/latest.env` for the default rollback path
- previously captured deployment state files under `deployment/state/dev/`
- access to pull the rollback target images if they are not already cached on the host

# Explicit non-goals

- Automatic rollback on deploy failure.
- Automatic database restore.
- Multi-environment rollback orchestration.
- Rollback approval automation beyond normal GitHub manual-trigger controls.
- Editing rollback script behavior.

# Validation checklist

- [ ] Rollback is documented as a separate manual workflow, not folded into the deploy workflow.
- [ ] The default rollback path uses the latest captured deployment state.
- [ ] Explicit image override inputs are documented as optional, not required.
- [ ] `deployment/scripts/rollback-dev.sh` remains the execution source of truth.
- [ ] The workflow avoids destructive actions such as DB restore or Docker prune.
- [ ] Operators can identify which image refs were rolled back to.
- [ ] Post-rollback health is checked and visible in workflow logs.

# Risks

- If state capture is missing or stale, the default rollback may not target the expected image refs.
- If a deployment included incompatible database changes, image rollback alone may not recover the system.
- If the rollback workflow exposes too many advanced options, operators may misuse it during stress.
- If rollback is implemented inside YAML instead of the script, the deploy and rollback paths will drift.
- If rollback targets mutable tags instead of immutable refs, recovery may be ambiguous.

