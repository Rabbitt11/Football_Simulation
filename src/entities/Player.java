package entities;

public abstract class Player extends Entity {

    int number;
    Ball ball;
    Team team;

    double spawnX;
    double spawnY;

    private double targetX;
    private double targetY;

    Player(int number, double xPos, double yPos) {
        super(xPos, yPos);
        this.number = number;
        this.spawnX = xPos;
        this.spawnY = yPos;
        this.targetX = xPos;
        this.targetY = yPos;
    }

    public void setMatchDetails(Ball ball, Team team) {
        this.ball = ball;
        this.team = team;
    }

    public void resetPositon() {
        xPos = spawnX;
        yPos = spawnY;
    }

    public void update() {
        // Pick a random destination within 50 pixels of their starting spot
        if (Math.random() < 0.02) { 
            targetX = spawnX + (Math.random() * 100 - 50);
            targetY = spawnY + (Math.random() * 100 - 50);
        }
        
        double dx = targetX - xPos;
        double dy = targetY - yPos;
        double distance = Math.sqrt(dx * dx + dy * dy);
        
        // Strict speed limit of 0.75 pixels per frame
        double speed = 0.75; 
        
        if (distance > speed) {
            xPos += (dx / distance) * speed;
            yPos += (dy / distance) * speed;
        }
    }

    public boolean hasPossession(Ball ball) {
        double dx = xPos - ball.getXPos();
        double dy = yPos - ball.getYPos();
        double distance = Math.sqrt(dx * dx + dy * dy);
        
        return distance < 15.0; 
    }

    public abstract void pass();
}