import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;
import java.io.File;

/**
 * Application initializer that runs when the web application starts
 * Creates necessary directories and initializes the AI model
 */
@WebListener
public class AppInitializer implements ServletContextListener {
    
    private static final String UPLOAD_DIRECTORY = "uploaded_images";
    private static final String MODEL_DIRECTORY = "models";
    private static final String IMAGE_DIRECTORY = "food_images";
    
    @Override
    public void contextInitialized(ServletContextEvent sce) {
        System.out.println("Initializing Diet Tracker application...");
        
        // Create directories if they don't exist
        createDirectory(UPLOAD_DIRECTORY);
        createDirectory(MODEL_DIRECTORY);
        createDirectory(IMAGE_DIRECTORY);
        
        // Initialize the image analyzers
        try {
            System.out.println("Initializing image analysis models...");
            ImageAnalyzer.initialize();
            System.out.println("Image analysis models initialized successfully!");
        } catch (Exception e) {
            System.err.println("Error initializing image analysis models: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        System.out.println("Diet Tracker application shutting down...");
    }
    
    private void createDirectory(String directoryPath) {
        File directory = new File(directoryPath);
        if (!directory.exists()) {
            if (directory.mkdirs()) {
                System.out.println("Created directory: " + directory.getAbsolutePath());
            } else {
                System.err.println("Failed to create directory: " + directory.getAbsolutePath());
            }
        }
    }
}
