package edu.softwarelab.starter.contract;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Validates a live response against the shared OpenAPI document.
 *
 * <p>This understands the small slice of JSON Schema that openapi.yaml
 * actually uses: $ref, type, required, properties, enum and the numeric and
 * string bounds. It is deliberately dependency-free beyond Jackson, which
 * Spring already brings in. If you would rather use a real validator,
 * swagger-request-validator is the usual Java choice — swap it in and keep the
 * tests as they are.
 */
public final class OpenApiCheck {

    private final JsonNode root;

    private OpenApiCheck(JsonNode root) {
        this.root = root;
    }

    /** Reads an OpenAPI document. Pass the .json file: Jackson does not read YAML here. */
    public static OpenApiCheck load(Path path) throws IOException {
        return new OpenApiCheck(new ObjectMapper().readTree(Files.readString(path)));
    }

    private JsonNode schema(String name) {
        JsonNode schema = root.path("components").path("schemas").path(name);
        if (schema.isMissingNode()) {
            throw new IllegalArgumentException("schema " + name + " is not defined in the document");
        }
        return schema;
    }

    /** Returns every problem found, so one failure names all of them. */
    public List<String> validate(String schemaName, JsonNode value) {
        List<String> problems = new ArrayList<>();
        check(schema(schemaName), value, schemaName, problems);
        return problems;
    }

    private JsonNode resolve(JsonNode schema) {
        JsonNode ref = schema.get("$ref");
        if (ref == null) {
            return schema;
        }
        String text = ref.asText();
        // Only local refs of the form #/components/schemas/Name are used here.
        return schema(text.substring(text.lastIndexOf('/') + 1));
    }

    private void check(JsonNode schema, JsonNode value, String path, List<String> problems) {
        schema = resolve(schema);
        String expectedType = schema.path("type").asText("");

        if (!expectedType.isEmpty() && !matchesType(expectedType, value)) {
            problems.add(path + ": expected type " + expectedType + ", got " + describe(value));
            return;
        }

        JsonNode enumeration = schema.get("enum");
        if (enumeration != null && !contains(enumeration, value)) {
            problems.add(path + ": value " + value + " is not one of " + enumeration);
        }

        switch (expectedType) {
            case "object" -> {
                JsonNode required = schema.get("required");
                if (required != null) {
                    for (JsonNode name : required) {
                        if (!value.has(name.asText())) {
                            problems.add(path + ": required field \"" + name.asText() + "\" is missing");
                        }
                    }
                }
                JsonNode properties = schema.path("properties");
                Iterator<Map.Entry<String, JsonNode>> fields = properties.fields();
                while (fields.hasNext()) {
                    Map.Entry<String, JsonNode> property = fields.next();
                    if (value.has(property.getKey())) {
                        check(property.getValue(), value.get(property.getKey()),
                                path + "." + property.getKey(), problems);
                    }
                }
            }
            case "string" -> {
                int length = value.asText().codePointCount(0, value.asText().length());
                if (schema.has("minLength") && length < schema.get("minLength").asInt()) {
                    problems.add(path + ": shorter than minLength " + schema.get("minLength").asInt());
                }
                if (schema.has("maxLength") && length > schema.get("maxLength").asInt()) {
                    problems.add(path + ": longer than maxLength " + schema.get("maxLength").asInt());
                }
            }
            case "integer", "number" -> {
                double number = value.asDouble();
                if (schema.has("minimum") && number < schema.get("minimum").asDouble()) {
                    problems.add(path + ": " + number + " is below minimum " + schema.get("minimum"));
                }
                if (schema.has("maximum") && number > schema.get("maximum").asDouble()) {
                    problems.add(path + ": " + number + " is above maximum " + schema.get("maximum"));
                }
            }
            default -> {
                // Nothing further to check for this type.
            }
        }
    }

    private static boolean matchesType(String expected, JsonNode value) {
        return switch (expected) {
            case "object" -> value.isObject();
            case "array" -> value.isArray();
            case "string" -> value.isTextual();
            case "boolean" -> value.isBoolean();
            case "number" -> value.isNumber();
            case "integer" -> value.isIntegralNumber();
            default -> true;
        };
    }

    private static String describe(JsonNode value) {
        if (value == null || value.isNull()) {
            return "null";
        }
        if (value.isObject()) {
            return "object";
        }
        if (value.isArray()) {
            return "array";
        }
        if (value.isTextual()) {
            return "string";
        }
        if (value.isBoolean()) {
            return "boolean";
        }
        if (value.isIntegralNumber()) {
            return "integer";
        }
        if (value.isNumber()) {
            return "number";
        }
        return value.getNodeType().toString().toLowerCase();
    }

    private static boolean contains(JsonNode enumeration, JsonNode value) {
        for (JsonNode candidate : enumeration) {
            if (candidate.equals(value)) {
                return true;
            }
        }
        return false;
    }
}
