# How to Run the Application with Maven

Now that you have Maven installed, here's how to run the application:

---

## Option 1: Using Command Line/Terminal

### Step 1: Open Terminal/Command Prompt

Open a terminal/command prompt and navigate to your project directory:

```bash
cd "C:\Users\janas\OneDrive - Kadir Has University\Desktop\5th semester courses\CMPE343 OOP\CMPE343-OOP-Project3-final-Group07"
```

### Step 2: Build the Project

```bash
mvn clean compile
```

Wait for it to finish - you should see `BUILD SUCCESS` at the end.

### Step 3: Run the Application

```bash
mvn javafx:run
```

The application should launch and show the Login window!

---

## Option 2: Using IntelliJ IDEA (Easier)

### Step 1: Open Maven Tool Window in IntelliJ

1. In IntelliJ IDEA, look at the **right side** of the window
2. You should see a **"Maven"** tab/icon
3. Click on it to open the Maven tool window
   - If you don't see it: **View** → **Tool Windows** → **Maven**

### Step 2: Build the Project

In the Maven tool window:
1. Expand: `demo` → `Lifecycle`
2. Double-click on **`clean`** (this cleans old build files)
3. Wait for it to finish
4. Double-click on **`compile`** (this compiles your code)
5. Wait for it to finish - should show `BUILD SUCCESS`

### Step 3: Run the Application

In the Maven tool window:
1. Expand: `demo` → `Plugins` → `javafx`
2. Double-click on **`javafx:run`**
3. The application should launch! 🎉

---

## Option 3: Using IntelliJ Terminal (Alternative)

1. In IntelliJ, click on the **Terminal** tab at the bottom (or press `Alt+F12`)
2. Make sure you're in the project root directory (where `pom.xml` is located)
3. Run these commands one by one:

```bash
mvn clean compile
mvn javafx:run
```

---

## Before Running - Important!

Make sure you have:

1. ✅ **MySQL Database Set Up:**
   ```bash
   mysql -u myuser -p1234 < greengrocer_db.sql
   ```
   Or run the SQL file in MySQL Workbench.

2. ✅ **MySQL Server Running:**
   - Make sure MySQL service is started
   - Check in Windows Services or MySQL Workbench

---

## Quick One-Line Command (After Database is Set Up)

If your database is already set up, you can build and run in one go:

```bash
mvn clean compile javafx:run
```

---

## Troubleshooting

### "mvn: command not found"
- Make sure Maven is in your PATH
- Restart your terminal/IntelliJ after installing Maven
- Verify: Run `mvn --version` in terminal

### "Cannot connect to database"
- Make sure MySQL is running
- Verify database `greengrocer_db` exists
- Check user credentials: `myuser@localhost` / `1234`

### Build fails
- Make sure you're in the project root (where `pom.xml` is)
- Check internet connection (Maven needs to download dependencies)
- Try: `mvn clean` then `mvn compile` again

---

## After the Application Launches

You should see a **Login** window. Use these credentials to test:

| Role | Username | Password |
|------|----------|----------|
| Customer | `cust` | `cust` |
| Carrier | `carr` | `carr` |
| Owner | `own` | `own` |

---

## Summary (TL;DR)

**In IntelliJ:**
1. Open Maven tool window (right side)
2. `demo` → `Plugins` → `javafx` → double-click `javafx:run`

**In Terminal:**
1. Navigate to project folder
2. Run: `mvn clean compile javafx:run`

Done! 🚀


