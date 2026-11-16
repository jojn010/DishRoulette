package com.example.dishroulette.core;

import java.text.Normalizer;
import java.util.*;

/**
 * Bounded food-name equivalences used identically by Android and the service; not a translator.
 */
public final class FoodNames {
    private FoodNames() {
    }

    private static final Map<String, List<String>> GROUPS = new HashMap<>();
    private static final Map<String, String> CANONICAL = new HashMap<>();

    static {
        add("rice", "rice,cooked rice,أرز,ارز,رز");
        add("tomato", "tomato,tomatoes,tomatos,بندورة,طماطم");
        add("eggs", "egg,eggs,بيض,بيضة");
        add("chicken", "chicken,cooked chicken,دجاج,فراخ");
        add("potato", "potato,potatoes,بطاطا,بطاطس");
        add("sweet potato", "sweet potato,sweet potatoes,بطاطا حلوة");
        add("onion", "onion,onions,بصل,بصلة");
        add("garlic", "garlic,ثوم");
        add("lemon", "lemon,lemons,ليمون");
        add("banana", "banana,bananas,موز,موزة");
        add("kiwi", "kiwi,kiwis,كيوي");
        add("apple", "apple,apples,تفاح,تفاحة");
        add("carrot", "carrot,carrots,جزر,جزرة");
        add("cucumber", "cucumber,cucumbers,خيار");
        add("spinach", "spinach,سبانخ");
        add("chickpeas", "chickpea,chickpeas,garbanzo beans,حمص");
        add("lentils", "lentil,lentils,عدس");
        add("beans", "bean,beans,فاصوليا");
        add("beef", "beef,لحم بقر,لحم بقري");
        add("lamb", "lamb,لحم غنم,لحم ضأن");
        add("fish", "fish,سمك,أسماك");
        add("milk", "milk,حليب,لبن");
        add("yogurt", "yogurt,yoghurt,لبن زبادي,زبادي");
        add("cheese", "cheese,جبن,جبنة");
        add("sesame", "sesame,سمسم");
        add("tahini", "tahini,طحينة,طحينية");
        add("peanuts", "peanut,peanuts,فول سوداني");
        add("soy", "soy,soya,صويا,فول الصويا");
        add("wheat", "wheat,قمح");
        add("gluten", "gluten,غلوتين,جلوتين");
        add("water", "water,ماء,مياه");
        add("salt", "salt,ملح");
        add("pepper", "pepper,black pepper,فلفل,فلفل أسود");
        add("cooking oil", "cooking oil,زيت طبخ,زيت طهي");
        add("olive oil", "olive oil,زيت زيتون");
        add("oats", "oats,oatmeal,شوفان");
        add("bread", "bread,خبز");
        add("pasta", "pasta,معكرونة,مكرونة");
        add("mushroom", "mushroom,mushrooms,فطر,مشروم");
        add("eggplant", "eggplant,aubergine,باذنجان");
        add("zucchini", "zucchini,courgette,كوسا");
        add("parsley", "parsley,بقدونس");
        add("coriander", "coriander,cilantro,كزبرة");
        add("cumin", "cumin,كمون");
        add("cinnamon", "cinnamon,قرفة");
        add("honey", "honey,عسل");
    }

    private static void add(String canonical, String values) {
        List<String> names = Arrays.asList(values.split(","));
        GROUPS.put(canonical, names);
        for (String name : names)
            CANONICAL.put(normalize(name), canonical);
    }

    public static String normalize(String s) {
        return Normalizer.normalize(s == null ? "" : s, Normalizer.Form.NFKD).replaceAll("\\p{M}", "").replace("ـ", "")
            .replace('ى', 'ي').replace('ة', 'ه').toLowerCase(Locale.ROOT).trim().replaceAll("\\s+", " ")
            .replaceAll("(?<![\\p{L}\\p{N}])ال(?=[\u0621-\u064A])", "");
    }

    public static String canonical(String name) {
        String normalized = normalize(name);
        String canonical = CANONICAL.get(normalized);
        if (canonical == null && normalized.startsWith("ال"))
            canonical = CANONICAL.get(normalized.substring(2));
        return canonical == null ? name : canonical;
    }

    public static List<String> equivalents(String name) {
        List<String> group = GROUPS.get(canonical(name));
        return group == null ? Collections.singletonList(name) : group;
    }
}
