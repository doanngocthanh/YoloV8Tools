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
 * Utility class to parse detection results from JSON output using Jackson
 */
public class DetectionParserNew {
    
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * Parse detection results from raw JSON output using Jackson
     */
    public static List<DetectionResult> parseDetections(String jsonOutput) {
        List<DetectionResult> detections = new ArrayList<>();
        
        try {
            System.out.println("=== DetectionParser.parseDetections (Jackson) ===");
            System.out.println("Input length: " + jsonOutput.length());
            
            // Extract JSON part from output (skip YOLO messages)
            String jsonPart = extractJsonFromOutput(jsonOutput);
            if (jsonPart == null) {
                System.out.println("No JSON found in output");
                return detections;
            }
            
            System.out.println("Extracted JSON: " + jsonPart);
            
            // Parse using Jackson
            YoloInferenceResponse response = objectMapper.readValue(jsonPart, YoloInferenceResponse.class);
            
            // Check for errors
            if (response.getError() != null && !response.getError().isEmpty()) {
                System.out.println("Error in response: " + response.getError());
                return detections;
            }
            
            // Convert DTO to DetectionResult objects
            if (response.getDetections() != null) {
                for (YoloDetection yoloDetection : response.getDetections()) {
                    DetectionResult detection = convertToDetectionResult(yoloDetection);
                    if (detection != null) {
                        detections.add(detection);
                        System.out.println("Converted detection: " + detection);
                    }
                }
            }
            
            System.out.println("Total detections parsed: " + detections.size());
            
        } catch (JsonProcessingException e) {
            System.err.println("JSON parsing error: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("Error parsing detections: " + e.getMessage());
            e.printStackTrace();
        }
        
        return detections;
    }
    
    /**
     * Extract JSON part from YOLO output
     */
    private static String extractJsonFromOutput(String output) {
        if (output == null || output.trim().isEmpty()) {
            return null;
        }
        
        // Find JSON start and end
        int jsonStart = output.indexOf("{");
        int jsonEnd = output.lastIndexOf("}");
        
        if (jsonStart >= 0 && jsonEnd > jsonStart) {
            String jsonPart = output.substring(jsonStart, jsonEnd + 1);
            System.out.println("Extracted JSON part: " + jsonPart);
            return jsonPart;
        }
        
        System.out.println("No valid JSON found in output");
        return null;
    }
    
    /**
     * Convert YoloDetection DTO to DetectionResult
     */
    private static DetectionResult convertToDetectionResult(YoloDetection yoloDetection) {
        try {
            // Extract bbox coordinates
            List<Double> bboxList = yoloDetection.getBbox();
            if (bboxList == null || bboxList.size() != 4) {
                System.err.println("Invalid bbox format: " + bboxList);
                return null;
            }
            
            double x = bboxList.get(0);
            double y = bboxList.get(1);
            double width = bboxList.get(2);
            double height = bboxList.get(3);
            
            BoundingBox bbox = new BoundingBox(x, y, width, height);
            
            String className = yoloDetection.getClassName();
            if (className == null || className.isEmpty()) {
                className = "Class_" + yoloDetection.getClassId();
            }
            
            return new DetectionResult(
                yoloDetection.getClassId(),
                className,
                yoloDetection.getConfidence(),
                bbox
            );
            
        } catch (Exception e) {
            System.err.println("Error converting YoloDetection: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Parse simple text format output (fallback)
     * Example: "Detection 1: Class ID: 0, Confidence: 91.15%, Bounding Box: [175.0, 389.2, 570.7, 479.0]"
     */
    public static List<DetectionResult> parseSimpleFormat(String textOutput) {
        List<DetectionResult> detections = new ArrayList<>();
        
        try {
            System.out.println("=== Parsing Simple Format ===");
            String[] lines = textOutput.split("\n");
            
            for (String line : lines) {
                if (line.contains("Detection") && line.contains("Class ID") && line.contains("Confidence")) {
                    DetectionResult detection = parseSimpleLine(line);
                    if (detection != null) {
                        detections.add(detection);
                    }
                }
            }
            
            System.out.println("Simple format parsed: " + detections.size() + " detections");
            
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
