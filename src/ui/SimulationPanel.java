package ui;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JPanel;
import entities.Player;
import entities.Ball;
import entities.Team;
import world.PlaySpace;
import world.Environment;
import world.GameMap;

public class SimulationPanel extends JPanel {

    private GameMap map;
    private Team redTeam;
    private Team blueTeam;
    private Ball ball;
    // Things drawn on top of the pitch (scoreboard, stats...), in the order added.
    private final List<Overlay> overlays = new ArrayList<>();

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

    public void addOverlay(Overlay overlay) {
        overlays.add(overlay);
    }

    private void drawPlayers(
            Graphics g,
            Player[] players,
            Color colour,
            int offsetX,
            int offsetY,
            double scale) {

        int playerSize = 20; // Fixed visual size for the UI
        g.setColor(colour);

        for (Player player : players) {
            int px = offsetX + (int) Math.round(player.getXPos() * scale) - (playerSize / 2);
            int py = offsetY + (int) Math.round(player.getYPos() * scale) - (playerSize / 2);
            g.fillOval(px, py, playerSize, playerSize);
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g); // Activates double buffering

        Environment environment = map.getEnvironment();
        PlaySpace playSpace = map.getPlaySpace();

        // Pitch Margins
        int marginX = 50;
        int marginY = 50;

        // Calculate dynamic scale to fit the real-world coordinates into the window
        double scaleX = (double) (getWidth() - (2 * marginX)) / playSpace.getLength();
        double scaleY = (double) (getHeight() - (2 * marginY)) / playSpace.getWidth();
        double scale = Math.min(scaleX, scaleY);

        int scaledPitchLength = (int) Math.round(playSpace.getLength() * scale);
        int scaledPitchWidth = (int) Math.round(playSpace.getWidth() * scale);

        // Draw Surrounding Terrain
        g.setColor(environment.getSurroundingTerrain().getDisplayColor());
        g.fillRect(0, 0, getWidth(), getHeight());

        // Draw Playing Pitch
        g.setColor(environment.getPlayingTerrain().getDisplayColor());
        g.fillRect(marginX, marginY, scaledPitchLength, scaledPitchWidth);

        // Draw Field Lines
        g.setColor(Color.WHITE);
        int centerX = marginX + (scaledPitchLength / 2);
        int centerY = marginY + (scaledPitchWidth / 2);

        // Halfway line
        g.drawLine(centerX, marginY, centerX, marginY + scaledPitchWidth);

        // Center circle (Scaled to standard pitch dimensions)
        int circleSize = (int) Math.round(1830 * scale); 
        if (circleSize < 50) circleSize = 150; // Fallback minimum size
        
        g.drawOval(
                centerX - (circleSize / 2),
                centerY - (circleSize / 2),
                circleSize,
                circleSize
        );

        // Draw Teams
        drawPlayers(g, redTeam.getPlayers(), redTeam.getColor(), marginX, marginY, scale);
        drawPlayers(g, blueTeam.getPlayers(), blueTeam.getColor(), marginX, marginY, scale);

        // Draw Ball
        int ballSize = 14;
        g.setColor(Color.WHITE);
        int ballX = marginX + (int) Math.round(ball.getXPos() * scale) - (ballSize / 2);
        int ballY = marginY + (int) Math.round(ball.getYPos() * scale) - (ballSize / 2);
        g.fillOval(ballX, ballY, ballSize, ballSize);

        // Draw overlays last so they sit on top of everything
        for (Overlay overlay : overlays) {
            overlay.draw(g, getWidth(), getHeight());
        }
    }
}