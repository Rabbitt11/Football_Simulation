package ui;

import java.awt.Graphics;

/*
 * Overlay - anything that draws on top of the pitch (scoreboard, stats box...).
 *
 * SimulationPanel keeps a List<Overlay> and draws each one after the pitch,
 * so new overlays can be added without changing the panel.
 * One abstract method, so a simple overlay can even be a lambda.
 */
@FunctionalInterface
public interface Overlay {

    void draw(Graphics g, int panelWidth, int panelHeight);
}