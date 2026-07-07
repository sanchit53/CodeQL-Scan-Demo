# CodeQL incremental scanning for a multi-module Java monolith

This repository uses an advanced CodeQL workflow optimized for a large Maven/Gradle Java monolith. The workflow intentionally sets `build-mode: none` so CodeQL uses buildless Java extraction instead of invoking Maven or Gradle builds.

## Production workflow highlights

- Workflow file: `.github/workflows/codeql-java-incremental.yml`.
- Language: `java` only. If Kotlin is present, `build-mode: none` will not analyze Kotlin files; move to `autobuild` or a manual build mode for mixed Java/Kotlin repositories.
- Pull requests and pushes are path-filtered to Java source, Maven/Gradle metadata, CodeQL configuration, and the workflow itself.
- `fetch-depth: 0` is used so the action can reliably compare pull request heads to the base branch.
- `concurrency.cancel-in-progress` avoids wasting CodeQL minutes on superseded commits.
- The scheduled weekly scan refreshes the default-branch baseline and keeps query coverage current.

## Memory and thread settings

The workflow sets:

- `CODEQL_RAM_MB=6144`
- `CODEQL_THREADS=2`
- `timeout-minutes=45`

These values match the practical ceiling of the standard Linux GitHub-hosted runner, where CodeQL normally has about 6 GB of memory and 2 hardware threads available. For a 100 MB Java repository, start with these conservative settings because increasing thread count without increasing RAM can make extractor finalization and query evaluation more likely to run out of memory.

Recommended scaling profile:

| Runner profile | RAM setting | Threads | When to use |
| --- | ---: | ---: | --- |
| `ubuntu-latest` standard runner | `6144` | `2` | Default for the 100 MB monolith and most PRs. |
| 4-core larger runner with 16 GB RAM | `12288` | `3` | Use if PR analysis regularly exceeds 30 minutes but does not OOM. |
| 8-core larger runner with 32 GB RAM | `24576` | `4` | Use for very large query packs, generated code, or frequent timeout pressure. |

Do not allocate all runner memory to CodeQL. Leave at least 2-4 GB for the operating system, the Actions runner, and upload/finalization overhead.

## Verification plan

1. Merge the workflow to the default branch and run it once with `workflow_dispatch` or by pushing a no-op Java change. This creates the default-branch baseline database/cache that later pull requests can reuse.
2. Open the completed default-branch run and confirm the `Initialize CodeQL for Java` log contains `build-mode: none` and does not contain an `Autobuild` step.
3. Create a pull request that modifies one Java file in one Maven/Gradle module only.
4. In the pull request run, inspect the `Initialize CodeQL for Java` and `Analyze Java` logs with step debug logging enabled if needed (`ACTIONS_STEP_DEBUG=true` repository secret).
5. Confirm the run restores CodeQL cache data and creates an overlay/differential database from the base commit rather than extracting every source file from scratch.
6. Repeat with a PR that changes a shared parent `pom.xml`, `settings.gradle`, or shared build logic file. Expect a broader re-extraction because dependency metadata can affect multiple modules.
7. Repeat with a PR that touches many modules. Expect the run to approach full baseline behavior; this is normal when changed files invalidate much of the source graph.
8. Compare wall-clock times across the baseline run, a one-module PR, a build-metadata PR, and a many-module PR. The one-module PR should be materially faster after the default-branch cache is warm.

## Console log indicators

Positive indicators:

- The CodeQL init command shows `build-mode: none` for Java.
- The workflow skips Maven/Gradle build and has no `github/codeql-action/autobuild` step.
- Logs mention restoring CodeQL caches, TRAP cache data, cached extraction artifacts, or overlay database creation.
- Pull request logs show substantially fewer extraction/finalization messages than the first default-branch baseline run.
- Analysis invokes CodeQL with the configured `--threads=2` and about `--ram=6144`.

Fallback or misconfiguration indicators:

- `Cannot build an overlay database because build-mode is set to "undefined" instead of "none". Falling back to creating a normal full database instead.`
- Any `Autobuild` step runs, or logs show Maven/Gradle being invoked as part of CodeQL analysis.
- `Detected X Kotlin files ... could not be processed without a build` appears in a mixed Java/Kotlin repository.
- The first PR after enabling the workflow behaves like a full scan because no default-branch baseline exists yet.
- Dependency or build-file changes cause broad cache invalidation.
- OOM, exit code 32, runner lost communication, or finalization failures indicate RAM/thread settings or runner size need adjustment.
