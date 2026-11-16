package com.example.dishroulette;

import java.util.List;

/**
 * Ingredient editing state and change notifications for the native home screen.
 */
final class PantryController {
    private final PantryEntries entries = new PantryEntries();
    private Runnable changed, focus;
    private boolean locked;

    public PantryEntries entries() {
        return entries;
    }

    public void setChangedListener(Runnable value) {
        changed = value;
        publish();
    }

    void setFocusListener(Runnable value) {
        focus = value;
    }

    void publish() {
        if (changed != null)
            changed.run();
    }

    public void restore(List<String> slots, int active) {
        entries.restore(slots, active);
        publish();
    }

    public void select(int slot) {
        if (locked)
            return;
        entries.select(slot);
        publish();
        focusSelected();
    }

    public void removeSelected() {
        if (!locked) {
            entries.edit("");
            publish();
        }
    }

    public void clearAll() {
        if (!locked) {
            entries.restore(null, 0);
            publish();
        }
    }

    public void focusSelected() {
        if (!locked && focus != null)
            focus.run();
    }

    public void lock(boolean value) {
        locked = value;
    }
}
