# Agent guide

## Working style
- For questions or suggestions, respond with an answer; edit files or implement only when the user explicitly asks for a change.
- Keep answers concise and grounded in code facts. Cite inspected symbols with short-label links (e.g. `[symbol](short/path)` or `[symbol](short/path#L)` when a location helps), using the shortest unambiguous path; avoid repeating references or line ranges. Do not guess behavior.
- Be methodical. Make the smallest coherent change that advances the task; do not rush a broad implementation.

## Before changing code
- Inspect existing code, tests, and build conventions before adding new code.
- Prefer reuse or targeted modification over new abstractions. Avoid duplication and code that is not required by the task.

## Development
- Use outside-in TDD: start from the behavior/integration boundary, add or update a failing test, implement the minimum to pass, then refactor.
- Follow existing project structure, naming, and style.

## Verification
- Run relevant checks before considering work done. For Kotlin/Gradle changes, use `./gradlew spotlessApply detekt` and the affected tests/tasks as appropriate.
- Fix formatting/static-analysis issues you introduce.

## Commits
- When asked to commit, use brief, clear commit messages that state the change (match existing history style).
