package raven.yolo.training;

import java.io.*;
import java.nio.file.*;
import java.util.concurrent.TimeUnit;

/**
 * Python Setup Manager
 * Quản lý việc cài đặt và kiểm tra Python environment
 * 
 * Features:
 * - SSL certificate bypass for corporate environments
 * - Multiple installation fallback methods
 * - Support for proxy/firewall environments
 * - Virtual environment management
 * 
 * SSL Bypass Options:
 * - Trusted hosts for PyPI domains
 * - Environment variables to disable SSL verification
 * - Multiple retry strategies for network issues
 */
public class PythonSetupManager {

    private static PythonSetupManager instance;
    private String pythonCommand = null;
    private boolean ultralyticInstalled = false;
    private String currentProjectId = null;
    private String currentVenvPath = null;

    private PythonSetupManager() {
    }

    public static PythonSetupManager getInstance() {
        if (instance == null) {
            instance = new PythonSetupManager();
        }
        return instance;
    }

    /**
     * Check if Python environment is ready for training
     */
    public boolean isPythonEnvironmentReady() {
        return findPythonCommand() != null && isUltralyticsInstalled();
    }    /**
     * Sanitize project ID for use in directory names
     */
    private String sanitizeProjectId(String projectId) {
        if (projectId == null) {
            return null;
        }
        // Replace spaces and special characters with underscores
        return projectId.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    /**
     * Set current project and switch to its virtual environment
     */
    public void setCurrentProject(String projectId, String workspacePath) {
        if (projectId != null && !projectId.equals(currentProjectId)) {
            currentProjectId = projectId;
            String sanitizedProjectId = sanitizeProjectId(projectId);
            currentVenvPath = Paths.get(workspacePath, "venv_project_" + sanitizedProjectId).toString();
            System.out.println("Switching to project environment: " + projectId);
            System.out.println("Virtual environment path: " + currentVenvPath);

            // Reset Python command to force re-detection with new venv
            pythonCommand = null;
            ultralyticInstalled = false;
        }
    }

    /**
     * Get virtual environment path for current project
     */
    public String getCurrentVenvPath() {
        return currentVenvPath;
    }    /**
     * Check if current project has a virtual environment
     */
    public boolean hasProjectVirtualEnvironment() {
        if (currentVenvPath == null) {
            System.out.println("No virtual environment path set");
            return false;
        }

        Path venvPath = Paths.get(currentVenvPath);
        Path pythonExe = venvPath.resolve("Scripts").resolve("python.exe");
        boolean exists = Files.exists(pythonExe);
        
        System.out.println("Checking virtual environment:");
        System.out.println("  - Venv path: " + currentVenvPath);
        System.out.println("  - Python exe path: " + pythonExe);
        System.out.println("  - Exists: " + exists);
        
        return exists;
    }

    /**
     * Get Python command for current project (considering virtual environment)
     */
    public String getProjectPythonCommand() {
        // First try project virtual environment
        if (hasProjectVirtualEnvironment()) {
            Path venvPath = Paths.get(currentVenvPath);
            Path pythonExe = venvPath.resolve("Scripts").resolve("python.exe");
            if (Files.exists(pythonExe)) {
                return pythonExe.toString();
            }
        }

        // Fallback to system Python
        return findPythonCommand();
    }

    /**
     * Check if ultralytics is installed in current project environment
     */
    public boolean isUltralyticsInstalledInProject() {
        String python = getProjectPythonCommand();
        if (python == null || !hasProjectVirtualEnvironment()) {
            System.out.println("[ERROR] Project virtual environment not found or python command is null. Skipping ultralytics verification.");
            return false;
        }
        return verifyUltralyticsInstallation(python, null);
    }

    /**
     * Reset cached Python command and re-scan for Python installations
     */
    public void refreshPythonEnvironment() {
        // Only refresh if needed, avoid excessive calls
        if (pythonCommand != null) {
            System.out.println("Python environment already detected: " + pythonCommand);
            return;
        }

        pythonCommand = null;
        ultralyticInstalled = false;
        System.out.println("Refreshing Python environment...");

        // Force refresh by calling findPythonCommand again
        String foundPython = findPythonCommand();
        if (foundPython != null) {
            System.out.println("Python refreshed successfully: " + foundPython);
            // Also check ultralytics installation
            isUltralyticsInstalled();
        } else {
            System.out.println("No Python found after refresh");
        }
    }

    /**
     * Force refresh even if Python is already detected
     */
    public void forceRefreshPythonEnvironment() {
        pythonCommand = null;
        ultralyticInstalled = false;
        System.out.println("Force refreshing Python environment...");

        String foundPython = findPythonCommand();
        if (foundPython != null) {
            System.out.println("Python refreshed successfully: " + foundPython);

            // Also check project-specific python if available
            String projectPython = getProjectPythonCommand();
            if (projectPython != null && !projectPython.equals(foundPython)) {
                System.out.println("Using project virtual environment: " + projectPython);
            }

            // Check ultralytics in project environment
            boolean hasUltralytics = isUltralyticsInstalledInProject();
            System.out.println("Ultralytics in project: " + hasUltralytics);
        } else {
            System.out.println("No Python found after refresh");
        }
    }

    /**
     * Get detailed Python environment information
     */
    public String getPythonEnvironmentInfo() {
        StringBuilder info = new StringBuilder();

        String python = findPythonCommand();
        if (python != null) {
            try {
                // Get Python version
                ProcessBuilder pb = new ProcessBuilder(python, "--version");
                pb.redirectErrorStream(true);
                Process process = pb.start();

                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                String versionOutput = reader.readLine();
                reader.close();
                process.waitFor(5, TimeUnit.SECONDS);

                info.append("Python Command: ").append(python).append("\n");
                info.append("Version: ").append(versionOutput != null ? versionOutput : "Unknown").append("\n");

                // Get Python executable path
                pb = new ProcessBuilder(python, "-c", "import sys; print(sys.executable)");
                pb.redirectErrorStream(true);
                process = pb.start();

                reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                String pathOutput = reader.readLine();
                reader.close();
                process.waitFor(5, TimeUnit.SECONDS);

                info.append("Executable Path: ").append(pathOutput != null ? pathOutput : "Unknown").append("\n");
                // Check pip
                pb = createPipCommandWithSSLBypass(python, "--version");
                pb.redirectErrorStream(true);
                process = pb.start();

                reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                String pipOutput = reader.readLine();
                reader.close();
                process.waitFor(5, TimeUnit.SECONDS);

                info.append("Pip: ").append(pipOutput != null ? pipOutput : "Not available").append("\n");

            } catch (Exception e) {
                info.append("Error getting Python info: ").append(e.getMessage()).append("\n");
            }
        } else {
            info.append("Python not found in system PATH\n");
        }

        // Check ultralytics
        boolean hasUltralytics = isUltralyticsInstalled();
        info.append("Ultralytics: ").append(hasUltralytics ? "Installed" : "Not installed").append("\n");

        return info.toString();
    }

    /**
     * Find available Python command
     */
    public String findPythonCommand() {
        if (pythonCommand != null) {
            return pythonCommand;
        }

        System.out.println("Searching for Python installations...");

        // First priority: Check project virtual environment
        if (hasProjectVirtualEnvironment()) {
            Path venvPath = Paths.get(currentVenvPath);
            Path pythonExe = venvPath.resolve("Scripts").resolve("python.exe");
            if (Files.exists(pythonExe)) {
                String venvPython = pythonExe.toString();
                String result = testPythonCommand(venvPython);
                if (result != null) {
                    System.out.println("Using project virtual environment: " + venvPython);
                    pythonCommand = result;
                    return pythonCommand;
                }
            }
        }

        // Second priority: Try simple commands that should work if Python is properly
        // in PATH
        String[] basicCommands = { "python", "python3", "py" };

        for (String cmd : basicCommands) {
            String result = testPythonCommand(cmd);
            if (result != null) {
                pythonCommand = result;
                return pythonCommand;
            }
        }

        // Try Windows-specific variations
        String[] windowsCommands = { "python.exe", "python3.exe", "py.exe" };

        for (String cmd : windowsCommands) {
            String result = testPythonCommand(cmd);
            if (result != null) {
                pythonCommand = result;
                return pythonCommand;
            }
        }

        // Use 'where' command on Windows to find Python installations
        String foundPath = findPythonWithWhereCommand();
        if (foundPath != null) {
            pythonCommand = foundPath;
            return pythonCommand;
        }

        // Scan common installation directories
        String scanResult = scanCommonPythonDirectories();
        if (scanResult != null) {
            pythonCommand = scanResult;
            return pythonCommand;
        }

        System.out.println("No compatible Python installation found");
        return null;
    }

    /**
     * Test a specific Python command
     */
    private String testPythonCommand(String cmd) {
        try {
            ProcessBuilder pb = new ProcessBuilder(cmd, "--version");
            pb.redirectErrorStream(true);
            Process process = pb.start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String output = reader.readLine();
            reader.close();

            boolean finished = process.waitFor(10, TimeUnit.SECONDS);
            int exitCode = finished ? process.exitValue() : -1;

            if (exitCode == 0 && output != null && output.toLowerCase().contains("python")) {
                String version = extractPythonVersion(output);
                if (isVersionCompatible(version)) {
                    System.out.println("Found compatible Python: " + cmd + " - " + output);
                    return cmd;
                }
            }

            if (!finished) {
                process.destroyForcibly();
            }
        } catch (Exception e) {
            // Command not found or failed
        }
        return null;
    }

    /**
     * Use Windows 'where' command to find Python
     */
    private String findPythonWithWhereCommand() {
        try {
            ProcessBuilder pb = new ProcessBuilder("where", "python");
            pb.redirectErrorStream(true);
            Process process = pb.start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.endsWith(".exe") && Files.exists(Paths.get(line))) {
                    String result = testPythonCommand(line);
                    if (result != null) {
                        reader.close();
                        process.waitFor(5, TimeUnit.SECONDS);
                        return line;
                    }
                }
            }
            reader.close();
            process.waitFor(5, TimeUnit.SECONDS);

        } catch (Exception e) {
            System.out.println("Failed to use 'where' command: " + e.getMessage());
        }

        // Also try 'where python3' and 'where py'
        for (String pythonCmd : new String[] { "python3", "py" }) {
            try {
                ProcessBuilder pb = new ProcessBuilder("where", pythonCmd);
                pb.redirectErrorStream(true);
                Process process = pb.start();

                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (line.endsWith(".exe") && Files.exists(Paths.get(line))) {
                        String result = testPythonCommand(line);
                        if (result != null) {
                            reader.close();
                            process.waitFor(5, TimeUnit.SECONDS);
                            return line;
                        }
                    }
                }
                reader.close();
                process.waitFor(5, TimeUnit.SECONDS);

            } catch (Exception e) {
                // Continue
            }
        }

        return null;
    }

    /**
     * Scan common Python installation directories
     */
    private String scanCommonPythonDirectories() {
        // Common Python installation paths on Windows
        String[] basePaths = {
                "C:\\Python",
                "C:\\Program Files\\Python",
                "C:\\Program Files (x86)\\Python",
                System.getProperty("user.home") + "\\AppData\\Local\\Programs\\Python",
                System.getProperty("user.home") + "\\AppData\\Local\\Microsoft\\WindowsApps"
        };

        for (String basePath : basePaths) {
            try {
                Path baseDir = Paths.get(basePath);
                if (!Files.exists(baseDir))
                    continue;

                // Look for directories that might contain Python
                Files.list(baseDir)
                        .filter(Files::isDirectory)
                        .forEach(dir -> {
                            String pythonExe = dir.toString() + "\\python.exe";
                            if (Files.exists(Paths.get(pythonExe))) {
                                String result = testPythonCommand(pythonExe);
                                if (result != null && pythonCommand == null) {
                                    pythonCommand = pythonExe;
                                }
                            }
                        });

                if (pythonCommand != null) {
                    return pythonCommand;
                }

            } catch (Exception e) {
                // Continue scanning other directories
            }
        }
        return null;
    }

    /**
     * Check if ultralytics is installed
     */
    public boolean isUltralyticsInstalled() {
        if (currentProjectId != null) {
            return isUltralyticsInstalledInProject();
        }
        if (ultralyticInstalled) {
            return true;
        }
        String python = findPythonCommand();
        if (python == null) {
            System.out.println("[ERROR] No python command found. Skipping ultralytics verification.");
            return false;
        }
        return verifyUltralyticsInstallation(python, null);
    }

    /**
     * Create and setup virtual environment for current project
     */
    public boolean setupProjectVirtualEnvironment(ProgressCallback callback) {
        if (currentProjectId == null || currentVenvPath == null) {
            if (callback != null)
                callback.onError("No project set for virtual environment");
            return false;
        }

        String python = findSystemPython(); // Use system Python, not venv Python
        if (python == null) {
            if (callback != null)
                callback.onError("System Python not found");
            return false;
        }

        try {
            Path venvPath = Paths.get(currentVenvPath);

            // Remove existing venv if it exists
            if (Files.exists(venvPath)) {
                if (callback != null)
                    callback.onProgress("Removing existing virtual environment...", 10);
                deleteDirectory(venvPath);
            }

            // Create virtual environment
            if (callback != null)
                callback.onProgress("Creating virtual environment for project: " + currentProjectId, 20);

            ProcessBuilder pb = new ProcessBuilder(python, "-m", "venv", venvPath.toString(), "--clear");
            pb.redirectErrorStream(true);
            Process process = pb.start();

            StringBuilder output = new StringBuilder();
            captureProcessOutput(process, output);

            int exitCode = process.waitFor(120, TimeUnit.SECONDS) ? process.exitValue() : -1;

            if (exitCode != 0) {
                String errorMsg = "Failed to create virtual environment. Exit code: " + exitCode +
                        "\nOutput: " + output.toString();
                if (callback != null)
                    callback.onError(errorMsg);
                return false;
            }

            // Get venv Python
            String venvPython = getProjectPythonCommand();
            // Upgrade pip
            if (callback != null)
                callback.onProgress("Upgrading pip...", 40);
            pb = createPipCommandWithSSLBypass(venvPython, "install", "--upgrade", "pip");
            pb.redirectErrorStream(true);
            process = pb.start();
            process.waitFor(60, TimeUnit.SECONDS);

            // Install ultralytics in virtual environment (no --user flag needed)
            if (callback != null)
                callback.onProgress("Installing ultralytics in virtual environment...", 60);
            pb = createSecurePipInstallCommand(venvPython, "ultralytics", false, false, false);
            pb.redirectErrorStream(true);
            process = pb.start();

            StringBuilder installOutput = new StringBuilder();
            monitorInstallationProgressWithOutput(process, callback, installOutput);

            exitCode = process.waitFor(300, TimeUnit.SECONDS) ? process.exitValue() : -1;

            if (exitCode == 0) {
                // Verify installation
                if (verifyUltralyticsInstallation(venvPython, callback)) {
                    if (callback != null)
                        callback.onProgress("Project virtual environment setup completed!", 100);
                    // Reset to force re-detection
                    pythonCommand = null;
                    ultralyticInstalled = false;
                    return true;
                } else {
                    if (callback != null)
                        callback.onError("Installation completed but verification failed");
                    return false;
                }
            } else {
                if (callback != null)
                    callback.onError("Failed to install ultralytics. Exit code: " + exitCode +
                            "\nOutput: " + installOutput.toString());
                return false;
            }

        } catch (Exception e) {
            if (callback != null)
                callback.onError("Error setting up virtual environment: " + e.getMessage());
            return false;
        }
    }

    /**
     * Find system Python (not virtual environment)
     */
    private String findSystemPython() {
        String[] commands = { "python", "python3", "py", "python.exe", "python3.exe", "py.exe" };

        for (String cmd : commands) {
            String result = testPythonCommand(cmd);
            if (result != null) {
                return result;
            }
        }

        // Try common installation paths
        String[] paths = {
                "C:\\Python312\\python.exe",
                "C:\\Python311\\python.exe",
                "C:\\Python310\\python.exe",
                "C:\\Users\\" + System.getProperty("user.name")
                        + "\\AppData\\Local\\Programs\\Python\\Python312\\python.exe",
                "C:\\Users\\" + System.getProperty("user.name")
                        + "\\AppData\\Local\\Programs\\Python\\Python311\\python.exe"
        };

        for (String path : paths) {
            if (Files.exists(Paths.get(path))) {
                String result = testPythonCommand(path);
                if (result != null) {
                    return result;
                }
            }
        }

        return null;
    }

    /**
     * Install ultralytics package with multiple fallback methods
     */
    public boolean installUltralytics(ProgressCallback callback) {
        String python = getProjectPythonCommand(); // Use project-specific Python
        if (python == null) {
            if (callback != null)
                callback.onError("Python not found");
            return false;
        }

        // Check if we're in a virtual environment
        boolean inVirtualEnv = hasProjectVirtualEnvironment() && python.contains("venv_");

        // Try different installation methods based on environment
        String[] installMethods;
        if (inVirtualEnv) {
            installMethods = new String[] {
                    "Basic pip install in venv",
                    "Pip install with upgrade in venv",
                    "Pip install minimal version in venv"
            };
        } else {
            installMethods = new String[] {
                    "Pip install with user flag",
                    "Basic pip install",
                    "Pip install with upgrade",
                    "Pip install minimal version"
            };
        }

        for (int i = 0; i < installMethods.length; i++) {
            String method = installMethods[i];
            if (callback != null)
                callback.onProgress("Trying " + method + "...", 10 + (i * 20));

            if (tryInstallUltralyticsMethod(python, i, callback, inVirtualEnv)) {
                ultralyticInstalled = true;
                if (callback != null)
                    callback.onProgress("Ultralytics installed successfully with " + method + "!", 100);
                return true;
            }

            // Wait a bit between attempts
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
            }
        }

        if (callback != null)
            callback.onError("Standard installation methods failed. Trying SSL bypass for corporate environments...");

        // Try SSL bypass method as last resort
        if (installUltralyticsWithSSLBypass(callback)) {
            return true;
        }

        if (callback != null)
            callback.onError("All installation methods failed including SSL bypass. " +
                    "\n\nThis usually indicates:" +
                    "\n1. Corporate firewall/proxy blocking PyPI access" +
                    "\n2. SSL certificate verification issues" +
                    "\n3. Network connectivity problems" +
                    "\n\nPlease contact your IT department or try manual installation.");
        return false;
    }

    /**
     * Try different ultralytics installation methods with SSL bypass
     */
    private boolean tryInstallUltralyticsMethod(String python, int methodIndex, ProgressCallback callback,
            boolean inVirtualEnv) {
        try {
            ProcessBuilder pb;
            switch (methodIndex) {
                case 0:
                    if (inVirtualEnv) {
                        // Basic pip install in venv (no --user flag)
                        pb = createSecurePipInstallCommand(python, "ultralytics", false, false, false);
                    } else {
                        // With user flag (most reliable for system Python)
                        pb = createSecurePipInstallCommand(python, "ultralytics", false, true, false);
                    }
                    break;
                case 1:
                    if (inVirtualEnv) {
                        // With upgrade in venv
                        pb = createSecurePipInstallCommand(python, "ultralytics", true, false, false);
                    } else {
                        // Basic pip install
                        pb = createSecurePipInstallCommand(python, "ultralytics", false, false, false);
                    }
                    break;
                case 2:
                    if (inVirtualEnv) {
                        // Minimal dependencies in venv
                        pb = createSecurePipInstallCommand(python, "ultralytics", false, false, true);
                    } else {
                        // With upgrade
                        pb = createSecurePipInstallCommand(python, "ultralytics", true, false, false);
                    }
                    break;
                case 3: // Only for system Python
                    if (!inVirtualEnv) {
                        // Minimal dependencies
                        pb = createSecurePipInstallCommand(python, "ultralytics", false, false, true);
                    } else {
                        return false; // No case 3 for venv
                    }
                    break;
                default:
                    return false;
            }
            pb.redirectErrorStream(true);
            Process process = pb.start();

            // Monitor output
            StringBuilder output = new StringBuilder();
            monitorInstallationProgressWithOutput(process, callback, output);

            // Increase timeout for large package downloads (torch is 216MB+)
            int exitCode = process.waitFor(600, TimeUnit.SECONDS) ? process.exitValue() : -1;

            // Check if ultralytics core was installed even if process failed
            String outputStr = output.toString();
            boolean coreInstalled = outputStr.contains("Successfully installed ultralytics");

            if (exitCode == 0) {
                // Verify installation
                return verifyUltralyticsInstallation(python, callback);
            } else if (coreInstalled) {
                // Core package installed but dependencies may have failed
                if (callback != null) {
                    callback.onProgress("Ultralytics core installed, verifying functionality...", -1);
                }
                // Try to verify - sometimes core package works without all dependencies
                if (verifyUltralyticsInstallation(python, callback)) {
                    if (callback != null) {
                        callback.onProgress(
                                "Ultralytics installed successfully (some dependencies may need manual installation)",
                                -1);
                    }
                    return true;
                } else {
                    if (callback != null) {
                        callback.onProgress("Core installed but verification failed, trying next method...", -1);
                    }
                    return false;
                }
            } else {
                if (callback != null) {
                    callback.onProgress("Method failed (exit code: " + exitCode + "), trying next...", -1);
                }
                return false;
            }

        } catch (Exception e) {
            if (callback != null) {
                callback.onProgress("Method failed (" + e.getMessage() + "), trying next...", -1);
            }
            return false;
        }
    }

    /**
     * Overload verifyUltralyticsInstallation to accept callback for error dialog
     */
    private boolean verifyUltralyticsInstallation(String python, ProgressCallback callback) {
        if (python == null || (currentVenvPath != null && !hasProjectVirtualEnvironment())) {
            String err = "[ERROR] Python command is null or venv does not exist. Cannot verify ultralytics.";
            System.out.println(err);
            if (callback != null) callback.onError(err);
            return false;
        }
        for (int attempt = 0; attempt < 3; attempt++) {
            try {
                if (attempt > 0) Thread.sleep(2000);
                ProcessBuilder pb = new ProcessBuilder(python, "-c", "import ultralytics; print('SUCCESS:', ultralytics.__version__)");
                pb.redirectErrorStream(true);
                Process process = pb.start();
                StringBuilder output = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) output.append(line).append("\n");
                }
                int exitCode = process.waitFor(15, TimeUnit.SECONDS) ? process.exitValue() : -1;
                String outputStr = output.toString().trim();
                if (exitCode == 0 && outputStr.contains("SUCCESS:")) {
                    System.out.println("Ultralytics verification successful: " + outputStr);
                    return true;
                } else {
                    System.out.println("Verification attempt " + (attempt + 1) + " failed. Exit code: " + exitCode);
                    if (!outputStr.isEmpty()) System.out.println("Output/Error: " + outputStr);

                    // Check for specific known issues
                    if (outputStr.contains("Microsoft Visual C++ Redistributable")) {
                        System.out.println("DETECTED: Missing Visual C++ Redistributable!");
                        showVisualCppRedistributableInfo();
                        return false; // Don't retry for this type of error
                    }

                    // Check for missing dependencies but core ultralytics exists
                    if (outputStr.contains("ModuleNotFoundError")
                            && !outputStr.contains("No module named 'ultralytics'")) {
                        System.out.println("DETECTED: Ultralytics core installed but missing dependencies");
                        // Try a simpler verification - just check if ultralytics module exists
                        if (verifyUltralyticsCore(python)) {
                            System.out.println(
                                    "Ultralytics core is functional despite missing optional dependencies");
                            return true;
                        }
                    }
                }

            } catch (Exception e) {
                System.out.println("Verification attempt " + (attempt + 1) + " error: " + e.getMessage());
            }
        }

        String failMsg = "Ultralytics verification failed after 3 attempts";
        System.out.println(failMsg);
        if (callback != null) callback.onError(failMsg);
        return false;
    }

    /**
     * Verify just the core ultralytics module without dependencies
     */
    private boolean verifyUltralyticsCore(String python) {
        try {
            ProcessBuilder pb = new ProcessBuilder(python, "-c",
                    "try:\n" +
                            "    import ultralytics\n" +
                            "    print('CORE_SUCCESS: ultralytics module found')\n" +
                            "except ImportError as e:\n" +
                            "    print('CORE_FAILED:', str(e))");
            pb.redirectErrorStream(true);
            Process process = pb.start();

            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }

            int exitCode = process.waitFor(10, TimeUnit.SECONDS) ? process.exitValue() : -1;
            String outputStr = output.toString().trim();

            return exitCode == 0 && outputStr.contains("CORE_SUCCESS");

        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Show information about Visual C++ Redistributable requirement
     */
    private void showVisualCppRedistributableInfo() {
        System.out.println("=== VISUAL C++ REDISTRIBUTABLE REQUIRED ===");
        System.out.println("Ultralytics/PyTorch requires Microsoft Visual C++ Redistributable.");
        System.out.println("Please install it from: https://aka.ms/vs/16/release/vc_redist.x64.exe");
        System.out.println("After installation, restart the application and try again.");
        System.out.println("===========================================");
    }

    /**
     * Create virtual environment and install dependencies
     */
    public boolean setupVirtualEnvironment(String workspacePath, ProgressCallback callback) {
        String python = findPythonCommand();
        if (python == null) {
            if (callback != null)
                callback.onError("Python not found");
            return false;
        }

        try {
            Path venvPath = Paths.get(workspacePath, "venv");

            // Remove existing venv if it exists
            if (Files.exists(venvPath)) {
                if (callback != null)
                    callback.onProgress("Removing existing virtual environment...", 10);
                deleteDirectory(venvPath);
            }

            // Create virtual environment
            if (callback != null)
                callback.onProgress("Creating virtual environment...", 20);

            ProcessBuilder pb = new ProcessBuilder(python, "-m", "venv", venvPath.toString(), "--clear");
            pb.redirectErrorStream(true);
            Process process = pb.start();

            // Capture output for debugging
            StringBuilder output = new StringBuilder();
            captureProcessOutput(process, output);

            int exitCode = process.waitFor(120, TimeUnit.SECONDS) ? process.exitValue() : -1;

            if (exitCode != 0) {
                String errorMsg = "Failed to create virtual environment. Exit code: " + exitCode +
                        "\nOutput: " + output.toString() +
                        "\nTry installing Python with 'Add to PATH' option checked.";
                if (callback != null)
                    callback.onError(errorMsg);
                return false;
            }

            // Get Python executable in venv
            String venvPython = getVenvPythonCommand(venvPath);
            // Upgrade pip first
            if (callback != null)
                callback.onProgress("Upgrading pip in virtual environment...", 30);
            pb = createPipCommandWithSSLBypass(venvPython, "install", "--upgrade", "pip");
            pb.redirectErrorStream(true);
            process = pb.start();

            StringBuilder pipOutput = new StringBuilder();
            captureProcessOutput(process, pipOutput);
            exitCode = process.waitFor(120, TimeUnit.SECONDS) ? process.exitValue() : -1;

            if (exitCode != 0) {
                if (callback != null)
                    callback.onProgress("Warning: Failed to upgrade pip: " + pipOutput.toString(), -1);
            }

            // Install ultralytics with retries and different approaches
            if (callback != null)
                callback.onProgress("Installing ultralytics in virtual environment...", 50);

            boolean installSuccess = false;
            String[] installMethods = {
                    "Standard install",
                    "Install with no dependencies first",
                    "Install with increased timeout"
            };

            for (int attempt = 0; attempt < installMethods.length && !installSuccess; attempt++) {
                if (callback != null)
                    callback.onProgress("Trying " + installMethods[attempt] + "...", 60 + attempt * 10);
                switch (attempt) {
                    case 0: // Standard install
                        pb = createSecurePipInstallCommand(venvPython, "ultralytics", false, false, false);
                        break;
                    case 1: // Install core package first, then dependencies
                        pb = createSecurePipInstallCommand(venvPython, "ultralytics", false, false, true);
                        break;
                    case 2: // Install with extended timeout and retries (already included in
                            // createSecurePipInstallCommand)
                        pb = createSecurePipInstallCommand(venvPython, "ultralytics", false, false, false);
                        break;
                }

                pb.redirectErrorStream(true);
                process = pb.start();
                // Monitor installation with timeout
                StringBuilder installOutput = new StringBuilder();
                monitorInstallationProgressWithOutput(process, callback, installOutput);

                exitCode = process.waitFor(900, TimeUnit.SECONDS) ? process.exitValue() : -1; // 15 minutes for large
                                                                                              // packages

                // Check if core package was installed even if exit code != 0
                String outputStr = installOutput.toString();
                boolean coreInstalled = outputStr.contains("Successfully installed ultralytics");

                if (exitCode == 0) {
                    // Verify installation
                    if (verifyUltralyticsInstallation(venvPython, callback)) {
                        installSuccess = true;
                        if (callback != null)
                            callback.onProgress("Virtual environment setup completed successfully!", 100);
                    } else {
                        if (callback != null)
                            callback.onProgress("Installation completed but verification failed, trying next method...",
                                    -1);
                    }
                } else if (coreInstalled) {
                    // Core installed but some dependencies may have failed
                    if (callback != null)
                        callback.onProgress("Ultralytics core installed, checking functionality...", -1);
                    if (verifyUltralyticsInstallation(venvPython, callback) || verifyUltralyticsCore(venvPython)) {
                        installSuccess = true;
                        if (callback != null)
                            callback.onProgress(
                                    "Virtual environment setup completed! (Some optional dependencies may need manual installation)",
                                    90);
                    } else {
                        if (callback != null)
                            callback.onProgress("Core installed but not functional, trying next method...", -1);
                    }
                } else {
                    String errorDetail = "Exit code: " + exitCode + "\nOutput: " + outputStr;
                    if (callback != null)
                        callback.onProgress("Install attempt " + (attempt + 1) + " failed: " + errorDetail, -1);

                    if (attempt == installMethods.length - 1) {
                        // Last attempt failed - check if we should suggest dependency installation
                        if (verifyUltralyticsCore(venvPython)) {
                            if (callback != null)
                                callback.onError("Ultralytics core is installed but some dependencies failed. " +
                                        "You can try using the 'Install Missing Dependencies' option or install manually.\n\n"
                                        + errorDetail);
                        } else {
                            if (callback != null)
                                callback.onError("Failed to install packages. " + errorDetail +
                                        "\n\nPossible solutions:\n" +
                                        "1. Check your internet connection\n" +
                                        "2. Try running as administrator\n" +
                                        "3. Disable antivirus temporarily\n" +
                                        "4. Use manual installation");
                        }
                    }
                }
            }

            return installSuccess;

        } catch (Exception e) {
            if (callback != null)
                callback.onError("Virtual environment setup error: " + e.getMessage());
            return false;
        }
    }

    /**
     * Delete directory recursively
     */
    private void deleteDirectory(Path directory) throws IOException {
        if (Files.exists(directory)) {
            Files.walk(directory)
                    .sorted((path1, path2) -> path2.compareTo(path1)) // Delete files before directories
                    .forEach(path -> {
                        try {
                            Files.delete(path);
                        } catch (IOException e) {
                            // Ignore errors, best effort
                        }
                    });
        }
    }

    /**
     * Get Python command for virtual environment
     */
    private String getVenvPythonCommand(Path venvPath) {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            return venvPath.resolve("Scripts").resolve("python.exe").toString();
        } else {
            return venvPath.resolve("bin").resolve("python").toString();
        }
    }

    /**
     * Extract Python version from output
     */
    private String extractPythonVersion(String output) {
        try {
            String[] parts = output.split(" ");
            for (String part : parts) {
                if (part.matches("\\d+\\.\\d+\\.\\d+")) {
                    return part;
                }
            }
        } catch (Exception e) {
            // Ignore
        }
        return "0.0.0";
    }

    /**
     * Check if Python version is compatible (3.8+)
     */
    private boolean isVersionCompatible(String version) {
        try {
            String[] parts = version.split("\\.");
            int major = Integer.parseInt(parts[0]);
            int minor = Integer.parseInt(parts[1]);

            return major > 3 || (major == 3 && minor >= 8);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Get current Python command
     */
    public String getPythonCommand() {
        return pythonCommand != null ? pythonCommand : findPythonCommand();
    }

    /**
     * Reset cached values
     */
    public void reset() {
        pythonCommand = null;
        ultralyticInstalled = false;
    }

    /**
     * Capture process output for debugging
     */
    private void captureProcessOutput(Process process, StringBuilder output) {
        new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            } catch (Exception e) {
                output.append("Error reading output: ").append(e.getMessage());
            }
        }).start();
    }

    /**
     * Monitor installation progress with detailed output capture
     */
    private void monitorInstallationProgressWithOutput(Process process, ProgressCallback callback,
            StringBuilder output) {
        if (callback == null)
            return;

        new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                int progress = 30;
                while ((line = reader.readLine()) != null && progress < 90) {
                    output.append(line).append("\n");

                    if (line.contains("Downloading") || line.contains("Installing")) {
                        progress = Math.min(progress + 3, 90);
                        callback.onProgress(line, progress);
                    } else if (line.contains("ERROR") || line.contains("Failed")) {
                        callback.onProgress("Error: " + line, -1);
                    } else if (line.contains("Successfully")) {
                        progress = Math.min(progress + 5, 90);
                        callback.onProgress(line, progress);
                    }
                }
            } catch (Exception e) {
                output.append("Error monitoring: ").append(e.getMessage());
            }
        }).start();
    }

    /**
     * Progress callback interface
     */
    public interface ProgressCallback {
        void onProgress(String message, int percentage);

        void onError(String error);
    }

    /**
     * Install Python directly on Windows using winget or chocolatey
     */
    public boolean installPythonOnWindows(ProgressCallback callback) {
        if (callback != null)
            callback.onProgress("Checking for Python installers...", 10);

        // Try winget first (Windows Package Manager)
        if (isWingetAvailable()) {
            return installPythonWithWinget(callback);
        }

        // Try chocolatey
        if (isChocolateyAvailable()) {
            return installPythonWithChocolatey(callback);
        }

        // Fallback to manual download and install
        return downloadAndInstallPython(callback);
    }

    /**
     * Check if winget is available
     */
    private boolean isWingetAvailable() {
        try {
            ProcessBuilder pb = new ProcessBuilder("winget", "--version");
            Process process = pb.start();
            boolean finished = process.waitFor(10, TimeUnit.SECONDS);
            return finished && process.exitValue() == 0;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Check if chocolatey is available
     */
    private boolean isChocolateyAvailable() {
        try {
            ProcessBuilder pb = new ProcessBuilder("choco", "--version");
            Process process = pb.start();
            boolean finished = process.waitFor(10, TimeUnit.SECONDS);
            return finished && process.exitValue() == 0;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Install Python using winget
     */
    private boolean installPythonWithWinget(ProgressCallback callback) {
        try {
            if (callback != null)
                callback.onProgress("Installing Python using Windows Package Manager...", 20);

            ProcessBuilder pb = new ProcessBuilder("winget", "install", "Python.Python.3.12",
                    "--accept-package-agreements", "--accept-source-agreements");
            pb.redirectErrorStream(true);
            Process process = pb.start();

            // Monitor progress
            StringBuilder output = new StringBuilder();
            monitorInstallationProgressWithOutput(process, callback, output);

            boolean finished = process.waitFor(300, TimeUnit.SECONDS); // 5 minutes timeout
            int exitCode = finished ? process.exitValue() : -1;

            if (!finished) {
                process.destroyForcibly();
                if (callback != null)
                    callback.onError("Installation timeout");
                return false;
            }
            if (exitCode == 0) {
                if (callback != null)
                    callback.onProgress("Python installed successfully!", 100);
                // Clear cached python command and refresh environment
                refreshPythonEnvironment();
                return true;
            } else {
                if (callback != null)
                    callback.onError(
                            "Installation failed with exit code: " + exitCode + "\nOutput: " + output.toString());
                return false;
            }

        } catch (Exception e) {
            if (callback != null)
                callback.onError("Error installing Python: " + e.getMessage());
            return false;
        }
    }

    /**
     * Install Python using chocolatey
     */
    private boolean installPythonWithChocolatey(ProgressCallback callback) {
        try {
            if (callback != null)
                callback.onProgress("Installing Python using Chocolatey...", 20);

            ProcessBuilder pb = new ProcessBuilder("choco", "install", "python", "-y");
            pb.redirectErrorStream(true);
            Process process = pb.start();

            // Monitor progress
            StringBuilder output = new StringBuilder();
            monitorInstallationProgressWithOutput(process, callback, output);

            boolean finished = process.waitFor(300, TimeUnit.SECONDS);
            int exitCode = finished ? process.exitValue() : -1;

            if (!finished) {
                process.destroyForcibly();
                if (callback != null)
                    callback.onError("Installation timeout");
                return false;
            }
            if (exitCode == 0) {
                if (callback != null)
                    callback.onProgress("Python installed successfully!", 100);
                refreshPythonEnvironment();
                return true;
            } else {
                if (callback != null)
                    callback.onError(
                            "Installation failed with exit code: " + exitCode + "\nOutput: " + output.toString());
                return false;
            }

        } catch (Exception e) {
            if (callback != null)
                callback.onError("Error installing Python: " + e.getMessage());
            return false;
        }
    }

    /**
     * Download and install Python manually from python.org
     */
    private boolean downloadAndInstallPython(ProgressCallback callback) {
        try {
            if (callback != null)
                callback.onProgress("Downloading Python installer from python.org...", 30);

            // Use PowerShell to download and install Python
            String powershellScript = "$url = 'https://www.python.org/ftp/python/3.12.0/python-3.12.0-amd64.exe'; " +
                    "$output = '$env:TEMP\\python-installer.exe'; " +
                    "Invoke-WebRequest -Uri $url -OutFile $output; " +
                    "Start-Process -FilePath $output -ArgumentList '/quiet', 'InstallAllUsers=1', 'PrependPath=1' -Wait; "
                    +
                    "Remove-Item $output";

            ProcessBuilder pb = new ProcessBuilder("powershell", "-Command", powershellScript);
            pb.redirectErrorStream(true);
            Process process = pb.start();

            if (callback != null)
                callback.onProgress("Installing Python... This may take several minutes.", 50);

            // Monitor output
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            StringBuilder output = new StringBuilder();
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
                System.out.println("Python install: " + line);
            }
            reader.close();

            boolean finished = process.waitFor(600, TimeUnit.SECONDS); // 10 minutes timeout
            int exitCode = finished ? process.exitValue() : -1;

            if (!finished) {
                process.destroyForcibly();
                if (callback != null)
                    callback.onError("Installation timeout");
                return false;
            }

            if (exitCode == 0) {
                if (callback != null)
                    callback.onProgress("Python installation completed! Please restart the application.", 100);
                pythonCommand = null;
                return true;
            } else {
                if (callback != null)
                    callback.onError(
                            "Installation failed. You may need to install Python manually from https://python.org\nOutput: "
                                    + output.toString());
                return false;
            }

        } catch (Exception e) {
            if (callback != null)
                callback.onError("Error downloading/installing Python: " + e.getMessage() +
                        "\n\nPlease install Python manually from https://python.org");
            return false;
        }
    }

    /**
     * Open Python.org download page in browser
     */
    public void openPythonDownloadPage() {
        try {
            java.awt.Desktop.getDesktop().browse(java.net.URI.create("https://www.python.org/downloads/"));
        } catch (Exception e) {
            System.err.println("Could not open browser: " + e.getMessage());
        }
    }

    /**
     * Check if CUDA is available in the current Python environment
     */
    public boolean isCudaAvailable() {
        try {
            String pythonCommand = getProjectPythonCommand();
            if (pythonCommand == null) {
                return false;
            }

            // Create a simple Python script to check CUDA availability
            String checkScript = "try:\n" +
                    "    import torch\n" +
                    "    print('CUDA_AVAILABLE:' + str(torch.cuda.is_available()))\n" +
                    "except ImportError:\n" +
                    "    # If torch is not available, try ultralytics\n" +
                    "    try:\n" +
                    "        import ultralytics\n" +
                    "        from ultralytics.utils.torch_utils import select_device\n" +
                    "        device = select_device('auto')\n" +
                    "        print('CUDA_AVAILABLE:' + str('cuda' in str(device)))\n" +
                    "    except:\n" +
                    "        print('CUDA_AVAILABLE:False')\n";

            Path tempScript = Files.createTempFile("cuda_check", ".py");
            Files.write(tempScript, checkScript.getBytes());

            ProcessBuilder pb = new ProcessBuilder(pythonCommand, tempScript.toString());
            pb.redirectErrorStream(true);
            Process process = pb.start();

            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                    if (line.startsWith("CUDA_AVAILABLE:")) {
                        String result = line.substring("CUDA_AVAILABLE:".length()).trim().toLowerCase();
                        Files.deleteIfExists(tempScript);
                        return "true".equals(result);
                    }
                }
            }

            process.waitFor();
            Files.deleteIfExists(tempScript);
            System.out.println("CUDA check completed. Output: " + output.toString());
            return false;

        } catch (Exception e) {
            System.out.println("Failed to check CUDA availability: " + e.getMessage());
            return false;
        }
    }

    /**
     * Get the optimal device for training based on availability
     * 
     * @param requestedDevice The requested device ('auto', 'cpu', 'cuda')
     * @return The optimal device to use
     */
    public String getOptimalDevice(String requestedDevice) {
        if ("cpu".equals(requestedDevice)) {
            return "cpu";
        }

        if ("cuda".equals(requestedDevice)) {
            // User explicitly requested CUDA, check if available
            if (isCudaAvailable()) {
                return "cuda";
            } else {
                System.out.println("CUDA requested but not available, falling back to CPU");
                return "cpu";
            }
        }

        if ("auto".equals(requestedDevice)) {
            // Auto mode: use CUDA if available, otherwise CPU
            if (isCudaAvailable()) {
                System.out.println("Auto mode: CUDA detected, using CUDA");
                return "cuda";
            } else {
                System.out.println("Auto mode: CUDA not available, using CPU");
                return "cpu";
            }
        }

        // Default fallback
        System.out.println("Unknown device requested: " + requestedDevice + ", using CPU");
        return "cpu";
    }

    /**
     * Create pip command with SSL bypass options for corporate environments
     */
    private ProcessBuilder createPipCommandWithSSLBypass(String python, String... pipArgs) {
        java.util.List<String> command = new java.util.ArrayList<>();
        command.add(python);
        command.add("-m");
        command.add("pip");

        // Add SSL bypass options for corporate environments
        command.add("--trusted-host");
        command.add("pypi.org");
        command.add("--trusted-host");
        command.add("pypi.python.org");
        command.add("--trusted-host");
        command.add("files.pythonhosted.org");

        // Add the actual pip arguments
        for (String arg : pipArgs) {
            command.add(arg);
        }

        return new ProcessBuilder(command);
    }

    /**
     * Create pip install command with SSL bypass and timeout options
     */
    private ProcessBuilder createSecurePipInstallCommand(String python, String packageName, boolean useUpgrade,
            boolean useUser, boolean noDeps) {
        java.util.List<String> command = new java.util.ArrayList<>();
        command.add(python);
        command.add("-m");
        command.add("pip");

        // Add SSL bypass options
        command.add("--trusted-host");
        command.add("pypi.org");
        command.add("--trusted-host");
        command.add("pypi.python.org");
        command.add("--trusted-host");
        command.add("files.pythonhosted.org");

        // Add install command
        command.add("install");

        // Add timeout and retry options
        command.add("--timeout");
        command.add("300");
        command.add("--retries");
        command.add("5");

        // Add optional flags
        if (useUpgrade) {
            command.add("--upgrade");
        }
        if (useUser) {
            command.add("--user");
        }
        if (noDeps) {
            command.add("--no-deps");
        }

        // Add package name
        command.add(packageName);

        return new ProcessBuilder(command);
    }

    /**
     * Install ultralytics with enhanced SSL bypass for corporate environments
     */
    public boolean installUltralyticsWithSSLBypass(ProgressCallback callback) {
        String python = getProjectPythonCommand();
        if (python == null) {
            if (callback != null)
                callback.onError("Python not found");
            return false;
        }

        boolean inVirtualEnv = hasProjectVirtualEnvironment() && python.contains("venv_");

        // Try advanced SSL bypass method
        try {
            ProcessBuilder pb;

            java.util.List<String> command = new java.util.ArrayList<>();
            command.add(python);
            command.add("-m");
            command.add("pip");

            // Add comprehensive SSL bypass options
            command.add("--trusted-host");
            command.add("pypi.org");
            command.add("--trusted-host");
            command.add("pypi.python.org");
            command.add("--trusted-host");
            command.add("files.pythonhosted.org");
            command.add("--trusted-host");
            command.add("*");

            command.add("install");
            command.add("--timeout");
            command.add("300");
            command.add("--retries");
            command.add("5");

            if (!inVirtualEnv) {
                command.add("--user");
            }

            command.add("ultralytics");

            pb = new ProcessBuilder(command);

            // Set environment variables to bypass SSL
            java.util.Map<String, String> env = pb.environment();
            env.put("PYTHONHTTPSVERIFY", "0");
            env.put("CURL_CA_BUNDLE", "");
            env.put("REQUESTS_CA_BUNDLE", "");

            pb.redirectErrorStream(true);
            Process process = pb.start();

            StringBuilder output = new StringBuilder();
            monitorInstallationProgressWithOutput(process, callback, output);

            int exitCode = process.waitFor(300, TimeUnit.SECONDS) ? process.exitValue() : -1;

            if (exitCode == 0 && verifyUltralyticsInstallation(python, callback)) {
                ultralyticInstalled = true;
                if (callback != null)
                    callback.onProgress("Ultralytics installed with SSL bypass!", 100);
                return true;
            }

        } catch (Exception e) {
            if (callback != null)
                callback.onError("SSL bypass installation failed: " + e.getMessage());
        }

        return false;
    }

    /**
     * Install missing dependencies after core ultralytics installation
     */
    public boolean installMissingDependencies(ProgressCallback callback) {
        String python = getProjectPythonCommand();
        if (python == null) {
            if (callback != null)
                callback.onError("Python not found");
            return false;
        }

        // List of essential dependencies that might be missing
        String[] dependencies = {
                "torch>=2.0.0",
                "torchvision>=0.15.0",
                "numpy>=1.20.0",
                "opencv-python>=4.6.0",
                "pillow>=8.3.0",
                "scipy>=1.7.0"
        };

        boolean allSuccess = true;
        int progress = 10;
        int progressStep = 80 / dependencies.length;

        for (String dep : dependencies) {
            if (callback != null)
                callback.onProgress("Installing " + dep + "...", progress);

            try {
                ProcessBuilder pb = createSecurePipInstallCommand(python, dep, false, false, false);
                pb.redirectErrorStream(true);
                Process process = pb.start();

                // Use longer timeout for large packages like torch
                int exitCode = process.waitFor(900, TimeUnit.SECONDS) ? process.exitValue() : -1; // 15 minutes

                if (exitCode != 0) {
                    if (callback != null)
                        callback.onProgress("Warning: Failed to install " + dep, -1);
                    allSuccess = false;
                } else {
                    if (callback != null)
                        callback.onProgress("Successfully installed " + dep, -1);
                }

                progress += progressStep;

            } catch (Exception e) {
                if (callback != null)
                    callback.onProgress("Error installing " + dep + ": " + e.getMessage(), -1);
                allSuccess = false;
            }
        }

        if (callback != null) {
            if (allSuccess) {
                callback.onProgress("All dependencies installed successfully!", 100);
            } else {
                callback.onProgress("Some dependencies failed to install - core functionality may still work", 90);
            }
        }

        return allSuccess;
    }

    /**
     * Check if ultralytics core is installed but dependencies are missing
     */
    public boolean isUltralyticsCoreInstalled() {
        String python = getProjectPythonCommand();
        if (python == null) {
            return false;
        }
        return verifyUltralyticsCore(python);
    }

    /**
     * Get detailed status of ultralytics installation
     */
    public String getUltralyticsInstallationStatus() {
        String python = getProjectPythonCommand();
        if (python == null) {
            return "Python not found";
        }

        if (verifyUltralyticsInstallation(python, null)) {
            return "Ultralytics fully installed and functional";
        } else if (verifyUltralyticsCore(python)) {
            return "Ultralytics core installed - some dependencies may be missing";
        } else {
            return "Ultralytics not installed";
        }
    }
}
