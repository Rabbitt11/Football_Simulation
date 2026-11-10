package entities;

import behaviour.MovementBehaviour;
import world.PlaySpace;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Optional;

public abstract class Player extends Entity {

    int number;
    Ball ball;
    Team team;
    private Team opposingTeam;

    double spawnX;
    double spawnY;
    double spawnZ;

    private double bodyRadiusCm = 30.0; // value chosen as a reasonable approximation of the average human body radius
                                        // (cm)
    private double reachRadiusCm = 100.0; // value chosen as a reasonable approximation of the average human arm reach
                                          // (cm)
    private final Movement movement;
    private final PlayerStats stats = new PlayerStats();

    private MovementBehaviour movementBehaviour;
    private MovementBehaviour defaultMovementBehaviour;

    private double touchCooldownRemaining = 0.0;
    private double dribbleLeadSpeed = 100.0; // cm/second above the forward running speed.
    private final double touchRecoverySeconds = 0.3; // temporary testing value :)
    private double passCooldownRemaining = 0.0;
    private final double passCooldownSeconds = 3.0;

    // Player Data

    Player(int number, double xPos, double yPos, double zPos) {
        this(number, xPos, yPos, zPos, 500.0, 300.0);
    }

    Player(
            int number,
            double xPos,
            double yPos,
            double zPos,
            double maxSpeed,
            double maxAcceleration) {

        super(xPos, yPos, zPos);
        this.number = number;
        this.spawnX = xPos;
        this.spawnY = yPos;
        this.spawnZ = zPos;
        this.movement = new Movement(maxSpeed, maxAcceleration);
    }

    public void setMatchDetails(Ball ball, Team team, Team opposingTeam) {
        this.ball = ball;
        this.team = team;
        this.opposingTeam = opposingTeam;
    }

    public void setMovementBehaviour(MovementBehaviour movementBehaviour) {
        this.movementBehaviour = movementBehaviour;
    }

    public void initialiseDefaultBehaviour(
            PlaySpace playSpace, boolean defendingLeft) {
        configureDefaultBehaviour(playSpace, defendingLeft);
        defaultMovementBehaviour = movementBehaviour;
    }

    public void restoreDefaultBehaviour() {
        setMovementBehaviour(defaultMovementBehaviour);
    }

    public MovementBehaviour getMovementBehaviour() {
        return movementBehaviour;
    }

    public void update(double deltaSeconds) {

        if (deltaSeconds <= 0.0) {
            return;
        }

        touchCooldownRemaining = Math.max(0.0, touchCooldownRemaining - deltaSeconds);
        passCooldownRemaining = Math.max(0.0, passCooldownRemaining - deltaSeconds);

        double startX = xPos;
        double startY = yPos;

        if (movementBehaviour != null) {
            movementBehaviour.update(this, deltaSeconds);
        } else {
            updateMovement(0.0, 0.0, 0.0, deltaSeconds);
        }

        // Stats: how far we ran this tick, and whether our team "has" the ball.
        // Possession = this player was the last one to touch the ball.
        stats.addDistance(Math.hypot(xPos - startX, yPos - startY));

        if (ball != null && ball.getLastToucher() == this) {
            stats.addPossession(deltaSeconds);
        }
    }

    public abstract void configureDefaultBehaviour(
            PlaySpace playSpace, boolean defendingLeft);

    // Player Movement Data

    public void moveTowards(
            double targetX,
            double targetY,
            double deltaSeconds) {
        double directionX = targetX - xPos;
        double directionY = targetY - yPos;
        double distance = Math.hypot(directionX, directionY);
        double targetAccuracyCm = 50.0;

        if (distance <= targetAccuracyCm) {
            updateMovement(0.0, 0.0, 0.0, deltaSeconds);
            return;
        }

        double desiredSpeed = movement.getMaxSpeed();

        updateMovement(
                directionX / distance * desiredSpeed,
                directionY / distance * desiredSpeed,
                0.0,
                deltaSeconds);
    }

    public void arriveTowards(
            double targetX,
            double targetY,
            double deltaSeconds) {
        double directionX = targetX - xPos;
        double directionY = targetY - yPos;
        double distance = Math.hypot(directionX, directionY);

        if (distance <= 10.0) {
            updateMovement(0.0, 0.0, 0.0, deltaSeconds);
            return;
        }

        double brakingSpeed = Math.sqrt(2.0 * movement.getMaxAcceleration() * Math.max(0.0, distance - 10.0));
        double approachSpeed = distance / 0.5;
        double desiredSpeed = Math.min(movement.getMaxSpeed(), Math.min(brakingSpeed, approachSpeed));

        updateMovement(
                directionX / distance * desiredSpeed,
                directionY / distance * desiredSpeed,
                0.0,
                deltaSeconds);
    }

    public void updateMovement(
            double desiredVelocityX,
            double desiredVelocityY,
            double desiredVelocityZ,
            double deltaSeconds) {
        movement.updateVelocity(desiredVelocityX, desiredVelocityY, desiredVelocityZ, deltaSeconds);
        xPos += movement.getVelocityX() * deltaSeconds;
        yPos += movement.getVelocityY() * deltaSeconds;
        zPos += movement.getVelocityZ() * deltaSeconds;
    }

    // Game Logic, Getters/Setters

    public boolean canTouch(Ball ball) {
        double horizontalDistance = Math.hypot(
                ball.getXPos() - xPos,
                ball.getYPos() - yPos);
        return horizontalDistance <= reachRadiusCm + ball.getRadiusCm()
                && Math.abs(ball.getZPos() - zPos) <= ball.getRadiusCm();
    }

    public boolean tryControlBall() {
        if (ball == null
                || touchCooldownRemaining > 0.0
                || !canTouch(ball)) {
            return false;
        }

        touchCooldownRemaining = touchRecoverySeconds;
        ball.stop();
        ball.recordTouch(this);
        return true;
    }

    public boolean tryKickTowards(
            double targetX,
            double targetY,
            double speedCmPerSecond) {
        if (ball == null
                || touchCooldownRemaining > 0.0
                || !canTouch(ball)) {
            return false;
        }

        touchCooldownRemaining = touchRecoverySeconds;
        ball.kickTowards(targetX, targetY, speedCmPerSecond, this);
        return true;
    }

    public boolean tryPassTo(

            Player receiver,
            double rollingDeceleration) {

        if (passCooldownRemaining > 0.0) {
            return false;
        }
        if (ball == null || receiver == null || receiver == this
                || team == null || receiver.team != team) {
            return false;
        }

        if (!Double.isFinite(rollingDeceleration)
                || rollingDeceleration < 0.0) {
            throw new IllegalArgumentException(
                    "Rolling deceleration MUST be finite And Non-Negative!");
        }

        double distance = Math.hypot(
                receiver.getXPos() - ball.getXPos(),
                receiver.getYPos() - ball.getYPos());

        if (distance <= 0.0) {
            return false;
        }

        double arrivalSpeed = 300.00; // current chosen pass arrival speed (in centimeters/s)
        double launchSpeed = Math.sqrt(
                arrivalSpeed * arrivalSpeed
                        + 2.0 * rollingDeceleration * distance);

        boolean passed = tryKickTowards(
                receiver.getXPos(),
                receiver.getYPos(),
                launchSpeed);

        if (passed) {
            passCooldownRemaining = passCooldownSeconds;
            stats.recordPass();
        }

        return passed;
    }

    public Optional<Player> choosePassReceiver(
            double targetX,
            double targetY,
            double minimumProgressCm) {

        if (!Double.isFinite(minimumProgressCm)
                || minimumProgressCm < 0.0) {
            throw new IllegalArgumentException("Minimum progress must be finite and non-negative!");
        }

        if (team == null) {
            return Optional.empty();
        }

        // double currentDistance = Math.hypot(
        // targetX - xPos,
        // targetY - yPos);

        return Arrays.stream(team.getPlayers())
                .filter(candidate -> candidate != null)
                .filter(candidate -> candidate != this)
                .filter(candidate -> Math.hypot(
                        candidate.getXPos() - xPos,
                        candidate.getYPos() - yPos) > 100.0)
                // .filter(candidate -> Math.hypot(
                // targetX - candidate.getXPos(),
                // targetY - candidate.getYPos())
                // <= currentDistance - minimumProgressCm)
                .min(Comparator.comparingDouble(candidate -> Math.hypot(
                        candidate.getXPos() - xPos,
                        candidate.getYPos() - yPos)));
    }

    public boolean isUnderPressure(double distanceCm) {
        if (!Double.isFinite(distanceCm) || distanceCm < 0.0) {
            throw new IllegalArgumentException(
                    "Pressure distance must be both Finite AND non-negative!");
        }

        if (opposingTeam == null) {
            return false;
        }

        return Arrays.stream(opposingTeam.getPlayers())
                .filter(opponent -> opponent != null)
                .anyMatch(opponent -> Math.hypot(
                        opponent.getXPos() - xPos,
                        opponent.getYPos() - yPos) <= distanceCm);
    }

    public boolean wasLastTouchedByOpponent() {
        if (ball == null || opposingTeam == null) {
            return false;
        }

        Player lastToucher = ball.getLastToucher();

        return lastToucher != null
                && Arrays.stream(opposingTeam.getPlayers())
                        .anyMatch(opponent -> opponent == lastToucher);
    }

    public double calculateGroundKickSpeed(
            double distanceCm,
            double rollingDeceleration) {
        if (!Double.isFinite(distanceCm) || distanceCm < 0.0
                || !Double.isFinite(rollingDeceleration)
                || rollingDeceleration <= 0.0) {
            throw new IllegalArgumentException(
                    "Remember! Distance must be Finite and Non-Negative; "
                            + "Rolling Deceleration must be Finite and Positive.");
        }

        return Math.sqrt(2.0 * rollingDeceleration * distanceCm);
    }

    // Spawn point
    public void resetPosition() {
        movement.stop(); // deletes any momentum :)
        touchCooldownRemaining = 0.0;
        xPos = spawnX;
        yPos = spawnY;
        zPos = spawnZ;
    }

    public double getBodyRadiusCm() {
        return bodyRadiusCm;
    }

    public double getReachRadiusCm() {
        return reachRadiusCm;
    }

    public double getVelocityX() {
        return movement.getVelocityX();
    }

    public double getVelocityY() {
        return movement.getVelocityY();
    }

    public double getDribbleLeadSpeed() {
        return dribbleLeadSpeed;
    }

    public Team getOpposingTeam() {
        return opposingTeam;
    }

    public PlayerStats getStats() {
        return stats;
    }

    public Team getTeam() {
        return team;
    }

    public int getNumber() {
        return number;
    }

    // Lets a Coach change how this player behaves after the next restart.
    public void setDefaultMovementBehaviour(MovementBehaviour behaviour) {
        this.defaultMovementBehaviour = behaviour;
    }

    // How strongly this player is pulled towards the ball when holding formation.
    // Each outfield role overrides this (polymorphism). Goalkeepers don't use it.
    public double getBaseBallInfluence() {
        return 0.0;
    }

}