package com.emsi.baclegend.service;

import com.emsi.baclegend.dao.MotDAO;
import com.emsi.baclegend.model.Categorie;
import com.emsi.baclegend.model.Mot;

import java.net.HttpURLConnection;
import java.net.URL;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class ServiceValidation {

    private MotDAO motDao;
    private static final String AI_API_URL = "https://openrouter.ai/api/v1/chat/completions";
    private static final String AI_MODEL = "nvidia/nemotron-3-nano-30b-a3b:free";

    public ServiceValidation() {
        this.motDao = new MotDAO();
    }

    public boolean validerMot(String contenu, Categorie categorie) {
        if (contenu == null || contenu.trim().isEmpty())
            return false;
        String motNormalise = contenu.trim();
        com.emsi.baclegend.model.Mot motLocal = motDao.trouverParContenu(motNormalise, categorie.getId());
        if (motLocal != null)
            return motLocal.isEstValide();
        boolean valide = validerMotAvecAI(motNormalise, categorie);
        if (valide) {
            motDao.sauvegarder(new Mot(motNormalise, categorie.getId()));
            return true;
        }
        return false;
    }

    private String apiKey() {
        return "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";
    }

    private boolean validerMotAvecAI(String mot, Categorie categorie) {
        try {
            String key = apiKey();
            if (key == null || key.isBlank()) {
                System.err.println("OPENROUTER_API_KEY not set; AI validation disabled.");
                return false;
            }

            String system = "You are a validator for a word game (Scattergories). " +
                    "Return ONLY valid JSON: {\"valid\": boolean}. " +
                    "Check if the word belongs to the category. " +
                    "Ignore case/accents. If it is a real thing/name/concept in that category, return true. " +
                    "Example: Cat: 'Animal', Word: 'tigre' -> {\"valid\": true}.";
            String user = "Category: " + categorie.getNom() + ", Word: " + mot;

            JsonObject request = new JsonObject();
            request.addProperty("model", AI_MODEL);
            request.addProperty("temperature", 0.1); // Slight creativity for variants
            // request.addProperty("top_p", ...); // Removed default 0.0

            JsonArray messages = new JsonArray();
            JsonObject systemMsg = new JsonObject();
            systemMsg.addProperty("role", "system");
            systemMsg.addProperty("content", system);
            messages.add(systemMsg);
            JsonObject userMsg = new JsonObject();
            userMsg.addProperty("role", "user");
            userMsg.addProperty("content", user);
            messages.add(userMsg);
            request.add("messages", messages);

            String jsonRequest = request.toString();
            URL url = new URL(AI_API_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setRequestProperty("Authorization", "Bearer " + key);
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            conn.setRequestProperty("Accept", "application/json");
            conn.setRequestProperty("HTTP-Referer", "http://localhost");
            conn.setRequestProperty("X-Title", "BacLegend");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(10000);

            try (java.io.OutputStream os = conn.getOutputStream()) {
                byte[] input = jsonRequest.getBytes("utf-8");
                os.write(input, 0, input.length);
            }

            int code = conn.getResponseCode();
            if (code == 200) {
                String response = lireReponseAI(conn);
                return parseValidationFromAi(response);
            } else {
                return false;
            }
        } catch (Exception e) {
            return false;
        }
    }

    private String lireReponseAI(HttpURLConnection conn) {
        try (java.io.BufferedReader reader = new java.io.BufferedReader(
                new java.io.InputStreamReader(conn.getInputStream(), "utf-8"))) {
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null)
                response.append(line);
            return response.toString();
        } catch (Exception e) {
            return "";
        }
    }

    private boolean parseValidationFromAi(String responseJson) {
        try {
            JsonObject root = JsonParser.parseString(responseJson).getAsJsonObject();
            JsonArray choices = root.getAsJsonArray("choices");
            if (choices == null || choices.size() == 0)
                return false;
            JsonObject first = choices.get(0).getAsJsonObject();
            JsonObject message = first.getAsJsonObject("message");
            if (message == null)
                return false;
            String content = message.has("content") && !message.get("content").isJsonNull()
                    ? message.get("content").getAsString().trim()
                    : "";
            if (content.startsWith("```")) {
                int firstNl = content.indexOf('\n');
                int lastFence = content.lastIndexOf("```");
                if (firstNl >= 0 && lastFence > firstNl)
                    content = content.substring(firstNl + 1, lastFence).trim();
            }
            try {
                JsonObject inner = JsonParser.parseString(content).getAsJsonObject();
                if (inner.has("valid"))
                    return inner.get("valid").getAsBoolean();
            } catch (Exception ignored) {
            }
            String lower = content.toLowerCase();
            if (lower.contains("true") || lower.contains("oui") || lower.contains("yes"))
                return true;
            if (lower.contains("false") || lower.contains("non"))
                return false;
            return false;
        } catch (Exception e) {
            return false;
        }
    }
}
