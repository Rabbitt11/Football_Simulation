package entities;

public class Entity {

    double xPos;
    double yPos;

    Entity(double xPos, double yPos) {
        this.xPos = xPos;
        this.yPos = yPos;
    }

    public double getXPos() {
        return xPos;
    }

    public double getYPos() {
        return yPos;
    }

   public void moveTo(double x, double y) {
        this.xPos = x;
        this.yPos = y;
    }
}
