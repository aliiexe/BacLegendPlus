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
    private static volatile boolean aiValidationDisabled = false; // Disable AI if API key is invalid

    public ServiceValidation() {
        this.motDao = new MotDAO();
    }

    public boolean validerMot(String contenu, Categorie categorie) {
        return validerMot(contenu, categorie, com.emsi.baclegend.App.gameLanguage);
    }

    public boolean validerMot(String contenu, Categorie categorie, String language) {
        if (contenu == null || contenu.trim().isEmpty()) {
            System.out.println("VALIDATION: Empty word provided");
            return false;
        }
        String motNormalise = contenu.trim().toLowerCase();
        System.out.println("VALIDATION: Checking word='" + motNormalise + "', category='" + categorie.getNom() + "' (id=" + categorie.getId() + "), lang=" + language);
        
        // Check database first
        try {
            com.emsi.baclegend.model.Mot motLocal = motDao.trouverParContenu(motNormalise, categorie.getId());
            if (motLocal != null) {
                boolean result = motLocal.isEstValide();
                System.out.println("VALIDATION: Found in database, result=" + result);
                return result;
            }
        } catch (Exception e) {
            System.err.println("VALIDATION: Database lookup error: " + e.getMessage());
            e.printStackTrace();
        }
        
        // Not in database, try AI validation (if enabled)
        if (aiValidationDisabled) {
            // When AI is disabled, do basic validation: accept words with reasonable length
            // (First letter check is already done before calling this method)
            if (motNormalise.length() >= 2) {
                System.out.println("VALIDATION: AI validation is disabled. Accepting word with basic validation (length >= 2).");
                // Save to database as valid so it can be reused
                try {
                    Mot mot = new Mot(motNormalise, categorie.getId());
                    mot.setEstValide(true);
                    motDao.sauvegarder(mot);
                    System.out.println("VALIDATION: Word saved to database as valid");
                } catch (Exception e) {
                    System.err.println("VALIDATION: Error saving to database: " + e.getMessage());
                    e.printStackTrace();
                }
                return true;
            } else {
                System.out.println("VALIDATION: AI validation is disabled. Word too short (length < 2), rejecting.");
                return false;
            }
        }
        
        System.out.println("VALIDATION: Not in database, trying AI validation...");
        try {
            boolean valide = validerMotAvecAI(motNormalise, categorie, language);
            if (valide) {
                System.out.println("VALIDATION: AI validated, saving to database");
                try {
                    motDao.sauvegarder(new Mot(motNormalise, categorie.getId()));
                    System.out.println("VALIDATION: Word saved to database");
                } catch (Exception e) {
                    System.err.println("VALIDATION: Error saving to database: " + e.getMessage());
                    e.printStackTrace();
                }
                return true;
            } else {
                System.out.println("VALIDATION: AI validation returned false");
            }
        } catch (Exception e) {
            System.err.println("VALIDATION: AI validation error: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    private String apiKey() {
        // Try to get from environment variable first, then fall back to hardcoded key
        String envKey = System.getenv("OPENROUTER_API_KEY");
        if (envKey != null && !envKey.isBlank()) {
            return envKey;
        }
        // Fallback to hardcoded key (may be invalid/expired)
        return "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";
    }

    private boolean validerMotAvecAI(String mot, Categorie categorie) {
        return validerMotAvecAI(mot, categorie, com.emsi.baclegend.App.gameLanguage);
    }

    private boolean validerMotAvecAI(String mot, Categorie categorie, String language) {
        try {
            String key = apiKey();
            if (key == null || key.isBlank()) {
                System.err.println("OPENROUTER_API_KEY not set; AI validation disabled.");
                return false;
            }

            String langInstruction = language.equals("FR") 
                ? "The word must be a valid French word. Validate only if the word exists in French and belongs to the category."
                : "The word must be a valid English word. Validate only if the word exists in English and belongs to the category.";

            String system = "You are a validator for a word game (Scattergories). " +
                    "Return ONLY valid JSON: {\"valid\": boolean}. " +
                    "Check if the word belongs to the category. " +
                    "Ignore case/accents. If it is a real thing/name/concept in that category, return true. " +
                    langInstruction + " " +
                    "Example: Cat: 'Animal', Word: 'tigre' -> {\"valid\": true}.";
            String user = "Category: " + categorie.getNom() + ", Word: " + mot + ", Language: " + (language.equals("FR") ? "French" : "English");

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
                // AI validation is working - re-enable it if it was previously disabled
                if (aiValidationDisabled) {
                    aiValidationDisabled = false;
                    System.out.println("AI VALIDATION: Re-enabled (API key is now working)");
                }
                String response = lireReponseAI(conn);
                boolean result = parseValidationFromAi(response);
                System.out.println("AI VALIDATION: word='" + mot + "', category='" + categorie.getNom() + "', lang=" + language + ", result=" + result);
                return result;
            } else {
                // Read error response for debugging
                String errorResponse = lireReponseAI(conn);
                System.err.println("AI VALIDATION ERROR: HTTP " + code + " - " + errorResponse);
                
                // If we get a 401 (Unauthorized), the API key is invalid - disable AI validation for this session
                if (code == 401) {
                    aiValidationDisabled = true;
                    System.err.println("========================================");
                    System.err.println("AI VALIDATION DISABLED: Invalid API key (HTTP 401)");
                    System.err.println("The OpenRouter API key is invalid or expired.");
                    System.err.println("To fix this:");
                    System.err.println("  1. Get a new API key from https://openrouter.ai/keys");
                    System.err.println("  2. Set it as environment variable: OPENROUTER_API_KEY=your_key_here");
                    System.err.println("  3. Or update the apiKey() method in ServiceValidation.java");
                    System.err.println("For now, validation will use database only.");
                    System.err.println("Words not in the database will be rejected.");
                    System.err.println("========================================");
                }
                return false;
            }
        } catch (Exception e) {
            System.err.println("AI VALIDATION EXCEPTION: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    private String lireReponseAI(HttpURLConnection conn) {
        try {
            java.io.InputStream inputStream = null;
            try {
                // Try to get input stream (for success responses)
                inputStream = conn.getInputStream();
            } catch (java.io.IOException e) {
                // If input stream fails, try error stream (for error responses)
                inputStream = conn.getErrorStream();
            }
            
            if (inputStream == null) {
                System.err.println("AI VALIDATION: No response stream available (code: " + conn.getResponseCode() + ")");
                return "";
            }
            
            try (java.io.BufferedReader reader = new java.io.BufferedReader(
                    new java.io.InputStreamReader(inputStream, "utf-8"))) {
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null)
                    response.append(line);
                return response.toString();
            }
        } catch (Exception e) {
            System.err.println("AI VALIDATION: Error reading response: " + e.getMessage());
            e.printStackTrace();
            return "";
        }
    }

    private boolean parseValidationFromAi(String responseJson) {
        try {
            System.out.println("AI RESPONSE: " + responseJson);
            if (responseJson == null || responseJson.trim().isEmpty()) {
                System.err.println("AI VALIDATION: Empty response");
                return false;
            }
            
            JsonObject root = JsonParser.parseString(responseJson).getAsJsonObject();
            JsonArray choices = root.getAsJsonArray("choices");
            if (choices == null || choices.size() == 0) {
                System.err.println("AI VALIDATION: No choices in response");
                return false;
            }
            JsonObject first = choices.get(0).getAsJsonObject();
            JsonObject message = first.getAsJsonObject("message");
            if (message == null) {
                System.err.println("AI VALIDATION: No message in response");
                return false;
            }
            String content = message.has("content") && !message.get("content").isJsonNull()
                    ? message.get("content").getAsString().trim()
                    : "";
            
            System.out.println("AI CONTENT: " + content);
            
            if (content.startsWith("```")) {
                int firstNl = content.indexOf('\n');
                int lastFence = content.lastIndexOf("```");
                if (firstNl >= 0 && lastFence > firstNl)
                    content = content.substring(firstNl + 1, lastFence).trim();
            }
            
            // Try to parse as JSON first
            try {
                JsonObject inner = JsonParser.parseString(content).getAsJsonObject();
                if (inner.has("valid")) {
                    boolean result = inner.get("valid").getAsBoolean();
                    System.out.println("AI PARSED JSON: valid=" + result);
                    return result;
                }
            } catch (Exception e) {
                System.out.println("AI: Could not parse as JSON, trying text parsing: " + e.getMessage());
            }
            
            // Fallback: parse text response
            String lower = content.toLowerCase();
            if (lower.contains("\"valid\":true") || lower.contains("valid:true") || 
                lower.contains("true") || lower.contains("oui") || lower.contains("yes")) {
                System.out.println("AI PARSED TEXT: true");
                return true;
            }
            if (lower.contains("\"valid\":false") || lower.contains("valid:false") || 
                lower.contains("false") || lower.contains("non")) {
                System.out.println("AI PARSED TEXT: false");
                return false;
            }
            
            System.err.println("AI VALIDATION: Could not determine validity from response: " + content);
            return false;
        } catch (Exception e) {
            System.err.println("AI VALIDATION PARSE EXCEPTION: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
}
