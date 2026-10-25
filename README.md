# Farmers-Market-Orders-Payments
# 🌾 Farmers' Market – Orders & Payments Management System

A console-based application developed in Java to manage a farmers' market, including vendors, products, inventory, customer orders, payments, deliveries, and product returns.

The system helps farmers and vendors manage their products and allows customers to place orders while maintaining accurate stock levels, payment records, and return information.

## ✨ Features

### 1. Vendor Management
- Add new vendors to the market.
- Prevent duplicate vendor names.
- Assign a unique ID to each vendor.
- Associate products with their respective vendors.

### 2. Product & Inventory Management
- Add products under registered vendors.
- Set product prices and opening stock.
- Display product details, prices, and available quantities.
- Prevent invalid prices and negative stock.
- Automatically update inventory when orders are confirmed or returns are approved.

### 3. Customer Order Management
- Place orders using available product IDs.
- Add multiple products to a single order.
- Validate requested quantities against available stock.
- Display an order summary before confirmation.
- Generate an invoice after successful confirmation.
- Maintain customer order history.

### 4. Payment Management
- Record full or partial payments.
- Support cash, UPI, and card as payment methods.
- Track the total amount paid and remaining balance.
- Prevent payments that exceed the outstanding amount.
- Display payment status as `UNPAID`, `PARTIALLY PAID`, or `PAID`.

### 5. Delivery Management
- Schedule delivery for confirmed orders.
- Collect the customer's delivery address and preferred date.
- Prevent delivery scheduling until the order is fully paid.
- Reject delivery dates in the past.
- Track delivery status.

### 6. Product Return & Refund Management
- Allow customers to request returns for ordered products.
- Apply a three-day return window from the recorded delivery date.
- Prevent returns exceeding the remaining returnable quantity.
- Automatically restock approved returns.
- Calculate refunds based on the product price and returned quantity.
- Display the return decision and rejection reason, when applicable.

### 7. Invoice & Order History
- Generate detailed invoices for confirmed orders.
- Display vendor names, product details, quantities, prices, and totals.
- Maintain customer order history.
- Track billed amounts, payments, refunds, and net customer spending.

## 🛠️ Technologies Used

- **Programming Language:** Java
- **Date Handling:** Java Time API (`LocalDate`, `ChronoUnit`)
- **Data Structures:** `LinkedHashMap`, `ArrayList`, `LinkedHashSet`
- **Input Handling:** Java `Scanner`
- **Storage:** In-memory collections

## 📋 Prerequisites

Install the following before running the project:

- Java Development Kit (JDK)
- A terminal or command prompt
- A code editor or IDE, such as VS Code, IntelliJ IDEA, or Eclipse

Verify your Java installation:

```bash
java -version
javac -version
```

## 🚀 Installation & Execution

### Step 1: Clone the repository

```bash
git clone <your-repository-url>
```

Replace `<your-repository-url>` with your GitHub repository URL.

### Step 2: Navigate to the project directory

```bash
cd <your-project-folder>
```

### Step 3: Compile the program

Save the source code as `FarmersMarket.java` and run:

```bash
javac FarmersMarket.java
```

### Step 4: Run the application

```bash
java FarmersMarket
```

To load sample data for demonstration, run:

```bash
java FarmersMarket demo
```

The demo mode preloads two vendors and four products.

## 🖥️ Application Menu

```text
=== Farmers' Market: Orders & Payments ===

1. Add Vendor
2. Add Product
3. Place Order
4. Record Payment
5. Schedule Delivery
6. Request Return
7. Display Inventory
8. Exit
```

Choose a menu option and follow the prompts displayed in the terminal.

## 🔄 Application Workflow

1. Add vendors to the market.
2. Register products under each vendor.
3. Place an order by selecting products and quantities.
4. Confirm the order to deduct the purchased quantities from stock.
5. Record full or partial payments.
6. Schedule delivery after the order is fully paid.
7. Request product returns within the permitted return period.
8. View updated inventory, invoices, refunds, and customer order history.

## 📁 Project Structure

```text
FarmersMarket/
├── FarmersMarket.java
└── README.md
```

The Java source file contains the main application and the supporting classes:

| Class | Responsibility |
|---|---|
| `FarmersMarket` | Main menu, input handling, inventory display, and order operations |
| `Vendor` | Vendor details and associated products |
| `Product` | Product details, pricing, and stock management |
| `OrderItem` | Purchased quantity, item total, and returnable quantity |
| `Order` | Order confirmation, totals, payments, refunds, and status |
| `Payment` | Payment amount, method, and date |
| `Delivery` | Delivery address, date, and status |
| `ReturnRequest` | Return validation, approval, stock restoration, and refund calculation |

## 💡 Business Rules

- A vendor cannot be registered twice using the same name, ignoring letter case.
- A vendor cannot sell the same product name twice, ignoring letter case.
- An order cannot be confirmed if the required stock is unavailable.
- Stock is deducted only when an order is confirmed.
- Delivery can be scheduled only after full payment.
- Returns must fall within the three-day return window.
- Returned quantities cannot exceed the quantity purchased minus previous approved returns.
- Approved returns restore inventory and calculate the corresponding refund.

## ⚠️ Current Limitations

- **In-memory storage:** All records are lost when the application exits.
- **Console interface:** The application currently runs in a terminal rather than through a graphical interface or website.
- **Simulated payments:** Payment methods are recorded, but no real payment gateway is integrated.
- **Basic delivery tracking:** Delivery status is calculated from the scheduled date; real-time tracking is not implemented.
- **No database integration:** A database can be added in a future version to preserve records permanently.

## 🔮 Future Enhancements

- Integrate MySQL or PostgreSQL for permanent data storage.
- Develop a web or Android interface.
- Add authentication and role-based access for vendors and customers.
- Integrate a secure online payment gateway.
- Implement delivery tracking and notifications.
- Generate downloadable invoices and sales reports.
- Add automated testing and stronger input validation.

## 🎯 Learning Outcomes

This project demonstrates the practical use of:

- Object-oriented programming in Java.
- Classes, objects, constructors, and encapsulation.
- Collections and data management.
- Menu-driven programming and input validation.
- Business rule enforcement.
- Date calculations and validation.
- Inventory management and transaction workflows.

## 👨‍💻 Author

**Your Name**

Developed as a Java project to demonstrate a simple, organized approach to farmers' market order and payment management.

---

⭐ If you find this project useful, consider giving the repository a star!
