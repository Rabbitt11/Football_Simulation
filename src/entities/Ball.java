package entities;

public class Ball extends Entity {

    private double targetX;
    private double targetY;
    private double speed = 6.0; 

    public Ball(double xPos, double yPos) {
        super(xPos, yPos);
        this.targetX = xPos;
        this.targetY = yPos;
    }

    public void update() {
        double dx = targetX - xPos;
        double dy = targetY - yPos;
        double distance = Math.sqrt(dx * dx + dy * dy);

        if (distance > speed) {
            xPos += (dx / distance) * speed;
            yPos += (dy / distance) * speed;
        } else {
            xPos = targetX;
            yPos = targetY;
        }
    }

    void move(double x, double y) {
        xPos += x;
        yPos += y;
        targetX = xPos;
        targetY = yPos;
    }

    @Override
    public void moveTo(double x, double y) {
        this.targetX = x;
        this.targetY = y;
    }
}