package entities;

/*
 * PlayerStats - running totals for one player during a match.
 *
 * Each Player owns exactly one PlayerStats and updates it as the game runs.
 * Collectors (see simulation.MatchStats) read these numbers afterwards
 * to build team reports.
 */
public class PlayerStats {

    private double distanceCm = 0.0;
    private double possessionSeconds = 0.0;
    private int passes = 0;
    private int shots = 0;

    void addDistance(double cm) {
        if (cm > 0.0) {
            distanceCm += cm;
        }
    }

    void addPossession(double seconds) {
        if (seconds > 0.0) {
            possessionSeconds += seconds;
        }
    }

    void recordPass() {
        passes++;
    }

    // public: shots are decided by AttackWithBall, which lives in the behaviour package
    public void recordShot() {
        shots++;
    }

    public void reset() {
        distanceCm = 0.0;
        possessionSeconds = 0.0;
        passes = 0;
        shots = 0;
    }

    public double getDistanceMetres() {
        return distanceCm / 100.0;
    }

    public double getPossessionSeconds() {
        return possessionSeconds;
    }

    public int getPasses() {
        return passes;
    }

    public int getShots() {
        return shots;
    }
}