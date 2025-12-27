# Quick Start Guide - GreenGrocer Application

## 🚀 Fast Setup (5 minutes)

### 1. Setup Database
```bash
mysql -u myuser -p1234 < greengrocer_db.sql
```

### 2. Build & Run

**Option A: Using Maven (if installed):**
```bash
mvn clean compile javafx:run
```

**Option B: Using Maven Wrapper (no installation needed):**
```bash
.\mvnw.cmd clean compile javafx:run
```

**Option C: In IntelliJ IDEA:**
1. Open Maven tool window (right sidebar)
2. Expand: `demo` → `Plugins` → `javafx`
3. Double-click `javafx:run`

### 3. Test Login
Use these credentials to test each role:

| Role | Username | Password |
|------|----------|----------|
| Customer | `cust` | `cust` |
| Carrier | `carr` | `carr` |
| Owner | `own` | `own` |

---

## 📋 Basic Test Flow

1. **Login as Customer** → Browse products → Add to cart → Checkout
2. **Login as Carrier** → View available orders → Select order → Mark delivered
3. **Login as Owner** → Manage products → View orders → Manage carriers

---

## ⚠️ Common Issues

**Problem:** "Cannot connect to database"
- ✅ Start MySQL service
- ✅ Verify user: `myuser@localhost` / `1234`
- ✅ Check database `greengrocer_db` exists

**Problem:** Build fails
- ✅ Run: `mvn clean compile`
- ✅ Check JDK version (should be 25)

**Problem:** Application won't start
- ✅ Check MySQL is running
- ✅ Verify all dependencies downloaded

---

For detailed instructions, see `RUN_INSTRUCTIONS.md`

