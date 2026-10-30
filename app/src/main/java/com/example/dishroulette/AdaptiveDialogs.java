package com.example.dishroulette;

import android.view.View;
import android.view.ViewGroup;
import androidx.appcompat.app.AlertDialog;

/** Give compact windows enough horizontal room for dialog actions and scrolling content. */
final class AdaptiveDialogs {
    private AdaptiveDialogs(){}
    static boolean compact(MainActivity activity){
        View root=activity.findViewById(R.id.main);
        float density=activity.getResources().getDisplayMetrics().density;
        int height=root==null?0:root.getHeight()-root.getPaddingTop()-root.getPaddingBottom();
        float limit=480*Math.max(1f,activity.getResources().getConfiguration().fontScale);
        return height>0?height/density<limit:activity.getResources().getConfiguration().screenHeightDp<limit;
    }
    static void fit(MainActivity activity,AlertDialog dialog){
        if(compact(activity)&&dialog.getWindow()!=null)dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.MATCH_PARENT);
    }
}
