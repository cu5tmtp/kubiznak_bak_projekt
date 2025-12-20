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

    @PostMapping("/api/generate")
    public ResponseEntity<String> generateCode(@RequestBody String userPrompt) {

        String className = extractClassName(userPrompt);

        List<String> fields = analyzeFields(userPrompt);

        StringBuilder code = new StringBuilder();
        code.append("import jakarta.persistence.*;\n");
        code.append("import lombok.Data;\n");
        code.append("import java.time.LocalDate;\n\n");

        code.append("@Entity\n");
        code.append("@Data // Lombok anotace pro gettery a settery\n");
        code.append("public class ").append(className).append(" {\n\n");

        code.append("    @Id\n");
        code.append("    @GeneratedValue(strategy = GenerationType.IDENTITY)\n");
        code.append("    private Long id;\n\n");

        for (String field : fields) {
            code.append("    ").append(field).append("\n");
        }

        code.append("}\n");

        return ResponseEntity.ok(code.toString());
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

    private List<String> analyzeFields(String text) {
        List<String> fields = new ArrayList<>();
        text = text.toLowerCase();

        if (text.contains("jméno") || text.contains("název")) {
            fields.add("private String nazev;");
        }
        if (text.contains("příjmení")) {
            fields.add("private String prijmeni;");
        }
        if (text.contains("email") || text.contains("mail")) {
            fields.add("private String email;");
        }
        if (text.contains("věk") || text.contains("počet")) {
            fields.add("private Integer vek;");
        }
        if (text.contains("cena") || text.contains("plat")) {
            fields.add("private java.math.BigDecimal cena;");
        }
        if (text.contains("datum") || text.contains("narození")) {
            fields.add("private LocalDate datum;");
        }
        if (text.contains("aktivní") || text.contains("smazáno")) {
            fields.add("private Boolean jeAktivni;");
        }
        if (text.contains("popis") || text.contains("detail")) {
            fields.add("private String popis;");
        }

        if (fields.isEmpty()) {
            fields.add("// Nepodařilo se detekovat specifická pole z popisu.");
            fields.add("// Zkuste napsat např: 'Entita Uživatel s jménem, emailem a věkem'");
        }

        return fields;
    }
}