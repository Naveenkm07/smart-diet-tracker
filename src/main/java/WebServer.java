import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.resource.Resource;

import java.io.File;
import java.net.URI;
import java.net.URL;

/**
 * Simple embedded web server to run the Diet Tracker application
 */
public class WebServer {
    private static final int PORT = 8080;
    
    public static void main(String[] args) throws Exception {
        // Create and configure server
        Server server = new Server(PORT);
        
        // Create servlet context
        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/");
        
        // Set resource base for static files
        URL webappDir = WebServer.class.getClassLoader().getResource(".");
        URI webRootUri = webappDir != null ? URI.create(webappDir.toURI().toASCIIString()) : 
                         new File("d:/MAJOR").toURI();
        context.setBaseResource(Resource.newResource(webRootUri));
        
        // Create necessary directories
        createDirectories();
        
        // Add the image analysis servlet
        ServletHolder imageAnalysisServlet = new ServletHolder(new ImageAnalysisServlet());
        context.addServlet(imageAnalysisServlet, "/analyze-image");
        
        // Add default servlet for static content
        ServletHolder defaultServlet = new ServletHolder("default", DefaultServlet.class);
        defaultServlet.setInitParameter("dirAllowed", "true");
        context.addServlet(defaultServlet, "/");
        
        // Set handler
        server.setHandler(context);
        
        // Initialize the image analyzer
        System.out.println("Initializing image analysis models...");
        ImageAnalyzer.initialize();
        
        // Start the server
        System.out.println("Starting web server on port " + PORT);
        server.start();
        System.out.println("Server started, open http://localhost:" + PORT + " in your browser");
        
        // Wait for server to finish
        server.join();
    }
    
    private static void createDirectories() {
        // Create directories if they don't exist
        String[] directories = {
            "uploaded_images",
            "models",
            "food_images"
        };
        
        for (String dir : directories) {
            File directory = new File(dir);
            if (!directory.exists()) {
                if (directory.mkdirs()) {
                    System.out.println("Created directory: " + directory.getAbsolutePath());
                } else {
                    System.err.println("Failed to create directory: " + directory.getAbsolutePath());
                }
            }
        }
    }
}
