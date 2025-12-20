package org.example.entitygen;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@SpringBootApplication
@RestController
public class EntityGenApp {

    public static void main(String[] args) {
        SpringApplication.run(EntityGenApp.class, args);
    }

    public static class GenerateRequest {
        public String prompt;
        public boolean generateAccessors;
    }

    @PostMapping("/api/generate")
    public ResponseEntity<String> generateCode(@RequestBody GenerateRequest request) {
        String userPrompt = request.prompt;

        //Získání názvu třídy
        String className = extractClassName(userPrompt);

        //Analýza polí
        List<EntityField> fields = analyzeFields(userPrompt);

        //Sestavení Java kódu
        StringBuilder code = new StringBuilder();
        code.append("import jakarta.persistence.*;\n");
        if (!request.generateAccessors) {
            code.append("import lombok.Data;\n");
        }
        code.append("import java.time.LocalDate;\n\n");

        code.append("@Entity\n");
        if (!request.generateAccessors) {
            code.append("@Data // Lombok generuje gettery/settery automaticky\n");
        }
        code.append("public class ").append(className).append(" {\n\n");

        // ID pole
        code.append("    @Id\n");
        code.append("    @GeneratedValue(strategy = GenerationType.IDENTITY)\n");
        code.append("    private Long id;\n\n");

        // Definice polí
        for (EntityField field : fields) {
            code.append("    private ").append(field.type).append(" ").append(field.name).append(";\n");
        }

        // Explicitní Gettery a Settery
        if (request.generateAccessors) {
            code.append("\n    // --- Gettery a Settery ---\n\n");

            // Get/Set pro ID
            appendAccessors(code, "Long", "id");

            // Get/Set pro ostatní
            for (EntityField field : fields) {
                appendAccessors(code, field.type, field.name);
            }
        }

        code.append("}\n");

        return ResponseEntity.ok(code.toString());
    }

    private void appendAccessors(StringBuilder sb, String type, String name) {
        String capitalized = name.substring(0, 1).toUpperCase() + name.substring(1);

        // Getter
        sb.append("    public ").append(type).append(" get").append(capitalized).append("() {\n");
        sb.append("        return this.").append(name).append(";\n");
        sb.append("    }\n\n");

        // Setter
        sb.append("    public void set").append(capitalized).append("(").append(type).append(" ").append(name).append(") {\n");
        sb.append("        this.").append(name).append(" = ").append(name).append(";\n");
        sb.append("    }\n\n");
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

    private static class EntityField {
        String type;
        String name;
        EntityField(String type, String name) { this.type = type; this.name = name; }
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
}