# CMPE343-OOP-Project3-final-Group07
JavaFX GreenGrocer Project

_Build a fully functional desktop application for a Local Greengrocer using JavaFX (GUI) and JDBC (MySQL).
The project must compile and run without errors, follow Object-Oriented Design principles, and generate JavaDoc documentation for all classes and public methods._

------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

**🔧 Tech Stack & Constraints**

- Language: Java
- GUI: JavaFX
- Database: MySQL
- DB Access: JDBC
- Documentation: JavaDoc
- Architecture: MVC + DAO pattern
- Error handling: Comprehensive try-catch and input validation
- Unlimited number of classes (higher is better)

------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

**🗄 Database Connection**

All users connect using:
USERNAME: myuser@localhost
PASSWORD: 1234

Required Tables (expand if needed):

_UserInfo_
- id (PK)
- username (unique)
- password
- role (customer, carrier, owner)
- address
- contact
- loyaltyPoints

_ProductInfo_
- id (PK)
- name
- type (fruit / vegetable)
- price
- stock
- imageLocation
- threshold

_OrderInfo_
- id (PK)
- orderTime
- deliveryTime
- products (serialized or relational mapping)
- userId
- carrierId
- isDelivered
- totalCost
- invoicePDF (BLOB)

------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

**👤 Default Users (Must Exist)**

-1] username: cust / password: cust → customer
-2] username: carr / password: carr → carrier
-3] username: own / password: own → owner

------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

**🖥 Application Flow**

🔐 Login & Registration
- Start with a Login UI
- Validate username/password
- On success → open role-based interface
- On failure → warning dialog
- Customers can register
  -- Validate unique username
  -- Strong password rules
  -- Save to DB

------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

**🧑‍🌾 Customer Interface**

- Window title: “Group07 GreenGrocer”
- Display username in a corner

- Show 12+ fruits and 12+ vegetables
  -- Inside TitledPanes
  -- Sorted alphabetically
  -- Each item shows name, image, price
  -- Hide products with zero stock
  
- Threshold logic:
  -- If stock ≤ threshold → double price

- Quantity input:
  -- Accept only positive double values
  -- Reject zero, negative, non-numeric

- Shopping cart:
  -- Opens in a separate stage
  -- Merge same products (e.g., 1.25kg + 0.75kg → 2kg)
  -- Show VAT and total
  -- Warn if stock insufficient

- Filters:
  -- Keyword filter
  -- Quick search bar

- Checkout:
  -- Delivery date within 48 hours
  -- Minimum cart value required
  -- Apply coupon if available
  -- Apply loyalty discount (clearly defined rule)
  -- Show order summary before confirmation

- On purchase:
  -- Update DB
  -- Generate PDF invoice
  -- Save invoice to DB and share with customer

- Extra features:
  -- Cancel orders within allowed timeframe
  -- View order history & delivery status
  -- Rate carrier
  -- Message owner
  -- Logout

------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

**🚚 Carrier Interface**

- View orders in 3 sections:
  -- Available
  -- Selected/Current
  -- Completed

- Available order details:
  -- Order ID
  -- Products
  -- Customer name & address
  -- Total (incl. VAT)
  -- Requested delivery date

- Rules:
  -- Multiple carriers cannot select same order

- After delivery:
  -- Enter delivery date
  -- Mark order completed

------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

**🏪 Owner Interface**

Owner can:
- Add / update / remove products
  -- Validate positive price & threshold
- Hire / fire carriers
- View all orders
- View & reply to customer messages
- Manage coupons & loyalty rules
- View carrier ratings
- View reports:
  -- Charts by product / time / revenue

------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

**❗ Mandatory Error Handling (Do NOT skip)**

Handle and prevent:
- Zero or negative product amount
- Non-double input for amount
- Displaying zero-stock products
- Duplicate products in cart
- Multiple carriers selecting same order
- Invalid threshold values
- Database failures
**Failure to handle these causes demo termination.**

------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

**🧱 Design Requirements**

- Use: Encapsulation, Inheritance, Polymorphism

- Create:
  -- Model classes (User, Customer, Carrier, Owner, Product, Order, CartItem…)
  -- DAO classes (UserDAO, ProductDAO, OrderDAO…)
  -- Controller classes per UI
  -- DatabaseAdapter class

- Follow clean code principles

- Generate JavaDoc for all classes
