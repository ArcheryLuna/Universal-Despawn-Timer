package dev.seb.despawntimer;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public record DespawnDuration(Mode mode, long milliseconds) {
    private static final Pattern DURATION = Pattern.compile("^([1-9][0-9]*)\\s*(t|s|m|h|d)?$", Pattern.CASE_INSENSITIVE);

    public enum Mode { DEFAULT, FINITE, NEVER }

    public DespawnDuration {
        if (mode == Mode.FINITE && milliseconds <= 0) {
            throw new IllegalArgumentException("A finite duration must be greater than zero");
        }
    }

    public long ticks() {
        if (mode != Mode.FINITE) {
            throw new IllegalStateException("Only finite durations have a tick count");
        }
        return 1L + (milliseconds - 1L) / 50L;
    }

    public static DespawnDuration parse(Object input) {
        if (input == null) {
            return new DespawnDuration(Mode.DEFAULT, 0);
        }

        String value = String.valueOf(input).trim().toLowerCase(Locale.ROOT);
        if (value.equals("default")) {
            return new DespawnDuration(Mode.DEFAULT, 0);
        }
        if (value.equals("never")) {
            return new DespawnDuration(Mode.NEVER, 0);
        }

        Matcher matcher = DURATION.matcher(value);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Invalid duration '" + input + "'");
        }

        long amount;
        try {
            amount = Long.parseLong(matcher.group(1));
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Duration is too large: '" + input + "'", exception);
        }

        String unit = matcher.group(2);
        long multiplier = switch (unit == null ? "s" : unit.toLowerCase(Locale.ROOT)) {
            case "t" -> 50L;
            case "s" -> 1_000L;
            case "m" -> 60_000L;
            case "h" -> 3_600_000L;
            case "d" -> 86_400_000L;
            default -> throw new IllegalArgumentException("Unsupported duration unit in '" + input + "'");
        };

        try {
            return new DespawnDuration(Mode.FINITE, Math.multiplyExact(amount, multiplier));
        } catch (ArithmeticException exception) {
            throw new IllegalArgumentException("Duration is too large: '" + input + "'", exception);
        }
    }
}
