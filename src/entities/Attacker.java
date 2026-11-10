package entities;

import behaviour.Behaviours;
import world.PlaySpace;

public class Attacker extends Player {

    public Attacker(int number, double xPos, double yPos, double zPos) {
        super(number, xPos, yPos, zPos);
    }

    public Attacker(
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
        setMovementBehaviour(
                Behaviours.holdFormation(
                        ball,
                        spawnX,
                        spawnY,
                        getBaseBallInfluence()));
    }

    @Override
    public double getBaseBallInfluence() {
        return 0.40;
    }
}