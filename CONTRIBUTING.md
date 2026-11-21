# Contributing to SCENAR

This guide defines how the SCENAR team works with Git and GitHub. Follow it for every change so that `main` always contains a stable, demonstrable version of the app.

## 1. Branches and ownership

| Branch | Owner | Purpose |
|---|---|---|
| `main` | Team leader | Stable, reviewed, presentation-ready code only |
| `feature/victim-app` | Victim-app developer | Emergency reporting, voice/text input, GPS UI, review screen, accessibility |
| `feature/ai-pipeline` | Technical lead | Incident model, AI integration, validation, fallbacks, shared data logic |
| `feature/responder-dashboard` | Responder developer | Responder dashboard, incident status, map and dispatch simulation |

Rules:

- Never develop directly on `main`.
- Never force-push to `main` or another teammate's branch.
- Open a pull request for every merge into `main`.
- Keep pull requests small enough to review.
- Do not merge code that does not build.
- The team leader performs or approves merges into `main`.

## 2. One-time setup

### Clone the repository

In Android Studio, select **Get from VCS**, choose Git, and enter:

```text
https://github.com/Ibrahim2709-raaz/scenar.git
```

Alternatively, use a terminal:

```bash
git clone https://github.com/Ibrahim2709-raaz/scenar.git
cd scenar
```

### Configure your Git identity

Run once on your computer, using your own details:

```bash
git config --global user.name "Your Name"
git config --global user.email "your-github-email@example.com"
```

Check the configuration:

```bash
git config --global --list
```

### Confirm the remote

```bash
git remote -v
```

The remote should be named `origin` and point to `Ibrahim2709-raaz/scenar`.

## 3. Starting work each day

Fetch the latest state from GitHub:

```bash
git fetch origin
```

Switch to your assigned branch:

```bash
git switch feature/victim-app
```

Replace the branch name with your assigned branch when necessary.

Download its latest commits:

```bash
git pull origin feature/victim-app
```

Check that you are on the correct branch before editing:

```bash
git branch --show-current
git status
```

In Android Studio, the current branch is also displayed in the bottom-right corner. Never begin editing while it says `main`.

## 4. Making and committing changes

Work on one logical change at a time. Before committing, make sure the project builds and inspect what changed:

```bash
git status
git diff
```

Stage only the intended files:

```bash
git add path/to/File.java
git add path/to/layout.xml
```

Avoid blindly using `git add .` because it may include local settings, secrets, generated files or unrelated work.

Commit with a short, meaningful message:

```bash
git commit -m "feat: add editable incident review screen"
```

Recommended commit prefixes:

| Prefix | Use |
|---|---|
| `feat:` | New user-visible feature |
| `fix:` | Bug fix |
| `ui:` | Layout, styling or accessibility change |
| `ai:` | Prompt, parsing or AI-validation change |
| `test:` | Tests or test data |
| `docs:` | Documentation |
| `refactor:` | Internal restructuring without changing behavior |
| `chore:` | Build configuration or maintenance |

Examples:

```text
feat: add emergency description form
ai: validate missing incident fields
fix: preserve report when AI request fails
ui: increase emergency button tap target
docs: document Bluetooth prototype limitations
```

Do not use messages such as `changes`, `stuff`, `update` or `final`.

## 5. Pushing your branch

The first time you push a branch:

```bash
git push -u origin feature/victim-app
```

After the upstream is set, use:

```bash
git push
```

Pushing a feature branch does not change `main`. It only uploads your branch so the team can review it.

If Git rejects the push, do not force it. First run:

```bash
git pull origin feature/victim-app
```

Resolve any conflicts, test, commit if required, and push again.

## 6. Bringing the latest `main` into your branch

Before opening a pull request—or whenever `main` has changed significantly—merge the latest stable code into your feature branch:

```bash
git status
git fetch origin
git switch main
git pull origin main
git switch feature/victim-app
git merge main
```

Replace `feature/victim-app` with your branch.

If there are no conflicts, build and test the combined app, then push:

```bash
git push
```

Use merging instead of rebasing during the hackathon unless the technical lead explicitly coordinates a rebase. It is easier for a small team and avoids rewriting shared branch history.

## 7. Resolving merge conflicts

A conflict means Git found overlapping changes and needs a human decision. It does not mean the work is lost.

List conflicted files:

```bash
git status
```

Open each conflicted file in Android Studio. The editor will show the competing versions. Decide whether to keep the current version, incoming version, or a correct combination of both.

Conflict markers look like this:

```text
[current branch marker]
your branch's code
[separator marker]
incoming code
[incoming branch marker]
```

Remove all conflict markers after combining the correct code. Then:

```bash
git add path/to/resolved-file
git commit -m "merge: resolve main conflicts"
git push
```

Build and run the complete flow after resolving conflicts. Ask the author of the other change when the correct resolution is unclear; do not guess.

To cancel an unfinished merge and return to the state before it began:

```bash
git merge --abort
```

## 8. Creating a pull request

After pushing your branch:

1. Open the SCENAR repository on GitHub.
2. Select **Pull requests**.
3. Select **New pull request**.
4. Set the base branch to `main`.
5. Set the compare branch to your feature branch.
6. Review the **Files changed** tab and ensure no secrets or unrelated files are included.
7. Add a clear title and description.
8. Create the pull request and request a review from the team leader.

Example title:

```text
Add editable AI incident review screen
```

Use this pull-request description:

```markdown
## What changed

- Added the incident review screen.
- Allowed users to correct AI-generated fields.
- Added loading and error states.

## How to test

1. Open the app.
2. Select Report Emergency.
3. Enter the sample incident.
4. Confirm that every report field can be edited.

## Evidence

- [ ] App builds successfully
- [ ] Tested on an emulator
- [ ] Tested on a physical device, if applicable
- [ ] Screenshots or recording attached

## Risks or unfinished work

- Voice input is not included in this pull request.
- Dispatch remains simulated.
```

Creating a pull request does not merge the branch. It starts the review process.

## 9. Reviewing a pull request

The reviewer should check:

- Does the project build?
- Does the feature work from beginning to end?
- Are errors and denied permissions handled?
- Can the user correct AI output?
- Are unknown values kept unknown instead of guessed?
- Are medical and location details handled carefully?
- Are buttons labelled and easy to tap?
- Are API keys, credentials and personal data absent from the commit?
- Are unrelated files excluded?

If changes are needed, leave specific comments. The author fixes the same feature branch, commits, and pushes again. The pull request updates automatically; do not create a second pull request.

## 10. Merging into `main`

Merge only when:

- The pull request has been reviewed.
- The app builds successfully.
- The feature has been tested.
- Conflicts are resolved.
- The demonstration flow still works.

For this hackathon, use **Squash and merge** on GitHub. This combines the feature's small commits into one clean commit on `main`.

After selecting **Squash and merge**:

1. Confirm the final commit message is meaningful.
2. Complete the merge.
3. Delete the remote feature branch only if its milestone is complete and nobody is still using it.

Do not merge both versions of a conflict merely to make Git accept it. The result must compile and behave correctly.

## 11. Updating your computer after a merge

Everyone should update their local `main`:

```bash
git switch main
git pull origin main
```

If continuing on an existing feature branch:

```bash
git switch feature/victim-app
git merge main
```

Then build the app before beginning new work.

If the feature branch was deleted remotely and its work is complete, delete the local copy:

```bash
git switch main
git branch -d feature/victim-app
git fetch --prune
```

Do not use the uppercase `-D` unless the technical lead has confirmed that the unmerged local work can be discarded.

## 12. Creating a smaller task branch

The three shared feature branches represent ownership areas. For a risky or isolated change, create a short-lived task branch from the relevant up-to-date feature branch:

```bash
git switch feature/ai-pipeline
git pull origin feature/ai-pipeline
git switch -c task/ai-json-validation
git push -u origin task/ai-json-validation
```

Open its pull request into `feature/ai-pipeline`, not directly into `main`. Once the ownership branch is stable, open a separate pull request from `feature/ai-pipeline` into `main`.

For a one-weekend project, use task branches only when two people must work in the same ownership area or the change could destabilize the shared branch.

## 13. Android files that must not be committed

Never commit:

```text
local.properties
.gradle/
build/
app/build/
*.jks
*.keystore
```

Do not commit:

- API keys or access tokens
- Passwords or credentials
- Real victim, patient or location data
- Personal test records
- Generated APKs unless the team explicitly creates a GitHub release
- IDE-specific temporary files

Before every push, check:

```bash
git status
git diff --cached
```

If a secret is committed, notify the technical lead immediately. Removing it in a later commit is not enough because it remains in Git history; the credential must also be revoked or rotated.

## 14. Android Studio equivalents

| Git task | Android Studio action |
|---|---|
| View current branch | Branch name in bottom-right corner |
| Change branch | **Git > Branches** |
| Pull | **Git > Pull** |
| Review changes | **Commit** tool window |
| Commit | Select intended files, enter message, choose **Commit** |
| Push | **Git > Push** |
| Resolve conflicts | **Git > Resolve Conflicts** |
| Update remote branches | **Git > Fetch** |

Always read the branch name, selected files and destination before confirming an Android Studio Git action.

## 15. Emergency recovery commands

See the current situation:

```bash
git status
git branch --show-current
git log --oneline --decorate -10
```

Temporarily save unfinished tracked and untracked work before switching branches:

```bash
git stash push -u -m "unfinished incident form"
```

Restore it later:

```bash
git stash pop
```

Undo the most recent local commit while keeping its changes for correction:

```bash
git reset --soft HEAD~1
```

Do not use `git reset --hard`, force-push, or delete unmerged branches without approval from the technical lead.

## 16. Recommended team routine

### At the start of a work session

```bash
git fetch origin
git switch your-feature-branch
git pull origin your-feature-branch
git merge origin/main
```

### During development

- Make one logical change.
- Build and test it.
- Inspect the diff.
- Commit with a meaningful message.
- Push regularly so the work is backed up and visible.

### Before a pull request

- Merge the latest `main` into the feature branch.
- Resolve conflicts.
- Run the complete emergency-reporting flow.
- Push the final branch state.
- Review the GitHub diff yourself.

### Before the Monday presentation

- Stop adding features.
- Merge only tested pull requests.
- Tag or record the final stable commit.
- Build the app from a clean checkout.
- Record a backup demo video.
- Keep `main` presentation-ready.

## Quick reference

```bash
# Start work
git fetch origin
git switch feature/victim-app
git pull origin feature/victim-app

# Save work
git status
git diff
git add path/to/file
git commit -m "feat: describe the change"
git push

# Incorporate stable work
git fetch origin
git switch main
git pull origin main
git switch feature/victim-app
git merge main
git push

# After a merged pull request
git switch main
git pull origin main
```

When uncertain, stop before running a destructive command and ask the technical lead. Preserving work is more important than keeping the Git history perfectly tidy.
