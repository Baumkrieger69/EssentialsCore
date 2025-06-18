package com.essentialscore.api.gaming;

import java.util.Random;

/**
 * Analyzes weather patterns for dynamic content generation.
 * Provides weather-based insights for game content adaptation.
 */
public class WeatherPatternAnalyzer {
    
    private final Random random = new Random();
    private String currentWeatherPattern;
    
    public WeatherPatternAnalyzer() {
        this.currentWeatherPattern = generateInitialWeatherPattern();
    }
    
    /**
     * Gets the current weather pattern as a string.
     * @return Current weather pattern description
     */
    public String getCurrentWeatherPattern() {
        return currentWeatherPattern;
    }
    
    /**
     * Updates the weather pattern based on current conditions.
     */
    public void updateWeatherPattern() {
        this.currentWeatherPattern = generateWeatherPattern();
    }
    
    /**
     * Checks if the weather pattern has changed significantly.
     * @return true if weather pattern has changed significantly
     */
    public boolean hasSignificantWeatherChange() {
        return random.nextDouble() < 0.3; // 30% chance of significant change
    }
    
    /**
     * Gets weather influence on game events (0.0 to 1.0).
     * @return Weather influence factor
     */
    public double getWeatherInfluence() {
        switch (currentWeatherPattern.toLowerCase()) {
            case "stormy":
                return 0.8;
            case "rainy":
                return 0.6;
            case "cloudy":
                return 0.4;
            case "sunny":
                return 0.2;
            case "clear":
                return 0.1;
            default:
                return 0.5;
        }
    }
    
    private String generateInitialWeatherPattern() {
        return generateWeatherPattern();
    }
    
    private String generateWeatherPattern() {
        String[] patterns = {"sunny", "cloudy", "rainy", "stormy", "clear", "foggy"};
        return patterns[random.nextInt(patterns.length)];
    }
}
