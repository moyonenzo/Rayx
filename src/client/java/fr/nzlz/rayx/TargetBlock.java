package fr.nzlz.rayx;

import net.minecraft.resources.Identifier;

public final class TargetBlock {

    private final Identifier id;
    private boolean enabled;
    private int color;

    public TargetBlock(
            Identifier id,
            boolean enabled,
            int color
    ) {
        this.id = id;
        this.enabled = enabled;
        this.color = color & 0xFFFFFF;
    }

    public Identifier id() {
        return id;
    }

    public boolean enabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int color() {
        return color;
    }

    public void setColor(int color) {
        this.color = color & 0xFFFFFF;
    }
}