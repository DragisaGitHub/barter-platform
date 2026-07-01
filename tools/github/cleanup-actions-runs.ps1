<#
.SYNOPSIS
    Cleans up GitHub Actions workflow run history using the GitHub CLI.

.DESCRIPTION
    Deletes old GitHub Actions workflow runs from the current repository.
    Supports filtering by workflow name, status/conclusion, and keeping the newest N runs.
    Never deletes workflow YAML files — only run history records.

.PARAMETER WorkflowName
    Optional. Filter runs to a specific workflow by name or filename (e.g. "CI" or "ci.yml").

.PARAMETER Status
    Optional. Filter runs by status or conclusion.
    Allowed values: completed, failure, success, cancelled, skipped

.PARAMETER KeepLast
    Optional. Number of most-recent matching runs to keep. Default: 0 (delete all matching).

.PARAMETER DryRun
    Switch. If set, only prints what would be deleted without actually deleting anything.

.PARAMETER Yes
    Switch. Skip interactive confirmation prompt (useful for CI or automation).

.EXAMPLE
    # Preview all runs that would be deleted (safe default)
    .\cleanup-actions-runs.ps1 -DryRun

.EXAMPLE
    # Delete all failed runs, keeping the last 3, with confirmation prompt
    .\cleanup-actions-runs.ps1 -Status failure -KeepLast 3

.EXAMPLE
    # Delete all runs for a specific workflow without prompting
    .\cleanup-actions-runs.ps1 -WorkflowName "CI" -Yes

.EXAMPLE
    # Dry-run: show what would be deleted for a specific workflow and status
    .\cleanup-actions-runs.ps1 -WorkflowName "ci.yml" -Status success -KeepLast 5 -DryRun

.EXAMPLE
    # Delete all completed runs across all workflows (non-interactive)
    .\cleanup-actions-runs.ps1 -Status completed -Yes
#>

[CmdletBinding(SupportsShouldProcess)]
param(
    [Parameter()]
    [string]$WorkflowName,

    [Parameter()]
    [ValidateSet('completed', 'failure', 'success', 'cancelled', 'skipped')]
    [string]$Status,

    [Parameter()]
    [ValidateRange(0, [int]::MaxValue)]
    [int]$KeepLast = 0,

    [Parameter()]
    [switch]$DryRun,

    [Parameter()]
    [switch]$Yes
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

# ── Helpers ──────────────────────────────────────────────────────────────────

function ConvertTo-UtcDateTime {
    <#
    .SYNOPSIS
        Robustly parses a datetime string into a UTC DateTime, regardless of system locale.
        Handles ISO 8601 (from gh api/gh run list --json on most platforms) as well as
        common locale-formatted strings that gh run list may emit on Windows.
    #>
    param([string]$Value)

    $invariant = [System.Globalization.CultureInfo]::InvariantCulture
    $roundtrip = [System.Globalization.DateTimeStyles]::RoundtripKind
    $utcStyles  = [System.Globalization.DateTimeStyles]::AssumeUniversal `
                  -bor [System.Globalization.DateTimeStyles]::AdjustToUniversal

    # 1. Try generic Parse with InvariantCulture + RoundtripKind — covers ISO 8601
    try {
        return [System.DateTime]::Parse($Value, $invariant, $roundtrip)
    } catch { }

    # 2. Explicit format fallback for locale-formatted strings gh may return on Windows
    $formats = @(
        'MM/dd/yyyy HH:mm:ss',
        'MM/dd/yyyy h:mm:ss tt',
        'M/d/yyyy HH:mm:ss',
        'M/d/yyyy h:mm:ss tt',
        'dd/MM/yyyy HH:mm:ss',
        'd/M/yyyy HH:mm:ss',
        'yyyy/MM/dd HH:mm:ss',
        'yyyy-MM-ddTHH:mm:ssZ',
        'yyyy-MM-ddTHH:mm:sszzz',
        'yyyy-MM-ddTHH:mm:ss'
    )

    foreach ($fmt in $formats) {
        try {
            return [System.DateTime]::ParseExact($Value, $fmt, $invariant, $utcStyles)
        } catch { }
    }

    throw "Could not parse datetime value: '$Value'. Please report this format so it can be added."
}

function Write-Header {
    param([string]$Text)
    Write-Host ""
    Write-Host "═══════════════════════════════════════════════════" -ForegroundColor Cyan
    Write-Host "  $Text" -ForegroundColor Cyan
    Write-Host "═══════════════════════════════════════════════════" -ForegroundColor Cyan
}

function Write-Step {
    param([string]$Text)
    Write-Host "→ $Text" -ForegroundColor DarkCyan
}

function Write-Success {
    param([string]$Text)
    Write-Host "✔ $Text" -ForegroundColor Green
}

function Write-Warn {
    param([string]$Text)
    Write-Host "⚠ $Text" -ForegroundColor Yellow
}

function Write-Deleted {
    param([string]$Text)
    Write-Host "  🗑  $Text" -ForegroundColor Red
}

function Write-Kept {
    param([string]$Text)
    Write-Host "  ✦  $Text" -ForegroundColor DarkGray
}

# ── Preflight checks ─────────────────────────────────────────────────────────

Write-Header "GitHub Actions Run Cleanup"

Write-Step "Checking for GitHub CLI (gh)..."
if (-not (Get-Command gh -ErrorAction SilentlyContinue)) {
    Write-Error "GitHub CLI (gh) is not installed or not found in PATH. Install it from https://cli.github.com/"
    exit 1
}
Write-Success "gh found: $(gh --version | Select-Object -First 1)"

Write-Step "Checking gh authentication..."
$authStatus = gh auth status 2>&1
if ($LASTEXITCODE -ne 0) {
    Write-Error "Not authenticated with GitHub CLI. Run 'gh auth login' (locally) or ensure GH_TOKEN is set (CI)."
    exit 1
}
Write-Success "Authenticated."

Write-Step "Detecting repository..."
$repoJson = gh repo view --json nameWithOwner 2>&1
if ($LASTEXITCODE -ne 0) {
    Write-Error "Could not detect repository. Make sure you are inside a Git repository cloned from GitHub."
    exit 1
}
$repo = ($repoJson | ConvertFrom-Json).nameWithOwner
Write-Success "Repository: $repo"

# ── Fetch workflow runs ───────────────────────────────────────────────────────

Write-Step "Fetching workflow runs..."

# Build gh run list arguments
$listArgs = @(
    'run', 'list',
    '--repo', $repo,
    '--limit', '1000',
    '--json', 'databaseId,name,workflowName,status,conclusion,createdAt,displayTitle'
)

if ($WorkflowName) {
    $listArgs += @('--workflow', $WorkflowName)
}

# gh run list supports --status for filtering by status/conclusion
if ($Status) {
    $listArgs += @('--status', $Status)
}

$runsJson = & gh @listArgs 2>&1
if ($LASTEXITCODE -ne 0) {
    Write-Error "Failed to fetch workflow runs: $runsJson"
    exit 1
}

$runs = $runsJson | ConvertFrom-Json

if ($runs.Count -eq 0) {
    Write-Warn "No workflow runs found matching the given filters."
    exit 0
}

Write-Success "Found $($runs.Count) run(s) matching filters."

# ── Apply KeepLast ────────────────────────────────────────────────────────────

# Enrich each run with a parsed UTC datetime so sorting is locale-independent
$runs = $runs | ForEach-Object {
    $_ | Add-Member -NotePropertyName _createdAtUtc -NotePropertyValue (ConvertTo-UtcDateTime $_.createdAt) -PassThru
}

# Sort runs by creation date descending (newest first) to determine which to keep
$sorted = $runs | Sort-Object -Property _createdAtUtc -Descending

$toKeep   = @()
$toDelete = @()

if ($KeepLast -gt 0) {
    # Group by workflowName so KeepLast applies per-workflow
    $byWorkflow = $sorted | Group-Object -Property workflowName

    foreach ($group in $byWorkflow) {
        $groupRuns = @($group.Group)
        if ($groupRuns.Count -le $KeepLast) {
            $toKeep   += $groupRuns
        } else {
            $toKeep   += $groupRuns[0..($KeepLast - 1)]
            $toDelete += $groupRuns[$KeepLast..($groupRuns.Count - 1)]
        }
    }
} else {
    $toDelete = $sorted
}

# ── Summary ───────────────────────────────────────────────────────────────────

Write-Header "Deletion Plan"

if ($DryRun) {
    Write-Warn "DRY RUN mode — nothing will be deleted."
    Write-Host ""
}

Write-Host "  Runs to delete : $($toDelete.Count)" -ForegroundColor Red
Write-Host "  Runs to keep   : $($toKeep.Count)"   -ForegroundColor Green
Write-Host ""

if ($toDelete.Count -eq 0) {
    Write-Success "Nothing to delete."
    exit 0
}

# Print what would be / will be deleted
Write-Host "Runs marked for deletion:" -ForegroundColor White
foreach ($run in $toDelete) {
    $ts = $run._createdAtUtc.ToString("yyyy-MM-dd HH:mm")
    Write-Deleted "[$ts] [$($run.workflowName)] #$($run.databaseId) — $($run.displayTitle) (status=$($run.status) conclusion=$($run.conclusion))"
}

if ($toKeep.Count -gt 0) {
    Write-Host ""
    Write-Host "Runs to keep:" -ForegroundColor White
    foreach ($run in ($toKeep | Sort-Object -Property _createdAtUtc -Descending)) {
        $ts = $run._createdAtUtc.ToString("yyyy-MM-dd HH:mm")
        Write-Kept "[$ts] [$($run.workflowName)] #$($run.databaseId) — $($run.displayTitle)"
    }
}

# ── Confirmation ──────────────────────────────────────────────────────────────

if (-not $DryRun) {
    if (-not $Yes) {
        Write-Host ""
        Write-Warn "This will permanently delete $($toDelete.Count) workflow run(s) from $repo."
        Write-Host "NOTE: Workflow YAML files are NOT affected — only run history will be removed." -ForegroundColor DarkGray
        Write-Host ""
        $answer = Read-Host "Proceed? [y/N]"
        if ($answer -notmatch '^[Yy]$') {
            Write-Host "Aborted." -ForegroundColor Yellow
            exit 0
        }
    } else {
        Write-Warn "Skipping confirmation (-Yes was passed)."
    }
}

# ── Execute deletion ──────────────────────────────────────────────────────────

if ($DryRun) {
    Write-Host ""
    Write-Success "Dry run complete. Re-run without -DryRun to perform actual deletion."
    exit 0
}

Write-Header "Deleting Runs"

$deleted = 0
$failed  = 0

foreach ($run in $toDelete) {
    $id = $run.databaseId
    Write-Step "Deleting run #$id ($($run.workflowName) — $($run.displayTitle))..."
    $result = gh run delete $id --repo $repo 2>&1
    if ($LASTEXITCODE -eq 0) {
        $deleted++
        Write-Success "Deleted #$id"
    } else {
        $failed++
        Write-Warn "Failed to delete #$id : $result"
    }
}

Write-Header "Summary"
Write-Host "  Deleted : $deleted" -ForegroundColor Green
if ($failed -gt 0) {
    Write-Host "  Failed  : $failed" -ForegroundColor Red
}
Write-Host "  Kept    : $($toKeep.Count)" -ForegroundColor DarkGray
Write-Host ""

if ($failed -gt 0) {
    Write-Warn "$failed run(s) could not be deleted (see warnings above)."
    exit 1
}

Write-Success "Cleanup complete."

