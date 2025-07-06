import java.io.*;
import java.util.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.Image;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.imageio.ImageIO;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

// Deep learning model imports
import org.deeplearning4j.nn.graph.ComputationGraph;
import org.deeplearning4j.nn.modelimport.keras.KerasModelImport;
import org.deeplearning4j.zoo.PretrainedType;
import org.deeplearning4j.zoo.ZooModel;
import org.deeplearning4j.zoo.model.VGG16;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.api.preprocessor.DataNormalization;
import org.nd4j.linalg.dataset.api.preprocessor.VGG16ImagePreProcessor;
import org.nd4j.linalg.factory.Nd4j;
import org.deeplearning4j.nn.api.Layer;

public class DietTracker {
    private static final String CSV_FILE = "food_log.csv";
    private static final String TRAINING_DATA_FILE = "food_training_data.csv";
    private static final Map<String, Double> WORD_FREQUENCIES = new HashMap<>();
    private static final Map<String, Double> CLASS_PROBABILITIES = new HashMap<>();
    private static final Map<String, Boolean> FOOD_TYPES = Map.of(
        "vegetables", true,
        "fruits", true,
        "nuts", true,
        "fish", true,
        "whole grains", true,
        "sweets", false,
        "fast food", false,
        "processed food", false,
        "soda", false
    );
    private static final String USER_PREFERENCES_FILE = "user_preferences.csv";
    private static final Map<String, Double> userPreferences = new HashMap<>();
    private static final Map<String, List<String>> mealPatterns = new HashMap<>();
    private static final String NUTRITIONAL_DATA_FILE = "nutritional_data.csv";
    private static final String USER_GOALS_FILE = "user_goals.csv";
    private static final Map<String, Map<String, Double>> nutritionalData = new HashMap<>();
    private static final Map<String, String> userGoals = new HashMap<>();
    private static final Map<String, List<String>> mealTemplates = new HashMap<>();
    private static final Map<String, List<String>> AI_RESPONSES = new HashMap<>();
    private static final Map<String, Double> USER_QUESTIONS = new HashMap<>();
    private static final String IMAGE_DIRECTORY = "food_images/";
    private static final String MODEL_DIRECTORY = "models/";
    private static final Map<String, List<String>> FOOD_IMAGE_DATABASE = new HashMap<>();
    private static final Map<String, String> IMAGE_TO_FOOD_MAPPING = new HashMap<>();
    private static final String[] FOOD_CLASSES = {
        "apple", "banana", "broccoli", "burger", "carrot", "donut", 
        "egg", "grapes", "pasta", "pizza", "salad", "steak", 
        "rice", "sandwich", "soup", "sushi", "tomato", "yogurt"
    };
    private static ComputationGraph foodRecognitionModel = null;

    private static void initializeClassifier() {
        try (Scanner fileScanner = new Scanner(new File(TRAINING_DATA_FILE))) {
            Map<String, Integer> classCounts = new HashMap<>();
            Map<String, Map<String, Integer>> wordCounts = new HashMap<>();
            
            while (fileScanner.hasNextLine()) {
                String[] parts = fileScanner.nextLine().split(",");
                if (parts.length >= 2) {
                    String food = parts[0].toLowerCase();
                    boolean isHealthy = Boolean.parseBoolean(parts[1]);
                    String className = isHealthy ? "healthy" : "unhealthy";
                    
                    // Update class counts
                    classCounts.merge(className, 1, Integer::sum);
                    
                    // Update word counts
                    String[] words = food.split("\\s+");
                    for (String word : words) {
                        wordCounts.computeIfAbsent(className, k -> new HashMap<>())
                                .merge(word, 1, Integer::sum);
                    }
                }
            }
            
            // Calculate class probabilities
            int totalSamples = classCounts.values().stream().mapToInt(Integer::intValue).sum();
            for (Map.Entry<String, Integer> entry : classCounts.entrySet()) {
                CLASS_PROBABILITIES.put(entry.getKey(), 
                    Math.log((double) entry.getValue() / totalSamples));
            }
            
            // Calculate word frequencies
            for (Map.Entry<String, Map<String, Integer>> classEntry : wordCounts.entrySet()) {
                String className = classEntry.getKey();
                int totalWords = classEntry.getValue().values().stream().mapToInt(Integer::intValue).sum();
                
                for (Map.Entry<String, Integer> wordEntry : classEntry.getValue().entrySet()) {
                    String word = wordEntry.getKey();
                    int count = wordEntry.getValue();
                    WORD_FREQUENCIES.put(className + "_" + word, 
                        Math.log((double) count / totalWords));
                }
            }
            
        } catch (FileNotFoundException e) {
            System.out.println("Training data not found. Using default classification.");
        }
    }

    private static boolean classifyFood(String foodName) {
        if (WORD_FREQUENCIES.isEmpty()) {
            // Fallback to default classification if no training data
            return FOOD_TYPES.getOrDefault(foodName.toLowerCase(), false);
        }
        
        String[] words = foodName.toLowerCase().split("\\s+");
        double healthyScore = CLASS_PROBABILITIES.getOrDefault("healthy", 0.0);
        double unhealthyScore = CLASS_PROBABILITIES.getOrDefault("unhealthy", 0.0);
        
        for (String word : words) {
            healthyScore += WORD_FREQUENCIES.getOrDefault("healthy_" + word, 0.0);
            unhealthyScore += WORD_FREQUENCIES.getOrDefault("unhealthy_" + word, 0.0);
        }
        
        return healthyScore > unhealthyScore;
    }

    private static void loadUserPreferences() {
        try (Scanner fileScanner = new Scanner(new File(USER_PREFERENCES_FILE))) {
            while (fileScanner.hasNextLine()) {
                String[] parts = fileScanner.nextLine().split(",");
                if (parts.length >= 2) {
                    userPreferences.put(parts[0], Double.parseDouble(parts[1]));
                }
            }
        } catch (FileNotFoundException e) {
            System.out.println("No user preferences found. Starting fresh!");
        }
    }

    private static void saveUserPreferences() {
        try (PrintWriter writer = new PrintWriter(new FileWriter(USER_PREFERENCES_FILE))) {
            for (Map.Entry<String, Double> entry : userPreferences.entrySet()) {
                writer.println(entry.getKey() + "," + entry.getValue());
            }
        } catch (IOException e) {
            System.out.println("Error saving user preferences: " + e.getMessage());
        }
    }

    private static void updateUserPreferences(String foodName, boolean liked) {
        double currentScore = userPreferences.getOrDefault(foodName, 0.5);
        double newScore = liked ? 
            Math.min(1.0, currentScore + 0.1) : 
            Math.max(0.0, currentScore - 0.1);
        userPreferences.put(foodName, newScore);
        saveUserPreferences();
    }

    private static void analyzeMealPatterns() {
        try (Scanner fileScanner = new Scanner(new File(CSV_FILE))) {
            Map<String, List<String>> dailyMeals = new HashMap<>();
            
            while (fileScanner.hasNextLine()) {
                String[] parts = fileScanner.nextLine().split(",");
                if (parts.length >= 4) {
                    String date = parts[0];
                    String food = parts[1];
                    dailyMeals.computeIfAbsent(date, k -> new ArrayList<>()).add(food);
                }
            }
            
            // Analyze patterns in daily meals
            for (List<String> meals : dailyMeals.values()) {
                for (int i = 0; i < meals.size(); i++) {
                    for (int j = i + 1; j < meals.size(); j++) {
                        String food1 = meals.get(i);
                        String food2 = meals.get(j);
                        mealPatterns.computeIfAbsent(food1, k -> new ArrayList<>()).add(food2);
                        mealPatterns.computeIfAbsent(food2, k -> new ArrayList<>()).add(food1);
                    }
                }
            }
        } catch (FileNotFoundException e) {
            System.out.println("No meal patterns found.");
        }
    }

    private static List<String> getMealRecommendations() {
        List<String> recommendations = new ArrayList<>();
        if (userPreferences.isEmpty()) {
            return recommendations;
        }

        // Get top 5 preferred foods
        List<String> topFoods = userPreferences.entrySet().stream()
            .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
            .limit(5)
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());

        // For each top food, find complementary foods
        for (String food : topFoods) {
            if (mealPatterns.containsKey(food)) {
                List<String> complementaryFoods = mealPatterns.get(food).stream()
                    .filter(f -> userPreferences.getOrDefault(f, 0.5) > 0.3)
                    .distinct()
                    .limit(2)
                    .collect(Collectors.toList());
                
                recommendations.add(food + " with " + String.join(" and ", complementaryFoods));
            }
        }

        return recommendations;
    }

    private static void showMealRecommendations() {
        analyzeMealPatterns();
        List<String> recommendations = getMealRecommendations();
        
        System.out.println("\nAI-Powered Meal Recommendations");
        System.out.println("Based on your preferences and eating patterns:");
        
        if (recommendations.isEmpty()) {
            System.out.println("Not enough data to make recommendations yet.");
            System.out.println("Keep logging your meals to get personalized suggestions!");
            return;
        }

        for (int i = 0; i < recommendations.size(); i++) {
            System.out.printf("%d. %s\n", i + 1, recommendations.get(i));
        }

        System.out.println("\nWould you like to rate any of these recommendations? (y/n)");
        Scanner scanner = new Scanner(System.in);
        if (scanner.nextLine().toLowerCase().startsWith("y")) {
            System.out.print("Enter the number of the recommendation to rate (1-" + recommendations.size() + "): ");
            try {
                int choice = Integer.parseInt(scanner.nextLine());
                if (choice > 0 && choice <= recommendations.size()) {
                    String meal = recommendations.get(choice - 1);
                    System.out.print("Did you like this recommendation? (y/n): ");
                    boolean liked = scanner.nextLine().toLowerCase().startsWith("y");
                    
                    // Update preferences for each food in the meal
                    String[] foods = meal.split(" with | and ");
                    for (String food : foods) {
                        updateUserPreferences(food.trim(), liked);
                    }
                    System.out.println("Thank you for your feedback! The AI will learn from your preferences.");
                }
            } catch (NumberFormatException e) {
                System.out.println("Invalid input.");
            }
        }
    }

    private static void loadNutritionalData() {
        try (Scanner fileScanner = new Scanner(new File(NUTRITIONAL_DATA_FILE))) {
            if (fileScanner.hasNextLine()) {
                // Skip header
                fileScanner.nextLine();
            }
            
            while (fileScanner.hasNextLine()) {
                String[] parts = fileScanner.nextLine().split(",");
                if (parts.length >= 8) {
                    Map<String, Double> nutrition = new HashMap<>();
                    nutrition.put("calories", Double.parseDouble(parts[1]));
                    nutrition.put("protein", Double.parseDouble(parts[2]));
                    nutrition.put("fat", Double.parseDouble(parts[3]));
                    nutrition.put("carbs", Double.parseDouble(parts[4]));
                    nutrition.put("fiber", Double.parseDouble(parts[5]));
                    nutritionalData.put(parts[0], nutrition);
                }
            }
        } catch (FileNotFoundException e) {
            System.out.println("Nutritional data not found.");
        }
    }

    private static void loadUserGoals() {
        try (Scanner fileScanner = new Scanner(new File(USER_GOALS_FILE))) {
            while (fileScanner.hasNextLine()) {
                String[] parts = fileScanner.nextLine().split(",");
                if (parts.length >= 2) {
                    userGoals.put(parts[0], parts[1]);
                }
            }
        } catch (FileNotFoundException e) {
            System.out.println("User goals not found. Please set up your goals first.");
        }
    }

    private static void setupUserGoals(Scanner scanner) {
        System.out.println("\nSetting up your health and fitness goals");
        System.out.println("----------------------------------------");
        
        System.out.println("Select your primary goal:");
        System.out.println("1. Weight Loss");
        System.out.println("2. Muscle Gain");
        System.out.println("3. Maintenance");
        System.out.println("4. General Health");
        
        int goalChoice = Integer.parseInt(scanner.nextLine());
        String goal = switch (goalChoice) {
            case 1 -> "weight_loss";
            case 2 -> "muscle_gain";
            case 3 -> "maintenance";
            case 4 -> "general_health";
            default -> "general_health";
        };
        
        System.out.print("Enter your current weight (kg): ");
        String currentWeight = scanner.nextLine();
        
        System.out.print("Enter your target weight (kg): ");
        String targetWeight = scanner.nextLine();
        
        System.out.print("Enter your daily calorie goal: ");
        String dailyCalories = scanner.nextLine();
        
        System.out.print("Enter your protein goal (g): ");
        String proteinGoal = scanner.nextLine();
        
        System.out.print("Enter your carbs goal (g): ");
        String carbsGoal = scanner.nextLine();
        
        System.out.print("Enter your fat goal (g): ");
        String fatGoal = scanner.nextLine();
        
        System.out.println("\nDietary Restrictions (comma-separated, e.g., vegetarian,vegan,gluten-free): ");
        String restrictions = scanner.nextLine();
        
        System.out.println("Allergies (comma-separated, e.g., peanuts,shellfish): ");
        String allergies = scanner.nextLine();
        
        try (PrintWriter writer = new PrintWriter(new FileWriter(USER_GOALS_FILE))) {
            writer.println("goal," + goal);
            writer.println("current_weight," + currentWeight);
            writer.println("target_weight," + targetWeight);
            writer.println("daily_calories," + dailyCalories);
            writer.println("protein_goal," + proteinGoal);
            writer.println("carbs_goal," + carbsGoal);
            writer.println("fat_goal," + fatGoal);
            writer.println("dietary_restrictions," + restrictions);
            writer.println("allergies," + allergies);
            writer.println("target_date," + LocalDate.now().plusMonths(3));
        } catch (IOException e) {
            System.out.println("Error saving user goals: " + e.getMessage());
        }
        
        loadUserGoals();
    }

    private static List<String> generateMealPlan() {
        List<String> mealPlan = new ArrayList<>();
        if (userGoals.isEmpty() || nutritionalData.isEmpty()) {
            return mealPlan;
        }

        double dailyCalories = Double.parseDouble(userGoals.get("daily_calories"));
        double proteinGoal = Double.parseDouble(userGoals.get("protein_goal"));
        double carbsGoal = Double.parseDouble(userGoals.get("carbs_goal"));
        double fatGoal = Double.parseDouble(userGoals.get("fat_goal"));
        
        // Split calories into meals (40% breakfast, 30% lunch, 30% dinner)
        double breakfastCalories = dailyCalories * 0.4;
        double lunchCalories = dailyCalories * 0.3;
        double dinnerCalories = dailyCalories * 0.3;
        
        // Generate meals based on goals and restrictions
        String goal = userGoals.get("goal");
        List<String> restrictions = Arrays.asList(userGoals.get("dietary_restrictions").split(","));
        List<String> allergies = Arrays.asList(userGoals.get("allergies").split(","));
        
        // Generate breakfast
        mealPlan.add("Breakfast (" + (int)breakfastCalories + " calories):");
        mealPlan.addAll(generateMeal(breakfastCalories, proteinGoal * 0.3, carbsGoal * 0.4, fatGoal * 0.3, 
            goal, restrictions, allergies, "breakfast"));
        
        // Generate lunch
        mealPlan.add("\nLunch (" + (int)lunchCalories + " calories):");
        mealPlan.addAll(generateMeal(lunchCalories, proteinGoal * 0.4, carbsGoal * 0.3, fatGoal * 0.3, 
            goal, restrictions, allergies, "lunch"));
        
        // Generate dinner
        mealPlan.add("\nDinner (" + (int)dinnerCalories + " calories):");
        mealPlan.addAll(generateMeal(dinnerCalories, proteinGoal * 0.3, carbsGoal * 0.3, fatGoal * 0.4, 
            goal, restrictions, allergies, "dinner"));
        
        return mealPlan;
    }

    private static List<String> generateMeal(double targetCalories, double targetProtein, 
            double targetCarbs, double targetFat, String goal, List<String> restrictions, 
            List<String> allergies, String mealType) {
        List<String> meal = new ArrayList<>();
        List<String> availableFoods = new ArrayList<>();
        
        // Filter foods based on restrictions and allergies
        for (Map.Entry<String, Map<String, Double>> entry : nutritionalData.entrySet()) {
            String food = entry.getKey();
            if (isFoodAllowed(food, restrictions, allergies)) {
                availableFoods.add(food);
            }
        }
        
        // Sort foods by relevance to goals
        availableFoods.sort((a, b) -> {
            double scoreA = calculateFoodScore(a, goal, targetProtein, targetCarbs, targetFat);
            double scoreB = calculateFoodScore(b, goal, targetProtein, targetCarbs, targetFat);
            return Double.compare(scoreB, scoreA);
        });
        
        // Build meal
        double currentCalories = 0;
        double currentProtein = 0;
        double currentCarbs = 0;
        double currentFat = 0;
        
        for (String food : availableFoods) {
            Map<String, Double> nutrition = nutritionalData.get(food);
            if (currentCalories + nutrition.get("calories") <= targetCalories * 1.1) {
                double portion = calculatePortion(food, targetCalories - currentCalories, 
                    targetProtein - currentProtein, targetCarbs - currentCarbs, targetFat - currentFat);
                
                if (portion > 0) {
                    meal.add(String.format("- %.1fg %s (%.0f cal, %.1fg protein, %.1fg carbs, %.1fg fat)",
                        portion, food, nutrition.get("calories") * portion/100,
                        nutrition.get("protein") * portion/100,
                        nutrition.get("carbs") * portion/100,
                        nutrition.get("fat") * portion/100));
                    
                    currentCalories += nutrition.get("calories") * portion/100;
                    currentProtein += nutrition.get("protein") * portion/100;
                    currentCarbs += nutrition.get("carbs") * portion/100;
                    currentFat += nutrition.get("fat") * portion/100;
                }
            }
            
            if (currentCalories >= targetCalories * 0.9) {
                break;
            }
        }
        
        // Add nutritional summary
        meal.add(String.format("\nTotal: %.0f calories, %.1fg protein, %.1fg carbs, %.1fg fat",
            currentCalories, currentProtein, currentCarbs, currentFat));
        
        return meal;
    }

    private static boolean isFoodAllowed(String food, List<String> restrictions, List<String> allergies) {
        // Check dietary restrictions
        if (restrictions.contains("vegetarian") && 
            (food.contains("meat") || food.contains("fish") || food.contains("chicken"))) {
            return false;
        }
        if (restrictions.contains("vegan") && 
            (food.contains("yogurt") || food.contains("milk") || food.contains("cheese"))) {
            return false;
        }
        
        // Check allergies
        for (String allergy : allergies) {
            if (food.toLowerCase().contains(allergy.toLowerCase())) {
                return false;
            }
        }
        
        return true;
    }

    private static double calculateFoodScore(String food, String goal, 
            double targetProtein, double targetCarbs, double targetFat) {
        Map<String, Double> nutrition = nutritionalData.get(food);
        double score = 0;
        
        switch (goal) {
            case "weight_loss":
                score = nutrition.get("protein") * 2 + nutrition.get("fiber") * 1.5 - 
                       nutrition.get("calories") * 0.01 - nutrition.get("fat") * 0.5;
                break;
            case "muscle_gain":
                score = nutrition.get("protein") * 3 + nutrition.get("calories") * 0.01 - 
                       nutrition.get("fat") * 0.3;
                break;
            case "maintenance":
                score = nutrition.get("protein") + nutrition.get("fiber") + 
                       (1 - Math.abs(nutrition.get("calories")/2000 - 1)) * 2;
                break;
            default:
                score = nutrition.get("protein") + nutrition.get("fiber") + 
                       nutrition.get("vitamins") + nutrition.get("minerals");
        }
        
        return score;
    }

    private static double calculatePortion(String food, double targetCalories, 
            double targetProtein, double targetCarbs, double targetFat) {
        Map<String, Double> nutrition = nutritionalData.get(food);
        double portion = 100; // Start with 100g
        
        // Adjust portion based on calories
        if (nutrition.get("calories") > 0) {
            portion = Math.min(portion, targetCalories / nutrition.get("calories") * 100);
        }
        
        // Adjust based on macronutrients
        if (nutrition.get("protein") > 0) {
            portion = Math.min(portion, targetProtein / nutrition.get("protein") * 100);
        }
        if (nutrition.get("carbs") > 0) {
            portion = Math.min(portion, targetCarbs / nutrition.get("carbs") * 100);
        }
        if (nutrition.get("fat") > 0) {
            portion = Math.min(portion, targetFat / nutrition.get("fat") * 100);
        }
        
        return Math.min(portion, 300); // Cap at 300g per food item
    }

    private static void showMealPlan() {
        if (userGoals.isEmpty()) {
            System.out.println("\nPlease set up your goals first!");
            return;
        }
        
        List<String> mealPlan = generateMealPlan();
        
        System.out.println("\nAI-Generated Meal Plan");
        System.out.println("=====================");
        System.out.println("Goal: " + userGoals.get("goal").replace("_", " "));
        System.out.println("Daily Target: " + userGoals.get("daily_calories") + " calories");
        System.out.println("Macros: " + userGoals.get("protein_goal") + "g protein, " + 
            userGoals.get("carbs_goal") + "g carbs, " + userGoals.get("fat_goal") + "g fat");
        System.out.println("Dietary Restrictions: " + userGoals.get("dietary_restrictions"));
        System.out.println("Allergies: " + userGoals.get("allergies"));
        System.out.println("\n" + String.join("\n", mealPlan));
    }

    private static void initializeAIChatbot() {
        // Nutrition knowledge base
        AI_RESPONSES.put("calories", Arrays.asList(
            "Calories are units of energy. For weight loss, you need a calorie deficit.",
            "To maintain weight, match calories to your daily energy needs.",
            "For muscle gain, you need a calorie surplus of 300-500 calories."
        ));
        
        AI_RESPONSES.put("protein", Arrays.asList(
            "Protein helps build and repair muscles. Aim for 0.8-2g per kg of body weight.",
            "Good protein sources include chicken, fish, tofu, and legumes.",
            "Protein can help with weight loss by increasing satiety."
        ));
        
        AI_RESPONSES.put("carbs", Arrays.asList(
            "Carbs are your body's main energy source. Choose complex carbs like whole grains.",
            "Carbs should make up 45-65% of your daily calories.",
            "Low-carb diets can help with weight loss but may affect energy levels."
        ));
        
        AI_RESPONSES.put("fat", Arrays.asList(
            "Healthy fats are essential for hormone production and nutrient absorption.",
            "Aim for 20-35% of daily calories from fat.",
            "Focus on healthy fats like avocados, nuts, and olive oil."
        ));
        
        AI_RESPONSES.put("weight_loss", Arrays.asList(
            "Weight loss requires a calorie deficit of 500-1000 calories per day.",
            "Focus on protein and fiber to stay full longer.",
            "Combine diet with regular exercise for best results."
        ));
        
        AI_RESPONSES.put("muscle_gain", Arrays.asList(
            "Muscle gain requires a calorie surplus and adequate protein.",
            "Aim for 1.6-2.2g of protein per kg of body weight.",
            "Progressive resistance training is essential for muscle growth."
        ));
        
        AI_RESPONSES.put("meal_planning", Arrays.asList(
            "Plan meals around your daily calorie and macro goals.",
            "Include protein, carbs, and healthy fats in each meal.",
            "Prepare meals in advance to stay on track."
        ));
        
        AI_RESPONSES.put("vegetarian", Arrays.asList(
            "Vegetarian diets can be healthy with proper planning.",
            "Good protein sources include legumes, tofu, and dairy.",
            "Ensure adequate iron and B12 intake."
        ));
        
        AI_RESPONSES.put("vegan", Arrays.asList(
            "Vegan diets require careful planning for complete nutrition.",
            "Focus on plant-based protein sources like legumes and quinoa.",
            "Consider supplements for B12 and omega-3."
        ));
    }

    private static String getAIResponse(String question) {
        question = question.toLowerCase();
        
        // Update question frequency
        USER_QUESTIONS.merge(question, 1.0, Double::sum);
        
        // Check for keywords in the question
        for (Map.Entry<String, List<String>> entry : AI_RESPONSES.entrySet()) {
            if (question.contains(entry.getKey())) {
                List<String> responses = entry.getValue();
                return responses.get(new Random().nextInt(responses.size()));
            }
        }
        
        // Check for nutritional calculations
        if (question.contains("calculate") || question.contains("how many")) {
            return calculateNutritionalAnswer(question);
        }
        
        // Check for goal-specific advice
        if (userGoals.containsKey("goal")) {
            String goal = userGoals.get("goal");
            if (AI_RESPONSES.containsKey(goal) && question.contains("goal") || question.contains("target")) {
                return AI_RESPONSES.get(goal).get(new Random().nextInt(AI_RESPONSES.get(goal).size()));
            }
        }
        
        // Default response for unknown questions
        return "I'm not sure about that. Could you rephrase your question about nutrition, diet, or meal planning?";
    }

    private static String calculateNutritionalAnswer(String question) {
        // Extract numbers from the question
        Pattern pattern = Pattern.compile("\\d+");
        Matcher matcher = pattern.matcher(question);
        List<Integer> numbers = new ArrayList<>();
        while (matcher.find()) {
            numbers.add(Integer.parseInt(matcher.group()));
        }
        
        if (numbers.isEmpty()) {
            return "I couldn't find any numbers in your question. Please include specific values.";
        }
        
        // Calculate BMI
        if (question.contains("bmi")) {
            if (numbers.size() >= 2) {
                double weight = numbers.get(0);
                double height = numbers.get(1) / 100.0; // convert cm to m
                double bmi = weight / (height * height);
                return String.format("Your BMI is %.1f. This is considered %s.", 
                    bmi, getBMICategory(bmi));
            }
        }
        
        // Calculate daily calories
        if (question.contains("calorie") || question.contains("calories")) {
            if (numbers.size() >= 2) {
                double weight = numbers.get(0);
                double activityLevel = numbers.get(1);
                double calories = calculateDailyCalories(weight, activityLevel);
                return String.format("Based on your weight of %.0f kg and activity level, " +
                    "your estimated daily calorie needs are %.0f calories.", weight, calories);
            }
        }
        
        // Calculate protein needs
        if (question.contains("protein")) {
            if (!numbers.isEmpty()) {
                double weight = numbers.get(0);
                double protein = weight * 1.6; // 1.6g per kg for active individuals
                return String.format("For your weight of %.0f kg, aim for %.0f grams of protein daily " +
                    "to support muscle maintenance and growth.", weight, protein);
            }
        }
        
        return "I can help calculate nutritional values. Please specify what you'd like to calculate " +
               "(BMI, calories, protein needs, etc.) and include your relevant measurements.";
    }

    private static String getBMICategory(double bmi) {
        if (bmi < 18.5) return "underweight";
        if (bmi < 25) return "normal weight";
        if (bmi < 30) return "overweight";
        return "obese";
    }

    private static double calculateDailyCalories(double weight, double activityLevel) {
        // Basic BMR calculation (Mifflin-St Jeor Equation)
        double bmr = 10 * weight + 6.25 * 170 - 5 * 30 + 5; // Assuming average height and age
        return bmr * (1.2 + (activityLevel - 1) * 0.175); // Activity multiplier
    }

    private static void chatWithAI(Scanner scanner) {
        System.out.println("\nAI Nutrition Assistant");
        System.out.println("=====================");
        System.out.println("Ask me anything about nutrition, diet, or meal planning!");
        System.out.println("Type 'exit' to return to the main menu.");
        System.out.println("\nExample questions:");
        System.out.println("- How many calories should I eat?");
        System.out.println("- What's a good protein goal?");
        System.out.println("- How do I plan my meals?");
        System.out.println("- Calculate my BMI (weight: 70, height: 170)");
        
        while (true) {
            System.out.print("\nYour question: ");
            String question = scanner.nextLine().trim();
            
            if (question.equalsIgnoreCase("exit")) {
                break;
            }
            
            String response = getAIResponse(question);
            System.out.println("\nAI: " + response);
            
            // If the response is a calculation, ask for feedback
            if (question.contains("calculate") || question.contains("how many")) {
                System.out.print("\nWas this helpful? (y/n): ");
                String feedback = scanner.nextLine().toLowerCase();
                if (feedback.startsWith("n")) {
                    System.out.println("I'm sorry I couldn't help. Could you rephrase your question?");
                }
            }
        }
    }

    private static void initializeImageRecognition() {
        // Create image directory if it doesn't exist
        new File(IMAGE_DIRECTORY).mkdirs();
        new File(MODEL_DIRECTORY).mkdirs();
        
        // Initialize food image database with common foods
        FOOD_IMAGE_DATABASE.put("apple", Arrays.asList("red", "round", "fruit"));
        FOOD_IMAGE_DATABASE.put("banana", Arrays.asList("yellow", "curved", "fruit"));
        FOOD_IMAGE_DATABASE.put("salad", Arrays.asList("green", "vegetables", "bowl"));
        FOOD_IMAGE_DATABASE.put("pizza", Arrays.asList("round", "cheese", "baked"));
        FOOD_IMAGE_DATABASE.put("sandwich", Arrays.asList("bread", "filled", "lunch"));
        FOOD_IMAGE_DATABASE.put("soup", Arrays.asList("liquid", "bowl", "hot"));
        FOOD_IMAGE_DATABASE.put("rice", Arrays.asList("white", "grains", "cooked"));
        FOOD_IMAGE_DATABASE.put("chicken", Arrays.asList("meat", "protein", "cooked"));
        FOOD_IMAGE_DATABASE.put("fish", Arrays.asList("seafood", "protein", "cooked"));
        FOOD_IMAGE_DATABASE.put("pasta", Arrays.asList("noodles", "carbs", "cooked"));
        
        // Load existing image mappings
        loadImageMappings();
        
        // Initialize the deep learning model for food recognition
        initializeDeepLearningModel();
    }
    
    private static void saveData() {
        try {
            // Create a StringBuilder to build the data string
            StringBuilder data = new StringBuilder();
            
            // Add food entries
            for (FoodEntry entry : foodEntries) {
                data.append(entry.getDate()).append(",")
                    .append(entry.getFoodName()).append(",")
                    .append(entry.getCalories()).append(",")
                    .append(entry.getServingSize()).append("\n");
            }
            
            // Save to file
            FileWriter writer = new FileWriter("diet_tracker_data.csv");
            writer.write(data.toString());
            writer.close();
            
            System.out.println("Data saved successfully.");
        } catch (IOException e) {
            System.out.println("Error saving data: " + e.getMessage());
        }
    }
    
    /**
     * Initialize the food image analysis system
     */
    private static void initializeFoodImageAnalysis() {
        try {
            System.out.println("Initializing food image analysis system...");
            FoodImageAnalyzer.initialize();
            System.out.println("Food image analysis system initialized successfully!");
        } catch (Exception e) {
            System.err.println("Error initializing food image analysis: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Analyze a food image using AI and add the detected food to the diet tracker
     */
    private static void analyzeFoodImage(Scanner scanner) {
        System.out.println("\n=== Food Image Analysis ===\n");
        System.out.println("This feature allows you to analyze a food image and track the detected foods.");
        System.out.println("Enter the path to the food image file:");
        String imagePath = scanner.nextLine();
        
        // Validate the image path
        File imageFile = new File(imagePath);
        if (!imageFile.exists() || !imageFile.isFile()) {
            System.out.println("Error: File does not exist or is not a valid file.");
            return;
        }
        
        // Check if it's an image file
        String fileName = imageFile.getName().toLowerCase();
        if (!fileName.endsWith(".jpg") && !fileName.endsWith(".jpeg") && 
            !fileName.endsWith(".png") && !fileName.endsWith(".gif") && 
            !fileName.endsWith(".bmp")) {
            System.out.println("Error: File is not a supported image format. Please use JPG, PNG, GIF, or BMP.");
            return;
        }
        
        System.out.println("\nAnalyzing image... This may take a moment.\n");
        
        try {
            // Save a copy of the image to the food_images directory
            File foodImagesDir = new File("food_images");
            if (!foodImagesDir.exists()) {
                foodImagesDir.mkdirs();
            }
            
            // Create a unique filename based on timestamp
            String timestamp = LocalDate.now().toString() + "_" + System.currentTimeMillis();
            String newImagePath = "food_images/" + timestamp + "_" + imageFile.getName();
            Files.copy(imageFile.toPath(), new File(newImagePath).toPath(), StandardCopyOption.REPLACE_EXISTING);
            
            // Analyze the food image
            String analysisResult = FoodImageAnalyzer.analyzeFoodImage(newImagePath);
            System.out.println(analysisResult);
            
            // Ask if the user wants to add the detected food to their tracker
            System.out.println("\nWould you like to add any of the detected foods to your tracker? (y/n)");
            String answer = scanner.nextLine().trim().toLowerCase();
            
            if (answer.equals("y") || answer.equals("yes")) {
                addFoodFromAnalysis(scanner);
            }
            
        } catch (Exception e) {
            System.out.println("Error analyzing image: " + e.getMessage());
        }
    }
    
    /**
     * Add a food entry based on the analysis results
     */
    private static void addFoodFromAnalysis(Scanner scanner) {
        System.out.println("\nEnter the name of the food you want to add:");
        String foodName = scanner.nextLine();
        
        System.out.println("Enter the serving size (in grams):");
        double servingSize = getDoubleInput(scanner);
        
        System.out.println("Enter the calorie count (if known, or 0 if unknown):");
        int calories = getIntInput(scanner);
        
        // If calories are unknown, make an estimate based on the food type
        if (calories == 0) {
            calories = estimateCalories(foodName, servingSize);
            System.out.println("Estimated calories for " + foodName + ": " + calories);
        }
        
        // Create and add the food entry
        String date = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
        FoodEntry entry = new FoodEntry(date, foodName, calories, servingSize);
        foodEntries.add(entry);
        
        System.out.println("Food entry added successfully!");
    }
    
    /**
     * Estimate calories based on food name and serving size
     */
    private static int estimateCalories(String foodName, double servingSize) {
        // Default calorie density per 100g for different food categories
        Map<String, Integer> calorieEstimates = new HashMap<>();
        calorieEstimates.put("fruit", 60);     // Average fruits ~60 calories per 100g
        calorieEstimates.put("vegetable", 40); // Average vegetables ~40 calories per 100g
        calorieEstimates.put("meat", 200);     // Average meats ~200 calories per 100g
        calorieEstimates.put("fish", 150);     // Average fish ~150 calories per 100g
        calorieEstimates.put("dairy", 120);    // Average dairy ~120 calories per 100g
        calorieEstimates.put("grain", 350);    // Average grains ~350 calories per 100g
        calorieEstimates.put("dessert", 400);  // Average desserts ~400 calories per 100g
        calorieEstimates.put("fast food", 300); // Average fast food ~300 calories per 100g
        
        // Some specific foods
        Map<String, Integer> specificFoods = new HashMap<>();
        specificFoods.put("apple", 52);
        specificFoods.put("banana", 89);
        specificFoods.put("orange", 47);
        specificFoods.put("broccoli", 34);
        specificFoods.put("carrot", 41);
        specificFoods.put("spinach", 23);
        specificFoods.put("chicken", 165);
        specificFoods.put("beef", 250);
        specificFoods.put("salmon", 208);
        specificFoods.put("rice", 130);
        specificFoods.put("bread", 265);
        specificFoods.put("pasta", 131);
        specificFoods.put("milk", 42);
        specificFoods.put("cheese", 350);
        specificFoods.put("yogurt", 59);
        specificFoods.put("pizza", 266);
        specificFoods.put("burger", 295);
        specificFoods.put("ice cream", 207);
        
        // Check if we have the specific food in our database
        String lowercaseFood = foodName.toLowerCase();
        if (specificFoods.containsKey(lowercaseFood)) {
            return (int)(specificFoods.get(lowercaseFood) * (servingSize / 100.0));
        }
        
        // If not, try to categorize it
        for (Map.Entry<String, Integer> entry : calorieEstimates.entrySet()) {
            if (lowercaseFood.contains(entry.getKey())) {
                return (int)(entry.getValue() * (servingSize / 100.0));
            }
        }
        
        // Default estimate if we can't categorize it
        return (int)(100 * (servingSize / 100.0));
    }
    
    private static void loadPretrainedModel() {
        try {
            System.out.println("Loading pre-trained VGG16 model from zoo...");
            // This will download a pre-trained model (may take some time on first run)
            ZooModel zooModel = VGG16.builder().build();
            foodRecognitionModel = (ComputationGraph) zooModel.initPretrained(PretrainedType.IMAGENET);
            
            // Save the model for future use
            File modelFile = new File(MODEL_DIRECTORY + "food_recognition_model.zip");
            foodRecognitionModel.save(modelFile, true);
            System.out.println("AI model initialized and saved for future use!");
        } catch (Exception e) {
            System.out.println("Error loading pre-trained model: " + e.getMessage());
            System.out.println("Will use fallback image recognition method.");
        }
    }
    
    private static void initializeClassifier() {
        // Initialize the classifier
        classifier = new FoodClassifier();
    }
    
    private static void initializeAIChatbot() {
        // Initialize the AI chatbot
        aiChatbot = new AINutritionAssistant();
    }
    
    private static void initializeImageRecognition() {
        // Initialize the image recognition system
        imageRecognition = new FoodImageAnalyzer();
    }
    
    private static void loadUserPreferences() {
        // Load user preferences from file
        try {
            Scanner scanner = new Scanner(new File(USER_PREFERENCES_FILE));
            while (scanner.hasNextLine()) {
                String[] parts = scanner.nextLine().split(",");
                if (parts.length >= 2) {
                    userPreferences.put(parts[0], Boolean.parseBoolean(parts[1]));
                }
            }
            scanner.close();
        } catch (FileNotFoundException e) {
            System.out.println("No user preferences found.");
        }
    }
    
    private static void loadNutritionalData() {
        // Load nutritional data from file
        try {
            Scanner scanner = new Scanner(new File(NUTRITIONAL_DATA_FILE));
            while (scanner.hasNextLine()) {
                String[] parts = scanner.nextLine().split(",");
                if (parts.length >= 6) {
                    String foodName = parts[0];
                    double calories = Double.parseDouble(parts[1]);
                    double protein = Double.parseDouble(parts[2]);
                    double fat = Double.parseDouble(parts[3]);
                    double carbs = Double.parseDouble(parts[4]);
                    double fiber = Double.parseDouble(parts[5]);
                    
                    Map<String, Double> nutrition = new HashMap<>();
                    nutrition.put("calories", calories);
                    nutrition.put("protein", protein);
                    nutrition.put("fat", fat);
                    nutrition.put("carbs", carbs);
                    nutrition.put("fiber", fiber);
                    
                    nutritionalData.put(foodName, nutrition);
                }
            }
            scanner.close();
        } catch (FileNotFoundException e) {
            System.out.println("No nutritional data found.");
        }
    }
    
    private static void loadUserGoals() {
        // Load user goals from file
        try {
            Scanner scanner = new Scanner(new File(USER_GOALS_FILE));
            while (scanner.hasNextLine()) {
                String[] parts = scanner.nextLine().split(",");
                if (parts.length >= 2) {
                    userGoals.put(parts[0], parts[1]);
                }
            }
            scanner.close();
        } catch (FileNotFoundException e) {
            System.out.println("No user goals found.");
        }
    }
    
    private static void processFoodImage(Scanner scanner) {
        System.out.println("\nFood Image Analysis");
        System.out.println("==================");
        System.out.println("Enter the path to your food image:");
        String imagePath = scanner.nextLine().trim();
        
        String result = analyzeFoodImage(imagePath);
        System.out.println("\nAnalysis Result: " + result);
        
        if (result.contains("I'm not sure")) {
            System.out.print("What food is this? ");
            String foodName = scanner.nextLine().trim().toLowerCase();
            saveImageMapping(new File(imagePath).getName(), foodName);
            System.out.println("Thank you! I'll remember this for next time.");
        }
        
        // Get nutritional information
        if (nutritionalData.containsKey(result.replace("This appears to be ", "").replace(".", ""))) {
            String foodName = result.replace("This appears to be ", "").replace(".", "");
            Map<String, Double> nutrition = nutritionalData.get(foodName);
            System.out.println("\nNutritional Information:");
            System.out.printf("Calories: %.0f\n", nutrition.get("calories"));
            System.out.printf("Protein: %.1fg\n", nutrition.get("protein"));
            System.out.printf("Carbs: %.1fg\n", nutrition.get("carbs"));
            System.out.printf("Fat: %.1fg\n", nutrition.get("fat"));
            
            System.out.print("\nWould you like to log this food? (y/n): ");
            if (scanner.nextLine().toLowerCase().startsWith("y")) {
                addFoodEntryFromImage(foodName, nutrition);
            }
        }
    }
    
    private static void addFoodEntryFromImage(String foodName, Map<String, Double> nutrition) {
        LocalDate date = LocalDate.now();
        boolean isHealthy = classifyFood(foodName);
        
        try (FileWriter fw = new FileWriter(CSV_FILE, true);
             BufferedWriter bw = new BufferedWriter(fw);
             PrintWriter out = new PrintWriter(bw)) {
            
            out.println(String.format("%s,%s,100,%b", date, foodName, isHealthy));
            System.out.println("Food entry added successfully!");
            
        } catch (IOException e) {
            System.out.println("Error saving food entry: " + e.getMessage());
        }
    }
    
    public static void main(String[] args) {
        initializeClassifier();
        initializeAIChatbot();
        initializeImageRecognition();
        loadUserPreferences();
        loadNutritionalData();
        loadUserGoals();
        Scanner scanner = new Scanner(System.in);
        
        while (true) {
            System.out.println("1. Add Food Entry");
            System.out.println("2. View Daily Summary");
            System.out.println("3. Analyze Weekly Report");
            System.out.println("4. Calculate BMI");
            System.out.println("5. Get AI Meal Recommendations");
            System.out.println("6. Set Up Goals & Preferences");
            System.out.println("7. Generate Meal Plan");
            System.out.println("8. Chat with AI Nutrition Assistant");
            System.out.println("9. Analyze Food Image");
            System.out.println("10. Exit");
            
            int choice;
            try {
                choice = Integer.parseInt(scanner.nextLine());
            } catch (NumberFormatException e) {
                System.out.println("Invalid input. Please enter a number.");
                continue;
            }
            
            switch (choice) {
                case 1:
                    addFoodEntry(scanner);
                    break;
                case 2:
                    viewDailySummary(scanner);
                    break;
                case 3:
                    analyzeWeeklyReport();
                    break;
                case 4:
                    calculateBMI(scanner);
                    break;
                case 5:
                    showMealRecommendations();
                    break;
                case 6:
                    setupUserGoals(scanner);
                    break;
                case 7:
                    showMealPlan();
                    break;
                case 8:
                    chatWithAI(scanner);
                    break;
                case 9:
                    processFoodImage(scanner);
                    break;
                case 10:
                    System.out.println("Exiting...");
                    scanner.close();
                    return;
                default:
                    System.out.println("Invalid option. Try again.");
            }
        }
    }

    private static void addFoodEntry(Scanner scanner) {
        System.out.print("Enter food name: ");
        String name = scanner.nextLine().toLowerCase();
        
        System.out.print("Enter quantity (grams): ");
        int quantity;
        try {
            quantity = Integer.parseInt(scanner.nextLine());
        } catch (NumberFormatException e) {
            System.out.println("Invalid quantity. Using default 100g.");
            quantity = 100;
        }
        
        LocalDate date = LocalDate.now();
        boolean isHealthy = classifyFood(name);
        
        try (FileWriter fw = new FileWriter(CSV_FILE, true);
             BufferedWriter bw = new BufferedWriter(fw);
             PrintWriter out = new PrintWriter(bw)) {
            
            out.println(String.format("%s,%s,%d,%b", date, name, quantity, isHealthy));
            System.out.println("Food entry added successfully!");
            System.out.println("AI Classification: " + (isHealthy ? "Healthy" : "Unhealthy"));
            
            // Ask for user feedback to improve recommendations
            System.out.print("Did you enjoy this food? (y/n): ");
            boolean liked = scanner.nextLine().toLowerCase().startsWith("y");
            updateUserPreferences(name, liked);
            
        } catch (IOException e) {
            System.out.println("Error saving food entry: " + e.getMessage());
        }
    }

    private static void viewDailySummary(Scanner scanner) {
        System.out.print("Enter date (YYYY-MM-DD) or leave blank for today: ");
        String dateInput = scanner.nextLine();
        LocalDate date = dateInput.isEmpty() ? LocalDate.now() : LocalDate.parse(dateInput);
        
        try (Scanner fileScanner = new Scanner(new File(CSV_FILE))) {
            int healthy = 0, unhealthy = 0;
            
            while (fileScanner.hasNextLine()) {
                String[] parts = fileScanner.nextLine().split(",");
                if (parts.length >= 4 && LocalDate.parse(parts[0]).equals(date)) {
                    if (Boolean.parseBoolean(parts[3])) healthy++;
                    else unhealthy++;
                }
            }
            
            System.out.println("\nDaily Summary for " + date);
            System.out.println("Healthy foods: " + healthy);
            System.out.println("Unhealthy foods: " + unhealthy);
            if (healthy + unhealthy > 0) {
                double ratio = (double) healthy / (healthy + unhealthy) * 100;
                System.out.printf("Healthy ratio: %.1f%%\n", ratio);
                System.out.println("Advice: " + (ratio >= 50 ? "Good job!" : "Try to eat more healthy foods!"));
            }
            
        } catch (FileNotFoundException e) {
            System.out.println("No data found for " + date);
        }
    }

    private static void analyzeWeeklyReport() {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(6);
        
        try (Scanner fileScanner = new Scanner(new File(CSV_FILE))) {
            Map<LocalDate, int[]> dailyCounts = new TreeMap<>();
            
            while (fileScanner.hasNextLine()) {
                String[] parts = fileScanner.nextLine().split(",");
                if (parts.length >= 4) {
                    LocalDate date = LocalDate.parse(parts[0]);
                    if (!date.isBefore(startDate) && !date.isAfter(endDate)) {
                        int[] counts = dailyCounts.getOrDefault(date, new int[2]);
                        if (Boolean.parseBoolean(parts[3])) counts[0]++;
                        else counts[1]++;
                        dailyCounts.put(date, counts);
                    }
                }
            }
            
            System.out.println("\nWeekly Report (" + startDate + " to " + endDate + ")");
            System.out.println("Date\t\tHealthy\tUnhealthy\tRatio");
             
            int totalHealthy = 0, totalUnhealthy = 0;
            for (Map.Entry<LocalDate, int[]> entry : dailyCounts.entrySet()) {
                int healthy = entry.getValue()[0];
                int unhealthy = entry.getValue()[1];
                totalHealthy += healthy;
                totalUnhealthy += unhealthy;
                
                double ratio = healthy + unhealthy > 0 ? 
                    (double) healthy / (healthy + unhealthy) * 100 : 0;
                
                System.out.printf("%s\t%d\t%d\t\t%.1f%%\n", 
                    entry.getKey(), healthy, unhealthy, ratio);
            }
            
            if (totalHealthy + totalUnhealthy > 0) {
                double totalRatio = (double) totalHealthy / (totalHealthy + totalUnhealthy) * 100;
                System.out.printf("\nTotal\t\t%d\t%d\t\t%.1f%%\n", 
                    totalHealthy, totalUnhealthy, totalRatio);
                System.out.println("Weekly Advice: " + 
                    (totalRatio >= 50 ? "Excellent week!" : "Next week, focus on healthier choices!"));
            }
            
        } catch (FileNotFoundException e) {
            System.out.println("No data found for the week.");
        }
    }

    private static void calculateBMI(Scanner scanner) {
        System.out.print("Enter weight in kg: ");
        double weight = Double.parseDouble(scanner.nextLine());
        
        System.out.print("Enter height in meters: ");
        double height = Double.parseDouble(scanner.nextLine());
        
        double bmi = weight / (height * height);
        
        System.out.printf("\nYour BMI: %.1f\n", bmi);
        System.out.print("Category: ");
        
        if (bmi < 18.5) {
            System.out.println("Underweight");
        } else if (bmi < 25) {
            System.out.println("Normal weight");
        } else if (bmi < 30) {
            System.out.println("Overweight");
        } else {
            System.out.println("Obese");
        }
    }
    
    private static void addNewFoodNutrition(Scanner scanner, String foodName) {
        System.out.println("\nAdding nutritional information for " + foodName);
        
        double calories = 0, protein = 0, fat = 0, carbs = 0, fiber = 0;
        
        try {
            System.out.print("Calories per 100g: ");
            calories = Double.parseDouble(scanner.nextLine());
            
            System.out.print("Protein (g) per 100g: ");
            protein = Double.parseDouble(scanner.nextLine());
            
            System.out.print("Fat (g) per 100g: ");
            fat = Double.parseDouble(scanner.nextLine());
            
            System.out.print("Carbs (g) per 100g: ");
            carbs = Double.parseDouble(scanner.nextLine());
            
            System.out.print("Fiber (g) per 100g: ");
            fiber = Double.parseDouble(scanner.nextLine());
            
            // Create nutrition map
            Map<String, Double> nutrition = new HashMap<>();
            nutrition.put("calories", calories);
            nutrition.put("protein", protein);
            nutrition.put("fat", fat);
            nutrition.put("carbs", carbs);
            nutrition.put("fiber", fiber);
            
            // Add to nutritional data
            nutritionalData.put(foodName, nutrition);
            
            // Save to file
            try (PrintWriter writer = new PrintWriter(new FileWriter(NUTRITIONAL_DATA_FILE, true))) {
                writer.println(String.format("%s,%.0f,%.1f,%.1f,%.1f,%.1f,0,0", 
                    foodName, calories, protein, fat, carbs, fiber));
                System.out.println("Nutritional information saved successfully!");
                
                // Ask if they want to log this food
                System.out.print("\nWould you like to log this food? (y/n): ");
                if (scanner.nextLine().toLowerCase().startsWith("y")) {
                    addFoodEntryFromImage(foodName, nutrition);
                }
            } catch (IOException e) {
                System.out.println("Error saving nutritional data: " + e.getMessage());
            }
        } catch (NumberFormatException e) {
            System.out.println("Invalid input. Nutritional information not saved.");
        }
    }
}ion e) {
                System.out.println("Invalid input. Please enter a number.");
                continue;
            }
            
            switch (choice) {
                case 1:
                    addFoodEntry(scanner);
                    break;
                case 2:
                    viewDailySummary(scanner);
                    break;
                case 3:
                    analyzeWeeklyReport();
                    break;
                case 4:
                    calculateBMI(scanner);
                    break;
                case 5:
                    showMealRecommendations();
                    break;
                case 6:
                    setupUserGoals(scanner);
                    break;
                case 7:
                    showMealPlan();
                    break;
                case 8:
                    chatWithAI(scanner);
                    break;
                case 9:
                    processFoodImage(scanner);
                    break;
                case 10:
                    System.out.println("Exiting...");
                    scanner.close();
                    return;
                default:
                    System.out.println("Invalid option. Try again.");
            }
        }
    }

    private static void addFoodEntry(Scanner scanner) {
        System.out.print("Enter food name: ");
        String name = scanner.nextLine().toLowerCase();
        
        System.out.print("Enter quantity (grams): ");
        int quantity;
        try {
            quantity = Integer.parseInt(scanner.nextLine());
        } catch (NumberFormatException e) {
            System.out.println("Invalid quantity. Using default 100g.");
            quantity = 100;
        }
        
        LocalDate date = LocalDate.now();
        boolean isHealthy = classifyFood(name);
        
        try (FileWriter fw = new FileWriter(CSV_FILE, true);
             BufferedWriter bw = new BufferedWriter(fw);
             PrintWriter out = new PrintWriter(bw)) {
            
            out.println(String.format("%s,%s,%d,%b", date, name, quantity, isHealthy));
            System.out.println("Food entry added successfully!");
            System.out.println("AI Classification: " + (isHealthy ? "Healthy" : "Unhealthy"));
            
            // Ask for user feedback to improve recommendations
            System.out.print("Did you enjoy this food? (y/n): ");
            boolean liked = scanner.nextLine().toLowerCase().startsWith("y");
            updateUserPreferences(name, liked);
            
        } catch (IOException e) {
            System.out.println("Error saving food entry: " + e.getMessage());
        }
    }

    private static void viewDailySummary(Scanner scanner) {
        System.out.print("Enter date (YYYY-MM-DD) or leave blank for today: ");
        String dateInput = scanner.nextLine();
        LocalDate date = dateInput.isEmpty() ? LocalDate.now() : LocalDate.parse(dateInput);
        
        try (Scanner fileScanner = new Scanner(new File(CSV_FILE))) {
            int healthy = 0, unhealthy = 0;
            
            while (fileScanner.hasNextLine()) {
                String[] parts = fileScanner.nextLine().split(",");
                if (parts.length >= 4 && LocalDate.parse(parts[0]).equals(date)) {
                    if (Boolean.parseBoolean(parts[3])) healthy++;
                    else unhealthy++;
                }
            }
            
            System.out.println("\nDaily Summary for " + date);
            System.out.println("Healthy foods: " + healthy);
            System.out.println("Unhealthy foods: " + unhealthy);
            if (healthy + unhealthy > 0) {
                double ratio = (double) healthy / (healthy + unhealthy) * 100;
                System.out.printf("Healthy ratio: %.1f%%\n", ratio);
                System.out.println("Advice: " + (ratio >= 50 ? "Good job!" : "Try to eat more healthy foods!"));
            }
            
        } catch (FileNotFoundException e) {
            System.out.println("No data found for " + date);
        }
    }

    private static void analyzeWeeklyReport() {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(6);
        
        try (Scanner fileScanner = new Scanner(new File(CSV_FILE))) {
            Map<LocalDate, int[]> dailyCounts = new TreeMap<>();
            
            while (fileScanner.hasNextLine()) {
                String[] parts = fileScanner.nextLine().split(",");
                if (parts.length >= 4) {
                    LocalDate date = LocalDate.parse(parts[0]);
                    if (!date.isBefore(startDate) && !date.isAfter(endDate)) {
                        int[] counts = dailyCounts.getOrDefault(date, new int[2]);
                        if (Boolean.parseBoolean(parts[3])) counts[0]++;
                        else counts[1]++;
                        dailyCounts.put(date, counts);
                    }
                }
            }
            
            System.out.println("\nWeekly Report (" + startDate + " to " + endDate + ")");
            System.out.println("Date\t\tHealthy\tUnhealthy\tRatio");
             
            int totalHealthy = 0, totalUnhealthy = 0;
            for (Map.Entry<LocalDate, int[]> entry : dailyCounts.entrySet()) {
                int healthy = entry.getValue()[0];
                int unhealthy = entry.getValue()[1];
                totalHealthy += healthy;
                totalUnhealthy += unhealthy;
                
                double ratio = healthy + unhealthy > 0 ? 
                    (double) healthy / (healthy + unhealthy) * 100 : 0;
                
                System.out.printf("%s\t%d\t%d\t\t%.1f%%\n", 
                    entry.getKey(), healthy, unhealthy, ratio);
            }
            
            if (totalHealthy + totalUnhealthy > 0) {
                double totalRatio = (double) totalHealthy / (totalHealthy + totalUnhealthy) * 100;
                System.out.printf("\nTotal\t\t%d\t%d\t\t%.1f%%\n", 
                    totalHealthy, totalUnhealthy, totalRatio);
                System.out.println("Weekly Advice: " + 
                    (totalRatio >= 50 ? "Excellent week!" : "Next week, focus on healthier choices!"));
            }
            
        } catch (FileNotFoundException e) {
            System.out.println("No data found for the week.");
        }
    }

    private static void calculateBMI(Scanner scanner) {
        System.out.print("Enter weight in kg: ");
        double weight = Double.parseDouble(scanner.nextLine());
        
        System.out.print("Enter height in meters: ");
        double height = Double.parseDouble(scanner.nextLine());
        
        double bmi = weight / (height * height);
        
        System.out.printf("\nYour BMI: %.1f\n", bmi);
        System.out.print("Category: ");
        
        if (bmi < 18.5) {
            System.out.println("Underweight");
        } else if (bmi < 25) {
            System.out.println("Normal weight");
        } else if (bmi < 30) {
            System.out.println("Overweight");
        } else {
            System.out.println("Obese");
        }
    }
} 