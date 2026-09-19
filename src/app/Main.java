package app;

import java.awt.*;
import javax.swing.JFrame;
import javax.swing.Timer; 

import entities.Ball;
import entities.Player;
import entities.TeamComp;
import entities.Team;
import ui.SimulationPanel;

import world.GameMap;
import world.PlaySpace;
import world.MapPreset;

public class Main {

        public static void main(String[] args) {

                GameMap map = new GameMap(MapPreset.DEFAULT);
                PlaySpace playSpace = map.getPlaySpace();

                // Swapped to JFrame for Swing compatibility
                JFrame frame = new JFrame("Football Simulation");
                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

                Player[] redPlayers = TeamComp.createLeftTeam(playSpace);
                Player[] bluePlayers = TeamComp.createRightTeam(playSpace);

                Team redTeam = new Team("Red Team", Color.RED, redPlayers);
                Team blueTeam = new Team("Blue Team", Color.BLUE, bluePlayers);

                Ball ball = new Ball(
                                playSpace.getWidth() / 2,
                                playSpace.getHeight() / 2);

                for (Player player : redPlayers) {
                        player.setMatchDetails(ball, redTeam);
                }

                for (Player player : bluePlayers) {
                        player.setMatchDetails(ball, blueTeam);
                }

                SimulationPanel panel = new SimulationPanel(
                                map,
                                redTeam,
                                blueTeam,
                                ball);

                panel.setPreferredSize(
                                new Dimension(
                                                (int) (playSpace.getWidth() + map.getMarginX() * 2),
                                                (int) (playSpace.getHeight() + map.getMarginY() * 2)));

                frame.add(panel);
                frame.pack();
                frame.setVisible(true);

                Timer gameLoop = new Timer(16, e -> {
                        ball.update();
                        for (Player p : redPlayers) p.update();
                        for (Player p : bluePlayers) p.update();
                        panel.repaint();
                });
                gameLoop.start();

                Timer timer = new Timer(1000, e -> {
                        Player attacker;
                        if (Math.random() < 0.5) {
                                attacker = redPlayers[5];
                        } else {
                                attacker = bluePlayers[5];
                        }
                        ball.moveTo(attacker.getXPos(), attacker.getYPos());
                });
                timer.setRepeats(false);
                timer.start();

                Timer passingTimer = new Timer(2000, e -> {
                        Player ballHolder = null;
                        for (Player player : redPlayers) {
                                if (player.hasPossession(ball)) {
                                        ballHolder = player;
                                        break;
                                }
                        }
                        if (ballHolder == null) {
                                for (Player player : bluePlayers) {
                                        if (player.hasPossession(ball)) {
                                                ballHolder = player;
                                                break;
                                        }
                                }
                        }
                        if (ballHolder != null) {
                                ballHolder.pass();
                        }
                });
                passingTimer.start();
        }
}