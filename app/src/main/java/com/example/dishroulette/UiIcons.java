package com.example.dishroulette;

import android.content.res.ColorStateList;
import android.widget.TextView;
import com.google.android.material.button.MaterialButton;

final class UiIcons {
    private UiIcons(){}
    static void button(MaterialButton b,int drawable){
        b.setIconResource(drawable);b.setIconSize(Math.round(19*b.getResources().getDisplayMetrics().density));
        b.setIconPadding(Math.round(7*b.getResources().getDisplayMetrics().density));b.setIconTint(b.getTextColors());b.setIconGravity(MaterialButton.ICON_GRAVITY_TEXT_START);
    }
    static void text(TextView t,int drawable){
        android.graphics.drawable.Drawable icon=androidx.appcompat.content.res.AppCompatResources.getDrawable(t.getContext(),drawable);
        if(icon==null)return;icon=icon.mutate();androidx.core.graphics.drawable.DrawableCompat.setTint(icon,t.getCurrentTextColor());int size=Math.round(20*t.getResources().getDisplayMetrics().density);icon.setBounds(0,0,size,size);t.setCompoundDrawablePadding(Math.round(8*t.getResources().getDisplayMetrics().density));t.setCompoundDrawablesRelative(icon,null,null,null);
    }
}
