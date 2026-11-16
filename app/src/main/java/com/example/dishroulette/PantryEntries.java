package com.example.dishroulette;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * The sole ingredient store. Empty slots are retained so editing never shifts saved ingredient positions.
 */
public final class PantryEntries {
    private final ArrayList<String> slots = new ArrayList<>();
    private int active;

    public PantryEntries() {
        ensure(5);
    }

    private void ensure(int index) {
        while (slots.size() <= index)
            slots.add("");
    }

    public int active() {
        return active;
    }

    public String text() {
        return slots.get(active);
    }

    public void edit(String text) {
        slots.set(active, text);
    }

    public void select(int index) {
        ensure(index);
        active = index;
    }

    public enum ValidationError {MULTIPLE_INGREDIENTS, DUPLICATE, NAME_TOO_LONG, SERVICE_LIMIT}

    /**
     * Find a free position without allocating a saved-state copy for every photo ingredient.
     */
    public int firstEmptySlot() {
        for (int i = 0; i < slots.size(); i++)
            if (slots.get(i).trim().isEmpty())
                return i;
        return slots.size();
    }

    public ValidationError validationError() {
        Set<String> names = new LinkedHashSet<>();
        int chars = 0;
        for (String value : slots) {
            // Check the original text before whitespace normalization erases line breaks.
            if (value.contains(",") || value.contains("،") || value.contains("؛")
                || value.contains(";") || value.contains("\n") || value.contains("\r")) {
                return ValidationError.MULTIPLE_INGREDIENTS;
            }
            String name = value.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
            if (name.isEmpty())
                continue;
            String canonical = RecipeCatalog.parseIngredients(name).iterator().next();
            if (!names.add(canonical))
                return ValidationError.DUPLICATE;
            chars += name.length();
            if (name.length() > 80)
                return ValidationError.NAME_TOO_LONG;
        }
        if (names.size() > 20 || chars > 300)
            return ValidationError.SERVICE_LIMIT;
        return null;
    }

    /**
     * Commit a validated entry; blank input cannot advance.
     */
    public boolean commit() {
        if (text().trim().isEmpty() || validationError() != null)
            return false;
        slots.set(active, text().trim());
        active++;
        ensure((active / 6) * 6 + 5);
        return true;
    }

    public Set<String> ingredients() {
        Set<String> result = new LinkedHashSet<>();
        for (String slot : slots)
            if (!slot.trim().isEmpty())
                result.add(slot.trim());
        return result;
    }

    public int count() {
        return ingredients().size();
    }

    public ArrayList<String> save() {
        return new ArrayList<>(slots);
    }

    public void restore(List<String> values, int selected) {
        slots.clear();
        if (values != null)
            for (String value : values)
                slots.add(value == null ? "" : value);
        active = Math.max(0, Math.min(selected, Math.max(0, slots.size() - 1)));
        ensure((active / 6) * 6 + 5);
    }
}
