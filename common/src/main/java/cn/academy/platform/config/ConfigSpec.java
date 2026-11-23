package cn.academy.platform.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/**
 * Configuration du mod, neutre de chargeur.
 *
 * <p>Forge fournit {@code ForgeConfigSpec} et Fabric n'a rien : cette classe garde ce que le mod
 * utilise reellement — des valeurs typees, des bornes, des listes, des commentaires — et ecrit un
 * fichier TOML simple, au meme format que celui de Forge ({@code [section]} puis
 * {@code cle = valeur}).
 *
 * <p>Les valeurs statiques du mod restent la reference pour le code de jeu ; {@link #load()} les
 * relit au demarrage, et {@link #save()} les reecrit quand un ecran les change.
 */
public final class ConfigSpec {

    /** Une valeur de configuration. */
    public abstract static class Value<T> {
        private final String path;
        private final T defaultValue;
        private final List<String> comments;
        private T value;

        Value(String path, T defaultValue, List<String> comments) {
            this.path = path;
            this.defaultValue = defaultValue;
            this.value = defaultValue;
            this.comments = List.copyOf(comments);
        }

        public String path() {
            return path;
        }

        public List<String> comments() {
            return comments;
        }

        public T get() {
            return value;
        }

        public T getDefault() {
            return defaultValue;
        }

        public void set(T newValue) {
            this.value = newValue;
        }

        /** Relit une chaine du fichier, ou garde le defaut si elle est illisible. */
        abstract void parse(String raw);

        /** Ecrit la valeur au format TOML. */
        abstract String write();

        @Override
        public String toString() {
            return path + " = " + write();
        }
    }

    public static final class BooleanValue extends Value<Boolean> {
        BooleanValue(String path, boolean defaultValue, List<String> comments) {
            super(path, defaultValue, comments);
        }

        @Override
        void parse(String raw) {
            String text = raw.trim();
            if (text.equalsIgnoreCase("true") || text.equalsIgnoreCase("false")) {
                set(Boolean.parseBoolean(text));
            }
        }

        @Override
        String write() {
            return Boolean.toString(get());
        }
    }

    public static final class IntValue extends Value<Integer> {
        private final int min;
        private final int max;

        IntValue(String path, int defaultValue, int min, int max, List<String> comments) {
            super(path, defaultValue, comments);
            this.min = min;
            this.max = max;
        }

        @Override
        void parse(String raw) {
            try {
                int parsed = Integer.parseInt(raw.trim());
                if (parsed >= min && parsed <= max) set(parsed);
            } catch (NumberFormatException ignored) {
                // Une valeur illisible laisse le defaut : c'est un reglage, pas un bug.
            }
        }

        @Override
        String write() {
            return Integer.toString(get());
        }
    }

    public static final class DoubleValue extends Value<Double> {
        private final double min;
        private final double max;

        DoubleValue(String path, double defaultValue, double min, double max, List<String> comments) {
            super(path, defaultValue, comments);
            this.min = min;
            this.max = max;
        }

        @Override
        void parse(String raw) {
            try {
                double parsed = Double.parseDouble(raw.trim());
                if (parsed >= min && parsed <= max) set(parsed);
            } catch (NumberFormatException ignored) {
                // Idem : on garde le defaut.
            }
        }

        @Override
        String write() {
            return Double.toString(get());
        }
    }

    public static final class StringValue extends Value<String> {
        StringValue(String path, String defaultValue, List<String> comments) {
            super(path, defaultValue, comments);
        }

        @Override
        void parse(String raw) {
            set(unquote(raw));
        }

        @Override
        String write() {
            return quote(get());
        }
    }

    public static final class ListValue extends Value<List<String>> {
        private final Predicate<String> validator;

        ListValue(String path, List<String> defaultValue, Predicate<String> validator,
                  List<String> comments) {
            super(path, List.copyOf(defaultValue), comments);
            this.validator = validator;
        }

        @Override
        void parse(String raw) {
            List<String> parsed = parseList(raw);
            if (parsed == null) return;
            for (String entry : parsed) {
                if (!validator.test(entry)) return;
            }
            set(List.copyOf(parsed));
        }

        @Override
        String write() {
            StringBuilder out = new StringBuilder("[");
            List<String> values = get();
            for (int i = 0; i < values.size(); i++) {
                if (i > 0) out.append(", ");
                out.append(quote(values.get(i)));
            }
            return out.append(']').toString();
        }
    }

    public static final class DoubleListValue extends Value<List<Double>> {
        DoubleListValue(String path, List<Double> defaultValue, List<String> comments) {
            super(path, List.copyOf(defaultValue), comments);
        }

        @Override
        void parse(String raw) {
            List<String> parts = parseList(raw);
            if (parts == null) return;
            List<Double> parsed = new ArrayList<>(parts.size());
            try {
                for (String part : parts) parsed.add(Double.parseDouble(part.trim()));
            } catch (NumberFormatException e) {
                return;
            }
            set(List.copyOf(parsed));
        }

        @Override
        String write() {
            StringBuilder out = new StringBuilder("[");
            List<Double> values = get();
            for (int i = 0; i < values.size(); i++) {
                if (i > 0) out.append(", ");
                out.append(values.get(i));
            }
            return out.append(']').toString();
        }
    }

    /** Constructeur de specification : meme forme que celui de Forge. */
    public static final class Builder {
        private final List<Value<?>> values = new ArrayList<>();
        private final List<String> comments = new ArrayList<>();
        private final List<String> path = new ArrayList<>();

        public Builder comment(String... lines) {
            comments.addAll(List.of(lines));
            return this;
        }

        public Builder push(String section) {
            path.add(section);
            return this;
        }

        public Builder pop() {
            if (!path.isEmpty()) path.remove(path.size() - 1);
            return this;
        }

        public BooleanValue define(String name, boolean defaultValue) {
            BooleanValue value = new BooleanValue(full(name), defaultValue, takeComments());
            values.add(value);
            return value;
        }

        public IntValue defineInRange(String name, int defaultValue, int min, int max) {
            IntValue value = new IntValue(full(name), defaultValue, min, max, takeComments());
            values.add(value);
            return value;
        }

        public DoubleValue defineInRange(String name, double defaultValue, double min, double max) {
            DoubleValue value = new DoubleValue(full(name), defaultValue, min, max, takeComments());
            values.add(value);
            return value;
        }

        public StringValue define(String name, String defaultValue) {
            StringValue value = new StringValue(full(name), defaultValue, takeComments());
            values.add(value);
            return value;
        }

        public ListValue defineList(String name, List<String> defaultValue, Predicate<String> validator) {
            ListValue value = new ListValue(full(name), defaultValue, validator, takeComments());
            values.add(value);
            return value;
        }

        public DoubleListValue defineList(String name, List<Double> defaultValue, double min, double max) {
            DoubleListValue value = new DoubleListValue(full(name), defaultValue, takeComments());
            values.add(value);
            return value;
        }

        public ConfigSpec build(Path file) {
            return new ConfigSpec(file, List.copyOf(values));
        }

        public ConfigSpec build() {
            return new ConfigSpec(null, List.copyOf(values));
        }

        private String full(String name) {
            return path.isEmpty() ? name : String.join(".", path) + "." + name;
        }

        private List<String> takeComments() {
            List<String> taken = List.copyOf(comments);
            comments.clear();
            return taken;
        }
    }

    private final Path file;
    private final List<Value<?>> values;
    private final Map<String, Value<?>> byPath = new LinkedHashMap<>();

    private ConfigSpec(Path file, List<Value<?>> values) {
        this.file = file;
        this.values = values;
        for (Value<?> value : values) byPath.put(value.path(), value);
    }

    public Path file() {
        return file;
    }

    /** Installe le fichier, quand l'emplacement depend du chargeur. */
    public void setFile(Path newFile) {
        this.filePath = newFile;
    }

    private Path filePath;

    private Path activeFile() {
        return filePath != null ? filePath : file;
    }

    /** Lit le fichier ; un fichier absent ou illisible laisse tous les defauts. */
    public void load() {
        Path target = activeFile();
        if (target == null || !Files.isRegularFile(target)) return;
        List<String> lines;
        try {
            lines = Files.readAllLines(target, StandardCharsets.UTF_8);
        } catch (IOException e) {
            return;
        }

        String section = "";
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#")) continue;
            if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
                section = trimmed.substring(1, trimmed.length() - 1).trim();
                continue;
            }
            int equals = trimmed.indexOf('=');
            if (equals <= 0) continue;
            String key = trimmed.substring(0, equals).trim();
            String raw = trimmed.substring(equals + 1).trim();
            Value<?> value = byPath.get(section.isEmpty() ? key : section + "." + key);
            if (value != null) value.parse(raw);
        }
    }

    /** Reecrit le fichier, sections et commentaires compris. Un echec ne fait pas tomber le jeu. */
    public void save() {
        Path target = activeFile();
        if (target == null) return;
        StringBuilder out = new StringBuilder();
        out.append("# AcademyCraft - configuration\n\n");

        String currentSection = null;
        for (Value<?> value : values) {
            String path = value.path();
            int dot = path.lastIndexOf('.');
            String section = dot < 0 ? "" : path.substring(0, dot);
            String key = dot < 0 ? path : path.substring(dot + 1);

            if (!section.equals(currentSection)) {
                currentSection = section;
                if (!section.isEmpty()) out.append('[').append(section).append("]\n");
            }
            for (String comment : value.comments()) out.append("# ").append(comment).append('\n');
            out.append(key).append(" = ").append(value.write()).append("\n\n");
        }

        try {
            if (target.getParent() != null) Files.createDirectories(target.getParent());
            Files.writeString(target, out.toString(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            // Disque plein, fichier verrouille : les valeurs restent en memoire.
        }
    }

    // ------------------------------------------------------------------
    // TOML minimal
    // ------------------------------------------------------------------

    private static String quote(String text) {
        return '"' + text.replace("\\", "\\\\").replace("\"", "\\\"") + '"';
    }

    private static String unquote(String raw) {
        String text = raw.trim();
        if (text.length() >= 2 && text.startsWith("\"") && text.endsWith("\"")) {
            text = text.substring(1, text.length() - 1);
            return text.replace("\\\"", "\"").replace("\\\\", "\\");
        }
        return text;
    }

    private static List<String> parseList(String raw) {
        String text = raw.trim();
        if (!text.startsWith("[") || !text.endsWith("]")) return null;
        text = text.substring(1, text.length() - 1).trim();
        if (text.isEmpty()) return List.of();

        List<String> out = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '"') {
                quoted = !quoted;
                current.append(c);
            } else if (c == ',' && !quoted) {
                out.add(unquote(current.toString()));
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        out.add(unquote(current.toString()));
        return out;
    }
}
