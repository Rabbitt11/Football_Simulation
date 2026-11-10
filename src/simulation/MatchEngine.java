package simulation;

import java.util.Arrays;
import java.util.Random;

import behaviour.AttackWithBall;
import entities.Attacker;
import entities.Ball;
import entities.Coach;
import entities.Player;
import entities.Referee;
import entities.Team;
import world.GameMap;
import world.PlaySpace;
import world.Terrain;

/*
 * MatchEngine - runs ONE match, one small time step at a time.
 *
 * It knows nothing about windows or drawing. That separation is the point:
 *   - Main calls tick() from a Swing timer and repaints  -> a watchable match
 *   - BatchSimulator calls tick() in a tight loop          -> a match in a fraction of a second
 *
 * Every engine owns ALL of its objects (ball, teams, players, coaches, match).
 * Nothing is shared between engines except immutable Tactics, so many engines
 * can run on different threads at the same time without interfering.
 */
public class MatchEngine {

    public static final double TICK_SECONDS = 0.02;

    // Random nudge to the kick-off spot, so matches with the same teams still differ (cm).
    private static final double KICK_OFF_JITTER_CM = 400.0;

    private final GameMap map;
    private final PlaySpace playSpace;
    private final Team redTeam;
    private final Team blueTeam;
    private final Ball ball;
    private final Match match;
    private final Coach redCoach;
    private final Coach blueCoach;
    private final Referee referee;
    private final Random random;

    private double clockAccumulator = 0.0;
    private long ticks = 0;

    public MatchEngine(GameMap map, Team redTeam, Team blueTeam, Ball ball,
            Ruleset ruleset, Random random) {

        this.map = map;
        this.playSpace = map.getPlaySpace();
        this.redTeam = redTeam;
        this.blueTeam = blueTeam;
        this.ball = ball;
        this.random = random;
        this.referee = new Referee(playSpace);

        // Red defends the left goal, blue defends the right.
        for (Player player : redTeam.getPlayers()) {
            player.setMatchDetails(ball, redTeam, blueTeam);
            player.initialiseDefaultBehaviour(playSpace, true);
        }
        for (Player player : blueTeam.getPlayers()) {
            player.setMatchDetails(ball, blueTeam, redTeam);
            player.initialiseDefaultBehaviour(playSpace, false);
        }

        this.match = new Match(redTeam, blueTeam, ball, ruleset);

        // The coaches are part of the simulation itself, so the engine registers them.
        this.redCoach = new Coach(redTeam, ball, playSpace.getLength(), true);
        this.blueCoach = new Coach(blueTeam, ball, playSpace.getLength(), false);
        match.addListener(redCoach);
        match.addListener(blueCoach);
    }

    // Kick-off. A coin toss decides who starts with the ball.
    public void start() {
        match.start();
        kickOff(random.nextBoolean() ? redTeam : blueTeam);
    }

    public boolean isFinished() {
        return !match.getGameState().isRunning();
    }

    // Advance the whole match by one small time step.
    public void tick(double deltaSeconds) {
        if (isFinished()) {
            return;
        }
        ticks++;

        // 1. Match clock: one whole second at a time.
        clockAccumulator += deltaSeconds;
        while (clockAccumulator >= 1.0) {
            clockAccumulator -= 1.0;
            match.update(1);
        }
        if (isFinished()) {
            return; // the final whistle went during this tick
        }

        // 2. Players think and move.
        // The team that moves second always reacts to the other team's NEW positions,
        // which is a small but real advantage. Alternating the order every tick
        // removes that bias (found by the average-vs-average control in BatchMain).
        Team first = (ticks % 2 == 0) ? redTeam : blueTeam;
        for (Player player : first.getPlayers()) {
            player.update(deltaSeconds);
        }
        for (Player player : otherTeam(first).getPlayers()) {
            player.update(deltaSeconds);
        }

        // 3. Ball rolls, slower on the rough grass outside the pitch.
        Terrain surface = playSpace.contains(ball.getXPos(), ball.getYPos())
                ? map.getEnvironment().getPlayingTerrain()
                : map.getEnvironment().getSurroundingTerrain();
        ball.update(deltaSeconds, surface.getRollingDeceleration());

        // 4. The referee reviews the ball; the engine acts on the decision.
        switch (referee.review(ball)) {
            case GOAL_IN_RIGHT_GOAL:
                goalFor(redTeam); // red attacks right, so this is a red goal
                break;
            case GOAL_IN_LEFT_GOAL:
                goalFor(blueTeam);
                break;
            case OUT_OF_PLAY:
                restartAfterOutOfPlay();
                break;
            case PLAY_ON:
            default:
                break;
        }
    }

    private void goalFor(Team scoringTeam) {
        Player scorer = ball.getLastToucher();
        boolean ownGoal = scorer != null && scorer.getTeam() != scoringTeam;

        match.recordGoal(scorer, scoringTeam, null, ownGoal);
        kickOff(otherTeam(scoringTeam)); // the team that conceded kicks off
    }

    // Simplified rule: whoever did NOT touch it last restarts with the ball.
    private void restartAfterOutOfPlay() {
        Player lastToucher = ball.getLastToucher();
        Team restartingTeam = lastToucher == null
                ? (random.nextBoolean() ? redTeam : blueTeam)
                : otherTeam(lastToucher.getTeam());
        kickOff(restartingTeam);
    }

    // Reset everyone and give the ball to `kickingTeam`.
    private void kickOff(Team kickingTeam) {
        Team defendingTeam = otherTeam(kickingTeam);

        redTeam.releaseChallenger();
        blueTeam.releaseChallenger();

        ball.stop();
        ball.recordTouch(null);
        ball.moveTo(
                playSpace.getLength() / 2 + (random.nextDouble() - 0.5) * KICK_OFF_JITTER_CM,
                playSpace.getWidth() / 2 + (random.nextDouble() - 0.5) * KICK_OFF_JITTER_CM,
                0.0);

        for (Player player : redTeam.getPlayers()) {
            player.resetPosition();
            player.restoreDefaultBehaviour(); // picks up any new tactics from the coach
        }
        for (Player player : blueTeam.getPlayers()) {
            player.resetPosition();
            player.restoreDefaultBehaviour();
        }

        // The kicking team's striker attacks; the defending team sends its nearest player to challenge.
        Player striker = Arrays.stream(kickingTeam.getPlayers())
                .filter(player -> player instanceof Attacker)
                .findFirst()
                .orElse(kickingTeam.getPlayers()[kickingTeam.getPlayers().length - 1]);

        striker.setMovementBehaviour(new AttackWithBall(
                ball, map, attackTargetX(kickingTeam), playSpace.getWidth() / 2));

        defendingTeam.assignChallenger(
                ball, map, attackTargetX(defendingTeam), playSpace.getWidth() / 2);
    }

    // Where each team attacks towards: red attacks the right goal, blue the left.
    private double attackTargetX(Team team) {
        return team == redTeam ? playSpace.getLength() - 200.0 : 200.0;
    }

    private Team otherTeam(Team team) {
        return team == redTeam ? blueTeam : redTeam;
    }

    // Snapshot of how the match ended. Uses the custom collector from step 4.
    public MatchResult result() {
        GameState state = match.getGameState();
        return new MatchResult(
                redTeam.getTactics().getName(),
                blueTeam.getTactics().getName(),
                state.getRedScore(),
                state.getBlueScore(),
                MatchStats.reportFor(redTeam),
                MatchStats.reportFor(blueTeam),
                ticks);
    }

    public Match getMatch() {
        return match;
    }

    public Coach getRedCoach() {
        return redCoach;
    }

    public Coach getBlueCoach() {
        return blueCoach;
    }

    public long getTicks() {
        return ticks;
    }
}