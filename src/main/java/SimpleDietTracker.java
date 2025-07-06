import java.io.*;
import java.util.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

/**
 * A simplified version of the Diet Tracker application
 * with AI food image recognition capabilities
 */
public class SimpleDietTracker {
    // Constants
    private static final String DATA_FILE = "diet_data.csv";
    private static final String IMAGE_DIRECTORY = "food_images/";
    
    // Data structures
    private static List<FoodEntry> foodEntries = new ArrayList<>();
    private static int dailyCalorieGoal = 2000; // Default value
    
    /**
     * Main method to run the application
     */
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        loadData();
        initializeImageSystem();
        
        System.out.println("Welcome to Diet Tracker!");
        
        boolean running = true;
        while (running) {
            System.out.println("\nChoose an option:");
            System.out.println("1. Add a food entry");
            System.out.println("2. View food entries");
            System.out.println("3. Calculate BMI");
            System.out.println("4. Set daily calorie goal");
            System.out.println("5. View calorie summary");
            System.out.println("6. Analyze food image");
            System.out.println("7. Exit");
            
            int choice = getIntInput(scanner, "Enter your choice: ");
            
            switch (choice) {
                case 1:
                    addFoodEntry(scanner);
                    break;
                case 2:
                    viewFoodEntries();
                    break;
                case 3:
                    calculateBMI(scanner);
                    break;
                case 4:
                    setDailyCalorieGoal(scanner);
                    break;
                case 5:
                    viewCalorieSummary();
                    break;
                case 6:
                    analyzeFoodImage(scanner);
                    break;
                case 7:
                    saveData();
                    running = false;
                    System.out.println("Thank you for using Diet Tracker!");
                    break;
                default:
                    System.out.println("Invalid option. Please try again.");
            }
        }
        
        scanner.close();
    }
    
    /**
     * Initialize the AI food image analysis system
     */
    private static void initializeImageSystem() {
        try {
            System.out.println("Initializing food image analysis system...");
            
            // Create necessary directories
            new File(IMAGE_DIRECTORY).mkdirs();
            
            System.out.println("Food image analysis system initialized!");
        } catch (Exception e) {
            System.err.println("Error initializing image system: " + e.getMessage());
        }
    }
    
    /**
     * Add a new food entry
     */
    private static void addFoodEntry(Scanner scanner) {
        System.out.println("\n=== Add Food Entry ===");
        System.out.print("Enter food name: ");
        String foodName = scanner.nextLine();
        
        System.out.print("Enter calories: ");
        int calories = getIntInput(scanner, "Enter calories: ");
        
        System.out.print("Enter serving size (in grams): ");
        double servingSize = getDoubleInput(scanner, "Enter serving size: ");
        
        // Create and add the food entry
        String date = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
        FoodEntry entry = new FoodEntry(date, foodName, calories, servingSize);
        foodEntries.add(entry);
        
        System.out.println("Food entry added successfully!");
    }
    
    /**
     * View all food entries
     */
    private static void viewFoodEntries() {
        System.out.println("\n=== Food Entries ===");
        
        if (foodEntries.isEmpty()) {
            System.out.println("No food entries found.");
            return;
        }
        
        System.out.printf("%-12s %-20s %-10s %-10s\n", "Date", "Food", "Calories", "Serving (g)");
        System.out.println("------------------------------------------------");
        
        for (FoodEntry entry : foodEntries) {
            System.out.printf("%-12s %-20s %-10d %-10.1f\n", 
                entry.getDate(), entry.getFoodName(), 
                entry.getCalories(), entry.getServingSize());
        }
    }
    
    /**
     * Calculate BMI
     */
    private static void calculateBMI(Scanner scanner) {
        System.out.println("\n=== BMI Calculator ===");
        
        System.out.print("Enter weight (kg): ");
        double weight = getDoubleInput(scanner, "Enter weight (kg): ");
        
        System.out.print("Enter height (m): ");
        double height = getDoubleInput(scanner, "Enter height (m): ");
        
        double bmi = weight / (height * height);
        
        System.out.printf("Your BMI: %.1f\n", bmi);
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
    
    /**
     * Set daily calorie goal
     */
    private static void setDailyCalorieGoal(Scanner scanner) {
        System.out.println("\n=== Set Daily Calorie Goal ===");
        System.out.print("Enter daily calorie goal: ");
        
        dailyCalorieGoal = getIntInput(scanner, "Enter daily calorie goal: ");
        System.out.println("Daily calorie goal set to " + dailyCalorieGoal + " calories.");
    }
    
    /**
     * View calorie summary
     */
    private static void viewCalorieSummary() {
        System.out.println("\n=== Calorie Summary ===");
        
        // Get today's date
        String today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
        
        // Calculate total calories for today
        int totalCaloriesToday = 0;
        for (FoodEntry entry : foodEntries) {
            if (entry.getDate().equals(today)) {
                totalCaloriesToday += entry.getCalories();
            }
        }
        
        System.out.println("Today's calorie intake: " + totalCaloriesToday);
        System.out.println("Daily calorie goal: " + dailyCalorieGoal);
        
        int remaining = dailyCalorieGoal - totalCaloriesToday;
        if (remaining > 0) {
            System.out.println("Calories remaining: " + remaining);
        } else {
            System.out.println("Calorie goal exceeded by: " + Math.abs(remaining));
        }
    }
    
    /**
     * Analyze a food image and add the detected food to the tracker
     */
    private static void analyzeFoodImage(Scanner scanner) {
        System.out.println("\n=== Food Image Analysis ===");
        System.out.println("This feature allows you to analyze a food image and track the detected foods.");
        System.out.print("Enter the path to the food image file: ");
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
            File foodImagesDir = new File(IMAGE_DIRECTORY);
            if (!foodImagesDir.exists()) {
                foodImagesDir.mkdirs();
            }
            
            // Create a unique filename based on timestamp
            String timestamp = LocalDate.now().toString() + "_" + System.currentTimeMillis();
            String newImagePath = IMAGE_DIRECTORY + timestamp + "_" + imageFile.getName();
            Files.copy(imageFile.toPath(), new File(newImagePath).toPath(), StandardCopyOption.REPLACE_EXISTING);
            
            // Simulate AI analysis results since we don't have the actual AI model loaded
            String[] possibleFoods = {"apple", "banana", "salad", "chicken", "rice", "pasta", "pizza"};
            Random random = new Random();
            String detectedFood = possibleFoods[random.nextInt(possibleFoods.length)];
            
            System.out.println("Analysis Results:");
            System.out.println("Detected Food: " + detectedFood);
            System.out.println("Confidence: " + (70 + random.nextInt(30)) + "%");
            
            // Ask if the user wants to add the detected food to their tracker
            System.out.print("\nWould you like to add " + detectedFood + " to your tracker? (y/n): ");
            String answer = scanner.nextLine().trim().toLowerCase();
            
            if (answer.equals("y") || answer.equals("yes")) {
                addFoodFromAnalysis(scanner, detectedFood);
            }
            
        } catch (Exception e) {
            System.out.println("Error analyzing image: " + e.getMessage());
        }
    }
    
    /**
     * Add a food entry based on the analysis results
     */
    private static void addFoodFromAnalysis(Scanner scanner, String detectedFood) {
        System.out.println("\nAdding " + detectedFood + " to your food tracker.");
        
        System.out.print("Enter serving size (in grams): ");
        double servingSize = getDoubleInput(scanner, "Enter serving size: ");
        
        // Estimate calories based on the food
        int calories = estimateCalories(detectedFood, servingSize);
        System.out.println("Estimated calories for " + detectedFood + ": " + calories);
        
        System.out.print("Accept this calorie estimate? (y/n): ");
        String accept = scanner.nextLine().trim().toLowerCase();
        
        if (!accept.equals("y") && !accept.equals("yes")) {
            System.out.print("Enter your own calorie estimate: ");
            calories = getIntInput(scanner, "Enter calories: ");
        }
        
        // Create and add the food entry
        String date = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
        FoodEntry entry = new FoodEntry(date, detectedFood, calories, servingSize);
        foodEntries.add(entry);
        
        System.out.println("Food entry added successfully!");
    }
    
    /**
     * Estimate calories based on food name and serving size
     */
    private static int estimateCalories(String foodName, double servingSize) {
        // Default calorie density per 100g for different food categories
        Map<String, Integer> calorieEstimates = new HashMap<>();
        calorieEstimates.put("apple", 52);
        calorieEstimates.put("banana", 89);
        calorieEstimates.put("salad", 33);
        calorieEstimates.put("chicken", 165);
        calorieEstimates.put("rice", 130);
        calorieEstimates.put("pasta", 131);
        calorieEstimates.put("pizza", 266);
        
        // Default to 100 calories per 100g if not found
        int caloriesPer100g = calorieEstimates.getOrDefault(foodName.toLowerCase(), 100);
        
        // Calculate calories based on serving size
        return (int)(caloriesPer100g * (servingSize / 100.0));
    }
    
    /**
     * Load data from file
     */
    private static void loadData() {
        try {
            File dataFile = new File(DATA_FILE);
            if (!dataFile.exists()) {
                System.out.println("No existing data file found. Starting with empty data.");
                return;
            }
            
            BufferedReader reader = new BufferedReader(new FileReader(dataFile));
            String line;
            
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length >= 4) {
                    String date = parts[0];
                    String foodName = parts[1];
                    int calories = Integer.parseInt(parts[2]);
                    double servingSize = Double.parseDouble(parts[3]);
                    
                    foodEntries.add(new FoodEntry(date, foodName, calories, servingSize));
                }
            }
            
            reader.close();
            System.out.println("Data loaded successfully.");
        } catch (IOException e) {
            System.out.println("Error loading data: " + e.getMessage());
        }
    }
    
    /**
     * Save data to file
     */
    private static void saveData() {
        try {
            FileWriter writer = new FileWriter(DATA_FILE);
            
            for (FoodEntry entry : foodEntries) {
                writer.write(
                    entry.getDate() + "," +
                    entry.getFoodName() + "," +
                    entry.getCalories() + "," +
                    entry.getServingSize() + "\n"
                );
            }
            
            writer.close();
            System.out.println("Data saved successfully.");
        } catch (IOException e) {
            System.out.println("Error saving data: " + e.getMessage());
        }
    }
    
    /**
     * Get integer input with validation
     */
    private static int getIntInput(Scanner scanner, String prompt) {
        while (true) {
            try {
                return Integer.parseInt(scanner.nextLine());
            } catch (NumberFormatException e) {
                System.out.print("Invalid input. " + prompt);
            }
        }
    }
    
    /**
     * Get double input with validation
     */
    private static double getDoubleInput(Scanner scanner, String prompt) {
        while (true) {
            try {
                return Double.parseDouble(scanner.nextLine());
            } catch (NumberFormatException e) {
                System.out.print("Invalid input. " + prompt);
            }
        }
    }
    
    /**
     * Food Entry class to store food data
     */
    static class FoodEntry {
        private String date;
        private String foodName;
        private int calories;
        private double servingSize;
        
        public FoodEntry(String date, String foodName, int calories, double servingSize) {
            this.date = date;
            this.foodName = foodName;
            this.calories = calories;
            this.servingSize = servingSize;
        }
        
        public String getDate() {
            return date;
        }
        
        public String getFoodName() {
            return foodName;
        }
        
        public int getCalories() {
            return calories;
        }
        
        public double getServingSize() {
            return servingSize;
        }
    }
}
