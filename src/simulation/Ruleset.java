package simulation;

public class Ruleset {

    private int firstHalfDuration;
    private int secondHalfDuration;
    private int extraTimeHalfDuration;

    private int playersPerTeam;

    private boolean extraTimeEnabled;
    private boolean penaltiesEnabled;

    
    public Ruleset() {
        this.firstHalfDuration = 45 * 60;
        this.secondHalfDuration = 45 * 60;
        this.extraTimeHalfDuration = 15 * 60;

        this.playersPerTeam = 11;

        this.extraTimeEnabled = true;
        this.penaltiesEnabled = true;
    }

    
    public Ruleset(
            int firstHalfDuration,
            int secondHalfDuration,
            int extraTimeHalfDuration,
            int playersPerTeam,
            boolean extraTimeEnabled,
            boolean penaltiesEnabled) {

        this.firstHalfDuration = firstHalfDuration;
        this.secondHalfDuration = secondHalfDuration;
        this.extraTimeHalfDuration = extraTimeHalfDuration;
        this.playersPerTeam = playersPerTeam;
        this.extraTimeEnabled = extraTimeEnabled;
        this.penaltiesEnabled = penaltiesEnabled;
    }

    public int getFirstHalfDuration() {
        return firstHalfDuration;
    }

    public int getSecondHalfDuration() {
        return secondHalfDuration;
    }

    public int getExtraTimeHalfDuration() {
        return extraTimeHalfDuration;
    }

    public int getPlayersPerTeam() {
        return playersPerTeam;
    }

    public boolean isExtraTimeEnabled() {
        return extraTimeEnabled;
    }

    public boolean isPenaltiesEnabled() {
        return penaltiesEnabled;
    }

    public void setFirstHalfDuration(int firstHalfDuration) {
        if (firstHalfDuration > 0) {
            this.firstHalfDuration = firstHalfDuration;
        }
    }

    public void setSecondHalfDuration(int secondHalfDuration) {
        if (secondHalfDuration > 0) {
            this.secondHalfDuration = secondHalfDuration;
        }
    }

    public void setExtraTimeHalfDuration(int extraTimeHalfDuration) {
        if (extraTimeHalfDuration > 0) {
            this.extraTimeHalfDuration = extraTimeHalfDuration;
        }
    }

    public void setPlayersPerTeam(int playersPerTeam) {
        if (playersPerTeam > 0) {
            this.playersPerTeam = playersPerTeam;
        }
    }

    public void setExtraTimeEnabled(boolean extraTimeEnabled) {
        this.extraTimeEnabled = extraTimeEnabled;
    }

    public void setPenaltiesEnabled(boolean penaltiesEnabled) {
        this.penaltiesEnabled = penaltiesEnabled;
    }
  

    public String toString() {
        return "Ruleset{" +
                "firstHalfDuration=" + firstHalfDuration +
                ", secondHalfDuration=" + secondHalfDuration +
                ", extraTimeHalfDuration=" + extraTimeHalfDuration +
                ", playersPerTeam=" + playersPerTeam +
                ", extraTimeEnabled=" + extraTimeEnabled +
                ", penaltiesEnabled=" + penaltiesEnabled +
                '}';
    }
}