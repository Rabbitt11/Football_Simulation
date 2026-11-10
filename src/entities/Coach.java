package entities;

import java.util.Arrays;
import java.util.stream.Stream;

import behaviour.Behaviours;
import simulation.GameState;
import simulation.GoalEvent;
import simulation.MatchListener;

/*
 * Coach - an OBSERVER of the Match.
 *
 * The coach stands on the touchline and listens for goals. After every goal
 * it looks at the score and picks a mentality for its team:
 *   losing             -> ATTACKING  (push the formation up the pitch)
 *   winning by 2+      -> DEFENSIVE  (drop deep and protect the lead)
 *   otherwise          -> BALANCED
 *
 * The team's preset Tactics (aggressive / average / defensive) are the
 * starting point; the mentality is adjusted on top of them.
 *
 * The coach changes tactics by giving each outfield player a NEW
 * MovementBehaviour (Strategy pattern). So two patterns work together:
 * Observer tells the coach WHEN to act, Strategy is HOW it changes the team.
 */
public class Coach extends Entity implements MatchListener {

    public enum Mentality {
        DEFENSIVE(-0.10, -0.10),
        BALANCED(0.0, 0.0),
        ATTACKING(0.15, 0.20);

        // fraction of the pitch length to move the formation forward (+) or back (-)
        private final double forwardShift;
        // extra pull towards the ball on top of each role's base influence
        private final double extraBallInfluence;

        Mentality(double forwardShift, double extraBallInfluence) {
            this.forwardShift = forwardShift;
            this.extraBallInfluence = extraBallInfluence;
        }
    }

    private final Team team;
    private final Ball ball;
    private final double pitchLength;
    private final boolean attacksRight;
    private Mentality mentality = Mentality.BALANCED;

    public Coach(Team team, Ball ball, double pitchLength, boolean attacksRight) {
        // stands on the halfway line, just off the pitch
        super(pitchLength / 2, -300.0, 0.0);
        this.team = team;
        this.ball = ball;
        this.pitchLength = pitchLength;
        this.attacksRight = attacksRight;
    }

    @Override
    public void onGoal(GoalEvent goal, GameState state) {
        Mentality next = chooseMentality(goalDifference(state));

        if (next != mentality) {
            mentality = next;
            applyMentality();
            System.out.println(team.getName() + " coach switches to " + mentality);
        }
    }

    // positive = we are winning
    private int goalDifference(GameState state) {
        if (team == state.getRedTeam()) {
            return state.getRedScore() - state.getBlueScore();
        }
        return state.getBlueScore() - state.getRedScore();
    }

    private Mentality chooseMentality(int goalDifference) {
        if (goalDifference < 0) {
            return Mentality.ATTACKING;
        }
        if (goalDifference >= 2) {
            return Mentality.DEFENSIVE;
        }
        return Mentality.BALANCED;
    }

    // At kick-off, set the team up according to its preset tactics.
    @Override
    public void onMatchStart(GameState state) {
        applyMentality();
        outfieldPlayers().forEach(Player::restoreDefaultBehaviour);
    }

    // Give every outfield player a new formation behaviour.
    // The team's preset Tactics are the starting point; the coach's Mentality adjusts on top.
    // It becomes their DEFAULT, so it takes effect when the match restarts after a goal.
    private void applyMentality() {
        double direction = attacksRight ? 1.0 : -1.0;
        Tactics tactics = team.getTactics();

        double totalShift = tactics.getFormationShift() + mentality.forwardShift;
        double extraInfluence = tactics.getPressing() + mentality.extraBallInfluence;

        outfieldPlayers().forEach(player -> {
            double anchorX = player.spawnX + direction * totalShift * pitchLength;
            double influence = clamp(player.getBaseBallInfluence() + extraInfluence);

            player.setDefaultMovementBehaviour(
                    Behaviours.holdFormation(ball, anchorX, player.spawnY, influence));
        });
    }

    private Stream<Player> outfieldPlayers() {
        return Arrays.stream(team.getPlayers())
                .filter(player -> !(player instanceof Goalkeeper));
    }

    private static double clamp(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }

    public Mentality getMentality() {
        return mentality;
    }

    public Team getTeam() {
        return team;
    }
}