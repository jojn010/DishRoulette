package com.example.dishroulette;

import java.util.List;

/** Lightweight Simple-mode adapter. No renderer, audio pool, model, or GL surface is constructed. */
final class NativeIngredientLoadout implements IngredientLoadout {
    private final IngredientMagazine magazine=new IngredientMagazine();
    private Runnable changed,focus;
    private boolean locked;
    public IngredientMagazine magazine(){return magazine;}
    public void setChangedListener(Runnable value){changed=value;publish();}
    void setFocusListener(Runnable value){focus=value;}
    void publish(){if(changed!=null)changed.run();}
    public void restore(List<String> slots,int active){magazine.restore(slots,active);publish();}
    public boolean isLoading(){return false;}
    public void select(int slot){if(locked)return;magazine.select(slot);publish();focusCartridge();}
    public void page(int direction){select(Math.max(0,magazine.page()+direction)*6);}
    public void clearChamber(){if(!locked){magazine.edit("");publish();}}
    public void clearAll(){if(!locked){magazine.restore(null,0);publish();}}
    public void focusCartridge(){if(!locked&&focus!=null)focus.run();}
    public void lock(boolean value){locked=value;}
    public void fire(Runnable complete){complete.run();}
    public void setGoal(String goal){}
    public void setDiscovery(boolean value){}
    public void refreshExperience(){}
    public void cancelFire(){locked=false;}
    public void finish(){locked=false;}
    public void inspect(){}
    public void resume(){}
    public void pause(){}
    public void dispose(){}
}
