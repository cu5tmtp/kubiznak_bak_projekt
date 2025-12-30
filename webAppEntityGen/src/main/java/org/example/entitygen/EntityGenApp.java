package org.example.entitygen;

import com.google.genai.Client;
import com.google.genai.types.Content;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.Part;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.beans.factory.annotation.Value;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@SpringBootApplication
@RestController
public class EntityGenApp {

    @Value("${gemini.api.key:}")
    private String apiKey;

    public static void main(String[] args) {
        SpringApplication.run(EntityGenApp.class, args);
    }

    public static class GenerateRequest {
        public String prompt;
        public boolean generateAccessors;
        public boolean useAI;
    }

    @PostMapping("/api/generate")
    public ResponseEntity<String> generateCode(@RequestBody GenerateRequest request) {
        if (request.useAI) {
            return ResponseEntity.ok(generateWithGemini(request.prompt));
        } else {
            return ResponseEntity.ok(generateHardcoded(request));
        }
    }

    private String generateWithGemini(String userPrompt) {
        if (apiKey == null || apiKey.isEmpty()) {
            return "// CHYBA: Chybí API klíč! Nastavte gemini.api.key v application.properties.";
        }

        try {
            Client client = Client.builder()
                    .apiKey(apiKey)
                    .build();

            String instructionText = "Jsi expertní Java vývojář. Vygeneruj kód pro jednu JPA entitu. " +
                    "Použij Jakarta Persistence, Lombok @Data a české názvy polí dle zadání. " +
                    "Vrať pouze čistý kód bez vysvětlování a bez Markdown značek (```java).";

            Content systemInstruction = Content.builder()
                    .role("system")
                    .parts(Collections.singletonList(Part.builder().text(instructionText).build()))
                    .build();

            GenerateContentConfig config = GenerateContentConfig.builder()
                    .systemInstruction(systemInstruction)
                    .build();

            GenerateContentResponse response = client.models.generateContent(
                    "gemini-2.5-flash-lite",
                    userPrompt,
                    config
            );

            String result = response.text();

            return result.replace("```java", "").replace("```", "").trim();

        } catch (Exception e) {
            return "// Chyba při volání google-genai API: " + e.getMessage();
        }
    }

    private String generateHardcoded(GenerateRequest request) {
        String userPrompt = request.prompt;
        String className = extractClassName(userPrompt);
        List<EntityField> fields = analyzeFields(userPrompt);

        StringBuilder code = new StringBuilder();
        code.append("import jakarta.persistence.*;\n");
        if (!request.generateAccessors) {
            code.append("import lombok.Data;\n");
        }
        code.append("import java.time.LocalDate;\n\n");

        code.append("@Entity\n");
        if (!request.generateAccessors) {
            code.append("@Data\n");
        }
        code.append("public class ").append(className).append(" {\n\n");

        code.append("    @Id\n");
        code.append("    @GeneratedValue(strategy = GenerationType.IDENTITY)\n");
        code.append("    private Long id;\n\n");

        for (EntityField field : fields) {
            code.append("    private ").append(field.type).append(" ").append(field.name).append(";\n");
        }

        if (request.generateAccessors) {
            code.append("\n    // --- Gettery a Settery ---\n\n");
            appendAccessors(code, "Long", "id");
            for (EntityField field : fields) {
                appendAccessors(code, field.type, field.name);
            }
        }

        code.append("}\n");
        return code.toString();
    }

    private void appendAccessors(StringBuilder sb, String type, String name) {
        String capitalized = name.substring(0, 1).toUpperCase() + name.substring(1);
        sb.append("    public ").append(type).append(" get").append(capitalized).append("() {\n");
        sb.append("        return this.").append(name).append(";\n    }\n\n");
        sb.append("    public void set").append(capitalized).append("(").append(type).append(" ").append(name).append(") {\n");
        sb.append("        this.").append(name).append(" = ").append(name).append(";\n    }\n\n");
    }

    private String extractClassName(String text) {
        Pattern p = Pattern.compile("(?:třída|entita|pro)\\s+(\\w+)", Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(text);
        if (m.find()) {
            String name = m.group(1);
            return name.substring(0, 1).toUpperCase() + name.substring(1);
        }
        return "GeneratedEntity";
    }

    private List<EntityField> analyzeFields(String text) {
        List<EntityField> fields = new ArrayList<>();
        text = text.toLowerCase();
        if (text.contains("jméno") || text.contains("název")) fields.add(new EntityField("String", "nazev"));
        if (text.contains("příjmení")) fields.add(new EntityField("String", "prijmeni"));
        if (text.contains("email") || text.contains("mail")) fields.add(new EntityField("String", "email"));
        if (text.contains("věk") || text.contains("počet")) fields.add(new EntityField("Integer", "vek"));
        if (text.contains("cena") || text.contains("plat")) fields.add(new EntityField("java.math.BigDecimal", "cena"));
        if (text.contains("datum")) fields.add(new EntityField("LocalDate", "datum"));
        if (text.contains("aktivní")) fields.add(new EntityField("Boolean", "jeAktivni"));
        if (fields.isEmpty()) fields.add(new EntityField("String", "popis"));
        return fields;
    }

    private static class EntityField {
        String type; String name;
        EntityField(String type, String name) { this.type = type; this.name = name; }
    }
}