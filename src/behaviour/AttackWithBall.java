package behaviour;

import java.util.Optional;

import entities.Ball;
import entities.Player;
import entities.Tactics;
import world.GameMap;
import world.Terrain;
import world.Goal;

public class AttackWithBall implements MovementBehaviour {

    private final double targetX;
    private final double targetY;
    private final Ball ball;
    private final GameMap map;
    private final DribbleTowardsPosition dribbling;

    public AttackWithBall(
            Ball ball,
            GameMap map,
            double targetX,
            double targetY) {
        this.ball = ball;
        this.map = map;
        this.dribbling = new DribbleTowardsPosition(ball, targetX, targetY, map);
        this.targetX = targetX;
        this.targetY = targetY;
    }

    @Override
    public void update(Player player, double deltaSeconds) {
        if (player.wasLastTouchedByOpponent()) {
            MovementBehaviour defending = new ChallengeForBall(
                    ball,
                    map,
                    targetX,
                    targetY);

            player.setMovementBehaviour(defending);
            defending.update(player, deltaSeconds);
            return;
        }

        // Choose the goal in our attacking direction.
        boolean attackingRight = targetX > map.getPlaySpace().getLength() / 2;

        Goal goal = attackingRight
                ? map.getPlaySpace().getRightGoal()
                : map.getPlaySpace().getLeftGoal();

        // Aim inside the centre of the goal.
        double goalX = goal.getX() + goal.getDepth() / 2;
        double goalY = goal.getY() + goal.getWidth() / 2;

        double distanceToGoal = Math.hypot(
                goalX - ball.getXPos(),
                goalY - ball.getYPos());

               // How far out we shoot depends on the team's tactics (preset file).
        Tactics tactics = player.getTeam().getTactics();

        if (distanceToGoal <= tactics.getShootingRangeCm() && player.canTouch(ball)) {
            boolean shot = player.tryKickTowards(
                    goalX,
                    goalY,
                    1500.0);

            if (shot) {
                player.restoreDefaultBehaviour();
                player.updateMovement(0.0, 0.0, 0.0, deltaSeconds);
                return;
            }
        }

        if (player.canTouch(ball) && player.isUnderPressure(tactics.getPassUnderPressureCm())) {
            Optional<Player> choice = player.choosePassReceiver(
                    targetX,
                    targetY,
                    200.0);

            if (choice.isPresent()) {
                Player receiver = choice.get();
                Terrain surface;

                if (map.getPlaySpace().contains(
                        ball.getXPos(), ball.getYPos())) {
                    surface = map.getEnvironment().getPlayingTerrain();
                } else {
                    surface = map.getEnvironment().getSurroundingTerrain();
                }

                boolean passed = player.tryPassTo(
                        receiver, surface.getRollingDeceleration());

                if (passed) {
                    receiver.setMovementBehaviour(
                            new ReceiveBall(
                                    ball,
                                    player,
                                    new AttackWithBall(ball, map, targetX, targetY)));

                    player.setMovementBehaviour(
                            Behaviours.holdFormation(
                                    ball,
                                    player.getXPos(),
                                    player.getYPos(),
                                    0.2));

                    player.updateMovement(0.0, 0.0, 0.0, deltaSeconds);
                    return;
                }
            }
        }

        dribbling.update(player, deltaSeconds);
    }
}
