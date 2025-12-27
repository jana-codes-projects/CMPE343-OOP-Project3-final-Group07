# Login Troubleshooting Guide

If you're getting "Invalid username or password" errors when trying to login with the default users (`cust`, `carr`, `own`), follow these steps:

## Step 1: Verify Database Setup

Make sure you've run the SQL script to create the database and populate users:

**Option A: Using MySQL Command Line**
```bash
mysql -u myuser -p1234 < greengrocer_db.sql
```

**Option B: Using MySQL Workbench**
1. Open MySQL Workbench
2. Connect to your MySQL server
3. Go to **File** → **Open SQL Script**
4. Select `greengrocer_db.sql`
5. Click **Execute** (⚡ icon)

**Option C: Copy-Paste in MySQL Command Line**
```sql
mysql -u myuser -p1234
```
Then paste the entire contents of `greengrocer_db.sql`

## Step 2: Verify Users Exist in Database

Run this SQL query to check if the default users exist:

```sql
USE greengrocer_db;
SELECT id, username, role, is_active, 
       LENGTH(password_hash) as hash_length,
       LEFT(password_hash, 10) as hash_preview
FROM users 
WHERE username IN ('cust', 'carr', 'own');
```

You should see 3 rows with:
- `hash_length` = 64 (SHA-256 produces 64-character hex strings)
- `is_active` = 1

## Step 3: Verify Database Connection

Check that your application can connect to the database:

1. Make sure MySQL service is running (check Windows Services)
2. Verify connection credentials in `DatabaseAdapter.java`:
   - URL: `jdbc:mysql://localhost:3306/greengrocer_db`
   - User: `myuser`
   - Password: `1234`

## Step 4: Test Password Hashing

If the users exist but login still fails, verify the password hashes match:

**In MySQL:**
```sql
USE greengrocer_db;
SELECT username, password_hash, SHA2('cust', 256) as expected_hash_cust
FROM users 
WHERE username = 'cust';
```

The `password_hash` should match `expected_hash_cust` for the `cust` user.

## Step 5: Check Application Logs

When you try to login, check the console/terminal output for any error messages:
- Database connection errors
- SQL exceptions
- Other authentication errors

## Common Issues and Solutions

### Issue 1: "Invalid username or password" for all users
**Solution:** Database not set up. Run `greengrocer_db.sql` script.

### Issue 2: Users exist but can't login
**Possible causes:**
- Password hash mismatch (should be fixed with UTF-8 encoding)
- Database connection issue
- User is inactive (`is_active = 0`)

**Solution:** 
1. Verify users are active: `SELECT username, is_active FROM users WHERE username IN ('cust', 'carr', 'own');`
2. Check database connection settings
3. Rebuild and restart the application

### Issue 3: Database connection fails
**Solution:**
1. Start MySQL service
2. Verify MySQL is listening on port 3306
3. Check firewall settings
4. Verify user credentials: `myuser@localhost` / `1234`

## Quick Test

After setting up the database, try this quick test:

1. **Run the application**
2. **Login with:**
   - Username: `cust`
   - Password: `cust`
3. **You should be logged in as a customer**

If this doesn't work, verify the database setup in Step 1.

---

## Manual User Creation (If needed)

If the default users don't exist, you can create them manually:

```sql
USE greengrocer_db;

-- Create cust user
INSERT INTO users (username, password_hash, role, is_active, address, phone) 
VALUES ('cust', SHA2('cust', 256), 'customer', 1, 'Default Address', '+90 555 000 0001');

-- Create carr user
INSERT INTO users (username, password_hash, role, is_active, address, phone) 
VALUES ('carr', SHA2('carr', 256), 'carrier', 1, 'Default Address', '+90 555 000 0002');

-- Create own user
INSERT INTO users (username, password_hash, role, is_active, address, phone) 
VALUES ('own', SHA2('own', 256), 'owner', 1, 'Default Address', '+90 555 000 0003');
```

