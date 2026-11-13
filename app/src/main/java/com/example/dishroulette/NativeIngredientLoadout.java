package com.example.dishroulette;

import java.util.List;

/** Ingredient editing state and change notifications for the native home screen. */
final class NativeIngredientLoadout {
    private final IngredientMagazine magazine=new IngredientMagazine();
    private Runnable changed,focus;
    private boolean locked;
    public IngredientMagazine magazine(){return magazine;}
    public void setChangedListener(Runnable value){changed=value;publish();}
    void setFocusListener(Runnable value){focus=value;}
    void publish(){if(changed!=null)changed.run();}
    public void restore(List<String> slots,int active){magazine.restore(slots,active);publish();}
    public void select(int slot){if(locked)return;magazine.select(slot);publish();focusCartridge();}
    public void clearChamber(){if(!locked){magazine.edit("");publish();}}
    public void clearAll(){if(!locked){magazine.restore(null,0);publish();}}
    public void focusCartridge(){if(!locked&&focus!=null)focus.run();}
    public void lock(boolean value){locked=value;}
}
