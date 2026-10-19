package com.example.dishroulette.core;

import java.text.Normalizer;
import java.util.*;
import java.util.regex.Pattern;

/** Conservative name screening, NOT certification of allergen absence or cross-contact safety. */
public final class IngredientExclusions {
    private IngredientExclusions() { }
    private static final Map<String, List<String>> ALIASES = new HashMap<>();
    static {
        add("Milk", "milk,dairy,butter,ghee,cheese,yogurt,yoghurt,cream,whey,casein,caseinate,kefir,paneer,labneh,halloumi,mozzarella,parmesan,feta");
        add("Eggs", "egg,eggs,albumen,albumin,mayonnaise,mayo,meringue,aioli");
        add("Peanuts", "peanut,peanuts,groundnut,groundnuts,arachis,satay");
        add("Tree nuts", "almond,almonds,walnut,walnuts,cashew,cashews,pistachio,pistachios,pecan,pecans,hazelnut,hazelnuts,macadamia,macadamias,brazil nut,brazil nuts,pine nut,pine nuts,nut,nuts,marzipan,praline");
        add("Soy", "soy,soya,soybean,soybeans,tofu,tempeh,edamame,miso,tamari,shoyu");
        add("Wheat", "wheat,semolina,durum,spelt,bulgur,couscous,farro,seitan,flour,bread,pasta,noodles,tortilla,tortillas,pita,freekeh");
        add("Gluten", "gluten,wheat,barley,rye,malt,semolina,durum,spelt,bulgur,couscous,farro,seitan,flour,bread,pasta,noodles,tortilla,tortillas,pita,freekeh,oats");
        add("Sesame", "sesame,tahini,halva,halvah,benne,gingelly");
        add("Fish", "fish,salmon,tuna,cod,anchovy,anchovies,sardine,sardines,trout,haddock,tilapia,mackerel,bonito,dashi,fish sauce,worcestershire");
        add("Shellfish", "shellfish,shrimp,shrimps,prawn,prawns,crab,lobster,crayfish,crawfish,krill,scampi");
        add("Molluscs", "mollusc,molluscs,mollusk,mollusks,mussel,mussels,clam,clams,oyster,oysters,scallop,scallops,squid,octopus,calamari,snail,snails");
        add("Mustard", "mustard,dijon"); add("Celery", "celery,celeriac"); add("Lupin", "lupin,lupine");
    }
    private static final List<String> UNCERTAIN = Arrays.asList("sauce", "dressing", "seasoning mix",
            "stock", "broth", "pesto", "bread", "tortilla", "tortillas", "pasta", "noodles",
            "mayonnaise", "chocolate", "protein powder", "curry paste", "bouillon");
    private static void add(String group, String terms) { ALIASES.put(group, Arrays.asList(terms.split(","))); }
    private static String normalize(String value) {
        return Normalizer.normalize(value == null ? "" : value, Normalizer.Form.NFKD)
                .replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT).replaceAll("[^\\p{L}\\p{N}]+", " ").trim();
    }
    private static boolean contains(String text, String term) {
        String normalized = normalize(term);
        return !normalized.isEmpty() && Pattern.compile("(?<![\\p{L}\\p{N}])" + Pattern.quote(normalized)
                + "(?:s|es)?(?![\\p{L}\\p{N}])").matcher(normalize(text)).find();
    }
    public static Set<String> conflicts(Collection<String> foods, Collection<String> allergies, Collection<String> custom) {
        Set<String> found = new LinkedHashSet<>();
        for (String food : foods) {
            for (String allergy : allergies) {
                List<String> terms = ALIASES.get(allergy);
                if (terms == null) { found.add(allergy); continue; }
                for (String term : terms) if (contains(food, term)) { found.add(allergy); break; }
            }
            for (String term : custom) if (contains(food, term)) found.add(term);
        }
        return found;
    }
    public static boolean hasUncertainIngredients(Collection<String> ingredients) {
        for (String ingredient : ingredients) for (String term : UNCERTAIN) if (contains(ingredient, term)) return true;
        return false;
    }
    public static List<String> parseCustom(String input) {
        LinkedHashSet<String> values = new LinkedHashSet<>();
        if (input == null) return new ArrayList<>();
        for (String term : input.split("[,;\\n]+")) {
            term = term.trim();
            if (term.isEmpty()) continue;
            if (term.length() > 40 || !term.matches("[\\p{L}][\\p{L}\\p{M} '\\-]*")) throw new IllegalArgumentException("Invalid exclusion");
            values.add(term.toLowerCase(Locale.ROOT));
        }
        if (values.size() > 10) throw new IllegalArgumentException("Too many exclusions");
        return new ArrayList<>(values);
    }
}
