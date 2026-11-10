package simulation;

/*
 * MatchResult - how one finished match turned out. Immutable, so results
 * produced on different threads can be collected and shared safely.
 */
public final class MatchResult {

    public enum Outcome {
        RED_WIN, DRAW, BLUE_WIN
    }

    private final String redTactics;
    private final String blueTactics;
    private final int redGoals;
    private final int blueGoals;
    private final TeamReport redReport;
    private final TeamReport blueReport;
    private final long ticks;

    public MatchResult(String redTactics, String blueTactics, int redGoals, int blueGoals,
            TeamReport redReport, TeamReport blueReport, long ticks) {
        this.redTactics = redTactics;
        this.blueTactics = blueTactics;
        this.redGoals = redGoals;
        this.blueGoals = blueGoals;
        this.redReport = redReport;
        this.blueReport = blueReport;
        this.ticks = ticks;
    }

    public Outcome outcome() {
        if (redGoals > blueGoals) {
            return Outcome.RED_WIN;
        }
        if (blueGoals > redGoals) {
            return Outcome.BLUE_WIN;
        }
        return Outcome.DRAW;
    }

    // The same match, described with the two teams swapped.
    // Used when a tactic played on the blue side but we want its point of view.
    public MatchResult swapped() {
        return new MatchResult(blueTactics, redTactics, blueGoals, redGoals,
                blueReport, redReport, ticks);
    }

    public int totalGoals() {
        return redGoals + blueGoals;
    }

    public double redPossessionPercent() {
        return redReport.possessionPercent(blueReport);
    }

    public String getRedTactics() {
        return redTactics;
    }

    public String getBlueTactics() {
        return blueTactics;
    }

    public int getRedGoals() {
        return redGoals;
    }

    public int getBlueGoals() {
        return blueGoals;
    }

    public TeamReport getRedReport() {
        return redReport;
    }

    public TeamReport getBlueReport() {
        return blueReport;
    }

    public long getTicks() {
        return ticks;
    }

    @Override
    public String toString() {
        return redTactics + " " + redGoals + " - " + blueGoals + " " + blueTactics;
    }
}