package raven.yolo.utils;

import raven.yolo.model.DetectionResult;
import raven.yolo.model.DetectionResult.BoundingBox;
import raven.yolo.model.dto.YoloInferenceResponse;
import raven.yolo.model.dto.YoloDetection;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class to parse detection results from JSON output
 */
public class DetectionParser {
      /**
     * Parse detection results from raw JSON output
     */
    public static List<DetectionResult> parseDetections(String jsonOutput) {
        List<DetectionResult> detections = new ArrayList<>();
        
        try {
            System.out.println("=== DetectionParser.parseDetections ===");
            System.out.println("Input: " + jsonOutput);
            
            // Find the detections array in JSON
            String detectionsArray = extractDetectionsArray(jsonOutput);
            System.out.println("Extracted detections array: " + detectionsArray);
            
            if (detectionsArray == null) {
                System.out.println("No detections array found");
                return detections;
            }
            
            // Parse each detection object
            List<String> detectionObjects = extractDetectionObjects(detectionsArray);
            System.out.println("Found " + detectionObjects.size() + " detection objects");
            
            for (int i = 0; i < detectionObjects.size(); i++) {
                String detectionObj = detectionObjects.get(i);
                System.out.println("Parsing detection object " + i + ": " + detectionObj);
                
                DetectionResult detection = parseDetectionObject(detectionObj);
                if (detection != null) {
                    detections.add(detection);
                    System.out.println("Successfully parsed detection: " + detection);
                } else {
                    System.out.println("Failed to parse detection object " + i);
                }
            }
            
        } catch (Exception e) {
            System.err.println("Error parsing detections: " + e.getMessage());
            e.printStackTrace();
        }
        
        System.out.println("Total detections parsed: " + detections.size());
        return detections;
    }
      /**
     * Extract the detections array from JSON
     */
    private static String extractDetectionsArray(String jsonOutput) {
        try {
            System.out.println("Extracting detections array from: " + jsonOutput);
            
            // First find the JSON part (skip YOLO output messages)
            int jsonStart = jsonOutput.indexOf("{");
            if (jsonStart == -1) {
                System.out.println("No JSON found in output");
                return null;
            }
            
            // Find the last closing brace to get complete JSON
            int jsonEnd = jsonOutput.lastIndexOf("}");
            if (jsonEnd == -1 || jsonEnd <= jsonStart) {
                System.out.println("No complete JSON found");
                return null;
            }
            
            String jsonPart = jsonOutput.substring(jsonStart, jsonEnd + 1);
            System.out.println("Extracted JSON part: " + jsonPart);            // Skip regex, use manual extraction directly
            System.out.println("Using manual extraction for detections array");
            
            // Manual approach: find the array boundaries
            int detectionsStart = jsonPart.indexOf("\"detections\":");
            if (detectionsStart != -1) {
                int arrayStart = jsonPart.indexOf("[", detectionsStart);
                if (arrayStart != -1) {
                    int bracketCount = 1;
                    int pos = arrayStart + 1;
                    
                    System.out.println("Starting bracket counting from position: " + pos);
                    
                    while (pos < jsonPart.length() && bracketCount > 0) {
                        char c = jsonPart.charAt(pos);
                        if (c == '[') {
                            bracketCount++;
                            System.out.println("Found '[' at " + pos + ", count: " + bracketCount);
                        } else if (c == ']') {
                            bracketCount--;
                            System.out.println("Found ']' at " + pos + ", count: " + bracketCount);
                        }
                        pos++;
                    }
                    
                    if (bracketCount == 0) {
                        String extractedArray = jsonPart.substring(arrayStart + 1, pos - 1);
                        System.out.println("Manual extraction successful. Length: " + extractedArray.length());
                        System.out.println("Manual extraction result: " + extractedArray);
                        return extractedArray;
                    } else {
                        System.out.println("Manual extraction failed - unmatched brackets. Final count: " + bracketCount);
                    }
                } else {
                    System.out.println("Could not find '[' after detections");
                }
            } else {
                System.out.println("Could not find 'detections:' in JSON");
            }
            
            return null;
            
        } catch (Exception e) {
            System.err.println("Error extracting detections array: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
      /**
     * Extract individual detection objects from the array
     */
    private static List<String> extractDetectionObjects(String detectionsArray) {
        List<String> objects = new ArrayList<>();
        
        try {
            System.out.println("Extracting detection objects from: " + detectionsArray);
            
            if (detectionsArray == null || detectionsArray.trim().isEmpty()) {
                System.out.println("Empty detections array");
                return objects;
            }
            
            String trimmed = detectionsArray.trim();
            
            // If it's a single object (doesn't start and end with [])
            if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
                System.out.println("Single detection object found");
                objects.add(trimmed);
                return objects;
            }
            
            // Multiple objects - parse with brace counting
            int braceCount = 0;
            int start = -1;
            
            for (int i = 0; i < trimmed.length(); i++) {
                char c = trimmed.charAt(i);
                
                if (c == '{') {
                    if (braceCount == 0) {
                        start = i;
                    }
                    braceCount++;
                } else if (c == '}') {
                    braceCount--;
                    if (braceCount == 0 && start != -1) {
                        String obj = trimmed.substring(start, i + 1);
                        System.out.println("Found detection object: " + obj);
                        objects.add(obj);
                        start = -1;
                    }
                }
            }
            
            System.out.println("Total detection objects extracted: " + objects.size());
            
        } catch (Exception e) {
            System.err.println("Error extracting detection objects: " + e.getMessage());
            e.printStackTrace();
        }
        
        return objects;
    }
      /**
     * Parse a single detection object
     */
    private static DetectionResult parseDetectionObject(String detectionObj) {
        try {
            System.out.println("Parsing detection object: " + detectionObj);
            
            // Extract class_id
            int classId = extractIntValue(detectionObj, "class_id");
            System.out.println("Extracted class_id: " + classId);
            
            // Extract class_name
            String className = extractStringValue(detectionObj, "class_name");
            System.out.println("Extracted class_name: " + className);
            
            // Extract confidence
            double confidence = extractDoubleValue(detectionObj, "confidence");
            System.out.println("Extracted confidence: " + confidence);
            
            // Extract bounding box
            BoundingBox bbox = extractBoundingBox(detectionObj);
            System.out.println("Extracted bbox: " + bbox);
            
            if (bbox != null) {
                DetectionResult result = new DetectionResult(classId, className, confidence, bbox);
                System.out.println("Created DetectionResult: " + result);
                return result;
            } else {
                System.out.println("Failed to extract bounding box");
            }
            
        } catch (Exception e) {
            System.err.println("Error parsing detection object: " + e.getMessage());
            e.printStackTrace();
        }
        
        return null;
    }
    
    /**
     * Extract integer value from JSON object
     */
    private static int extractIntValue(String json, String key) {
        Pattern pattern = Pattern.compile("\"" + key + "\"\\s*:\\s*(\\d+)");
        Matcher matcher = pattern.matcher(json);
        
        if (matcher.find()) {
            return Integer.parseInt(matcher.group(1));
        }
        
        return 0;
    }
    
    /**
     * Extract string value from JSON object
     */
    private static String extractStringValue(String json, String key) {
        Pattern pattern = Pattern.compile("\"" + key + "\"\\s*:\\s*\"([^\"]+)\"");
        Matcher matcher = pattern.matcher(json);
        
        if (matcher.find()) {
            return matcher.group(1);
        }
        
        return "Unknown";
    }
    
    /**
     * Extract double value from JSON object
     */
    private static double extractDoubleValue(String json, String key) {
        Pattern pattern = Pattern.compile("\"" + key + "\"\\s*:\\s*([0-9.]+)");
        Matcher matcher = pattern.matcher(json);
        
        if (matcher.find()) {
            return Double.parseDouble(matcher.group(1));
        }
        
        return 0.0;
    }
    
    /**
     * Extract bounding box from JSON object
     */
    private static BoundingBox extractBoundingBox(String json) {
        // Look for "bbox": [x, y, width, height]
        Pattern pattern = Pattern.compile("\"bbox\"\\s*:\\s*\\[\\s*([0-9.]+)\\s*,\\s*([0-9.]+)\\s*,\\s*([0-9.]+)\\s*,\\s*([0-9.]+)\\s*\\]");
        Matcher matcher = pattern.matcher(json);
        
        if (matcher.find()) {
            double x = Double.parseDouble(matcher.group(1));
            double y = Double.parseDouble(matcher.group(2));
            double width = Double.parseDouble(matcher.group(3));
            double height = Double.parseDouble(matcher.group(4));
            
            return new BoundingBox(x, y, width, height);
        }
        
        return null;
    }
    
    /**
     * Parse simple text format output
     * Example: "Detection 1: Class ID: 0, Confidence: 91.15%, Bounding Box: [175.0, 389.2, 570.7, 479.0]"
     */
    public static List<DetectionResult> parseSimpleFormat(String textOutput) {
        List<DetectionResult> detections = new ArrayList<>();
        
        try {
            String[] lines = textOutput.split("\n");
            
            for (String line : lines) {
                if (line.contains("Detection") && line.contains("Class ID") && line.contains("Confidence")) {
                    DetectionResult detection = parseSimpleLine(line);
                    if (detection != null) {
                        detections.add(detection);
                    }
                }
            }
            
        } catch (Exception e) {
            System.err.println("Error parsing simple format: " + e.getMessage());
        }
        
        return detections;
    }
    
    /**
     * Parse a single line of simple format
     */
    private static DetectionResult parseSimpleLine(String line) {
        try {
            // Extract class ID
            Pattern classPattern = Pattern.compile("Class ID:\\s*(\\d+)");
            Matcher classMatcher = classPattern.matcher(line);
            int classId = 0;
            if (classMatcher.find()) {
                classId = Integer.parseInt(classMatcher.group(1));
            }
            
            // Extract confidence
            Pattern confPattern = Pattern.compile("Confidence:\\s*([0-9.]+)%");
            Matcher confMatcher = confPattern.matcher(line);
            double confidence = 0.0;
            if (confMatcher.find()) {
                confidence = Double.parseDouble(confMatcher.group(1)) / 100.0;
            }
            
            // Extract bounding box
            Pattern bboxPattern = Pattern.compile("Bounding Box:\\s*\\[([0-9.]+),\\s*([0-9.]+),\\s*([0-9.]+),\\s*([0-9.]+)\\]");
            Matcher bboxMatcher = bboxPattern.matcher(line);
            BoundingBox bbox = null;
            if (bboxMatcher.find()) {
                double x = Double.parseDouble(bboxMatcher.group(1));
                double y = Double.parseDouble(bboxMatcher.group(2));
                double width = Double.parseDouble(bboxMatcher.group(3));
                double height = Double.parseDouble(bboxMatcher.group(4));
                bbox = new BoundingBox(x, y, width, height);
            }
            
            if (bbox != null) {
                return new DetectionResult(classId, "Class_" + classId, confidence, bbox);
            }
            
        } catch (Exception e) {
            System.err.println("Error parsing line: " + line + " - " + e.getMessage());
        }
        
        return null;
    }
}
