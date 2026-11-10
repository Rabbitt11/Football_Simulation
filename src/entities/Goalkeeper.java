package entities;

import behaviour.Behaviours;
import world.PlaySpace;

public class Goalkeeper extends Player {

    public Goalkeeper(int number, double xPos, double yPos, double zPos) {
        super(number, xPos, yPos, zPos);
    }

    public Goalkeeper(
            int number,
            double xPos,
            double yPos,
            double zPos,
            double maxSpeed,
            double maxAcceleration) {
        super(number, xPos, yPos, zPos, maxSpeed, maxAcceleration);
    }

    @Override
    public void configureDefaultBehaviour(
            PlaySpace playSpace, boolean defendingLeft) {

        if (defendingLeft) {
            setMovementBehaviour(
                    Behaviours.guardGoal(ball, playSpace.getLeftGoal(), 200.0));

        } else {
            setMovementBehaviour(Behaviours.guardGoal(ball, playSpace.getRightGoal(), playSpace.getLength() - 200.0));
        }
    }

}