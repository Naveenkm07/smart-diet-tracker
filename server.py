from flask import Flask, request, jsonify
from flask_cors import CORS
from PIL import Image
import io
import colorsys

app = Flask(__name__)
CORS(app)  # Enable CORS for all routes

def analyze_image_colors(image):
    # Convert image to RGB if it isn't already
    if image.mode != 'RGB':
        image = image.convert('RGB')
    
    # Get image dimensions
    width, height = image.size
    
    # Sample pixels (every 10th pixel to speed up analysis)
    pixels = []
    for x in range(0, width, 10):
        for y in range(0, height, 10):
            pixels.append(image.getpixel((x, y)))
    
    # Calculate average color
    avg_r = sum(p[0] for p in pixels) / len(pixels)
    avg_g = sum(p[1] for p in pixels) / len(pixels)
    avg_b = sum(p[2] for p in pixels) / len(pixels)
    
    # Convert RGB to HSV
    h, s, v = colorsys.rgb_to_hsv(avg_r/255, avg_g/255, avg_b/255)
    
    # Generate description based on colors
    description = []
    
    # Brightness analysis
    if v > 0.8:
        description.append("The image is very bright")
    elif v < 0.3:
        description.append("The image is quite dark")
    
    # Color saturation analysis
    if s > 0.7:
        description.append("with vibrant colors")
    elif s < 0.3:
        description.append("with muted colors")
    
    # Color tone analysis
    if 0.05 <= h <= 0.15:
        description.append("dominated by warm yellow tones")
    elif 0.15 < h <= 0.25:
        description.append("dominated by orange tones")
    elif 0.25 < h <= 0.35:
        description.append("dominated by red tones")
    elif 0.35 < h <= 0.45:
        description.append("dominated by pink tones")
    elif 0.45 < h <= 0.55:
        description.append("dominated by purple tones")
    elif 0.55 < h <= 0.65:
        description.append("dominated by blue tones")
    elif 0.65 < h <= 0.75:
        description.append("dominated by cyan tones")
    elif 0.75 < h <= 0.85:
        description.append("dominated by green tones")
    
    # Image size analysis
    if width * height > 1000000:  # More than 1 megapixel
        description.append("in a high-resolution image")
    elif width * height < 100000:  # Less than 0.1 megapixel
        description.append("in a low-resolution image")
    
    return " ".join(description) if description else "The image has a balanced composition"

@app.route('/analyze-image', methods=['POST'])
def analyze_image():
    try:
        if 'image' not in request.files:
            return jsonify({'error': 'No image file provided'}), 400
        
        file = request.files['image']
        if file.filename == '':
            return jsonify({'error': 'No selected file'}), 400

        # Read and process the image
        image_bytes = file.read()
        image = Image.open(io.BytesIO(image_bytes))
        
        # Analyze the image
        description = analyze_image_colors(image)
        return jsonify({'description': description})

    except Exception as e:
        return jsonify({'error': str(e)}), 500

if __name__ == '__main__':
    app.run(debug=True, port=5000) 