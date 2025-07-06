import java.io.*;
import java.util.*;
import javax.servlet.*;
import javax.servlet.http.*;
import javax.servlet.annotation.*;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;

/**
 * Servlet that handles image upload and analysis using the ImageAnalyzer
 */
@WebServlet("/analyze-image")
@MultipartConfig(
    fileSizeThreshold = 1024 * 1024, // 1 MB
    maxFileSize = 1024 * 1024 * 5,   // 5 MB
    maxRequestSize = 1024 * 1024 * 10 // 10 MB
)
public class ImageAnalysisServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final String UPLOAD_DIRECTORY = "uploaded_images";
    
    @Override
    public void init() throws ServletException {
        // Initialize the ImageAnalyzer when the servlet starts
        try {
            ImageAnalyzer.initialize();
        } catch (Exception e) {
            throw new ServletException("Failed to initialize ImageAnalyzer", e);
        }
    }
    
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        // Configure JSON response
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();
        
        try {
            // Check if request contains multipart content
            if (!ServletFileUpload.isMultipartContent(request)) {
                out.print("{\"error\": \"Request doesn't contain multipart content\"}");
                return;
            }
            
            // Create upload directory if it doesn't exist
            File uploadDir = new File(UPLOAD_DIRECTORY);
            if (!uploadDir.exists()) {
                uploadDir.mkdir();
            }
            
            // Process file upload
            Part filePart = request.getPart("image");
            String fileName = getSubmittedFileName(filePart);
            
            if (fileName == null || fileName.isEmpty()) {
                out.print("{\"error\": \"No file selected\"}");
                return;
            }
            
            // Validate file type
            if (!isImageFile(fileName)) {
                out.print("{\"error\": \"Only image files are allowed\"}");
                return;
            }
            
            // Generate unique file name to prevent overwriting
            String uniqueFileName = System.currentTimeMillis() + "_" + fileName;
            String filePath = UPLOAD_DIRECTORY + File.separator + uniqueFileName;
            
            // Save the file
            filePart.write(filePath);
            
            // Analyze the image using ImageAnalyzer
            String analysisResult = ImageAnalyzer.analyzeImage(filePath);
            
            // Return the analysis result
            out.print("{\"success\": true, \"description\": \"" + 
                    escapeJsonString(analysisResult) + "\"}");
            
        } catch (Exception e) {
            out.print("{\"error\": \"" + escapeJsonString(e.getMessage()) + "\"}");
            e.printStackTrace();
        }
    }
    
    // Helper method to get the submitted filename
    private String getSubmittedFileName(Part part) {
        for (String cd : part.getHeader("content-disposition").split(";")) {
            if (cd.trim().startsWith("filename")) {
                return cd.substring(cd.indexOf('=') + 1).trim().replace("\"", "");
            }
        }
        return null;
    }
    
    // Helper method to validate image file types
    private boolean isImageFile(String fileName) {
        String extension = fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
        return extension.equals("jpg") || extension.equals("jpeg") || 
               extension.equals("png") || extension.equals("gif") || 
               extension.equals("bmp");
    }
    
    // Helper method to escape special characters in JSON strings
    private String escapeJsonString(String input) {
        if (input == null) {
            return "";
        }
        
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < input.length(); i++) {
            char ch = input.charAt(i);
            switch (ch) {
                case '\"':
                    sb.append("\\\"");
                    break;
                case '\\':
                    sb.append("\\\\");
                    break;
                case '\b':
                    sb.append("\\b");
                    break;
                case '\f':
                    sb.append("\\f");
                    break;
                case '\n':
                    sb.append("\\n");
                    break;
                case '\r':
                    sb.append("\\r");
                    break;
                case '\t':
                    sb.append("\\t");
                    break;
                default:
                    sb.append(ch);
            }
        }
        return sb.toString();
    }
}
