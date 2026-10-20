package com.example.dishroulette;

import java.util.List;

/** Shared ingredient-input contract. Only the Advanced implementation owns OpenGL/audio. */
interface IngredientLoadout {
    IngredientMagazine magazine();
    void setChangedListener(Runnable changed);
    void restore(List<String> slots,int active);
    boolean isLoading();
    void select(int slot);
    void page(int direction);
    void clearChamber();
    void clearAll();
    void focusCartridge();
    void setGoal(String goal);
    void setDiscovery(boolean value);
    void refreshExperience();
    void lock(boolean value);
    void fire(Runnable complete);
    void cancelFire();
    void finish();
    void inspect();
    void resume();
    void pause();
    void dispose();
}
