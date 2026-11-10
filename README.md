# Football Simulation

Our COMP2000 project. It's a 6-a-side football match in Java. Each player decides
for itself where to move, the coaches change tactics depending on the score, and
there's a batch mode that plays a few hundred matches to see which tactic wins most.

Only standard Java is used, no libraries.

## Running it

Open the project folder (the one with `src` and `teams` in it) and run one of these:

- `src/app/Main.java` - watch a single match. Red plays aggressive, Blue plays
  defensive. Two halves of 2.5 minutes, with a scoreboard and stats at half time
  and full time.
- `src/app/BatchMain.java` - no window. Plays 200 matches for each match-up and
  prints the results. You can pass a different number as an argument, e.g. `400`.

## What's going on in a match

The players hold their positions in a formation and get pulled towards the ball.
Whoever is nearest goes to win it back, and the player with the ball dribbles,
passes when someone gets close, or shoots once they're in range.

Each team has a coach. If their team is losing, the coach pushes everyone further
up the pitch. If they're winning by two or more, they drop back.

The referee checks for goals and for the ball going out. After a goal, the team
that conceded kicks off.

Every player keeps track of how far they've run, how long they had the ball, and
how many passes and shots they made. That's what shows up in the stats box.

## Tactics

Teams are loaded from the files in `teams/presets/`. There are three:
`aggressive`, `average` and `defensive`. Each file has the six players first,
then the tactics:

```
Goalkeeper,1
Defender,2
Defender,3
Midfielder,4
Midfielder,5
Attacker,6
formationShift=0.10
pressing=0.15
shootingRange=2400
passUnderPressure=300
```

- `formationShift` - how far up (or back) the whole team plays
- `pressing` - how much players get drawn towards the ball
- `shootingRange` - how close to goal (in cm) before shooting
- `passUnderPressure` - how close an opponent gets (in cm) before passing

If a value is wrong, for example a typo in a name or a value that isn't a number,
the loader prints an error and the team falls back to balanced tactics.

## Results

These came from running BatchMain with a few hundred matches per match-up:

- Aggressive vs defensive ended up roughly even. The aggressive team only had the
  ball about 35% of the time but still scored more goals.
- The average (balanced) team beat both of the other two more often than it lost.
- average vs average was our check that the simulation is fair. At first one side
  was winning a lot more, which turned out to be because the same team always
  moved first every tick. Now the order alternates and teams swap sides between
  matches, and it comes out even.

With small numbers of matches the percentages jump around a lot, so 400 or more
is better.

## Code layout

```
src/
  app/         Main and BatchMain
  behaviour/   the different ways a player can move
  entities/    players, ball, team, tactics, coach, referee
  simulation/  match engine, score, stats, batch runs
  ui/          drawing the pitch, scoreboard, stats box
  world/       pitch size, goals, grass
```

## Design notes

- Strategy: a player's movement is a `MovementBehaviour` that can be swapped
  during the match. The simple ones are lambdas in `Behaviours`, the ones that
  need to remember things (like `ReceiveBall`) are classes.
- Observer: `Match` tells everything registered as a `MatchListener` when there's
  a goal, half time or full time. The coaches, scoreboard and stats box all
  listen.
- Template method: `Player.initialiseDefaultBehaviour()` runs the same steps for
  every player, and each role fills in `configureDefaultBehaviour()`.
- `MatchEngine` has all the match logic and doesn't know about the window, so the
  same code runs in `Main` and in batch mode.
- `MatchStats.toTeamReport()` is a custom collector that builds a team's stats.
- `BatchSimulator` runs matches with a parallel stream. Each match creates its own
  objects and the only shared thing is `Tactics`, which can't be changed, so the
  threads don't interfere. BatchMain also checks that the parallel results match
  running them one at a time.

## Limitations

- Possession just means whoever touched the ball last.
- Every restart is a kick-off from the middle, so no throw-ins, corners or
  goal kicks.
- The ball only rolls along the ground.