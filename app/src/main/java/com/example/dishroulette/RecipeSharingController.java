package com.example.dishroulette;

import android.content.*;
import android.os.Bundle;
import android.text.InputFilter;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AlertDialog;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

/** Explicit clipboard/sharesheet export and review-before-save offline imports. */
final class RecipeSharingController {
    private final MainActivity activity;
    private AlertDialog importing,exporting;
    private TextInputEditText input;
    private String reviewCode;
    RecipeSharingController(MainActivity activity){this.activity=activity;}
    private int dp(int n){return Math.round(n*activity.getResources().getDisplayMetrics().density);}
    void share(RecipeCatalog.Recipe recipe){
        final String code;
        try{code=RecipeShareCodec.encode(recipe);}catch(RuntimeException e){message(R.string.share_failed);return;}
        AlertDialog dialog=new MaterialAlertDialogBuilder(activity).setTitle(R.string.share_recipe)
            .setMessage(activity.getString(R.string.share_recipe_help,recipe.title))
            .setPositiveButton(R.string.copy_recipe_code,(d,w)->{
                android.content.ClipboardManager clipboard=(android.content.ClipboardManager)activity.getSystemService(Context.CLIPBOARD_SERVICE);
                clipboard.setPrimaryClip(ClipData.newPlainText(activity.getString(R.string.recipe_code_label),code));message(R.string.recipe_code_copied);
            }).setNeutralButton(R.string.share_recipe,(d,w)->{
                Intent send=new Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_SUBJECT,recipe.title).putExtra(Intent.EXTRA_TEXT,code);
                activity.startActivity(Intent.createChooser(send,activity.getString(R.string.share_recipe)));
            }).setNegativeButton(R.string.cancel,null).create();
        exporting=dialog;dialog.setOnDismissListener(d->{if(exporting==dialog)exporting=null;});
        dialog.setOnShowListener(d->{dialog.getButton(AlertDialog.BUTTON_POSITIVE).setId(R.id.copyRecipeCode);UiTypography.apply(dialog.getWindow().getDecorView());});dialog.show();AdaptiveDialogs.fit(activity,dialog);
    }
    void importCode(){edit("");}
    private void edit(String draft){
        reviewCode=null;
        LinearLayout page=new LinearLayout(activity);page.setOrientation(LinearLayout.VERTICAL);page.setPadding(dp(24),dp(8),dp(24),0);
        TextView help=new TextView(activity);help.setText(R.string.import_recipe_help);help.setTextSize(15);help.setTextColor(UiColors.color(activity,R.color.muted));page.addView(help);
        TextInputLayout field=new TextInputLayout(activity);field.setHint(activity.getString(R.string.recipe_code_label));page.addView(field,new LinearLayout.LayoutParams(-1,-2));
        input=new TextInputEditText(activity);input.setId(R.id.recipeCodeInput);input.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE|android.text.InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);input.setTextDirection(View.TEXT_DIRECTION_LTR);input.setMinLines(3);input.setMaxLines(5);input.setFilters(new InputFilter[]{new InputFilter.LengthFilter(RecipeShareCodec.MAX_CODE+1)});field.addView(input,new LinearLayout.LayoutParams(-1,-2));input.setText(draft);if(AdaptiveDialogs.compact(activity)){input.setMinLines(1);input.setSingleLine(true);}
        ScrollView viewport=new ScrollView(activity);viewport.addView(page);
        AlertDialog editor=new MaterialAlertDialogBuilder(activity).setTitle(R.string.import_recipe).setView(viewport)
            .setPositiveButton(R.string.review_recipe,null).setNegativeButton(R.string.cancel,null).create();
        importing=editor;
        editor.setOnDismissListener(d->{if(importing==editor){importing=null;input=null;}});
        editor.setOnShowListener(d->{UiTypography.apply(editor.getWindow().getDecorView());editor.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{
            String code=input.getText()==null?"":input.getText().toString();
            try{RecipeCatalog.Recipe recipe=RecipeShareCodec.decode(code);editor.dismiss();review(recipe,code.trim());}
            catch(RuntimeException e){field.setError(activity.getString(R.string.invalid_recipe_code));input.requestFocus();}
        });});editor.show();AdaptiveDialogs.fit(activity,editor);
    }
    private void review(RecipeCatalog.Recipe recipe,String code){
        reviewCode=code;
        ScrollView scroll=new ScrollView(activity);TextView content=new TextView(activity);content.setPadding(dp(24),dp(8),dp(24),dp(16));content.setTextSize(16);content.setTextColor(UiColors.color(activity,R.color.ink));content.setTextIsSelectable(true);content.setTextDirection(View.TEXT_DIRECTION_FIRST_STRONG);
        content.setText(activity.getString(R.string.shared_recipe_review,activity.getString(R.string.shared_recipe_note),recipe.description,KitchenScreens.meta(activity,recipe),activity.getString(R.string.ingredients_title),recipe.ingredients,activity.getString(R.string.method_title),recipe.method));UiTypography.apply(content);scroll.addView(content);
        AlertDialog preview=new MaterialAlertDialogBuilder(activity).setTitle(recipe.title).setView(scroll)
            .setPositiveButton(R.string.import_save,null).setNegativeButton(R.string.cancel,null).create();
        importing=preview;
        preview.setOnDismissListener(d->{if(importing==preview){importing=null;reviewCode=null;}});
        preview.setOnShowListener(d->{UiTypography.apply(preview.getWindow().getDecorView());preview.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{
            if(activity.saveSharedRecipe(recipe))preview.dismiss();
        });});preview.show();AdaptiveDialogs.fit(activity,preview);
    }
    void save(Bundle state){
        if(importing==null||!importing.isShowing())return;
        if(reviewCode!=null)state.putString("shared_recipe_review",reviewCode);
        else if(input!=null)state.putString("shared_recipe_draft",input.getText()==null?"":input.getText().toString());
    }
    void restore(Bundle state){
        if(state==null)return;
        String code=state.getString("shared_recipe_review");
        if(code!=null){try{review(RecipeShareCodec.decode(code),code);}catch(RuntimeException e){edit(code);}}
        else if(state.containsKey("shared_recipe_draft"))edit(state.getString("shared_recipe_draft",""));
    }
    void dispose(){if(importing!=null)importing.dismiss();if(exporting!=null)exporting.dismiss();}
    private void message(int text){Toast.makeText(activity,text,Toast.LENGTH_SHORT).show();}
}
