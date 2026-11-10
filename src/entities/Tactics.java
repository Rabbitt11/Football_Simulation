package entities;

import java.util.Map;

/*
 * Tactics - HOW a team likes to play, loaded from a preset file.
 *
 * Immutable: all fields are final and there are no setters. A team's
 * tactics can be shared or read from anywhere without anyone changing
 * them by accident. To "change tactics", you make a new Tactics object.
 *
 *   formationShift     fraction of the pitch the whole formation moves up (+) or back (-)
 *   pressing           extra pull towards the ball for every outfield player
 *   shootingRangeCm    how close to goal a carrier must be before shooting
 *   passUnderPressureCm  how close an opponent gets before the carrier passes
 */
public final class Tactics {

    private final String name;
    private final double formationShift;
    private final double pressing;
    private final double shootingRangeCm;
    private final double passUnderPressureCm;

    public Tactics(
            String name,
            double formationShift,
            double pressing,
            double shootingRangeCm,
            double passUnderPressureCm) {

        // Fail fast: a bad preset file should stop at load time,
        // not cause strange behaviour halfway through a match.
        if (formationShift < -0.3 || formationShift > 0.3) {
            throw new IllegalArgumentException("formationShift must be between -0.3 and 0.3");
        }
        if (pressing < -0.3 || pressing > 0.5) {
            throw new IllegalArgumentException("pressing must be between -0.3 and 0.5");
        }
        if (shootingRangeCm <= 0 || passUnderPressureCm <= 0) {
            throw new IllegalArgumentException("Distances must be positive");
        }

        this.name = name;
        this.formationShift = formationShift;
        this.pressing = pressing;
        this.shootingRangeCm = shootingRangeCm;
        this.passUnderPressureCm = passUnderPressureCm;
    }

    // The values the game used before presets existed.
    public static Tactics balanced() {
        return new Tactics("balanced", 0.0, 0.0, 1800.0, 500.0);
    }

    // Build from key=value pairs. Any key that is missing keeps the balanced value.
    public static Tactics fromMap(String name, Map<String, Double> values) {
        Tactics defaults = balanced();

        return new Tactics(
                name,
                values.getOrDefault("formationShift", defaults.formationShift),
                values.getOrDefault("pressing", defaults.pressing),
                values.getOrDefault("shootingRange", defaults.shootingRangeCm),
                values.getOrDefault("passUnderPressure", defaults.passUnderPressureCm));
    }

    public String getName() {
        return name;
    }

    public double getFormationShift() {
        return formationShift;
    }

    public double getPressing() {
        return pressing;
    }

    public double getShootingRangeCm() {
        return shootingRangeCm;
    }

    public double getPassUnderPressureCm() {
        return passUnderPressureCm;
    }

    @Override
    public String toString() {
        return name;
    }
}