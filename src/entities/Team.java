package entities;

import java.awt.Color;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Optional;
import behaviour.ChallengeForBall;
import behaviour.MovementBehaviour;
import world.GameMap;

public class Team {

    private String name;
    private Color color;
    private Player[] players;
    private Player nominatedChallenger;
    private MovementBehaviour nominatedChallengeBehaviour;
    private final Tactics tactics;

    public Team(String name, Color color, Player[] players) {
        this(name, color, players, Tactics.balanced());
    }

    public Team(String name, Color color, Player[] players, Tactics tactics) {
        this.name = name;
        this.color = color;
        this.players = players;
        this.tactics = tactics;
    }

    // Sequential on purpose. With only 6 players, splitting the work across threads
    // costs far more than it saves (thread hand-off vs six distance checks).
    // Parallelism is used where it pays off: whole matches in BatchSimulator.
    public Optional<Player> chooseChallenger(Ball ball) {
        return Arrays.stream(players)
                .filter(player -> player != null)
                .filter(player -> !(player instanceof Goalkeeper))
                .min(Comparator.comparingDouble(player -> Math.hypot(
                        player.getXPos() - ball.getXPos(),
                        player.getYPos() - ball.getYPos())));
    }

    public void assignChallenger(
            Ball ball,
            GameMap map,
            double attackTargetX,
            double attackTargetY) {

        releaseChallenger();

        Optional<Player> choice = chooseChallenger(ball);

        if (choice.isPresent()) {
            nominatedChallenger = choice.get();

            nominatedChallengeBehaviour = new ChallengeForBall(
                    ball,
                    map,
                    attackTargetX,
                    attackTargetY);
            nominatedChallenger.setMovementBehaviour(nominatedChallengeBehaviour);
        }
    }

    public void releaseChallenger() {
        if (nominatedChallenger != null
                && nominatedChallenger.getMovementBehaviour() == nominatedChallengeBehaviour) {
            nominatedChallenger.restoreDefaultBehaviour();
        }

        nominatedChallenger = null;
        nominatedChallengeBehaviour = null;
    }

    public String getName() {
        return name;
    }

    public Color getColor() {
        return color;
    }

    public Player[] getPlayers() {
        return players;
    }

    public Tactics getTactics() {
        return tactics;
    }

}