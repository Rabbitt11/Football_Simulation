package simulation;

/*
 * TeamReport - the finished summary for one team, produced by the custom
 * collector in MatchStats. Immutable: once built, it never changes.
 */
public final class TeamReport {

    private final String teamName;
    private final String tactics;
    private final int players;
    private final double possessionSeconds;
    private final int passes;
    private final int shots;
    private final double distanceMetres;
    private final int topRunnerNumber;
    private final double topRunnerMetres;

    public TeamReport(
            String teamName,
            String tactics,
            int players,
            double possessionSeconds,
            int passes,
            int shots,
            double distanceMetres,
            int topRunnerNumber,
            double topRunnerMetres) {

        this.teamName = teamName;
        this.tactics = tactics;
        this.players = players;
        this.possessionSeconds = possessionSeconds;
        this.passes = passes;
        this.shots = shots;
        this.distanceMetres = distanceMetres;
        this.topRunnerNumber = topRunnerNumber;
        this.topRunnerMetres = topRunnerMetres;
    }

    // Possession as a percentage of both teams' combined possession time.
    public double possessionPercent(TeamReport opponent) {
        double total = possessionSeconds + opponent.possessionSeconds;
        return total == 0.0 ? 50.0 : 100.0 * possessionSeconds / total;
    }

    public String summaryAgainst(TeamReport opponent) {
        return String.format(
                "%s (%s)  possession %.0f%%  passes %d  shots %d  distance %.2f km  top runner #%d (%.0f m)",
                teamName, tactics, possessionPercent(opponent), passes, shots,
                distanceMetres / 1000.0, topRunnerNumber, topRunnerMetres);
    }

    public String getTeamName() {
        return teamName;
    }

    public String getTactics() {
        return tactics;
    }

    public int getPlayers() {
        return players;
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

    public double getDistanceMetres() {
        return distanceMetres;
    }

    public int getTopRunnerNumber() {
        return topRunnerNumber;
    }

    public double getTopRunnerMetres() {
        return topRunnerMetres;
    }
}