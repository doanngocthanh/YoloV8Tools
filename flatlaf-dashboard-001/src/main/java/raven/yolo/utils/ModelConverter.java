package raven.yolo.utils;

import raven.yolo.training.PythonSetupManager;
import raven.yolo.manager.WorkspaceManager;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for converting YOLO models to different formats
 */
public class ModelConverter {
    
    public enum ModelFormat {
        PYTORCH(".pt"),
        ONNX(".onnx"),
        TENSORRT(".engine"),
        TORCHSCRIPT(".torchscript");
        
        private final String extension;
        
        ModelFormat(String extension) {
            this.extension = extension;
        }
        
        public String getExtension() {
            return extension;
        }
    }    /**
     * Convert a PyTorch model to ONNX format using project's Python environment
     */
    public static boolean convertToOnnx(String modelPath, String outputPath, String projectPath) throws IOException {
        System.out.println("=== ModelConverter.convertToOnnx ===");
        System.out.println("Model path: " + modelPath);
        System.out.println("Output path: " + outputPath);
        System.out.println("Project path: " + projectPath);
        
        File modelFile = new File(modelPath);
        if (!modelFile.exists()) {
            throw new IOException("Model file not found: " + modelPath);
        }
          // Get Python command for the project
        List<String> command = getPythonCommand(projectPath);
        System.out.println("Python command: " + command);        String pythonScript = String.format(
            "import sys\n" +
            "import os\n" +
            "import ssl\n" +
            "try:\n" +
            "    # Set SSL context to bypass certificate verification\n" +
            "    ssl._create_default_https_context = ssl._create_unverified_context\n" +
            "    \n" +
            "    # Use Ultralytics directly - it will auto-install dependencies\n" +
            "    from ultralytics import YOLO\n" +
            "    print('Loading model...')\n" +
            "    model = YOLO('%s')\n" +
            "    print('Starting ONNX export...')\n" +
            "    model.export(format='onnx', dynamic=True, simplify=True)\n" +
            "    print('ONNX export completed successfully')\n" +
            "except Exception as e:\n" +
            "    print(f'Error during ONNX export: {e}')\n" +
            "    import traceback\n" +
            "    traceback.print_exc()\n" +
            "    sys.exit(1)",
            modelPath.replace("\\", "/")
        );
          return executeCommandWithScript(command, pythonScript, new File(modelFile.getParent()));
    }
    
    /**
     * Convert a PyTorch model to ONNX format (deprecated - use version with projectPath)
     */
    @Deprecated
    public static boolean convertToOnnx(String modelPath, String outputPath) throws IOException {
        return convertToOnnx(modelPath, outputPath, getCurrentProjectPath());
    }
      /**
     * Convert a PyTorch model to TensorRT format using project's Python environment
     */
    public static boolean convertToTensorRT(String modelPath, String outputPath, String projectPath) throws IOException {
        File modelFile = new File(modelPath);
        if (!modelFile.exists()) {
            throw new IOException("Model file not found: " + modelPath);
        }        List<String> command = getPythonCommand(projectPath);
          String pythonScript = String.format(
            "import sys\n" +
            "import subprocess\n" +
            "try:\n" +
            "    # TensorRT export typically requires CUDA and TensorRT libraries\n" +
            "    from ultralytics import YOLO\n" +
            "    model = YOLO('%s')\n" +
            "    model.export(format='engine', dynamic=True, workspace=4)\n" +
            "    print('TensorRT export completed successfully')\n" +
            "except Exception as e:\n" +
            "    print(f'Error during TensorRT export: {{e}}')\n" +
            "    print('Note: TensorRT export requires CUDA and TensorRT to be installed')\n" +
            "    sys.exit(1)",
            modelPath.replace("\\", "/")
        );
          return executeCommandWithScript(command, pythonScript, new File(modelFile.getParent()));
    }
    
    /**
     * Convert a PyTorch model to TensorRT format (deprecated - use version with projectPath)
     */
    @Deprecated
    public static boolean convertToTensorRT(String modelPath, String outputPath) throws IOException {
        return convertToTensorRT(modelPath, outputPath, getCurrentProjectPath());
    }
      /**
     * Convert a PyTorch model to TorchScript format using project's Python environment
     */
    public static boolean convertToTorchScript(String modelPath, String outputPath, String projectPath) throws IOException {
        File modelFile = new File(modelPath);
        if (!modelFile.exists()) {
            throw new IOException("Model file not found: " + modelPath);
        }        List<String> command = getPythonCommand(projectPath);
          String pythonScript = String.format(
            "import sys\n" +
            "try:\n" +
            "    from ultralytics import YOLO\n" +
            "    model = YOLO('%s')\n" +
            "    model.export(format='torchscript')\n" +
            "    print('TorchScript export completed successfully')\n" +
            "except Exception as e:\n" +
            "    print(f'Error during TorchScript export: {{e}}')\n" +
            "    sys.exit(1)",
            modelPath.replace("\\", "/")
        );
          return executeCommandWithScript(command, pythonScript, new File(modelFile.getParent()));
    }
    
    /**
     * Convert a PyTorch model to TorchScript format (deprecated - use version with projectPath)
     */
    @Deprecated
    public static boolean convertToTorchScript(String modelPath, String outputPath) throws IOException {
        return convertToTorchScript(modelPath, outputPath, getCurrentProjectPath());
    }
    
    /**
     * Get available conversion formats for a given model
     */
    public static List<ModelFormat> getAvailableConversions(String modelPath) {
        List<ModelFormat> available = new ArrayList<>();
        
        if (modelPath.toLowerCase().endsWith(".pt")) {
            available.add(ModelFormat.ONNX);
            available.add(ModelFormat.TENSORRT);
            available.add(ModelFormat.TORCHSCRIPT);
        }
        
        return available;
    }
    
    /**
     * Check if a model format is supported for inference
     */
    public static boolean isInferenceSupported(String modelPath) {
        String lower = modelPath.toLowerCase();
        return lower.endsWith(".pt") || lower.endsWith(".onnx") || 
               lower.endsWith(".engine") || lower.endsWith(".torchscript");
    }
      /**
     * Run inference on an image using a model
     */
    public static String runInference(String modelPath, String imagePath, double confidence, String projectPath) throws IOException {
        File modelFile = new File(modelPath);
        File imageFile = new File(imagePath);
        
        if (!modelFile.exists()) {
            throw new IOException("Model file not found: " + modelPath);
        }
        if (!imageFile.exists()) {
            throw new IOException("Image file not found: " + imagePath);
        }        List<String> command = getPythonCommand(projectPath);
        
        // Create a proper multi-line Python script
        String pythonScript = String.format(
            "import json\n" +
            "from ultralytics import YOLO\n" +
            "try:\n" +
            "    model = YOLO('%s')\n" +
            "    results = model('%s', conf=%.2f)\n" +
            "    detections = []\n" +
            "    for r in results:\n" +
            "        if hasattr(r, 'boxes') and r.boxes is not None:\n" +
            "            for i in range(len(r.boxes)):\n" +
            "                box = r.boxes[i]\n" +
            "                x1, y1, x2, y2 = box.xyxy[0].tolist()\n" +
            "                x, y, w, h = x1, y1, x2-x1, y2-y1\n" +
            "                det = {\n" +
            "                    'class_id': int(box.cls.item()),\n" +
            "                    'class_name': model.names[int(box.cls.item())] if hasattr(model, 'names') else f'class_{int(box.cls.item())}',\n" +
            "                    'confidence': float(box.conf.item()),\n" +
            "                    'bbox': [x, y, w, h]\n" +
            "                }\n" +
            "                detections.append(det)\n" +
            "    result = {'detections': detections, 'image_shape': list(results[0].orig_shape)}\n" +
            "    print(json.dumps(result))\n" +
            "except Exception as e:\n" +
            "    error_result = {'error': str(e), 'detections': []}\n" +
            "    print(json.dumps(error_result))",
            modelPath.replace("\\", "/"),
            imagePath.replace("\\", "/"),
            confidence        );
        
        System.out.println("=== runInference ===");
        System.out.println("Model path: " + modelPath);
        System.out.println("Image path: " + imagePath);
        System.out.println("Confidence: " + confidence);
        System.out.println("Project path: " + projectPath);
        
        // Create temporary Python script file
        File tempScript = File.createTempFile("yolo_inference_", ".py");
        tempScript.deleteOnExit();
        
        System.out.println("Created temp script: " + tempScript.getAbsolutePath());
          
        // Write script to temporary file
        try (java.io.FileWriter writer = new java.io.FileWriter(tempScript)) {
            writer.write(pythonScript);
        }
        
        System.out.println("Written Python script to temp file:");
        System.out.println("--- Script Content ---");
        System.out.println(pythonScript);
        System.out.println("--- End Script ---");
        
        // Add script file path to command
        command.add(tempScript.getAbsolutePath());
        
        System.out.println("Final command: " + command);
        
        try {
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.directory(new File(getCurrentProjectPath()));
            pb.redirectErrorStream(true);
            
            System.out.println("Starting inference process...");
            Process process = pb.start();
            
            // Read output with timeout
            java.util.concurrent.CompletableFuture<String> outputFuture = java.util.concurrent.CompletableFuture.supplyAsync(() -> {
                StringBuilder output = new StringBuilder();
                try (java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        System.out.println("Inference output: " + line);
                        output.append(line).append("\n");
                    }
                } catch (Exception e) {
                    System.err.println("Error reading inference output: " + e.getMessage());
                }
                return output.toString();
            });
            
            // Wait for process with timeout (3 minutes)
            boolean finished = process.waitFor(3, java.util.concurrent.TimeUnit.MINUTES);
            
            if (!finished) {
                System.err.println("Inference timed out after 3 minutes, destroying...");
                process.destroyForcibly();
                throw new IOException("Inference timed out after 3 minutes");
            }
            
            int exitCode = process.exitValue();
            String output = outputFuture.get(10, java.util.concurrent.TimeUnit.SECONDS);
            
            System.out.println("Inference finished with exit code: " + exitCode);
            System.out.println("Inference output:");
            System.out.println(output);
            
            if (exitCode != 0) {
                throw new IOException("Inference failed with exit code: " + exitCode + "\nOutput: " + output);
            }
              return output;
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Inference was interrupted", e);
        } catch (java.util.concurrent.TimeoutException e) {
            throw new IOException("Timeout waiting for inference output", e);
        } catch (java.util.concurrent.ExecutionException e) {
            throw new IOException("Error getting inference output", e);
        } finally {
            // Clean up temporary file
            boolean deleted = tempScript.delete();
            System.out.println("Temp inference script deleted: " + deleted);
        }
    }/**
     * Get Python command for a specific project
     */
    private static List<String> getPythonCommand(String projectPath) {
        List<String> command = new ArrayList<>();
        
        try {
            System.out.println("=== ModelConverter.getPythonCommand ===");
            System.out.println("Project path: " + projectPath);
            
            // Use PythonSetupManager singleton to get proper Python command
            PythonSetupManager pythonSetup = PythonSetupManager.getInstance();
            
            // Extract project ID from project path
            String projectId = new File(projectPath).getName();
            String workspacePath = new File(projectPath).getParent();
            
            System.out.println("Extracted project ID: " + projectId);
            System.out.println("Extracted workspace path: " + workspacePath);
            
            // Set current project in PythonSetupManager
            pythonSetup.setCurrentProject(projectId, workspacePath);
            
            // Force refresh to ensure we get the project environment
            pythonSetup.reset();
            
            String pythonCommand = pythonSetup.getPythonCommand();
            System.out.println("PythonSetupManager returned command: " + pythonCommand);
            
            if (pythonCommand != null) {
                // Check if this is a virtual environment python
                if (pythonCommand.contains("venv_" + projectId) || pythonCommand.contains("Scripts")) {
                    // Direct path to venv python
                    System.out.println("Using venv python directly: " + pythonCommand);
                    command.add(pythonCommand);
                } else {
                    // System python - need to check if we should activate venv
                    String venvPath = pythonSetup.getCurrentVenvPath();
                    System.out.println("Current venv path: " + venvPath);
                    System.out.println("Has project virtual environment: " + pythonSetup.hasProjectVirtualEnvironment());
                      if (venvPath != null && pythonSetup.hasProjectVirtualEnvironment()) {
                        // Use project-specific venv python directly
                        String venvPython = venvPath + File.separator + "Scripts" + File.separator + "python.exe";
                        System.out.println("Using project-specific venv python: " + venvPython);
                        command.add(venvPython);
                    } else {
                        // Try to find any available virtual environment in the workspace
                        String fallbackVenv = findAvailableVirtualEnvironment(workspacePath);
                        if (fallbackVenv != null) {
                            String fallbackPython = fallbackVenv + File.separator + "Scripts" + File.separator + "python.exe";
                            System.out.println("Using fallback venv python: " + fallbackPython);
                            command.add(fallbackPython);
                        } else {
                            // Use system python
                            System.out.println("Using system python: " + pythonCommand);
                            command.add(pythonCommand);
                        }
                    }
                }
            } else {
                // Fallback to system python
                System.out.println("No python command found, using fallback: python");
                command.add("python");
            }
            
            System.out.println("Final command: " + command);
            
        } catch (Exception e) {
            // Fallback to system python
            System.err.println("Failed to get project Python command, using system python: " + e.getMessage());
            e.printStackTrace();
            command.add("python");
        }
        
        return command;
    }
    
    /**
     * Get current project path from WorkspaceManager
     */
    private static String getCurrentProjectPath() {
        try {
            return WorkspaceManager.getInstance().getCurrentWorkspacePath();
        } catch (Exception e) {
            // Fallback to default workspace
            return System.getProperty("user.home") + File.separator + "YoloV8Workspace";
        }
    }    /**
     * Execute command with script using temporary file
     */
    private static boolean executeCommandWithScript(List<String> command, String pythonScript, File workingDir) {
        File tempScript = null;
        try {
            System.out.println("=== executeCommandWithScript ===");
            
            // Create temporary Python script file
            tempScript = File.createTempFile("yolo_script_", ".py");
            tempScript.deleteOnExit();
            
            System.out.println("Created temp script: " + tempScript.getAbsolutePath());
            
            // Write script to temporary file
            try (java.io.FileWriter writer = new java.io.FileWriter(tempScript)) {
                writer.write(pythonScript);
            }
            
            System.out.println("Written Python script to temp file:");
            System.out.println("--- Script Content ---");
            System.out.println(pythonScript);
            System.out.println("--- End Script ---");
            
            // Add script file path to command
            command.add(tempScript.getAbsolutePath());
            
            System.out.println("Final command: " + command);
            System.out.println("Working directory: " + workingDir.getAbsolutePath());
            
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.directory(workingDir);
            pb.redirectErrorStream(true);
            
            System.out.println("Starting process...");
            Process process = pb.start();
            
            // Read process output in real-time
            java.util.concurrent.CompletableFuture<String> outputFuture = java.util.concurrent.CompletableFuture.supplyAsync(() -> {
                StringBuilder output = new StringBuilder();
                try (java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        System.out.println("Process output: " + line);
                        output.append(line).append("\n");
                    }
                } catch (Exception e) {
                    System.err.println("Error reading process output: " + e.getMessage());
                }
                return output.toString();
            });
              // Wait for process with timeout (10 minutes for dependency installation)
            boolean finished = process.waitFor(10, java.util.concurrent.TimeUnit.MINUTES);
            
            if (!finished) {
                System.err.println("Process timed out after 10 minutes, destroying...");
                process.destroyForcibly();
                return false;
            }
            
            int exitCode = process.exitValue();
            String output = outputFuture.get(10, java.util.concurrent.TimeUnit.SECONDS);
            
            System.out.println("Process finished with exit code: " + exitCode);
            System.out.println("Process output:");
            System.out.println(output);
            
            return exitCode == 0;
        } catch (Exception e) {
            System.err.println("Error executing command: " + e.getMessage());
            e.printStackTrace();
            return false;
        } finally {
            if (tempScript != null) {
                boolean deleted = tempScript.delete();
                System.out.println("Temp script deleted: " + deleted);
            }
        }
    }/**
     * Find any available virtual environment in the workspace
     */
    private static String findAvailableVirtualEnvironment(String workspacePath) {
        try {
            File workspaceDir = new File(workspacePath);
            if (!workspaceDir.exists() || !workspaceDir.isDirectory()) {
                return null;
            }
            
            // Look for any directory that starts with "venv_"
            File[] venvDirs = workspaceDir.listFiles(file -> 
                file.isDirectory() && file.getName().startsWith("venv_"));
            
            if (venvDirs != null && venvDirs.length > 0) {
                // Use the first available venv
                String venvPath = venvDirs[0].getAbsolutePath();
                System.out.println("Found available venv: " + venvPath);
                
                // Check if python.exe exists in this venv
                File pythonExe = new File(venvPath, "Scripts" + File.separator + "python.exe");
                if (pythonExe.exists()) {
                    System.out.println("Verified python.exe exists in venv: " + pythonExe.getAbsolutePath());
                    return venvPath;
                } else {
                    System.out.println("python.exe not found in venv: " + pythonExe.getAbsolutePath());
                }
            }
            
            return null;
        } catch (Exception e) {
            System.err.println("Error finding available virtual environment: " + e.getMessage());
            return null;
        }
    }
}
