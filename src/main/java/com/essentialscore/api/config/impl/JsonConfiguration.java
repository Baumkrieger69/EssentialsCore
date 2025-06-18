package com.essentialscore.api.config.impl;

import com.essentialscore.api.config.Configuration;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * JSON implementation of the Configuration interface.
 */
public class JsonConfiguration implements Configuration {
    private static final Logger logger = Logger.getLogger(JsonConfiguration.class.getName());
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    
    private final File file;
    private JsonObject root;
    private FileConfiguration yamlConfig;
    
    /**
     * Creates a new JSON configuration.
     *
     * @param file The configuration file
     */
    public JsonConfiguration(File file) {
        this.file = file;
        this.root = new JsonObject();
        this.yamlConfig = new YamlConfiguration();
    }
    
    @Override
    public boolean load() {
        try {
            if (!file.exists()) {
                file.getParentFile().mkdirs();
                file.createNewFile();
                save();
                return true;
            }
            
            JsonParser parser = new JsonParser();
            try (FileReader reader = new FileReader(file)) {
                JsonElement element = parser.parse(reader);
                if (element.isJsonObject()) {
                    root = element.getAsJsonObject();
                    
                    // Convert JSON to YAML for compatibility
                    yamlConfig = new YamlConfiguration();
                    convertJsonToYaml(root, yamlConfig);
                    
                    return true;
                } else {
                    logger.warning("JSON configuration is not an object: " + file.getPath());
                    return false;
                }
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to load JSON configuration: " + file.getPath(), e);
            return false;
        }
    }
    
    @Override
    public boolean save() {
        try {
            // Ensure parent directory exists
            if (!file.getParentFile().exists()) {
                file.getParentFile().mkdirs();
            }
            
            // Convert YAML to JSON for saving
            convertYamlToJson(yamlConfig, root);
            
            try (FileWriter writer = new FileWriter(file)) {
                GSON.toJson(root, writer);
                return true;
            }
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to save JSON configuration: " + file.getPath(), e);
            return false;
        }
    }
    
    @Override
    public boolean reload() {
        return load();
    }
    
    @Override
    public void set(String path, Object value) {
        yamlConfig.set(path, value);
    }
    
    @Override
    public Object get(String path) {
        return yamlConfig.get(path);
    }
    
    @Override
    public String getString(String path) {
        return yamlConfig.getString(path);
    }
    
    @Override
    public String getString(String path, String defaultValue) {
        return yamlConfig.getString(path, defaultValue);
    }
    
    @Override
    public int getInt(String path) {
        return yamlConfig.getInt(path);
    }
    
    @Override
    public int getInt(String path, int defaultValue) {
        return yamlConfig.getInt(path, defaultValue);
    }
    
    @Override
    public boolean getBoolean(String path) {
        return yamlConfig.getBoolean(path);
    }
    
    @Override
    public boolean getBoolean(String path, boolean defaultValue) {
        return yamlConfig.getBoolean(path, defaultValue);
    }
    
    @Override
    public List<String> getStringList(String path) {
        return yamlConfig.getStringList(path);
    }
    
    @Override
    public FileConfiguration getFileConfiguration() {
        return yamlConfig;
    }
    
    @Override
    public File getFile() {
        return file;
    }
    
    @Override
    public boolean contains(String path) {
        return yamlConfig.contains(path);
    }
    
    @Override
    public Set<String> getKeys(boolean deep) {
        return yamlConfig.getKeys(deep);
    }
    
    @Override
    public ConfigurationSection getSection(String path) {
        return yamlConfig.getConfigurationSection(path);
    }
    
    @Override
    public ConfigurationSection createSection(String path) {
        return yamlConfig.createSection(path);
    }
    
    @Override
    public Map<String, Object> getValues(boolean deep) {
        return yamlConfig.getValues(deep);
    }
    
    /**
     * Converts a JSON object to YAML configuration.
     *
     * @param json The JSON object
     * @param yaml The YAML configuration
     */
    private void convertJsonToYaml(JsonObject json, FileConfiguration yaml) {
        for (Map.Entry<String, JsonElement> entry : json.entrySet()) {
            String key = entry.getKey();
            JsonElement value = entry.getValue();
            
            if (value.isJsonObject()) {
                // Nested object
                ConfigurationSection section = yaml.createSection(key);
                convertJsonToYaml(value.getAsJsonObject(), section);
            } else if (value.isJsonArray()) {
                // Array
                List<Object> list = new ArrayList<>();
                value.getAsJsonArray().forEach(element -> {
                    if (element.isJsonPrimitive()) {
                        list.add(convertJsonPrimitiveToObject(element.getAsJsonPrimitive()));
                    } else if (element.isJsonObject()) {
                        Map<String, Object> map = new HashMap<>();
                        convertJsonToMap(element.getAsJsonObject(), map);
                        list.add(map);
                    }
                });
                yaml.set(key, list);
            } else if (value.isJsonPrimitive()) {
                // Primitive value
                yaml.set(key, convertJsonPrimitiveToObject(value.getAsJsonPrimitive()));
            } else if (value.isJsonNull()) {
                // Null value
                yaml.set(key, null);
            }
        }
    }
    
    /**
     * Converts a JSON object to YAML configuration section.
     *
     * @param json The JSON object
     * @param section The configuration section
     */
    private void convertJsonToYaml(JsonObject json, ConfigurationSection section) {
        for (Map.Entry<String, JsonElement> entry : json.entrySet()) {
            String key = entry.getKey();
            JsonElement value = entry.getValue();
            
            if (value.isJsonObject()) {
                // Nested object
                ConfigurationSection nestedSection = section.createSection(key);
                convertJsonToYaml(value.getAsJsonObject(), nestedSection);
            } else if (value.isJsonArray()) {
                // Array
                List<Object> list = new ArrayList<>();
                value.getAsJsonArray().forEach(element -> {
                    if (element.isJsonPrimitive()) {
                        list.add(convertJsonPrimitiveToObject(element.getAsJsonPrimitive()));
                    } else if (element.isJsonObject()) {
                        Map<String, Object> map = new HashMap<>();
                        convertJsonToMap(element.getAsJsonObject(), map);
                        list.add(map);
                    }
                });
                section.set(key, list);
            } else if (value.isJsonPrimitive()) {
                // Primitive value
                section.set(key, convertJsonPrimitiveToObject(value.getAsJsonPrimitive()));
            } else if (value.isJsonNull()) {
                // Null value
                section.set(key, null);
            }
        }
    }
    
    /**
     * Converts a JSON object to a map.
     *
     * @param json The JSON object
     * @param map The map to populate
     */
    private void convertJsonToMap(JsonObject json, Map<String, Object> map) {
        for (Map.Entry<String, JsonElement> entry : json.entrySet()) {
            String key = entry.getKey();
            JsonElement value = entry.getValue();
            
            if (value.isJsonObject()) {
                // Nested object
                Map<String, Object> nestedMap = new HashMap<>();
                convertJsonToMap(value.getAsJsonObject(), nestedMap);
                map.put(key, nestedMap);
            } else if (value.isJsonArray()) {
                // Array
                List<Object> list = new ArrayList<>();
                value.getAsJsonArray().forEach(element -> {
                    if (element.isJsonPrimitive()) {
                        list.add(convertJsonPrimitiveToObject(element.getAsJsonPrimitive()));
                    } else if (element.isJsonObject()) {
                        Map<String, Object> nestedMap = new HashMap<>();
                        convertJsonToMap(element.getAsJsonObject(), nestedMap);
                        list.add(nestedMap);
                    }
                });
                map.put(key, list);
            } else if (value.isJsonPrimitive()) {
                // Primitive value
                map.put(key, convertJsonPrimitiveToObject(value.getAsJsonPrimitive()));
            } else if (value.isJsonNull()) {
                // Null value
                map.put(key, null);
            }
        }
    }
    
    /**
     * Converts a JSON primitive to a Java object.
     *
     * @param primitive The JSON primitive
     * @return The Java object
     */
    private Object convertJsonPrimitiveToObject(JsonPrimitive primitive) {
        if (primitive.isBoolean()) {
            return primitive.getAsBoolean();
        } else if (primitive.isNumber()) {
            Number number = primitive.getAsNumber();
            
            // Check if it's an integer or a double
            double doubleValue = number.doubleValue();
            if (doubleValue == Math.floor(doubleValue)) {
                return number.intValue();
            } else {
                return doubleValue;
            }
        } else {
            return primitive.getAsString();
        }
    }
    
    /**
     * Converts YAML configuration to a JSON object.
     *
     * @param yaml The YAML configuration
     * @param json The JSON object to populate
     */
    private void convertYamlToJson(FileConfiguration yaml, JsonObject json) {
        for (String key : yaml.getKeys(false)) {
            Object value = yaml.get(key);
            json.add(key, convertObjectToJsonElement(value));
        }
    }
    
    /**
     * Converts a configuration section to a JSON object.
     *
     * @param section The configuration section
     * @param json The JSON object to populate
     */
    private void convertYamlToJson(ConfigurationSection section, JsonObject json) {
        for (String key : section.getKeys(false)) {
            Object value = section.get(key);
            json.add(key, convertObjectToJsonElement(value));
        }
    }
    
    /**
     * Converts a Java object to a JSON element.
     *
     * @param obj The Java object
     * @return The JSON element
     */
    private JsonElement convertObjectToJsonElement(Object obj) {
        if (obj == null) {
            return null;
        } else if (obj instanceof ConfigurationSection) {
            // Configuration section
            ConfigurationSection section = (ConfigurationSection) obj;
            JsonObject json = new JsonObject();
            convertYamlToJson(section, json);
            return json;
        } else if (obj instanceof Map) {
            // Map
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) obj;
            JsonObject json = new JsonObject();
            
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                json.add(entry.getKey(), convertObjectToJsonElement(entry.getValue()));
            }
            
            return json;
        } else if (obj instanceof List) {
            // List
            @SuppressWarnings("unchecked")
            List<Object> list = (List<Object>) obj;
            com.google.gson.JsonArray jsonArray = new com.google.gson.JsonArray();
            
            for (Object item : list) {
                jsonArray.add(convertObjectToJsonElement(item));
            }
            
            return jsonArray;
        } else if (obj instanceof String) {
            // String
            return new JsonPrimitive((String) obj);
        } else if (obj instanceof Number) {
            // Number
            return new JsonPrimitive((Number) obj);
        } else if (obj instanceof Boolean) {
            // Boolean
            return new JsonPrimitive((Boolean) obj);
        } else {
            // Other - convert to string
            return new JsonPrimitive(obj.toString());
        }
    }
} 
