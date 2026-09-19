package ui;

import java.awt.*;
import javax.swing.JPanel;
import entities.Player;
import entities.Ball;
import entities.Team;
import world.Goal;
import world.PlaySpace;
import world.Environment;
import world.GameMap;

public class SimulationPanel extends JPanel {

    private GameMap map;
    private Team redTeam;
    private Team blueTeam;
    private Ball ball;

    public SimulationPanel(
            GameMap map,
            Team redTeam,
            Team blueTeam,
            Ball ball) {

        this.map = map;
        this.redTeam = redTeam;
        this.blueTeam = blueTeam;
        this.ball = ball;
    }

    private void drawPlayers(
            Graphics g,
            Player[] players,
            Color colour,
            int offsetX,
            int offsetY) {

        int playerSize = 24;
        g.setColor(colour);

        for (Player player : players) {
            g.fillOval(
                    offsetX + (int) player.getXPos() - playerSize / 2,
                    offsetY + (int) player.getYPos() - playerSize / 2,
                    playerSize,
                    playerSize);
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g); // This single line activates double buffering

        PlaySpace playSpace = map.getPlaySpace();
        Environment environment = map.getEnvironment();

        int marginX = (int) map.getMarginX();
        int marginY = (int) map.getMarginY();

        g.setColor(environment.getSurroundingTerrain().getDisplayColor());
        g.fillRect(0, 0, getWidth(), getHeight());

        g.setColor(environment.getPlayingTerrain().getDisplayColor());
        g.fillRect(marginX, marginY, (int) playSpace.getWidth(), (int) playSpace.getHeight());

        g.setColor(Color.WHITE);

        Goal leftGoal = playSpace.getLeftGoal();
        Goal rightGoal = playSpace.getRightGoal();

        int centerX = marginX + (int) playSpace.getWidth() / 2;
        int centerY = marginY + (int) playSpace.getHeight() / 2;

        g.drawLine(centerX, marginY, centerX, marginY + (int) playSpace.getHeight());

        int circleSize = 150;
        g.drawOval(
                centerX - circleSize / 2, 
                centerY - circleSize / 2, 
                circleSize, 
                circleSize
        );

        g.drawRect(
                marginX + (int) leftGoal.getX(),
                marginY + (int) leftGoal.getY(),
                (int) leftGoal.getWidth(),
                (int) leftGoal.getHeight());

        g.drawRect(
                marginX + (int) rightGoal.getX(),
                marginY + (int) rightGoal.getY(),
                (int) rightGoal.getWidth(),
                (int) rightGoal.getHeight());

        drawPlayers(g, redTeam.getPlayers(), redTeam.getColor(), marginX, marginY);
        drawPlayers(g, blueTeam.getPlayers(), blueTeam.getColor(), marginX, marginY);

        int ballSize = 12;
        g.setColor(Color.WHITE);
        g.fillOval(
                marginX + (int) ball.getXPos() - ballSize / 2,
                marginY + (int) ball.getYPos() - ballSize / 2,
                ballSize,
                ballSize);

        g.setColor(Color.BLACK);
        g.drawOval(
                marginX + (int) ball.getXPos() - ballSize / 2,
                marginY + (int) ball.getYPos() - ballSize / 2,
                ballSize,
                ballSize);
    }
}