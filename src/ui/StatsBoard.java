package ui;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.util.List;

import simulation.GameState;
import simulation.GoalEvent;
import simulation.MatchListener;
import simulation.MatchPhase;
import simulation.MatchStats;

/*
 * StatsBoard - an OBSERVER of the Match and an OVERLAY on the panel.
 *
 * At half-time and full-time it asks MatchStats (the collectors) for a
 * report, prints it to the console, and shows it in a box on screen.
 */
public class StatsBoard implements MatchListener, Overlay {

    private static final long HALF_TIME_MILLIS = 7000;

    private List<String> lines = List.of();
    private String heading = "";
    private long visibleUntil = 0;

    @Override
    public void onGoal(GoalEvent goal, GameState state) {
        // goals don't open the stats box
    }

    @Override
    public void onPhaseChange(MatchPhase newPhase, GameState state) {
        if (newPhase == MatchPhase.SECOND_HALF) {
            show("HALF-TIME STATS", state, System.currentTimeMillis() + HALF_TIME_MILLIS);
        }
    }

    @Override
    public void onMatchEnd(GameState state) {
        show("FULL-TIME STATS", state, Long.MAX_VALUE);
    }

    private void show(String title, GameState state, long until) {
        heading = title;
        lines = MatchStats.reportLines(state.getRedTeam(), state.getBlueTeam());
        visibleUntil = until;

        System.out.println("---- " + title + " ----");
        lines.forEach(System.out::println);
    }

    @Override
    public void draw(Graphics g, int panelWidth, int panelHeight) {
        if (lines.isEmpty() || System.currentTimeMillis() > visibleUntil) {
            return;
        }

        g.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        FontMetrics metrics = g.getFontMetrics();
        int lineHeight = metrics.getHeight();

        int boxWidth = lines.stream()
                .mapToInt(metrics::stringWidth)
                .max()
                .orElse(200) + 30;
        int boxHeight = lineHeight * (lines.size() + 1) + 20;
        int x = (panelWidth - boxWidth) / 2;
        int y = panelHeight / 2 + 40;

        g.setColor(new Color(0, 0, 0, 200));
        g.fillRoundRect(x, y, boxWidth, boxHeight, 12, 12);

        g.setColor(Color.YELLOW);
        g.drawString(heading, x + 15, y + 10 + metrics.getAscent());

        g.setColor(Color.WHITE);
        for (int i = 0; i < lines.size(); i++) {
            g.drawString(lines.get(i), x + 15, y + 10 + metrics.getAscent() + lineHeight * (i + 1));
        }
    }
}