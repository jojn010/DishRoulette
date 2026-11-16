package com.example.dishroulette;

import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.*;

public class PantryEntriesTest {
    @Test
    public void moreThanSixEntriesRemainAvailable() {
        PantryEntries m = new PantryEntries();
        for (String s : Arrays.asList("Chicken", "Rice", "Tomato", "Onion", "Garlic", "Lemon")) {
            m.edit(s);
            assertTrue(m.commit());
        }
        assertEquals(6, m.active());
        assertEquals(6, m.count());
        assertFalse(m.commit());
        assertEquals(6, m.active());
        m.edit("Pepper");
        assertTrue(m.commit());
        assertEquals(7, m.count());
        m.select(0);
        assertEquals("Chicken", m.text());
    }

    @Test
    public void editingDeletingAndRestoringKeepStableSlots() {
        PantryEntries m = new PantryEntries();
        m.edit("Chicken");
        m.commit();
        m.edit("Rice");
        m.commit();
        m.select(0);
        m.edit("Tofu");
        assertEquals(2, m.count());
        assertFalse(m.ingredients().contains("Chicken"));
        PantryEntries restored = new PantryEntries();
        restored.restore(m.save(), m.active());
        assertEquals("Tofu", restored.text());
        restored.edit("");
        assertEquals(1, restored.count());
        restored.select(1);
        assertEquals("Rice", restored.text());
    }

    @Test
    public void duplicatesAndServiceLimitsAreExplicitErrors() {
        PantryEntries m = new PantryEntries();
        m.edit("Rice");
        m.commit();
        m.edit(" rice ");
        assertNotNull(m.validationError());
        assertFalse(m.commit());
        assertEquals(1, m.active());
        m.edit("");
        for (int i = 1; i <= 20; i++) {
            m.edit("item" + i);
            m.select(i + 1);
        }
        assertNotNull(m.validationError());
        assertEquals(21, m.count());
    }

    @Test
    public void pastedIngredientListsCannotBypassSingleEntryValidation() {
        for (String separator : Arrays.asList("\n", "\r\n", "\r", ",", ";", "،", "؛")) {
            PantryEntries m = new PantryEntries();
            String pasted = "rice" + separator + "chicken";
            m.edit(pasted);
            assertEquals(PantryEntries.ValidationError.MULTIPLE_INGREDIENTS, m.validationError());
            assertFalse(m.commit());
            assertEquals(0, m.active());
            assertEquals(pasted, m.text());
            m.edit("rice");
            assertNull(m.validationError());
            assertTrue(m.commit());
        }
    }

    @Test
    public void spacesAndTabsRemainValidInsideAnIngredientName() {
        PantryEntries m = new PantryEntries();
        m.edit("  olive\t oil  ");
        assertNull(m.validationError());
        assertTrue(m.commit());
    }
}
