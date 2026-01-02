# Requirements Compliance Report
## CMPE343-OOP-Project3-final-Group07

**Date:** Generated Report  
**Project:** JavaFX GreenGrocer Project

---

## Executive Summary

This report evaluates the project against all specified requirements. **Several critical issues were found that need to be addressed before submission.**

### Critical Issues (Must Fix)
1. ❌ **Database credentials incorrect** - Should be `myuser`/`1234`, currently `emirfurqan`/`Emir0`
2. ❌ **Default users missing** - No evidence of cust/cust, carr/carr, own/own users
3. ❌ **Threshold logic not applied** - Method exists but not used in cart calculations
4. ❌ **48-hour delivery validation missing** - Only checks if past, not within 48 hours
5. ❌ **Minimum cart value validation missing**
6. ❌ **Coupon application missing** - Schema exists but not implemented in checkout
7. ❌ **Loyalty discount missing** - Field exists but not implemented
8. ❌ **PDF invoice not real PDF** - Stored as text bytes, not actual PDF
9. ❌ **JavaDoc missing** - No documentation found in model classes
10. ❌ **Window title incorrect** - Should be "Group07 GreenGrocer" for customer interface

---

## Detailed Requirements Check

### ✅ Tech Stack & Constraints

| Requirement | Status | Notes |
|------------|--------|-------|
| Java | ✅ | Project uses Java |
| JavaFX | ✅ | GUI implemented with JavaFX |
| MySQL | ✅ | Database connection configured |
| JDBC | ✅ | Database access via JDBC |
| JavaDoc | ❌ | **MISSING** - No JavaDoc found in model classes |
| MVC + DAO pattern | ✅ | Clear separation: Models, DAOs, Controllers |
| Error handling | ⚠️ | Partial - some error handling present but incomplete |
| Unlimited classes | ✅ | Good class count (30+ classes) |

### ❌ Database Connection

| Requirement | Expected | Found | Status |
|------------|----------|-------|--------|
| Username | `myuser` | `emirfurqan` | ❌ **FAIL** |
| Password | `1234` | `Emir0` | ❌ **FAIL** |

**Location:** `app.properties`

**Fix Required:** Update `app.properties`:
```properties
db.user=myuser
db.password=1234
```

### ❌ Default Users

| User | Username | Password | Role | Status |
|------|----------|----------|------|--------|
| Customer | `cust` | `cust` | customer | ❌ **NOT FOUND** |
| Carrier | `carr` | `carr` | carrier | ❌ **NOT FOUND** |
| Owner | `own` | `own` | owner | ❌ **NOT FOUND** |

**Issue:** No SQL script or initialization code found to create default users.

**Fix Required:** Create initialization script or add to database setup.

### ✅ Database Schema

| Table | Status | Notes |
|-------|--------|-------|
| users | ✅ | Matches requirements (id, username, password, role, address, contact) |
| products | ✅ | Matches requirements (id, name, type, price, stock, imageLocation, threshold) |
| orders | ✅ | Matches requirements (id, orderTime, deliveryTime, products, userId, carrierId, isDelivered, totalCost, invoicePDF) |

**Additional tables properly implemented:** coupons, order_items, messages, ratings, invoices, cart_items

### ✅ Login & Registration

| Feature | Status | Notes |
|---------|--------|-------|
| Login UI | ✅ | Implemented with validation |
| Username/password validation | ✅ | Working |
| Role-based interface routing | ✅ | Correctly routes to customer/carrier/owner |
| Warning dialog on failure | ✅ | Uses ToastService |
| Customer registration | ✅ | Implemented |
| Unique username validation | ✅ | Checks for duplicates |
| Strong password rules | ✅ | Requires: 6+ chars, uppercase, lowercase, number |
| Save to DB | ✅ | Working |

### ⚠️ Customer Interface

| Feature | Status | Notes |
|---------|--------|-------|
| Window title "Group07 GreenGrocer" | ❌ | Currently "Gr7Project3 - Login" |
| Display username | ✅ | Shown in corner |
| 12+ fruits and 12+ vegetables | ✅ | 30 product images available |
| TitledPanes | ✅ | Uses category-card (equivalent to TitledPane) |
| Sorted alphabetically | ✅ | `products.sort((p1, p2) -> p1.getName().compareToIgnoreCase(p2.getName()))` |
| Name, image, price display | ✅ | All displayed |
| Hide zero stock | ✅ | `products.removeIf(p -> p.getStockKg() <= 0)` |
| **Threshold logic (double price)** | ❌ | **Method exists but NOT USED** - `CartItem.getUnitPrice()` uses `product.getPrice()` instead of `product.getEffectivePrice()` |
| Quantity input (positive double only) | ✅ | Validates with NumberFormatException |
| Shopping cart (separate stage) | ✅ | Opens in new scene |
| Merge same products | ✅ | Uses UNIQUE constraint in cart_items table |
| Show VAT and total | ✅ | VAT calculated (20%) |
| Warn if stock insufficient | ✅ | Warning shown |
| Keyword filter/search | ✅ | Search bar implemented |
| Delivery date within 48 hours | ❌ | **Only checks if past, not within 48 hours** |
| Minimum cart value | ❌ | **NOT IMPLEMENTED** |
| Apply coupon | ❌ | **NOT IMPLEMENTED** - Schema exists but no UI/logic |
| Apply loyalty discount | ❌ | **NOT IMPLEMENTED** - Field exists but always 0 |
| Order summary before confirmation | ⚠️ | Cart view shows items, but no explicit summary dialog |
| Update DB on purchase | ✅ | Working |
| Generate PDF invoice | ⚠️ | **Generates TEXT, not PDF** - stored as bytes but not real PDF format |
| Save invoice to DB | ✅ | Saved to invoices table |
| Cancel orders | ⚠️ | Need to verify implementation |
| View order history | ✅ | OrdersController implemented |
| Delivery status | ✅ | Shown in orders |
| Rate carrier | ⚠️ | RatingDao exists, need to verify UI |
| Message owner | ✅ | Implemented |
| Logout | ✅ | Working |

**Critical Issues:**
1. **Threshold logic not applied:** `CartItem.getUnitPrice()` should use `product.getEffectivePrice()` which doubles price when `stockKg <= thresholdKg`
2. **48-hour validation missing:** Should check `requested.isBefore(LocalDateTime.now().plusHours(48))`
3. **Coupon system:** Schema exists but no UI or application logic
4. **Loyalty discount:** Field exists but always set to 0
5. **PDF invoice:** Currently text, should be actual PDF (requires iText or similar library)

### ✅ Carrier Interface

| Feature | Status | Notes |
|---------|--------|-------|
| 3 sections (Available, Selected/Current, Completed) | ✅ | Implemented in CarrierController |
| Order ID display | ✅ | Shown |
| Products display | ✅ | Lists order items |
| Customer name & address | ✅ | Retrieved from UserDao |
| Total (incl. VAT) | ✅ | Displayed |
| Requested delivery date | ✅ | Shown |
| Multiple carriers cannot select same order | ✅ | Uses `assignOrderToCarrier` with status check |
| Enter delivery date after delivery | ✅ | DatePicker for delivery time |
| Mark order completed | ✅ | `markOrderDelivered` method |

### ⚠️ Owner Interface

| Feature | Status | Notes |
|---------|--------|-------|
| Add/update/remove products | ✅ | Implemented |
| Validate positive price & threshold | ⚠️ | Need to verify validation |
| Hire/fire carriers | ✅ | `setCarrierActive` method exists |
| View all orders | ✅ | `getAllOrders` implemented |
| View & reply to messages | ✅ | MessageDao and UI implemented |
| Manage coupons | ⚠️ | CouponDao exists, need to verify UI |
| Manage loyalty rules | ⚠️ | LoyaltyContainer exists, need to verify |
| View carrier ratings | ✅ | RatingDao implemented |
| View reports (charts) | ⚠️ | ChartContainer exists, need to verify implementation |

### ⚠️ Error Handling

| Error Type | Status | Notes |
|-----------|--------|-------|
| Zero or negative product amount | ✅ | Validated in `handleAddToCart` |
| Non-double input for amount | ✅ | Catches `NumberFormatException` |
| Displaying zero-stock products | ✅ | Filtered out in `initialize()` |
| Duplicate products in cart | ✅ | Handled by UNIQUE constraint |
| Multiple carriers selecting same order | ✅ | Status check in `assignOrderToCarrier` |
| Invalid threshold values | ⚠️ | Need to verify validation in owner interface |
| Database failures | ⚠️ | Some try-catch, but could be more comprehensive |

### ❌ Design Requirements

| Requirement | Status | Notes |
|------------|--------|-------|
| Encapsulation | ✅ | Private fields with getters/setters |
| Inheritance | ⚠️ | Limited use - mostly composition |
| Polymorphism | ⚠️ | Limited use of interfaces/polymorphism |
| Model classes | ✅ | User, Product, Order, CartItem, Coupon, Message, Rating |
| DAO classes | ✅ | UserDao, ProductDao, OrderDao, CartDao, CouponDao, MessageDao, RatingDao, CarrierDao |
| Controller classes | ✅ | LoginController, RegisterController, CustomerController, CartController, CarrierController, OwnerController, OrdersController |
| DatabaseAdapter class | ⚠️ | `Db` class exists but is more of a connection manager |
| Clean code principles | ✅ | Generally well-structured |
| **JavaDoc for all classes** | ❌ | **MISSING** - No JavaDoc found |

---

## Summary of Missing/Incorrect Features

### Critical (Must Fix Before Submission)

1. **Database Credentials** - Must be `myuser`/`1234`
2. **Default Users** - Must create cust/cust, carr/carr, own/own
3. **Threshold Logic** - `CartItem.getUnitPrice()` must use `product.getEffectivePrice()`
4. **48-Hour Validation** - Add check: `requested.isBefore(LocalDateTime.now().plusHours(48))`
5. **Minimum Cart Value** - Implement validation (e.g., minimum 10₺)
6. **Coupon Application** - Implement UI and logic in checkout
7. **Loyalty Discount** - Implement calculation and application
8. **PDF Invoice** - Generate actual PDF (not text bytes)
9. **JavaDoc** - Add documentation to all classes and public methods
10. **Window Title** - Change to "Group07 GreenGrocer" for customer interface

### Recommended Fixes

1. **Order Summary Dialog** - Add explicit confirmation dialog before order placement
2. **Cancel Orders** - Verify and document cancellation functionality
3. **Report Charts** - Verify chart implementation in owner interface
4. **Error Handling** - Enhance database error handling
5. **Inheritance/Polymorphism** - Consider adding more OOP patterns if time permits

---

## Files That Need Modification

1. `app.properties` - Fix database credentials
2. `src/main/java/com/cmpe343/model/CartItem.java` - Use `getEffectivePrice()`
3. `src/main/java/com/cmpe343/fx/controller/CartController.java` - Add 48-hour validation, minimum cart value, coupon/loyalty logic
4. `src/main/java/com/cmpe343/dao/OrderDao.java` - Generate real PDF invoices
5. All model classes - Add JavaDoc
6. All DAO classes - Add JavaDoc
7. All controller classes - Add JavaDoc
8. Database initialization - Create default users script
9. `src/main/java/com/cmpe343/fx/controller/CustomerController.java` - Fix window title
10. `src/main/java/com/cmpe343/fx/MainFx.java` - Fix window title for customer interface

---

## Conclusion

The project demonstrates good structure and implements many core features correctly. However, **10 critical issues** must be addressed to meet all requirements. The most critical are:

1. Database credentials
2. Default users
3. Threshold logic application
4. JavaDoc documentation
5. Coupon/loyalty discount implementation
6. 48-hour delivery validation
7. PDF invoice generation

**Recommendation:** Fix all critical issues before submission to ensure full compliance with requirements.
