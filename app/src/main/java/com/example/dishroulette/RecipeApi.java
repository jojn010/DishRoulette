package com.example.dishroulette;

import com.google.gson.annotations.SerializedName;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface RecipeApi {
    @POST("recipes/suggest") Call<RecipeSuggestion> suggest(@Body Request request);

    final class Request {
        @SerializedName("ingredients") final List<String> ingredients;
        @SerializedName("meal") final String meal;
        @SerializedName("avoidTitle") final String avoidTitle;
        @SerializedName("mode") final String mode;
        @SerializedName("goal") final String goal;
        @SerializedName("cuisine") final String cuisine;
        @SerializedName("allergies") final List<String> allergies;
        @SerializedName("customExclusions") final List<String> customExclusions;
        @SerializedName("likedDishes") final List<Favorite> likedDishes;
        Request(Set<String> ingredients, String meal, String avoidTitle, SpinProfile profile, List<Favorite> likedDishes) {
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
    static RecipeApi create() {
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(5, TimeUnit.SECONDS).readTimeout(60, TimeUnit.SECONDS)
                .callTimeout(65, TimeUnit.SECONDS).retryOnConnectionFailure(false).build();
        // The development helper keeps adb reverse active for both phones and emulators.
        return new Retrofit.Builder().baseUrl(BuildConfig.RECIPE_BASE_URL)
                .client(client).addConverterFactory(GsonConverterFactory.create())
                .build().create(RecipeApi.class);
    }
}
