import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.*;

// Farmers' Market - orders, payments, deliveries, returns
// run: javac FarmersMarket.java && java FarmersMarket   (add "demo" to preload sample data)
// TODO: everything is in-memory, persist to file/db if this ever needs to survive a restart

public class FarmersMarket {

    static final Scanner sc = new Scanner(System.in);
    static final String LINE = "------------------------------------------------------------";

    static final Map<Integer, Vendor> vendors = new LinkedHashMap<>();
    static final Map<Integer, Product> products = new LinkedHashMap<>();
    static final Map<Integer, Order> orders = new LinkedHashMap<>();
    static final Map<String, List<Order>> history = new LinkedHashMap<>(); // key = lowercase customer name

    public static void main(String[] args) {
        System.out.println("=== Farmers' Market: Orders & Payments ===");
        if (args.length > 0 && args[0].equals("demo")) seed();

        try {
            while (true) {
                System.out.println("\n1. Add Vendor\n2. Add Product\n3. Place Order\n4. Record Payment"
                        + "\n5. Schedule Delivery\n6. Request Return\n7. Display Inventory\n8. Exit");
                int choice = askInt("Choice: ");
                switch (choice) {
                    case 1: addVendor(); break;
                    case 2: addProduct(); break;
                    case 3: placeOrder(); break;
                    case 4: recordPayment(); break;
                    case 5: scheduleDelivery(); break;
                    case 6: requestReturn(); break;
                    case 7: displayInventory(); break;
                    case 8: System.out.println("Bye!"); return;
                    default: System.out.println("  Pick a number from 1 to 8.");
                }
            }
        } catch (NoSuchElementException e) {
            System.out.println("\nInput closed, exiting.");
        }
    }

    // ---------- menu actions ----------

    static void addVendor() {
        String name = ask("Vendor name: ");
        if (name.isEmpty()) { System.out.println("  Name can't be empty."); return; }
        for (Vendor v : vendors.values())
            if (v.name.equalsIgnoreCase(name)) {
                System.out.println("  Vendor already exists (#" + v.id + ").");
                return;
            }
        Vendor v = new Vendor(name);
        vendors.put(v.id, v);
        System.out.println("  Added vendor #" + v.id + " - " + v.name);
        showStock(Collections.singletonList(v));
    }

    static void addProduct() {
        if (vendors.isEmpty()) { System.out.println("  Add a vendor first."); return; }
        for (Vendor v : vendors.values()) System.out.println("  #" + v.id + " " + v.name);
        Vendor v = vendors.get(askInt("Vendor id: "));
        if (v == null) { System.out.println("  No such vendor."); return; }

        String name = ask("Product name: ");
        double price = askDouble("Price per unit (Rs.): ");
        int stock = askInt("Opening stock: ");
        if (name.isEmpty() || price <= 0 || stock < 0) {
            System.out.println("  Invalid product details.");
            return;
        }
        if (v.find(name) != null) {
            System.out.println("  " + v.name + " already sells " + name + ".");
            return;
        }
        Product p = new Product(v, name, price, stock);
        products.put(p.id, p);
        System.out.println("  Added product #" + p.id + " under " + v.name);
        showStock(Collections.singletonList(v));
    }

    static void placeOrder() {
        if (products.isEmpty()) { System.out.println("  No products listed yet."); return; }
        String cust = ask("Customer name: ");
        if (cust.isEmpty()) { System.out.println("  Need a customer name."); return; }

        showStock(vendors.values());
        Map<Product, Integer> cart = new LinkedHashMap<>();
        while (true) {
            int pid = askInt("Product id (0 to finish): ");
            if (pid == 0) break;
            Product p = products.get(pid);
            if (p == null) { System.out.println("  No such product."); continue; }

            int qty = askInt("Qty: ");
            if (qty <= 0) { System.out.println("  Qty must be positive."); continue; }
            int already = cart.getOrDefault(p, 0);
            // rule 1: only what the vendor actually has
            if (qty + already > p.getStock()) {
                System.out.println("  " + p.vendor.name + " only has " + (p.getStock() - already)
                        + " more " + p.name + " available.");
                continue;
            }
            cart.merge(p, qty, Integer::sum);
        }
        if (cart.isEmpty()) { System.out.println("  Empty cart, nothing to order."); return; }

        Order o = new Order(cust);
        for (Map.Entry<Product, Integer> e : cart.entrySet())
            o.items.add(new OrderItem(e.getKey(), e.getValue()));

        System.out.println("\n  Order summary:");
        for (OrderItem i : o.items)
            System.out.printf("   %d x %s (%s) = %.2f%n", i.qty, i.product.name, i.product.vendor.name, i.lineTotal());
        System.out.printf("   Total: %.2f%n", o.total());

        if (!ask("Confirm order? (y/n): ").equalsIgnoreCase("y")) {
            System.out.println("  Order discarded, stock untouched.");
            return;
        }
        if (!o.confirm()) {
            System.out.println("  Stock changed while ordering, couldn't confirm.");
            return;
        }
        orders.put(o.id, o);
        history.computeIfAbsent(cust.toLowerCase(), k -> new ArrayList<>()).add(o);

        printInvoice(o);
        showStock(o.vendors());
        printHistory(cust);
    }

    static void recordPayment() {
        Order o = pickOrder();
        if (o == null) return;
        if (o.isSettled()) { System.out.println("  Order #" + o.id + " is already settled."); return; }

        System.out.printf("  Order #%d  total %.2f  paid %.2f  due %.2f%n", o.id, o.total(), o.paid(), o.due());
        double amt = askDouble("Amount: ");
        if (amt <= 0 || amt > o.due() + 0.005) {
            System.out.println("  Amount must be more than 0 and not above the due amount.");
            return;
        }
        String method = ask("Method (cash/upi/card): ");
        Payment pay = new Payment(o, amt, method.isEmpty() ? "cash" : method);
        o.payments.add(pay);

        System.out.printf("%n  Payment #%d: Rs.%.2f via %s%n", pay.id, pay.amount, pay.method);
        System.out.printf("  Order #%d  paid %.2f  due %.2f  -> %s%n", o.id, o.paid(), Math.max(0, o.due()), o.paymentStatus());
        if (o.isSettled()) System.out.println("  Settled. Delivery can be scheduled now.");
        showStock(o.vendors());
    }

    static void scheduleDelivery() {
        Order o = pickOrder();
        if (o == null) return;
        if (o.delivery != null) { System.out.println("  Already scheduled: " + o.deliveryStatus()); return; }

        // rule 3: no delivery until paid in full
        if (!o.isSettled()) {
            System.out.printf("  Payment pending (%.2f due). Settle it before scheduling delivery.%n", o.due());
            return;
        }
        String addr = ask("Delivery address: ");
        if (addr.isEmpty()) { System.out.println("  Address needed."); return; }
        LocalDate when = askDate("Delivery date yyyy-MM-dd (blank = today): ");
        if (when.isBefore(LocalDate.now())) { System.out.println("  Can't schedule in the past."); return; }

        o.delivery = new Delivery(o, addr, when);
        System.out.println("\n  Delivery #" + o.delivery.id + " for order #" + o.id);
        System.out.println("  To:     " + addr);
        System.out.println("  Date:   " + when);
        System.out.println("  Status: " + o.delivery.status());
        showStock(o.vendors());
    }

    static void requestReturn() {
        Order o = pickOrder();
        if (o == null) return;

        System.out.println("  Items in order #" + o.id + ":");
        for (int i = 0; i < o.items.size(); i++) {
            OrderItem it = o.items.get(i);
            System.out.printf("   %d) %s (%s)  bought %d, returned %d%n", i + 1, it.product.name,
                    it.product.vendor.name, it.qty, it.getReturned());
        }
        int idx = askInt("Item #: ");
        if (idx < 1 || idx > o.items.size()) { System.out.println("  No such item."); return; }
        int qty = askInt("Qty to return: ");
        String reason = ask("Reason: ");
        // date prompt is mostly so the return window can be tested without waiting days
        LocalDate when = askDate("Return date yyyy-MM-dd (blank = today): ");

        ReturnRequest r = new ReturnRequest(o, o.items.get(idx - 1), qty, reason, when);
        r.process();
        o.returns.add(r);

        System.out.println("\n  Return #" + r.id + " decision: " + r.summary());
        showStock(o.vendors());
        printHistory(o.customer);
    }

    static void displayInventory() {
        System.out.println("\n== Inventory by vendor ==");
        if (vendors.isEmpty()) System.out.println("  No vendors yet.");
        else showStock(vendors.values());

        System.out.println("\n== Customer order history ==");
        if (history.isEmpty()) System.out.println("  No orders yet.");
        for (List<Order> list : history.values()) printHistory(list.get(0).customer);
    }

    // ---------- printing ----------

    static void showStock(Collection<Vendor> vs) {
        System.out.println("\n  Vendor inventory:");
        for (Vendor v : vs) {
            System.out.println("  Vendor #" + v.id + " - " + v.name);
            if (v.products.isEmpty()) { System.out.println("     (no products)"); continue; }
            System.out.printf("     %-4s %-18s %9s %6s%n", "ID", "Product", "Price", "Stock");
            for (Product p : v.products)
                System.out.printf("     %-4d %-18s %9.2f %6d%n", p.id, p.name, p.price, p.getStock());
        }
    }

    static void printInvoice(Order o) {
        System.out.println("\n" + LINE);
        System.out.printf("INVOICE #%d                     Date: %s%n", o.id, o.date);
        System.out.println("Customer: " + o.customer);
        System.out.println(LINE);
        System.out.printf("%-14s %-16s %4s %8s %10s%n", "Vendor", "Product", "Qty", "Price", "Amount");
        for (OrderItem i : o.items)
            System.out.printf("%-14s %-16s %4d %8.2f %10.2f%n", i.product.vendor.name, i.product.name,
                    i.qty, i.product.price, i.lineTotal());
        System.out.println(LINE);
        System.out.printf("%-14s %-16s %4s %8s %10.2f%n", "TOTAL", "", "", "", o.total());
        System.out.println("Payment:  " + o.paymentStatus());
        System.out.println("Delivery: " + o.deliveryStatus());
        System.out.println(LINE);
    }

    static void printHistory(String cust) {
        List<Order> list = history.get(cust.toLowerCase());
        if (list == null) return;

        System.out.println("\n  Order history - " + list.get(0).customer);
        double billed = 0, paid = 0, refunded = 0;
        for (Order o : list) {
            System.out.printf("   #%d  %s  total %.2f  paid %.2f  refunded %.2f  [%s | %s]%n", o.id, o.date,
                    o.total(), o.paid(), o.refunded(), o.paymentStatus(), o.deliveryStatus());
            for (OrderItem i : o.items)
                System.out.println("      " + i.qty + " x " + i.product.name + " (" + i.product.vendor.name + ")"
                        + (i.getReturned() > 0 ? "  [returned " + i.getReturned() + "]" : ""));
            for (ReturnRequest r : o.returns) System.out.println("      return #" + r.id + ": " + r.summary());
            billed += o.total();
            paid += o.paid();
            refunded += o.refunded();
        }
        System.out.printf("   Orders: %d | Billed: %.2f | Paid: %.2f | Refunded: %.2f | Net spend: %.2f%n",
                list.size(), billed, paid, refunded, paid - refunded);
    }

    // ---------- input helpers ----------

    static String ask(String prompt) {
        System.out.print(prompt);
        return sc.nextLine().trim();
    }

    static int askInt(String prompt) {
        while (true) {
            try {
                return Integer.parseInt(ask(prompt));
            } catch (NumberFormatException e) {
                System.out.println("  Whole numbers only.");
            }
        }
    }

    static double askDouble(String prompt) {
        while (true) {
            try {
                return Double.parseDouble(ask(prompt));
            } catch (NumberFormatException e) {
                System.out.println("  Enter a number.");
            }
        }
    }

    static LocalDate askDate(String prompt) {
        while (true) {
            String s = ask(prompt);
            if (s.isEmpty()) return LocalDate.now();
            try {
                return LocalDate.parse(s);
            } catch (DateTimeParseException e) {
                System.out.println("  Use yyyy-MM-dd.");
            }
        }
    }

    static Order pickOrder() {
        if (orders.isEmpty()) { System.out.println("  No confirmed orders yet."); return null; }
        System.out.print("  Orders: ");
        for (Order o : orders.values()) System.out.print("#" + o.id + "(" + o.customer + ") ");
        System.out.println();
        Order o = orders.get(askInt("Order id: "));
        if (o == null) System.out.println("  No such order.");
        return o;
    }

    static void seed() {
        Vendor a = new Vendor("Ravi Organics");
        Vendor b = new Vendor("Meena Dairy");
        vendors.put(a.id, a);
        vendors.put(b.id, b);
        for (Product p : new Product[] {
                new Product(a, "Tomatoes", 40, 50), new Product(a, "Spinach", 25, 30),
                new Product(b, "Milk", 60, 20), new Product(b, "Paneer", 120, 10) })
            products.put(p.id, p);
        System.out.println("(demo data loaded)");
    }
}

class Vendor {
    private static int seq = 1;
    final int id = seq++;
    final String name;
    final List<Product> products = new ArrayList<>();

    Vendor(String name) { this.name = name; }

    Product find(String pname) {
        for (Product p : products)
            if (p.name.equalsIgnoreCase(pname)) return p;
        return null;
    }
}

class Product {
    private static int seq = 1;
    final int id = seq++;
    final Vendor vendor;
    final String name;
    final double price;
    private int stock;

    Product(Vendor vendor, String name, double price, int stock) {
        this.vendor = vendor;
        this.name = name;
        this.price = price;
        this.stock = stock;
        vendor.products.add(this);
    }

    int getStock() { return stock; }

    boolean take(int qty) {
        if (qty > stock) return false;
        stock -= qty;
        return true;
    }

    void restock(int qty) { stock += qty; }
}

class OrderItem {
    final Product product;   // rule 5: item is tied straight to the vendor's product
    final int qty;
    private int returned;

    OrderItem(Product product, int qty) {
        this.product = product;
        this.qty = qty;
    }

    double lineTotal() { return product.price * qty; }
    int getReturned() { return returned; }
    int returnable() { return qty - returned; }
    void markReturned(int n) { returned += n; }
}

class Order {
    private static int seq = 1;
    int id; // handed out on confirm so discarded drafts don't burn ids
    final String customer;
    final LocalDate date = LocalDate.now();
    final List<OrderItem> items = new ArrayList<>();
    final List<Payment> payments = new ArrayList<>();
    final List<ReturnRequest> returns = new ArrayList<>();
    Delivery delivery;
    private boolean confirmed;
    private double refunded;

    Order(String customer) { this.customer = customer; }

    // rule 2: stock only drops here
    boolean confirm() {
        if (confirmed) return false;
        for (OrderItem i : items)
            if (i.qty > i.product.getStock()) return false;
        for (OrderItem i : items) i.product.take(i.qty);
        id = seq++;
        confirmed = true;
        return true;
    }

    double total() {
        double t = 0;
        for (OrderItem i : items) t += i.lineTotal();
        return t;
    }

    double paid() {
        double t = 0;
        for (Payment p : payments) t += p.amount;
        return t;
    }

    double due() { return total() - paid(); }
    boolean isSettled() { return due() < 0.005; }
    double refunded() { return refunded; }
    void addRefund(double amt) { refunded += amt; }

    String paymentStatus() { return isSettled() ? "PAID" : paid() > 0 ? "PARTIALLY PAID" : "UNPAID"; }

    String deliveryStatus() {
        return delivery == null ? "NOT SCHEDULED" : delivery.status() + " on " + delivery.date;
    }

    Set<Vendor> vendors() {
        Set<Vendor> s = new LinkedHashSet<>();
        for (OrderItem i : items) s.add(i.product.vendor);
        return s;
    }
}

class Payment {
    private static int seq = 1;
    final int id = seq++;
    final Order order;
    final double amount;
    final String method;
    final LocalDate date = LocalDate.now();

    Payment(Order order, double amount, String method) {
        this.order = order;
        this.amount = amount;
        this.method = method;
    }
}

class Delivery {
    private static int seq = 1;
    final int id = seq++;
    final Order order;
    final String address;
    final LocalDate date;

    Delivery(Order order, String address, LocalDate date) {
        this.order = order;
        this.address = address;
        this.date = date;
    }

    String status() { return date.isAfter(LocalDate.now()) ? "SCHEDULED" : "DELIVERED"; }
}

class ReturnRequest {
    static final int WINDOW_DAYS = 3; // produce doesn't keep, so a short window
    private static int seq = 1;
    final int id = seq++;
    final Order order;
    final OrderItem item;
    final int qty;
    final String reason;
    final LocalDate date;
    private boolean approved;
    private String note = "pending";

    ReturnRequest(Order order, OrderItem item, int qty, String reason, LocalDate date) {
        this.order = order;
        this.item = item;
        this.qty = qty;
        this.reason = reason;
        this.date = date;
    }

    // rule 4: within policy, and never more than was bought (minus earlier returns)
    void process() {
        Delivery d = order.delivery;
        if (d == null) { reject("order has no delivery on record yet"); return; }

        long days = ChronoUnit.DAYS.between(d.date, date);
        if (days < 0) { reject("not delivered yet (delivery on " + d.date + ")"); return; }
        if (days > WINDOW_DAYS) {
            reject("outside the " + WINDOW_DAYS + "-day return window (" + days + " days after delivery)");
            return;
        }
        if (qty <= 0 || qty > item.returnable()) {
            reject("qty " + qty + " is more than the " + item.returnable() + " still returnable (bought " + item.qty + ")");
            return;
        }

        item.markReturned(qty);
        item.product.restock(qty); // rule 2: stock goes back up on approved returns
        double refund = qty * item.product.price;
        order.addRefund(refund);
        approved = true;
        note = String.format("refund Rs.%.2f, %d back in %s's stock", refund, qty, item.product.vendor.name);
    }

    private void reject(String why) { note = why; }

    String summary() {
        return (approved ? "APPROVED" : "REJECTED") + " - " + qty + " x " + item.product.name
                + " - " + note + (reason.isEmpty() ? "" : " (reason: " + reason + ")");
    }
}
