package entities;

import world.Goal;
import world.PlaySpace;

/*
 * Referee - decides what the ball's position MEANS for the match.
 *
 * MatchEngine moves everything; after each tick it asks the referee to review
 * the ball. Keeping these rules here gives each class one job:
 *   MatchEngine  - runs time forward
 *   Referee      - applies the laws of the game
 *   Match        - keeps the score and tells the observers
 *
 * A future Ruleset could hand the referee different laws (e.g. throw-ins)
 * without the engine changing at all.
 */
public class Referee extends Entity {

    public enum Decision {
        PLAY_ON,
        GOAL_IN_LEFT_GOAL,
        GOAL_IN_RIGHT_GOAL,
        OUT_OF_PLAY
    }

    // How far the ball may roll past a line before play is stopped (cm).
    private static final double OUT_OF_PLAY_MARGIN_CM = 100.0;

    private final PlaySpace playSpace;

    public Referee(PlaySpace playSpace) {
        // stands on the centre spot
        super(playSpace.getLength() / 2, playSpace.getWidth() / 2, 0.0);
        this.playSpace = playSpace;
    }

    public Decision review(Ball ball) {
        if (isInside(ball, playSpace.getLeftGoal())) {
            return Decision.GOAL_IN_LEFT_GOAL;
        }
        if (isInside(ball, playSpace.getRightGoal())) {
            return Decision.GOAL_IN_RIGHT_GOAL;
        }
        if (isOutOfPlay(ball)) {
            return Decision.OUT_OF_PLAY;
        }
        return Decision.PLAY_ON;
    }

    // The whole ball must be inside the goal frame.
    private boolean isInside(Ball ball, Goal goal) {
        double radius = ball.getRadiusCm();

        return ball.getXPos() - radius >= goal.getX()
                && ball.getXPos() + radius <= goal.getX() + goal.getDepth()
                && ball.getYPos() - radius >= goal.getY()
                && ball.getYPos() + radius <= goal.getY() + goal.getWidth()
                && ball.getZPos() + radius <= goal.getHeight();
    }

    private boolean isOutOfPlay(Ball ball) {
        double x = ball.getXPos();
        double y = ball.getYPos();

        return x < -OUT_OF_PLAY_MARGIN_CM
                || x > playSpace.getLength() + OUT_OF_PLAY_MARGIN_CM
                || y < -OUT_OF_PLAY_MARGIN_CM
                || y > playSpace.getWidth() + OUT_OF_PLAY_MARGIN_CM;
    }
}