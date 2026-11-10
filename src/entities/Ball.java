package entities;

public class Ball extends Entity {

    private double radiusCm = 11.0; // chosen ball radius.
    private double velocityX = 0.0;
    private double velocityY = 0.0;
    private double velocityZ = 0.0;
    private Player lastKicker;
    private Player lastToucher;

    public Ball(double xPos, double yPos, double zPos) {
        super(xPos, yPos, zPos);
    }

    public void update(double deltaSeconds, double rollingDeceleration) {
        // xPos += velocityX * deltaSeconds;
        // yPos += velocityY * deltaSeconds;
        // zPos += velocityZ * deltaSeconds;

        if (deltaSeconds <= 0.0) {
            return;
        }

        double speed = Math.hypot(velocityX, velocityY);

        // only apply ground resistance/deceleration while ball is grounded.
        if (speed > 0.0 && zPos == 0.0 && velocityZ == 0.0) {
            double movingTime = deltaSeconds;

            if (rollingDeceleration > 0.0) {
                movingTime = Math.min(deltaSeconds, speed / rollingDeceleration);
            }

            double distance = speed * movingTime - 0.5 * rollingDeceleration * movingTime * movingTime;

            xPos += velocityX / speed * distance;
            yPos += velocityY / speed * distance;

            double newSpeed = Math.max(0.0, speed - rollingDeceleration * deltaSeconds);

            velocityX = velocityX / speed * newSpeed;
            velocityY = velocityY / speed * newSpeed;
        } else {
            xPos += velocityX * deltaSeconds;
            yPos += velocityY * deltaSeconds;
        }

        zPos += velocityZ * deltaSeconds;

    }

    public void kickTowards(double targetX, double targetY, double speedCmPerSecond, Player kicker) {
        double directionX = targetX - xPos;
        double directionY = targetY - yPos;
        double distance = Math.hypot(directionX, directionY);

        lastKicker = kicker;
        recordTouch(kicker);

        if (distance == 0) {
            velocityX = 0;
            velocityY = 0;
            velocityZ = 0;
            return;
        }

        velocityX = (directionX / distance) * speedCmPerSecond;
        velocityY = (directionY / distance) * speedCmPerSecond;
        velocityZ = 0; // No vertical movement for now, to be added at a later point once it works on
                       // 2d plane.

    }

    public void stop() {
        velocityX = 0;
        velocityY = 0;
        velocityZ = 0;
    }

    public void moveTo(double x, double y, double z) {
        xPos = x;
        yPos = y;
        zPos = z;
    }

    public boolean isMoving() {
        return velocityX != 0
                || velocityY != 0
                || velocityZ != 0;
    }

    public double getRadiusCm() {
        return radiusCm;
    }

    public Player getLastKicker() {
        return lastKicker;
    }

    public Player getLastToucher() {
        return lastToucher;
    }

    public void recordTouch(Player player) {
        lastToucher = player;
    }

}