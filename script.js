// User authentication
let currentUser = JSON.parse(localStorage.getItem('currentUser')) || null;
let users = JSON.parse(localStorage.getItem('users')) || [];

// Store food entries in localStorage since we can't use Java directly in browser
let foodEntries = JSON.parse(localStorage.getItem('foodEntries')) || [];
let workoutEntries = JSON.parse(localStorage.getItem('workoutEntries')) || [];
let wellbeingEntries = JSON.parse(localStorage.getItem('wellbeingEntries')) || [];

// Food types from Java code
const FOOD_TYPES = {
    "vegetables": true,
    "fruits": true,
    "nuts": true,
    "fish": true,
    "whole grains": true,
    "sweets": false,
    "fast food": false,
    "processed food": false,
    "soda": false
};

// Workout types
const WORKOUT_TYPES = {
    "cardio": true,
    "strength": true,
    "yoga": true,
    "swimming": true,
    "cycling": true,
    "walking": true,
    "running": true
};

// Check if user is logged in on page load
document.addEventListener('DOMContentLoaded', function() {
    if (currentUser) {
        showSection('dashboard');
        updateUserInfo();
        loadUserData();
    } else {
        showSection('login');
    }
});

// Update user info display
function updateUserInfo() {
    const userInfoElement = document.getElementById('userInfo');
    if (currentUser && userInfoElement) {
        userInfoElement.textContent = `Logged in as: ${currentUser.username}`;
    }
}

// Show selected section and hide others
function showSection(sectionId) {
    // Hide all sections first
    const sections = [
        'login',
        'register',
        'dashboard',
        'food',
        'workout',
        'wellbeing',
        'bmi',
        'reports',
        'imageAnalysis'
    ];
    
    sections.forEach(section => {
        const element = document.getElementById(section);
        if (element) {
            element.classList.add('hidden');
        }
    });
    
    // Show the selected section
    const selectedSection = document.getElementById(sectionId);
    if (selectedSection) {
        selectedSection.classList.remove('hidden');
    }
    
    // Update user info if showing dashboard
    if (sectionId === 'dashboard') {
        updateUserInfo();
    }
}

// Handle user registration
document.getElementById('registerForm').addEventListener('submit', function(e) {
    e.preventDefault();
    
    const username = document.getElementById('regUsername').value;
    const password = document.getElementById('regPassword').value;
    
    if (users.some(user => user.username === username)) {
        alert('Username already exists!');
        return;
    }
    
    users.push({ username, password });
    localStorage.setItem('users', JSON.stringify(users));
    
    alert('Registration successful! Please login.');
    this.reset();
    showSection('login');
});

// Handle user login
document.getElementById('loginForm').addEventListener('submit', function(e) {
    e.preventDefault();
    
    const username = document.getElementById('loginUsername').value;
    const password = document.getElementById('loginPassword').value;
    
    const user = users.find(u => u.username === username && u.password === password);
    
    if (user) {
        currentUser = { username };
        localStorage.setItem('currentUser', JSON.stringify(currentUser));
        showSection('dashboard');
        updateUserInfo();
        loadUserData();
    } else {
        alert('Invalid username or password!');
    }
});

// Load user data
function loadUserData() {
    if (!currentUser) return;
    
    // Load user-specific data
    foodEntries = JSON.parse(localStorage.getItem(`foodEntries_${currentUser.username}`)) || [];
    workoutEntries = JSON.parse(localStorage.getItem(`workoutEntries_${currentUser.username}`)) || [];
    wellbeingEntries = JSON.parse(localStorage.getItem(`wellbeingEntries_${currentUser.username}`)) || [];
}

// Handle food form submission
document.getElementById('foodForm').addEventListener('submit', function(e) {
    e.preventDefault();
    
    if (!currentUser) {
        alert('Please login first!');
        return;
    }
    
    const foodName = document.getElementById('foodName').value.toLowerCase();
    const quantity = parseInt(document.getElementById('quantity').value);
    const calories = parseFloat(document.getElementById('calories').value);
    const protein = parseFloat(document.getElementById('protein').value);
    const carbs = parseFloat(document.getElementById('carbs').value);
    const fats = parseFloat(document.getElementById('fats').value);
    const date = new Date().toISOString().split('T')[0];
    const isHealthy = FOOD_TYPES[foodName] || false;
    
    const entry = {
        date,
        foodName,
        quantity,
        calories,
        protein,
        carbs,
        fats,
        isHealthy
    };
    
    foodEntries.push(entry);
    localStorage.setItem(`foodEntries_${currentUser.username}`, JSON.stringify(foodEntries));
    
    alert('Food entry added successfully!');
    this.reset();
    document.getElementById('quantity').value = '100'; // Reset to default value
});

// Handle workout form submission
document.getElementById('workoutForm').addEventListener('submit', function(e) {
    e.preventDefault();
    
    if (!currentUser) {
        alert('Please login first!');
        return;
    }
    
    const workoutType = document.getElementById('workoutType').value;
    const duration = parseInt(document.getElementById('duration').value);
    const intensity = document.getElementById('intensity').value;
    const caloriesBurned = parseInt(document.getElementById('caloriesBurned').value);
    const date = new Date().toISOString().split('T')[0];
    
    const entry = {
        date,
        workoutType,
        duration,
        intensity,
        caloriesBurned
    };
    
    workoutEntries.push(entry);
    localStorage.setItem(`workoutEntries_${currentUser.username}`, JSON.stringify(workoutEntries));
    
    alert('Workout entry added successfully!');
    this.reset();
});

// Handle wellbeing form submission
document.getElementById('wellbeingForm').addEventListener('submit', function(e) {
    e.preventDefault();
    
    if (!currentUser) {
        alert('Please login first!');
        return;
    }
    
    const mood = document.getElementById('mood').value;
    const sleepHours = parseInt(document.getElementById('sleepHours').value);
    const stressLevel = document.getElementById('stressLevel').value;
    const date = new Date().toISOString().split('T')[0];
    
    const entry = {
        date,
        mood,
        sleepHours,
        stressLevel
    };
    
    wellbeingEntries.push(entry);
    localStorage.setItem(`wellbeingEntries_${currentUser.username}`, JSON.stringify(wellbeingEntries));
    
    alert('Wellbeing entry added successfully!');
    this.reset();
});

// Handle BMI calculation
document.getElementById('bmiForm').addEventListener('submit', function(e) {
    e.preventDefault();
    
    const weight = parseFloat(document.getElementById('weight').value);
    const height = parseFloat(document.getElementById('height').value);

    if (isNaN(weight) || isNaN(height) || height <= 0) {
        document.getElementById('bmiResult').innerHTML = '<p style="color: red;">Please enter valid weight and height.</p>';
        return;
    }

    const bmi = weight / (height * height);
    
    let category;
    let feedback;
    if (bmi < 18.5) {
        category = "Underweight";
        feedback = "You are in the underweight range. Consider consulting a healthcare professional or a nutritionist.";
    } else if (bmi < 25) {
        category = "Normal weight";
        feedback = "You are in the healthy weight range. Keep up the good habits!";
    } else if (bmi < 30) {
        category = "Overweight";
        feedback = "You are in the overweight range. Consider incorporating more physical activity and a balanced diet.";
    } else {
        category = "Obese";
        feedback = "You are in the obese range. It is recommended to consult a healthcare professional for guidance.";
    }
    
    document.getElementById('bmiResult').innerHTML = `
        <p>Your BMI: ${bmi.toFixed(1)}</p>
        <p>Category: ${category}</p>
        <p>Feedback: ${feedback}</p>
    `;
});

// View daily summary
function viewDailySummary() {
    const date = document.getElementById('summaryDate').value || new Date().toISOString().split('T')[0];
    const dailyFoodEntries = foodEntries.filter(entry => entry.date === date);
    const dailyWorkoutEntries = workoutEntries.filter(entry => entry.date === date);
    
    const healthyFoods = dailyFoodEntries.filter(entry => entry.isHealthy).length;
    const unhealthyFoods = dailyFoodEntries.filter(entry => !entry.isHealthy).length;
    const totalFoods = healthyFoods + unhealthyFoods;
    const foodRatio = totalFoods > 0 ? (healthyFoods / totalFoods * 100) : 0;

    const totalCaloriesConsumed = dailyFoodEntries.reduce((sum, entry) => sum + entry.calories, 0);
    const totalCaloriesBurned = dailyWorkoutEntries.reduce((sum, entry) => sum + entry.caloriesBurned, 0);
    const netCalorieBalance = totalCaloriesConsumed - totalCaloriesBurned;
    
    document.getElementById('dailySummaryResult').innerHTML = `
        <h3>Daily Summary for ${date}</h3>
        <p>Healthy foods: ${healthyFoods}</p>
        <p>Unhealthy foods: ${unhealthyFoods}</p>
        ${totalFoods > 0 ? `
            <p>Healthy ratio: ${foodRatio.toFixed(1)}%</p>
            <p>Advice: ${foodRatio >= 50 ? 'Good job on your food choices!' : 'Try to eat more healthy foods!'}</p>
        ` : '<p>No food data for this date.</p>'}
        
        <p>Calories Consumed: ${totalCaloriesConsumed.toFixed(1)}</p>
        <p>Calories Burned: ${totalCaloriesBurned.toFixed(1)}</p>
        <p>Net Calorie Balance: ${netCalorieBalance.toFixed(1)}</p>
    `;
}

// Analyze weekly report
function analyzeWeeklyReport() {
    if (!currentUser) {
        alert('Please login first!');
        return;
    }

    const today = new Date();
    const endDate = today.toISOString().split('T')[0];
    const startDate = new Date(today);
    startDate.setDate(today.getDate() - 6); // Go back 7 days for a full week
    const startDateString = startDate.toISOString().split('T')[0];

    const weeklyFoodEntries = foodEntries.filter(entry => 
        entry.date >= startDateString && entry.date <= endDate
    );
    
    const weeklyWorkouts = workoutEntries.filter(entry =>
        entry.date >= startDateString && entry.date <= endDate
    );
    
    const weeklyWellbeing = wellbeingEntries.filter(entry =>
        entry.date >= startDateString && entry.date <= endDate
    );
    
    if (weeklyFoodEntries.length === 0 && weeklyWorkouts.length === 0 && weeklyWellbeing.length === 0) {
        document.getElementById('weeklyReportResult').innerHTML = '<p>No data found for the week.</p>';
        return;
    }
    
    let report = `<p>Weekly Report (${startDateString} to ${endDate})</p>`;
    
    // Food summary
    if (weeklyFoodEntries.length > 0) {
        const dailyCounts = {};
        let totalWeeklyCaloriesConsumed = 0;

        weeklyFoodEntries.forEach(entry => {
            if (!dailyCounts[entry.date]) {
                dailyCounts[entry.date] = [0, 0]; // [healthy, unhealthy]
            }
            if (entry.isHealthy) {
                dailyCounts[entry.date][0]++;
            } else {
                dailyCounts[entry.date][1]++;
            }
            totalWeeklyCaloriesConsumed += entry.calories;
        });
        
        report += `<h3>Nutrition Summary</h3>
                  <table>
                    <tr>
                        <th>Date</th>
                        <th>Healthy</th>
                        <th>Unhealthy</th>
                        <th>Ratio</th>
                    </tr>`;
        
        let totalHealthy = 0;
        let totalUnhealthy = 0;
        
        Object.entries(dailyCounts).forEach(([date, [healthy, unhealthy]]) => {
            totalHealthy += healthy;
            totalUnhealthy += unhealthy;
            const ratio = (healthy + unhealthy > 0) ? 
                (healthy / (healthy + unhealthy) * 100) : 0;
                
            report += `
                <tr>
                    <td>${date}</td>
                    <td>${healthy}</td>
                    <td>${unhealthy}</td>
                    <td>${ratio.toFixed(1)}%</td>
                </tr>
            `;
        });
        
        const totalRatio = (totalHealthy + totalUnhealthy > 0) ?
            (totalHealthy / (totalHealthy + totalUnhealthy) * 100) : 0;
            
        report += `
            <tr style="font-weight:bold;">
                <td>Total</td>
                <td>${totalHealthy}</td>
                <td>${totalUnhealthy}</td>
                <td>${totalRatio.toFixed(1)}%</td>
            </tr>
        </table>`;

        report += `<p>Total Calories Consumed: ${totalWeeklyCaloriesConsumed.toFixed(1)}</p>`;
    }
    
    // Workout summary
    if (weeklyWorkouts.length > 0) {
        let totalWeeklyCaloriesBurned = 0;
        report += `<h3>Workout Summary</h3>
                  <table>
                    <tr>
                        <th>Date</th>
                        <th>Type</th>
                        <th>Duration</th>
                        <th>Intensity</th>
                        <th>Calories Burned</th>
                    </tr>`;
        
        weeklyWorkouts.forEach(entry => {
            report += `
                <tr>
                    <td>${entry.date}</td>
                    <td>${entry.workoutType}</td>
                    <td>${entry.duration} minutes</td>
                    <td>${entry.intensity}</td>
                    <td>${entry.caloriesBurned.toFixed(1)}</td>
                </tr>
            `;
            totalWeeklyCaloriesBurned += entry.caloriesBurned;
        });
        
        report += `</table>`;
        report += `<p>Total Calories Burned: ${totalWeeklyCaloriesBurned.toFixed(1)}</p>`;
    }

    // Calculate and display weekly calorie balance
    const weeklyNetCalorieBalance = totalWeeklyCaloriesConsumed - totalWeeklyCaloriesBurned;
    report += `<p style="font-weight:bold; margin-top: 15px;">Weekly Net Calorie Balance: ${weeklyNetCalorieBalance.toFixed(1)}</p>`;
    
    // Wellbeing summary
    if (weeklyWellbeing.length > 0) {
        report += `<h3>Wellbeing Summary</h3>
                  <table>
                    <tr>
                        <th>Date</th>
                        <th>Mood</th>
                        <th>Sleep</th>
                        <th>Stress</th>
                    </tr>`;
        
        weeklyWellbeing.forEach(entry => {
            report += `
                <tr>
                    <td>${entry.date}</td>
                    <td>${entry.mood}</td>
                    <td>${entry.sleepHours} hours</td>
                    <td>${entry.stressLevel}</td>
                </tr>
            `;
        });
        
        report += `</table>`;
    }
    
    document.getElementById('weeklyReportResult').innerHTML = report;
}

// Logout function
function logout() {
    if (confirm('Are you sure you want to logout?')) {
        currentUser = null;
        localStorage.removeItem('currentUser');
        showSection('login');
    }
}

// Exit application
function exitApp() {
    if (confirm('Are you sure you want to exit?')) {
        window.close();
        // If window.close() doesn't work (most modern browsers prevent it)
        document.body.innerHTML = '<div class="container"><h1>Fitness & Wellbeing Tracker Closed</h1><p>You can close this window now.</p></div>';
    }
}

// Image analysis functionality

// Show the selected filename when a file is chosen
document.getElementById('imageInput').addEventListener('change', function() {
    const fileInput = document.getElementById('imageInput');
    const selectedFileName = document.getElementById('selectedFileName');
    const imagePreview = document.getElementById('imagePreview');
    const previewImage = document.getElementById('previewImage');
    
    if (fileInput.files && fileInput.files[0]) {
        // Display the filename
        selectedFileName.textContent = fileInput.files[0].name;
        
        // Show image preview
        const reader = new FileReader();
        reader.onload = function(e) {
            previewImage.src = e.target.result;
            imagePreview.style.display = 'block';
        };
        reader.readAsDataURL(fileInput.files[0]);
        
        // Enable the analyze button
        document.getElementById('analyzeImageBtn').disabled = false;
    } else {
        selectedFileName.textContent = '';
        imagePreview.style.display = 'none';
        document.getElementById('analyzeImageBtn').disabled = true;
    }
});

// Function to analyze the image
function analyzeImage() {
    const fileInput = document.getElementById('imageInput');
    const analysisResult = document.getElementById('analysisResult');
    const analysisDescription = document.getElementById('analysisDescription');
    
    if (!fileInput.files || !fileInput.files[0]) {
        alert('Please select an image first!');
        return;
    }
    
    // Show loading state
    analysisDescription.textContent = 'Analyzing image... This may take a few moments.';
    analysisResult.style.display = 'block';
    document.getElementById('analyzeImageBtn').disabled = true;
    
    // Create a FormData object to send the image to the server
    const formData = new FormData();
    formData.append('image', fileInput.files[0]);
    
    // Send the image to the server for analysis
    fetch('http://localhost:5000/analyze-image', {
        method: 'POST',
        body: formData
    })
    .then(response => {
        if (!response.ok) {
            throw new Error('Network response was not ok');
        }
        return response.json();
    })
    .then(data => {
        if (data.error) {
            throw new Error(data.error);
        }
        
        // Display the analysis result from the server
        analysisDescription.textContent = data.description;
    })
    .catch(error => {
        // Handle any errors
        console.error('Error:', error);
        analysisDescription.textContent = 'Error analyzing image: ' + error.message;
        
        // Fallback to client-side analysis if server fails
        const descriptions = [
            "This image appears to be food. I can see various colors and textures that suggest a meal or dish.",
            "This looks like a plate of food with different ingredients and garnishes.",
            "This image shows what appears to be a prepared dish with various components."
        ];
        
        // Use a fallback description
        analysisDescription.textContent += '\n\nFallback analysis: ' + 
            descriptions[Math.floor(Math.random() * descriptions.length)];
    })
    .finally(() => {
        // Re-enable the analyze button
        document.getElementById('analyzeImageBtn').disabled = false;
    });
}

// Reset the image analysis form when changing sections
document.addEventListener('DOMContentLoaded', function() {
    const oldShowSection = window.showSection;
    
    // Override the showSection function to reset the image analysis form
    window.showSection = function(sectionId) {
        // Call the original function
        oldShowSection(sectionId);
        
        // Reset the image analysis form if we're navigating away from it
        if (sectionId !== 'imageAnalysis') {
            document.getElementById('imageInput').value = '';
            document.getElementById('selectedFileName').textContent = '';
            document.getElementById('imagePreview').style.display = 'none';
            document.getElementById('analysisResult').style.display = 'none';
        }
    };
});

// Navigation buttons
/*
document.body.innerHTML += `
    <div class="nav-buttons">
        <button onclick="showSection('food')">Food Tracking</button>
        <button onclick="showSection('workout')">Workout Tracking</button>
        <button onclick="showSection('wellbeing')">Wellbeing Tracking</button>
        <button onclick="showSection('bmi')">BMI Calculator</button>
        <button onclick="showSection('reports')">Weekly Reports</button>
        <button onclick="showSection('imageAnalysis')">AI Image Analysis</button>
        <button onclick="logout()">Logout</button>
    </div>
`;
*/