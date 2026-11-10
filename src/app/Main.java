package app;

import java.awt.*;

import java.io.IOException;
import javax.swing.JFrame;
import javax.swing.Timer;

import entities.Ball;
import entities.Coach;
import entities.Player;
import entities.Tactics;
import entities.Team;
import entities.TeamComp;
import entities.TeamLoader;

import simulation.Match;
import simulation.Ruleset;

import ui.Scoreboard;
import ui.SimulationPanel;

import world.GameMap;
import world.MapPreset;
import world.PlaySpace;
import world.Terrain;

public class Main {

    // A short match so the showcase can see both halves: 2.5 minutes each.
    private static final int HALF_LENGTH_SECONDS = 150;

    public static void main(String[] args) {

        GameMap map = new GameMap(MapPreset.DEFAULT);
        PlaySpace playSpace = map.getPlaySpace();

               // Change these two lines to try different match-ups.
        String redPreset = "teams/presets/aggressive.txt";
        String bluePreset = "teams/presets/defensive.txt";

        Player[] redPlayers = loadTeamOrDefault(redPreset, playSpace, true);
        Player[] bluePlayers = loadTeamOrDefault(bluePreset, playSpace, false);
        Team redTeam = new Team("Red Team", Color.RED, redPlayers, loadTacticsOrDefault(redPreset));
        Team blueTeam = new Team("Blue Team", Color.BLUE, bluePlayers, loadTacticsOrDefault(bluePreset));

        Ball ball = new Ball(
                playSpace.getLength() / 2,
                playSpace.getWidth() / 2,
                0.0);

        for (Player player : redPlayers) {
            player.setMatchDetails(ball, redTeam, blueTeam);
            player.initialiseDefaultBehaviour(playSpace, true);
        }

        for (Player player : bluePlayers) {
            player.setMatchDetails(ball, blueTeam, redTeam);
            player.initialiseDefaultBehaviour(playSpace, false);
        }

        // ---------- Observer pattern: Match is the subject ----------
        Ruleset shortMatch = new Ruleset(
                HALF_LENGTH_SECONDS, HALF_LENGTH_SECONDS, 60, 6, false, false);
        Match match = new Match(redTeam, blueTeam, ball, shortMatch);

        // Red defends the left goal, so red attacks right.
        Coach redCoach = new Coach(redTeam, ball, playSpace.getLength(), true);
        Coach blueCoach = new Coach(blueTeam, ball, playSpace.getLength(), false);
        Scoreboard scoreboard = new Scoreboard(match.getGameState(), redCoach, blueCoach);

        match.addListener(scoreboard);
        match.addListener(redCoach);
        match.addListener(blueCoach);
        // A listener can also be a lambda, because MatchListener is a functional interface.
        match.addListener((goal, state) -> System.out.println("GOAL  " + goal + "   Score: " + state.getScore()));

        EventQueue.invokeLater(() -> {
            // Ensures that the GUI is created on the Event Dispatch Thread (EDT)
            JFrame frame = new JFrame("Football Simulation");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

            SimulationPanel panel = new SimulationPanel(
                    map,
                    redTeam,
                    blueTeam,
                    ball);
            panel.setScoreboard(scoreboard);

            // Set the preferred size of the panel to 1280x720 pixels due to map scaling issues
            panel.setPreferredSize(new Dimension(1280, 720));

            frame.add(panel);
            frame.pack();
            frame.setVisible(true);

                        // Start first: the coaches set up their preset tactics on kick-off...
            match.start();

            // ...then the demo gives the ball carrier and challenger their special behaviours.
            SimulationSetup.setupAttackingDemo(ball, redPlayers, bluePlayers, map);
            blueTeam.assignChallenger(ball, map, 200.0, playSpace.getWidth() / 2);

            Timer timer = new Timer(1000, e -> {
                Player attacker;
                if (Math.random() < 0.5) {
                    attacker = redPlayers[5];
                } else {
                    attacker = bluePlayers[5];
                }
                ball.moveTo(attacker.getXPos(), attacker.getYPos(), 0.0);
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
                if (ballHolder != null) {
                    ballHolder.pass();
                }
            });
            passingTimer.start();

            // The match clock: one tick per real second.
            Timer clockTimer = new Timer(1000, e -> match.update(1));
            clockTimer.start();

            Timer movementTimer = new Timer(20, e -> {
                // Freeze the pitch once the final whistle has gone.
                if (!match.getGameState().isRunning()) {
                    panel.repaint();
                    return;
                }

                for (Player player : redPlayers) {
                    player.update(0.02);
                }

                for (Player player : bluePlayers) {
                    player.update(0.02);
                }

                Terrain surface;

                if (playSpace.contains(ball.getXPos(), ball.getYPos())) {
                    surface = map.getEnvironment().getPlayingTerrain();
                } else {
                    surface = map.getEnvironment().getSurroundingTerrain();
                }

                // Update the ball's position every 20 milliseconds (0.02 seconds)
                ball.update(0.02, surface.getRollingDeceleration());

                boolean inLeftGoal = SimulationSetup.isBallInGoal(ball, playSpace.getLeftGoal());
                boolean inRightGoal = SimulationSetup.isBallInGoal(ball, playSpace.getRightGoal());

                if (inLeftGoal || inRightGoal) {
                    // Red attacks right, so a ball in the right goal is a red goal.
                    Team scoringTeam = inRightGoal ? redTeam : blueTeam;
                    Player scorer = ball.getLastToucher();
                    boolean ownGoal = scorer != null && scorer.getTeam() != scoringTeam;

                    // Notify every observer (scoreboard, coaches, console)...
                    match.recordGoal(scorer, scoringTeam, null, ownGoal);

                    // ...then restart. Players pick up any new tactics from their coach here.
                    blueTeam.releaseChallenger();
                    SimulationSetup.resetDemo(ball, redPlayers, bluePlayers, map);
                    blueTeam.assignChallenger(ball, map, 200.0, playSpace.getWidth() / 2);
                }
                panel.repaint();
            });
            movementTimer.start();
        });
    }
    private static Tactics loadTacticsOrDefault(String filename) {
        try {
            return TeamLoader.loadTactics(filename);
        } catch (IOException | IllegalArgumentException e) {
            System.out.println("Could not load tactics from " + filename + ": " + e.getMessage());
            System.out.println("Using balanced tactics.");
            return Tactics.balanced();
        }
    }


    private static Player[] loadTeamOrDefault(
            String filename,
            PlaySpace playSpace,
            boolean leftSide) {

        try {
            return TeamLoader.loadTeam(filename, playSpace, leftSide);
        } catch (IOException | IllegalArgumentException e) {
            System.out.println("Could not load " + filename + ": " + e.getMessage());
            System.out.println("Using the default team.");
        }

        if (leftSide) {
            return TeamComp.createLeftTeam(playSpace);
        } else {
            return TeamComp.createRightTeam(playSpace);
        }
    }
}