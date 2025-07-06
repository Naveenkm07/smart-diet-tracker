# 🍎 Smart Diet Tracker

A comprehensive diet and nutrition tracking application with AI-powered image analysis and intelligent meal recommendations.

## ✨ Features

### 🍽️ **Food Tracking**
- Add food entries with detailed nutritional information
- Track calories, protein, carbs, and fat intake
- Categorize meals by type (breakfast, lunch, dinner, snack)
- Daily and weekly nutritional summaries

### 🤖 **AI-Powered Image Analysis**
- Upload food images for automatic analysis
- Color-based image description and analysis
- Integration with Python Flask backend for image processing
- Real-time image analysis results

### 📊 **Health Analytics**
- BMI calculator with health category classification
- Daily nutritional goal tracking
- Meal pattern analysis and recommendations
- Personalized health insights

### 🎯 **Smart Recommendations**
- AI-powered meal suggestions based on preferences
- Nutritional goal-based meal planning
- Dietary restriction and allergy considerations
- Personalized food recommendations

## 🛠️ Technology Stack

### **Frontend**
- HTML5, CSS3, JavaScript
- Responsive design for all devices
- Local storage for data persistence
- Modern UI/UX with animations

### **Backend**
- **Java**: Core application logic and web server
- **Python Flask**: Image analysis API
- **Maven**: Java dependency management
- **Jetty**: Embedded web server

### **AI/ML**
- **Deep Learning4J**: Java-based deep learning
- **Pillow**: Python image processing
- **VGG16**: Pre-trained image recognition model
- **Custom ML**: Food classification algorithms

## 🚀 Quick Start

### Prerequisites
- Java 11 or higher
- Python 3.7+
- Maven
- Chrome browser (recommended)

### Installation

1. **Clone the repository**
   ```bash
   git clone https://github.com/Naveenkm07/smart-diet-tracker.git
   cd smart-diet-tracker
   ```

2. **Install Python dependencies**
   ```bash
   pip install -r requirements.txt
   ```

3. **Build Java application**
   ```bash
   mvn clean package
   ```

### Running the Application

1. **Start the Python Flask server** (for image analysis)
   ```bash
   python server.py
   ```
   The server will run on `http://127.0.0.1:5000`

2. **Open the web application**
   - Open `simple_diet_tracker.html` in your browser
   - Or use the full Java web server (if configured)

## 📁 Project Structure

```
smart-diet-tracker/
├── src/main/java/          # Java source files
│   ├── DietTracker.java    # Main diet tracking logic
│   ├── WebServer.java      # Web server implementation
│   ├── ImageAnalyzer.java  # Image analysis components
│   └── ...
├── server.py               # Python Flask image analysis server
├── simple_diet_tracker.html # Standalone web application
├── requirements.txt        # Python dependencies
├── pom.xml                # Maven configuration
├── *.csv                  # Data files (nutritional data, food logs)
└── food_images/           # Sample food images
```

## 🎯 Usage Guide

### Adding Food Entries
1. Fill out the food entry form
2. Enter food name, calories, and macros
3. Select meal type
4. Click "Add Food Entry"

### Image Analysis
1. Upload a food image
2. Click "Analyze Image"
3. View the AI-generated description
4. Add the analyzed food to your log

### Viewing Reports
1. Click "Refresh Summary" to see daily totals
2. Track your nutritional goals
3. Monitor your progress over time

### BMI Calculation
1. Enter your weight (kg) and height (cm)
2. Click "Calculate BMI"
3. View your BMI category and health status

## 🔧 Configuration

### Customizing Nutritional Goals
Edit the `user_goals.csv` file to set personalized targets:
```csv
goal,calories,protein,carbs,fat
weight_loss,1800,150,200,60
muscle_gain,2200,180,250,80
maintenance,2000,150,225,70
```

### Adding New Foods
Update `nutritional_data.csv` with new food items:
```csv
food_name,calories,protein,carbs,fat
apple,52,0.3,14,0.2
chicken_breast,165,31,0,3.6
```

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## 📝 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 👨‍💻 Author

**Naveen KM**
- GitHub: [@Naveenkm07](https://github.com/Naveenkm07)
- Project: [Smart Diet Tracker](https://github.com/Naveenkm07/smart-diet-tracker)

## 🙏 Acknowledgments

- Deep Learning4J community for ML capabilities
- Flask framework for Python backend
- Jetty for Java web server
- All contributors and testers

---

⭐ **Star this repository if you find it helpful!** 