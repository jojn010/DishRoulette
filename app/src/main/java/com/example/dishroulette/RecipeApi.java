package com.example.dishroulette;

import com.google.gson.annotations.SerializedName;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import retrofit2.Call;

public interface RecipeApi {
    Call<RecipeSuggestion> suggest(Request request);

    final class Request {
        @SerializedName("bilingual") final boolean bilingual = true;
        @SerializedName("ingredients") final List<String> ingredients;
        @SerializedName("servings") final int servings;
        @SerializedName("meal") final String meal;
        @SerializedName("language") final String language;
        @SerializedName("avoidTitle") final String avoidTitle;
        @SerializedName("mode") final String mode;
        @SerializedName("goal") final String goal;
        @SerializedName("cuisine") final String cuisine;
        @SerializedName("allergies") final List<String> allergies;
        @SerializedName("customExclusions") final List<String> customExclusions;
        @SerializedName("likedDishes") final List<Favorite> likedDishes;
        Request(Set<String> ingredients, String meal, String avoidTitle, SpinProfile profile, List<Favorite> likedDishes) {
            this(ingredients,meal,avoidTitle,profile,likedDishes,"en");
        }
        Request(Set<String> ingredients,String meal,String avoidTitle,SpinProfile profile,List<Favorite> likedDishes,String language){
            this.language=language;
            this.servings=profile.servings;
            this.ingredients = new ArrayList<>(ingredients);
            this.meal = meal;
            this.avoidTitle = avoidTitle;
            this.mode = profile.mode;
            this.goal = profile.goal;
            this.cuisine = profile.cuisine;
            this.allergies = new ArrayList<>(profile.allergies);
            this.customExclusions = new ArrayList<>(profile.customExclusions);
            this.likedDishes = new ArrayList<>(likedDishes);
        }
    }
    final class Favorite {
        @SerializedName("title") final String title;
        @SerializedName("mainIngredients") final List<String> mainIngredients;
        Favorite(RecipeCatalog.Recipe recipe) {
            title = recipe.title;
            mainIngredients = new ArrayList<>();
            List<String> source = recipe.discovery && recipe.additionalIngredients != null ? recipe.additionalIngredients : recipe.keys;
            for (String ingredient : source) {
                if (ingredient != null && ingredient.length() <= 80) mainIngredients.add(ingredient);
                if (mainIngredients.size() == 10) break;
            }
        }
    }
    static RecipeApi create(android.content.Context context) { return new PersonalGeminiApi(context); }
}
