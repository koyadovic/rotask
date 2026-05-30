# Rotask

Android app to split your daily training time across grouped tasks with configurable weights. Each group is its own time block; inside a group, sessions rotate toward the task that is furthest behind its share of the day.

<p align="center">
  <img src="docs/screenshots/home.jpg" alt="Home screen with the task list and their weights" width="320">
  <img src="docs/screenshots/work.jpg" alt="Paused work session screen with Skip, Stop and Start controls" width="320">
</p>

## Idea

- **Groups** organize work by topic (e.g. *Music*, *Programming*). Each group owns its own daily minutes budget.
- **Tasks** belong to a group. Each task has a name, an optional description (what to train) and a weight (decimal: `1`, `1.5`, ...).
- Each task can be **active** or **paused**. Paused tasks don't rotate and are hidden behind a per-group show/hide control.
- Within a group, the daily minutes are divided across the active tasks proportionally to their weights. Example: a *Music* group with 240 min/day and three active tasks of weights `2`, `1`, `1` → 120/60/60 min today.
- Press **Start work** on a group to pick the active task with the largest percentage still pending today (so a small task at 100% pending beats a large task at 50% pending). On ties, the larger absolute remaining wins.
- Press the play button on an individual pending task to work on only that task; completion returns to the home screen instead of rotating to the next one.
- Press the check button on an individual pending task to mark all of its remaining daily target as complete.
- The work screen opens paused. You press play when ready. In rotation mode, when a task auto-completes or you press Skip, the next-most-incomplete task **in the same group** is loaded, also paused.
- Running sessions use elapsed device time and schedule a system alarm, so the counter catches up and the completion sound can fire while the phone is locked.
- The settings screen lets you choose a specific system completion sound and export/import a JSON backup through the Android file picker, including Google Drive when available.
- Each day is independent: nothing carries over, working over the target doesn't bank credit.

## Stack

- Kotlin 2.0 + Jetpack Compose (Material 3)
- Room for local persistence
- Navigation Compose
- minSdk 26, targetSdk 35

## Project layout

```
app/src/main/java/com/rotask/
├── data/        Entities + DAOs + AppDatabase (Room) + Migrations
├── domain/      TaskScheduler (per-group %-incomplete pick) and RotaskRepository
└── ui/
    ├── home/    HomeScreen + HomeViewModel
    ├── work/    WorkScreen + WorkViewModel
    ├── theme/   Colors, typography, Theme
    └── format/  Formatting helpers (mm:ss, weights)
```

## Build

```bash
./gradlew assembleDebug
```

Recommended: open the project in Android Studio (Hedgehog+) and let it sync Gradle the first time.

## Localization

Default language is English (`res/values`). Spanish translation lives in `res/values-es`. The system picks the user's locale automatically.

## Algorithm (summary)

Each day, for each active task `t` in a group `g`:

```
target_t        = g.dailyMinutes * 60 * weight_t / sum_active_weights_in_g
worked_t        = seconds worked on t today (sum over today's work_sessions)
remaining_t     = max(0, target_t - worked_t)
pct_incomplete  = remaining_t / target_t        # 0 means done; 1 means untouched
```

`pickNextInGroup(g)` returns the active task in `g` with the largest `pct_incomplete`; ties are broken by the larger `remaining_t`. At midnight every task in every group resets to a fresh `target_t` with `worked_t = 0` — no carryover, no banked credit.

## License

MIT. See [LICENSE](LICENSE).
