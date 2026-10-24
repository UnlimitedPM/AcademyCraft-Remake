package cn.academy.terminal.tutorial;

public final class TutorialOpening {
    private TutorialOpening() {
    }

    public static double fade(double age, double start, double duration) {
        return Math.max(0, Math.min(1, (age - start) / duration));
    }

    public static double logoAlpha(int index, double age) {
        double start = switch (index) {
            case 0 -> 1.75;
            case 1 -> 1.3;
            case 2 -> 0.65;
            case 3 -> 0.1;
            default -> throw new IllegalArgumentException("Unknown tutorial logo: " + index);
        };
        return fade(age, start, 0.3);
    }

    public static double panelAlpha(double age) {
        return fade(age, 1.75, 0.3);
    }

    public static double logoY(double age) {
        return 63 + (-36 - 63) * fade(age, 0.7, 0.4);
    }

    public static double lineEnd(double age) {
        return 500 * fade(age, 0.4, 0.3);
    }

    public static double lineStart(double age) {
        return 50 + 150 * fade(age, 0.7, 0.2);
    }

    public static boolean listVisible(double age) {
        return age > 2.4;
    }

    public static double closingAlpha(double age) {
        return 1 - fade(age, 0, 0.3);
    }
}