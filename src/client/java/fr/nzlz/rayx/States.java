package fr.nzlz.rayx;

public final class States {

    private static boolean enabled = false;

    private States() {
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static void toggle() {
        enabled = !enabled;
    }

    public static void setEnabled(boolean value) {
        enabled = value;
    }
}
