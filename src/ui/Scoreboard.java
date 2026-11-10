package ui;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;

import entities.Coach;
import simulation.GameState;
import simulation.GoalEvent;
import simulation.MatchListener;
import simulation.MatchPhase;

/*
 * Scoreboard - an OBSERVER of the Match and an OVERLAY on the panel.
 * Draws the score, the clock, the coaches' mentalities and goal alerts.
 *
 * It reads the score straight from GameState every frame, and uses the
 * onGoal notification only to flash a "GOAL!" banner for a few seconds.
 */
public class Scoreboard implements MatchListener, Overlay {

    private static final long BANNER_MILLIS = 2500;

    private final GameState state;
    private final Coach[] coaches;

    private String bannerText = null;
    private long bannerUntil = 0;

    public Scoreboard(GameState state, Coach... coaches) {
        this.state = state;
        this.coaches = coaches;
    }

    @Override
    public void onGoal(GoalEvent goal, GameState state) {
        bannerText = "GOAL!  " + goal;
        bannerUntil = System.currentTimeMillis() + BANNER_MILLIS;
    }

    @Override
    public void onPhaseChange(MatchPhase newPhase, GameState state) {
        bannerText = newPhase == MatchPhase.SECOND_HALF ? "SECOND HALF" : newPhase.toString();
        bannerUntil = System.currentTimeMillis() + BANNER_MILLIS;
    }

    @Override
    public void onMatchEnd(GameState state) {
        bannerText = "FULL TIME  " + state.getRedTeam().getName() + " "
                + state.getScore() + " " + state.getBlueTeam().getName();
        bannerUntil = Long.MAX_VALUE; // stays on screen
    }

    @Override
    public void draw(Graphics g, int panelWidth, int panelHeight) {
        int seconds = state.getElapsedTime();
        String clock = String.format("%02d:%02d", seconds / 60, seconds % 60);

        String scoreLine = state.getRedTeam().getName() + "  "
                + state.getRedScore() + " - " + state.getBlueScore() + "  "
                + state.getBlueTeam().getName() + "     " + clock
                + "   " + state.getCurrentPhase();

        // top bar
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 18));
        drawCentred(g, scoreLine, panelWidth, 32, new Color(0, 0, 0, 170), Color.WHITE);

        // coach mentalities, bottom-left
        g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
        g.setColor(Color.WHITE);
        int y = panelHeight - 15 - 18 * (coaches.length - 1);
        for (Coach coach : coaches) {
            g.drawString(coach.getTeam().getName() + " (" + coach.getTeam().getTactics()
                    + ") coach: " + coach.getMentality(), 15, y);
            y += 18;
        }

        // goal / phase banner in the middle of the screen
        if (bannerText != null && System.currentTimeMillis() < bannerUntil) {
            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 28));
            drawCentred(g, bannerText, panelWidth, panelHeight / 2,
                    new Color(0, 0, 0, 190), Color.YELLOW);
        }
    }

    private void drawCentred(Graphics g, String text, int panelWidth, int baselineY,
            Color background, Color foreground) {
        FontMetrics metrics = g.getFontMetrics();
        int textWidth = metrics.stringWidth(text);
        int x = (panelWidth - textWidth) / 2;

        g.setColor(background);
        g.fillRoundRect(x - 14, baselineY - metrics.getAscent() - 6,
                textWidth + 28, metrics.getHeight() + 12, 12, 12);

        g.setColor(foreground);
        g.drawString(text, x, baselineY);
    }
}