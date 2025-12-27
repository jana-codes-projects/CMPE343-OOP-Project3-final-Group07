# Fix for "Module com.example.demo not found" Error

## Problem
IntelliJ IDEA is trying to run the application as a Java module but can't find the compiled classes on the module path.

The error occurs because IntelliJ's default run configuration doesn't properly set up the module path to include your compiled classes.

## Solution Options

### Option 1: Run via Maven (Recommended) ✅

This is the easiest and most reliable way:

1. **Open Maven Tool Window:**
   - In IntelliJ, go to **View** → **Tool Windows** → **Maven**
   - Or click the Maven icon on the right sidebar

2. **Run via Maven:**
   - Expand: `demo` → `Plugins` → `javafx` → `javafx:run`
   - Double-click on `javafx:run`
   - This will compile and run the application correctly

**OR use Terminal in IntelliJ:**
- Go to **Terminal** tab at the bottom
- Run: `.\mvnw.cmd javafx:run`

---

### Option 2: Fix IntelliJ Run Configuration

If you want to run directly from IntelliJ (not via Maven), you need to configure it properly:

1. **Delete the existing run configuration:**
   - Go to **Run** → **Edit Configurations...**
   - Select the existing `HelloApplication` configuration
   - Click **-** to delete it

2. **Create a new Run Configuration:**
   - Click **+** → **Application**
   - Set the following:
     - **Name:** `GreenGrocer App`
     - **Module:** `demo` (should auto-detect)
     - **Main class:** `com.example.demo.HelloApplication`
     - **JRE:** Java 25 (or your JDK version)

3. **Important - Set working directory:**
   - Expand **Environment variables**
   - Set **Working directory** to the project root (where pom.xml is located)

4. **The configuration should work, but if it still fails:**
   - IntelliJ should automatically detect the module and set up the classpath correctly
   - If not, try: **Build** → **Rebuild Project** first

**However, Maven (Option 1) is still the recommended way** as it handles all module configuration automatically!

---

### Option 3: Create a Launcher Class (Alternative)

If you want to avoid module path issues, we can create a simple launcher that doesn't rely on modules. But this is not recommended as it defeats the purpose of using modules.

---

## Quick Fix (Easiest)

**Just run via Maven:**

1. Open Terminal in IntelliJ (Alt+F12)
2. Type: `.\mvnw.cmd clean compile javafx:run`
3. Press Enter

This will:
- Clean previous builds
- Compile the project
- Run the JavaFX application with all dependencies correctly configured

---

## Why This Happens

IntelliJ's default run configuration for Java modules needs the compiled classes to be on the module path. When using Maven with the JavaFX plugin, Maven handles all the module path configuration automatically, which is why it works better.

The Maven JavaFX plugin (`javafx:run`) is specifically designed to handle JavaFX applications with modules correctly, including:
- Setting up the module path
- Including all dependencies
- Configuring JavaFX modules
- Handling the module system properly

---

## Recommendation

**Use Maven to run the application** - it's the intended way for Maven projects and handles all the complexity automatically.

