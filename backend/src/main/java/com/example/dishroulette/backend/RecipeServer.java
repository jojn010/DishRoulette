package com.example.dishroulette.backend;

import com.google.gson.*;
import com.sun.net.httpserver.*;
import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.Properties;
import java.util.concurrent.*;

/** Local development server. Deliberately binds loopback; public hosting requires auth and TLS. */
public final class RecipeServer implements AutoCloseable {
    private final HttpServer server;
    private final ExecutorService workers = new ThreadPoolExecutor(4, 4, 0, TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(16));
    private final Semaphore generationSlots = new Semaphore(2);
    private final GeminiRecipes gemini;
    private long windowStart = System.nanoTime();
    private int requestCount;

    RecipeServer(int port, GeminiRecipes gemini) throws IOException {
        this.gemini = gemini;
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", port), 16);
        server.setExecutor(workers);
        server.createContext("/", this::handle);
    }
    void start() { server.start(); }
    int port() { return server.getAddress().getPort(); }
    @Override public void close() { server.stop(0); workers.shutdownNow(); }

    private synchronized boolean allowRequest() {
        long now = System.nanoTime();
        if (now - windowStart > TimeUnit.MINUTES.toNanos(1)) { windowStart = now; requestCount = 0; }
        return ++requestCount <= 12;
    }

    private void handle(HttpExchange exchange) throws IOException {
        try (exchange) {
            String path = exchange.getRequestURI().getPath();
            if (path.equals("/health") && exchange.getRequestMethod().equals("GET")) {
                reply(exchange, 200, "{\"status\":\"running\"}"); return;
            }
            if (!path.equals("/recipes/suggest")) { error(exchange, 404, "NOT_FOUND"); return; }
            if (!exchange.getRequestMethod().equals("POST")) { error(exchange, 405, "METHOD_NOT_ALLOWED"); return; }
            // Android does not send Origin. Reject browser-origin requests to the local paid API proxy.
            if (exchange.getRequestHeaders().containsKey("Origin")) { error(exchange, 403, "ORIGIN_NOT_ALLOWED"); return; }
            String type = exchange.getRequestHeaders().getFirst("Content-Type");
            if (type == null || !type.toLowerCase(java.util.Locale.ROOT).startsWith("application/json")) {
                error(exchange, 415, "JSON_REQUIRED"); return;
            }
            byte[] bytes = exchange.getRequestBody().readNBytes(16385);
            if (bytes.length > 16384) { error(exchange, 413, "REQUEST_TOO_LARGE"); return; }
            JsonObject input;
            try { input = JsonParser.parseString(new String(bytes, StandardCharsets.UTF_8)).getAsJsonObject(); }
            catch (RuntimeException e) { error(exchange, 400, "INVALID_INGREDIENTS"); return; }
            if (!allowRequest() || !generationSlots.tryAcquire()) { error(exchange, 429, "RATE_LIMITED"); return; }
            try { reply(exchange, 200, gemini.suggest(input).toString()); }
            catch (GeminiRecipes.ApiFailure e) { error(exchange, e.status, e.code); }
            finally { generationSlots.release(); }
        }
    }
    private static void error(HttpExchange exchange, int status, String code) throws IOException {
        JsonObject body = new JsonObject(); body.addProperty("code", code); reply(exchange, status, body.toString());
    }
    private static void reply(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.getResponseHeaders().set("Cache-Control", "no-store");
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
    }
    public static void main(String[] args) throws IOException {
        Properties config = new Properties();
        Path file = Path.of("local.properties");
        if (Files.isRegularFile(file)) try (Reader reader = Files.newBufferedReader(file)) { config.load(reader); }
        String key = System.getenv("GEMINI_API_KEY");
        if (key == null || key.isBlank()) key = config.getProperty("GEMINI_API_KEY", "").trim();
        String model = System.getenv("GEMINI_MODEL");
        if (model == null || model.isBlank()) model = config.getProperty("GEMINI_MODEL", "gemini-3.5-flash-lite").trim();
        RecipeServer service = new RecipeServer(8080, new GeminiRecipes(key, model));
        Runtime.getRuntime().addShutdownHook(new Thread(service::close));
        service.start();
        System.out.println("DishRoulette backend listening at http://127.0.0.1:8080");
        System.out.println(key.isBlank() ? "Gemini key missing. Set backend/local.properties and restart." : "Gemini key configured.");
    }
}
