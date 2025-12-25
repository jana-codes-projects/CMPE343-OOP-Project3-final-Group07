# Step-by-Step Instructions to Run and Test the GreenGrocer Application

## Prerequisites

1. **Java Development Kit (JDK) 25** or compatible version
2. **MySQL Server** installed and running
3. **Maven** installed (or use the included Maven wrapper)
4. **MySQL user created** with the required credentials

---

## Step 1: Set Up MySQL Database

### 1.1 Create MySQL User (if not already exists)

Open MySQL Command Line or MySQL Workbench and run:

```sql
CREATE USER 'myuser'@'localhost' IDENTIFIED BY '1234';
GRANT ALL PRIVILEGES ON *.* TO 'myuser'@'localhost';
FLUSH PRIVILEGES;
```

### 1.2 Create and Populate the Database

Run the SQL script to create the database schema and seed data:

**Option A: Using MySQL Command Line**
```bash
mysql -u myuser -p1234 < greengrocer_db.sql
```

**Option B: Using MySQL Workbench**
1. Open MySQL Workbench
2. Connect to your MySQL server
3. Go to **File** → **Open SQL Script**
4. Select `greengrocer_db.sql`
5. Click **Execute** (⚡ icon) or press `Ctrl+Shift+Enter`

**Option C: Copy-Paste Method**
1. Open `greengrocer_db.sql` in a text editor
2. Copy all contents
3. Open MySQL Command Line or Workbench
4. Paste and execute

### 1.3 Verify Database Creation

Run this query to verify:

```sql
USE greengrocer_db;
SHOW TABLES;
SELECT COUNT(*) FROM users;
SELECT COUNT(*) FROM products;
```

You should see 8 tables and data in users/products tables.

---

## Step 2: Build the Project

### 2.1 Open Terminal/Command Prompt

Navigate to the project root directory:
```bash
cd "C:\Users\janas\OneDrive - Kadir Has University\Desktop\5th semester courses\CMPE343 OOP\CMPE343-OOP-Project3-final-Group07"
```

### 2.2 Build with Maven

**Option A: Using Maven Wrapper (Recommended)**
```bash
.\mvnw.cmd clean compile
```

**Option B: Using Maven (if installed)**
```bash
mvn clean compile
```

### 2.3 Verify Build Success

You should see `BUILD SUCCESS` at the end. If there are errors, fix them before proceeding.

---

## Step 3: Run the Application

### 3.1 Run with Maven

**Option A: Using Maven Wrapper**
```bash
.\mvnw.cmd javafx:run
```

**Option B: Using Maven**
```bash
mvn javafx:run
```

**Option C: Using IntelliJ IDEA**
1. Open the project in IntelliJ IDEA
2. Right-click on `HelloApplication.java`
3. Select **Run 'HelloApplication.main()'**

### 3.2 Expected Result

The application should launch and display the **Login** window (500x400 pixels).

---

## Step 4: Test the Application

### 4.1 Test Login with Default Users

The application has three default user accounts:

#### **Customer Login:**
- **Username:** `cust`
- **Password:** `cust`
- **Expected:** Opens Customer interface showing products

#### **Carrier Login:**
- **Username:** `carr`
- **Password:** `carr`
- **Expected:** Opens Carrier interface with order management

#### **Owner Login:**
- **Username:** `own`
- **Password:** `own`
- **Expected:** Opens Owner interface with admin functions

---

### 4.2 Test Customer Features

1. **Login as Customer** (`cust` / `cust`)

2. **Browse Products:**
   - View vegetables in the "Vegetables" tab
   - View fruits in the "Fruits" tab
   - Products should be sorted alphabetically
   - Products with zero stock should not appear
   - Check threshold pricing (if stock ≤ threshold, price should be doubled)

3. **Search Products:**
   - Enter a keyword in the search field (e.g., "Domates", "Elma")
   - Click "Search"
   - Verify filtered results appear

4. **Add to Cart:**
   - Enter a quantity (e.g., 1.5 kg) for a product
   - Click "Add to Cart"
   - Verify success message
   - **Test validation:**
     - Try entering 0 or negative numbers → Should show error
     - Try entering non-numeric text → Should show error
     - Try entering quantity greater than stock → Should show warning

5. **Shopping Cart:**
   - Click "Shopping Cart" button
   - Verify all added items appear
   - Verify quantities are merged for same products
   - Check VAT calculation (20%)
   - **Test coupon:**
     - Enter a coupon code (e.g., "HOSGELDIN25")
     - Click "Apply Coupon"
     - Verify discount is applied

6. **Checkout:**
   - Select a delivery date (within 48 hours)
   - Enter delivery time (HH:mm format, e.g., "14:30")
   - Click "Checkout"
   - **Test validation:**
     - Try delivery date more than 48 hours away → Should show error
     - Try invalid time format → Should show error
     - If cart total < minimum (50 TL) → Should show warning
   - Verify order is created
   - Check for PDF invoice generation message

7. **View Orders:**
   - Click "My Orders" button
   - View order history

8. **Logout:**
   - Click "Logout" button
   - Should return to login screen

---

### 4.3 Test Carrier Features

1. **Login as Carrier** (`carr` / `carr`)

2. **View Available Orders:**
   - Go to "Available Orders" tab
   - Should see orders with status "CREATED"
   - View order details (ID, total, delivery date, customer info)

3. **Select Order:**
   - Click "Select Order" on an available order
   - Order should move to "My Orders" tab with status "ASSIGNED"
   - **Test: Try selecting the same order again (if still in Available) → Should fail (already assigned)**

4. **Complete Order:**
   - Go to "My Orders" tab
   - Click "Mark as Delivered" on an assigned order
   - Order should move to "Completed Orders" tab

5. **View Completed Orders:**
   - Go to "Completed Orders" tab
   - Verify delivered orders are listed

---

### 4.4 Test Owner Features

1. **Login as Owner** (`own` / `own`)

2. **Manage Products:**
   - Go to "Products" tab
   - View all products
   - **Add Product:**
     - Click "Add Product"
     - Fill in: Name, Type (VEG/FRUIT), Price, Stock, Threshold
     - Click "Save"
     - Verify product appears in list
   - **Edit Product:**
     - Click "Edit" on a product
     - Modify price, stock, or threshold
     - Click "Save"
     - Verify changes appear
   - **Remove Product:**
     - Click "Remove" on a product
     - Confirm deletion
     - Product should be deactivated (not deleted from DB)

3. **Manage Carriers:**
   - Go to "Carriers" tab
   - View all carriers
   - **Fire Carrier:**
     - Click "Fire" on an active carrier
     - Carrier should become inactive
   - **Hire Carrier:**
     - Click "Hire" on an inactive carrier
     - Carrier should become active

4. **View Orders:**
   - Go to "Orders" tab
   - View all orders (all statuses)
   - Check order details

5. **View Messages:**
   - Go to "Messages" tab
   - View customer messages

6. **View Reports:**
   - Go to "Reports" tab
   - View report generation options

---

### 4.5 Test Registration

1. From the login screen, click **"Register"**

2. Fill in registration form:
   - Username (must be unique, at least 3 characters)
   - Password (at least 4 characters)
   - Address
   - Phone

3. Click "Register"

4. **Test validation:**
   - Try existing username → Should show error
   - Try short username (< 3 chars) → Should show error
   - Try short password (< 4 chars) → Should show error
   - Leave fields empty → Should show error

5. Register successfully → Should return to login screen

6. Login with new credentials → Should work

---

### 4.6 Test Error Handling

**Customer Interface:**
- ✅ Zero/negative quantity → Error message
- ✅ Non-numeric quantity → Error message
- ✅ Quantity > stock → Warning message
- ✅ Delivery date > 48 hours → Error message
- ✅ Invalid time format → Error message
- ✅ Cart total < minimum → Warning message

**Owner Interface:**
- ✅ Zero/negative price → Error message
- ✅ Zero/negative threshold → Error message

**Database:**
- ✅ Try operations when MySQL is stopped → Should handle gracefully

---

## Step 5: Verify Database Updates

### 5.1 Check Order Creation

After placing an order as a customer:

```sql
USE greengrocer_db;
SELECT * FROM orders ORDER BY id DESC LIMIT 5;
SELECT * FROM order_items WHERE order_id = [latest_order_id];
SELECT * FROM invoices WHERE order_id = [latest_order_id];
```

### 5.2 Check Stock Updates

After an order is placed:

```sql
SELECT id, name, stock_kg FROM products WHERE id = [product_id];
```

Stock should be reduced by the ordered quantity.

### 5.3 Check Carrier Assignment

After a carrier selects an order:

```sql
SELECT id, carrier_id, status FROM orders WHERE id = [order_id];
```

Status should be "ASSIGNED" and carrier_id should be set.

---

## Troubleshooting

### Application won't start
- ✅ Check MySQL is running
- ✅ Verify database `greengrocer_db` exists
- ✅ Check database connection credentials (myuser@localhost / 1234)
- ✅ Verify Java version (should be JDK 25)

### Build errors
- ✅ Run `mvn clean` then `mvn compile`
- ✅ Check if all dependencies are downloaded
- ✅ Verify Maven is using correct JDK version

### Database connection errors
- ✅ Verify MySQL user exists: `myuser@localhost` with password `1234`
- ✅ Check MySQL server is running
- ✅ Verify database `greengrocer_db` exists

### Login fails
- ✅ Verify default users exist in database
- ✅ Check password hashing (should use SHA-256)
- ✅ Try registering a new user

---

## Quick Test Checklist

- [ ] Database created and populated
- [ ] Application builds successfully
- [ ] Application launches (login screen appears)
- [ ] Can login as customer
- [ ] Can login as carrier
- [ ] Can login as owner
- [ ] Can register new customer
- [ ] Can browse products
- [ ] Can add products to cart
- [ ] Can checkout (create order)
- [ ] Order appears in carrier's available orders
- [ ] Carrier can select order
- [ ] Carrier can mark order as delivered
- [ ] Owner can view orders
- [ ] Owner can manage products
- [ ] Owner can manage carriers
- [ ] All error validations work correctly

---

## Notes

- The initial window size is 960x540 pixels (centered)
- Products are sorted alphabetically
- Threshold logic: When stock ≤ threshold, price doubles
- VAT rate is 20%
- Minimum cart value is 50 TL
- Delivery date must be within 48 hours
- PDF invoices are generated and stored in database
- Loyalty discount: 5% (10+ orders), 10% (25+ orders), 15% (50+ orders)

