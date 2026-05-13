    package com.restaurant.app;

import com.formdev.flatlaf.FlatLightLaf;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
import java.util.regex.*;
import java.sql.*;

public class MainApp {

    private static final String API_BASE_URL = "http://localhost:8080";

    private JFrame buyerFrame, sellerFrame;
    private ChatView buyerChatView, sellerInboxView;
    private JTextField buyerMessageField, buyerPhoneField, buyerAddressField;

    private String currentBuyer = "Buyer";
    private int currentOrderNumber = 0;
    private long currentOrderStartTime = 0;
    private String latestBuyerRequest = "";
    private int invoiceCounter = 1;

    private void add(Container p, Component c, GridBagConstraints gc, int x, int y, double weightx) {
        gc.gridx = x;
        gc.gridy = y;
        gc.weightx = weightx;
        p.add(c, gc);
    }

    private final List<String> buyerList = new ArrayList<>();
    private final Map<String, Integer> buyerOrderCount = new HashMap<>();
    private final Map<String, BuyerProfile> buyerProfiles = new HashMap<>();

    private final List<String> sellerList = new ArrayList<>();
    private final Map<String, SellerProfile> sellerProfiles = new HashMap<>();
    private final Map<String, Double> sellerRatings = new HashMap<>();
    private final Map<String, Integer> sellerRatingCount = new HashMap<>();

    private JPanel buyerOffersGrid, recommendationArea;
    private final Map<String, BuyerOfferCard> buyerOfferCards = new LinkedHashMap<>();
    private final Map<String, JPanel> sellerOrderPanels = new LinkedHashMap<>();
    private final Map<String, JLabel> sellerOrderFromLabels = new LinkedHashMap<>();
    private final Map<String, JLabel> sellerRatingLabels = new LinkedHashMap<>();

    private final List<OrderRecord> allOrderHistory = new ArrayList<>();
    private final List<Transaction> allTransactions = new ArrayList<>();
    private double totalRevenue = 0;

    private final Map<String, OfferMeta> latestOfferMeta = new HashMap<>();
    
    private static class CartItem {
        String sellerName;
        String itemName;
        double unitPrice;
        int qty;
        int orderNum;
        int itemIndex; // Index in the offer card
        
        CartItem(String seller, String item, double price, int q, int num, int idx) {
            this.sellerName = seller;
            this.itemName = item;
            this.unitPrice = price;
            this.qty = q;
            this.orderNum = num;
            this.itemIndex = idx;
        }
    }
    private final List<CartItem> cartItems = new ArrayList<>();
    private RoundedButton cartBtn;

    private static class ParsedResult {
        String itemName = "";
        double price = 0;
        int qty = 1;
        String size = "";
    }

    private static final Color PRIMARY_PINK = new Color(255, 183, 197); // Sakura Pink
    private static final Color SECONDARY_PINK = new Color(255, 153, 172); // Strawberry Pink
    private static final Color ACCENT_PINK = new Color(251, 111, 146); // Deep Rose
    
    private static final Color PINK_LIGHT = new Color(255, 240, 243); // Lavender Blush
    private static final Color PINK_MEDIUM = PRIMARY_PINK;
    private static final Color PINK_DARK = ACCENT_PINK;

    private static final Color ROSE_LIGHT = new Color(255, 224, 230);
    private static final Color ROSE_MEDIUM = SECONDARY_PINK;
    private static final Color ROSE_DARK = new Color(191, 54, 104); // Dark Berry

    private static final Color BG_COLOR = new Color(255, 250, 251);
    private static final Color GREEN = new Color(255, 133, 161); // Hot Pink
    private static final Color ORANGE = SECONDARY_PINK; 
    private static final Color PURPLE = new Color(224, 170, 255); // Keep a bit of Lavender for contrast
    private static final Color GOLD = new Color(255, 229, 153); // Soft Gold for stars
    private static final Color SKY = SECONDARY_PINK;

    private static final Font FONT_NORMAL = new Font("SansSerif", Font.PLAIN, 13);
    private static final Font FONT_BOLD = new Font("SansSerif", Font.BOLD, 13);
    private static final Font FONT_TITLE = new Font("SansSerif", Font.BOLD, 18);
    private static final Font FONT_HEADER = new Font("SansSerif", Font.BOLD, 20);
    private static final Font FONT_SMALL = new Font("SansSerif", Font.PLAIN, 11);
    private static final Font FONT_BUTTON = new Font("SansSerif", Font.BOLD, 14);
    private static final Font FONT_CHAT = new Font("SansSerif", Font.PLAIN, 16);
    private static final Font FONT_SYSTEM = new Font("SansSerif", Font.PLAIN, 15);

    public MainApp() {
        initData();

        buyerChatView = new ChatView(PINK_LIGHT, SKY, PINK_DARK, Color.DARK_GRAY);
        sellerInboxView = new ChatView(
                ROSE_LIGHT,
                ROSE_LIGHT,
                ROSE_DARK,
                ROSE_DARK
        );

        buildBuyerWindow();
        buildSellerWindow();

        SwingUtilities.invokeLater(() -> {
            buyerChatView.addSystem("🌸 Welcome!");
            sellerInboxView.addSystem("📥 Incoming Requests ready.");
            refreshAllSellerRatingLabels();
            refreshAllBuyerCardRatings();
        });
    }

    private void initData() {
        buyerList.add("Buyer");
        buyerOrderCount.put("Buyer", 0);
        buyerProfiles.put("Buyer", new BuyerProfile("Buyer"));

        boolean apiLoaded = loadSellersFromDatabase();
        if (!apiLoaded) loadDefaultSellers();
    }

    private boolean loadSellersFromDatabase() {
    try {
        Connection conn = DatabaseConnection.connect();
        String query = "SELECT * FROM sellers";
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery(query);

        sellerList.clear();
        sellerProfiles.clear();

        while (rs.next()) {
            String name = rs.getString("name");

            SellerProfile sp = new SellerProfile(name);
            sp.sellerId = rs.getString("sellerId");
            sp.phone = rs.getString("phone");
            sp.shortVideoUrl = rs.getString("shortVideoUrl");

            sellerList.add(name);
            sellerProfiles.put(name, sp);
            sellerRatings.put(name, 0.0);
            sellerRatingCount.put(name, 0);
        }

        conn.close();
        return true;

    } catch (Exception e) {
        System.out.println("DB Error: " + e.getMessage());
        return false;
    }
}


    private void loadDefaultSellers() {
        String[] phones = {
                "6289626937487",
                "6281313330251",
                "6281555853532",
                "62895365530096",
                "6281399893170"
        };

        for (int i = 1; i <= 5; i++) {
            String seller = "Seller " + i;
            sellerList.add(seller);
            sellerRatings.put(seller, 0.0);
            sellerRatingCount.put(seller, 0);

            SellerProfile sp = new SellerProfile(seller);
            sp.sellerId = "S-" + (1000 + i - 1);
            sp.phone = phones[i - 1];
            sp.shortVideoUrl = "https://www.youtube.com/results?search_query=food";
            sellerProfiles.put(seller, sp);
        }
    }


 private void saveOrderToDatabase(String buyer, String seller, int orderNum) {
    try {
        System.out.println(">>> DIPANGGIL SAVE ORDER");
        System.out.println(">>> MASUK SAVE ORDER");
        System.out.println("buyer: " + buyer + ", seller: " + seller);

        Connection conn = DatabaseConnection.connect();

        String query = "INSERT INTO orders (buyer, seller, orderNumber, date, time) VALUES (?, ?, ?, CURDATE(), CURTIME())";

        PreparedStatement ps = conn.prepareStatement(query);
        ps.setString(1, buyer);
        ps.setString(2, seller);
        ps.setInt(3, orderNum);

        int rows = ps.executeUpdate();

        System.out.println(">>> INSERT BERHASIL, rows = " + rows);

        conn.close();

    } catch (Exception e) {
        System.out.println(">>> ERROR INSERT: " + e.getMessage());
    }
}
    private void saveTransactionToDatabase(Transaction t) {
    try {
        Connection conn = DatabaseConnection.connect();
        String query = "INSERT INTO transactions (buyer, seller, orderId, invoiceNo, itemName, unitPrice, qty, totalAmount, paymentMethod, paymentStatus, paymentDetail, proofPath, rating, review_comment, date, time) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        PreparedStatement ps = conn.prepareStatement(query);
        ps.setString(1, t.buyerName);
        ps.setString(2, t.sellerName);
        ps.setString(3, t.orderId);
        ps.setString(4, t.invoiceNo);
        ps.setString(5, t.itemName);
        ps.setDouble(6, t.unitPrice);
        ps.setInt(7, t.qty);
        ps.setDouble(8, t.totalAmount);
        ps.setString(9, t.paymentMethod);
        ps.setString(10, t.paymentStatus);
        ps.setString(11, t.paymentDetail);
        ps.setString(12, t.proofPath);
        ps.setInt(13, t.rating);
        ps.setString(14, t.reviewComment);
        ps.setString(15, t.date);
        ps.setString(16, t.timestamp);

        ps.executeUpdate();
        conn.close();

    } catch (Exception e) {
        System.out.println("Insert transaksi gagal: " + e.getMessage());
    }
}
    private List<SellerProfile> parseSellerJson(String json) {
        List<SellerProfile> result = new ArrayList<>();
        if (json == null || json.isBlank()) return result;

        Pattern objectPattern = Pattern.compile("\\{(.*?)\\}");
        Matcher objectMatcher = objectPattern.matcher(json);

        while (objectMatcher.find()) {
            String obj = objectMatcher.group(1);

            String name = extractJsonValue(obj, "name");
            String sellerId = extractJsonValue(obj, "sellerId");
            String phone = extractJsonValue(obj, "phone");
            String shortVideoUrl = extractJsonValue(obj, "shortVideoUrl");

            if (name != null && !name.isBlank()) {
                SellerProfile sp = new SellerProfile(name);
                sp.sellerId = sellerId == null ? "" : sellerId;
                sp.phone = phone == null ? "" : phone;
                sp.shortVideoUrl = shortVideoUrl == null ? "" : shortVideoUrl;
                result.add(sp);
            }
        }
        return result;
    }

    private String extractJsonValue(String obj, String key) {
        Pattern p = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*\"(.*?)\"");
        Matcher m = p.matcher(obj);
        return m.find() ? m.group(1) : null;
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

   
    private JLabel styledLabel(String text, Font font, Color color) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(font);
        lbl.setForeground(color);
        return lbl;
    }
    // ✅ OVERLOADING
    private JLabel styledLabel(String text) {
        return styledLabel(text, FONT_NORMAL, Color.BLACK);
    }

    private JTextField styledTextField(Color borderColor) {
        JTextField tf = new JTextField();
        tf.setFont(FONT_NORMAL);
        tf.setBackground(Color.WHITE);
        tf.setBorder(new CompoundBorder(
                new LineBorder(borderColor, 2, true),
                new EmptyBorder(6, 10, 6, 10)
        ));
        return tf;
    }

    private RoundedButton styledButton(String text, Color bg, int radius) {
        RoundedButton btn = new RoundedButton(text, bg, Color.WHITE, radius);
        btn.setFont(FONT_BOLD);
        btn.setBorder(new EmptyBorder(8, 14, 8, 14));
        return btn;
    }

    private RoundedPanel styledCard(Color border, Color bg, int radius) {
        RoundedPanel p = new RoundedPanel(new BorderLayout(0, 10), radius, bg);
        p.setBorder(new CompoundBorder(new LineBorder(border, 2, true), new EmptyBorder(12, 12, 12, 12)));
        return p;
    }

    private JScrollPane transparentScroll(Component c) {
        JScrollPane sp = new JScrollPane(c);
        sp.setBorder(null);
        sp.setOpaque(false);
        sp.getViewport().setOpaque(false);
        sp.getVerticalScrollBar().setUnitIncrement(12);
        return sp;
    }

    private void buildBuyerWindow() {
        buyerFrame = new JFrame("🍕 Buyer Dashboard");
        buyerFrame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        buyerFrame.setSize(1180, 760);
        buyerFrame.setContentPane(wrapAsPage(createBuyerPanel()));
        buyerFrame.setVisible(true);

        buyerFrame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                if (sellerFrame == null || !sellerFrame.isDisplayable()) System.exit(0);
            }
        });
    }

    private void buildSellerWindow() {
        sellerFrame = new JFrame("🏪 Seller Dashboard");
        sellerFrame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        sellerFrame.setSize(1180, 760);
        sellerFrame.setContentPane(wrapAsPage(createSellerPanel()));
        sellerFrame.setVisible(true);

        if (buyerFrame != null && buyerFrame.isDisplayable()) {
            positionFramesSideBySide(buyerFrame, sellerFrame);
        }
    }

    private JComponent wrapAsPage(JComponent content) {
        JPanel page = new JPanel(new BorderLayout());
        page.setBackground(BG_COLOR);
        page.setBorder(new EmptyBorder(14, 14, 14, 14));
        page.add(content, BorderLayout.CENTER);
        return page;
    }

    private void positionFramesSideBySide(JFrame buyer, JFrame seller) {
        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();

        int gap = 10;
        int maxW = Math.max(500, screen.width / 2 - 20);
        int maxH = Math.max(600, screen.height - 80);

        int buyerW = Math.min(buyer.getWidth(), maxW);
        int sellerW = Math.min(seller.getWidth(), maxW);
        int h = Math.min(Math.max(buyer.getHeight(), seller.getHeight()), maxH);

        buyer.setSize(buyerW, h);
        seller.setSize(sellerW, h);

        int totalW = buyerW + sellerW + gap;
        int startX = Math.max(0, (screen.width - totalW) / 2);
        int y = Math.max(0, (screen.height - h) / 2);

        buyer.setLocation(startX, y);
        seller.setLocation(startX + buyerW + gap, y);
    }

    private JPanel createBuyerPanel() {
        RoundedPanel panel = styledCard(PINK_MEDIUM, Color.WHITE, 25);
        panel.setShadow(5, new Color(0, 0, 0, 15));

        JLabel title = styledLabel("🍕 RTZ Restaurant - Buyer Dashboard", FONT_HEADER, PINK_DARK);

        JPanel profileBar = new JPanel(new GridBagLayout());
        profileBar.setOpaque(false);
        profileBar.setBorder(new EmptyBorder(10, 10, 15, 10));

        JLabel buyerName = styledLabel(currentBuyer, FONT_BOLD, PINK_DARK);
        buyerPhoneField = styledTextField(PINK_MEDIUM);
        buyerAddressField = styledTextField(PINK_MEDIUM);

        BuyerProfile bp = buyerProfiles.get(currentBuyer);
        if (bp != null) {
            buyerPhoneField.setText(bp.phone);
            buyerAddressField.setText(bp.address);
        }

        RoundedButton saveProfile = styledButton("💾 Save Profile", PINK_DARK, 18);
        saveProfile.addActionListener(e -> saveBuyerProfile());

        RoundedButton openMyMapBtn = styledButton("🗺 My Location", PURPLE, 18);
        openMyMapBtn.addActionListener(e -> openBuyerMap(currentBuyer));

        RoundedButton payHistoryBtn = styledButton("💳 My Orders", ORANGE, 18);
        payHistoryBtn.addActionListener(e -> showBuyerOrders());

        RoundedButton bestBtn = styledButton("✨ Best Offers", SKY, 18);
        bestBtn.addActionListener(e -> highlightBestOffers(true));

        cartBtn = styledButton("🛒 Cart (0)", GOLD, 18);
        cartBtn.setForeground(Color.BLACK);
        cartBtn.addActionListener(e -> showCartDialog());

        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(5, 5, 5, 5);
        gc.fill = GridBagConstraints.HORIZONTAL;

        add(profileBar, styledLabel("👤 User:", FONT_BOLD, Color.BLACK), gc, 0, 0, 0);
        add(profileBar, buyerName, gc, 1, 0, 0.1);
        add(profileBar, new JLabel("📲 WA:"), gc, 2, 0, 0);
        add(profileBar, buyerPhoneField, gc, 3, 0, 0.2);
        add(profileBar, new JLabel("🏠 Address:"), gc, 4, 0, 0);
        add(profileBar, buyerAddressField, gc, 5, 0, 0.4);
        
        JPanel buttonRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttonRow.setOpaque(false);
        buttonRow.add(saveProfile);
        buttonRow.add(openMyMapBtn);
        buttonRow.add(payHistoryBtn);
        buttonRow.add(bestBtn);
        buttonRow.add(cartBtn);
        add(profileBar, buttonRow, gc, 6, 0, 0.3);

        JPanel north = new JPanel(new BorderLayout());
        north.setOpaque(false);
        north.add(title, BorderLayout.NORTH);
        north.add(profileBar, BorderLayout.CENTER);
        panel.add(north, BorderLayout.NORTH);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        split.setBorder(null);
        split.setDividerSize(8);
        split.setResizeWeight(0.35);

        RoundedPanel left = styledCard(PINK_MEDIUM, Color.WHITE, 20);
        left.add(styledLabel("💬 Chat Assistant", FONT_TITLE, PINK_DARK), BorderLayout.NORTH);
        left.add(buyerChatView.getView(), BorderLayout.CENTER);

        RoundedPanel right = styledCard(ROSE_MEDIUM, Color.WHITE, 20);
        right.add(styledLabel("🍱 Available Offers ✨", FONT_TITLE, ROSE_DARK), BorderLayout.NORTH);

        buyerOffersGrid = new JPanel();
        buyerOffersGrid.setOpaque(false);
        buyerOffersGrid.setLayout(new BoxLayout(buyerOffersGrid, BoxLayout.Y_AXIS));

        recommendationArea = new JPanel();
        recommendationArea.setOpaque(false);
        recommendationArea.setLayout(new BoxLayout(recommendationArea, BoxLayout.Y_AXIS));
        recommendationArea.setVisible(false);

        recommendationArea.setAlignmentX(Component.LEFT_ALIGNMENT);

        buyerOffersGrid.add(recommendationArea);
        buyerOffersGrid.add(Box.createRigidArea(new Dimension(0, 15)));
        
        JLabel allOffersTitle = styledLabel("🍱 All Available Offers", FONT_BOLD, Color.GRAY);
        allOffersTitle.setBorder(new EmptyBorder(0, 10, 10, 0));
        allOffersTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        buyerOffersGrid.add(allOffersTitle);

        buyerOfferCards.clear();
        for (String s : sellerList) {
            BuyerOfferCard card = new BuyerOfferCard(s);
            buyerOfferCards.put(s, card);
            card.root.setAlignmentX(Component.LEFT_ALIGNMENT);
            buyerOffersGrid.add(card.root);
            buyerOffersGrid.add(Box.createRigidArea(new Dimension(0, 10)));
        }

        right.add(transparentScroll(buyerOffersGrid), BorderLayout.CENTER);

        split.setLeftComponent(left);
        split.setRightComponent(right);
        panel.add(split, BorderLayout.CENTER);

        JPanel inputPanel = new JPanel(new BorderLayout(12, 0));
        inputPanel.setOpaque(false);
        inputPanel.setBorder(new EmptyBorder(12, 10, 10, 10));

        buyerMessageField = new JTextField();
        buyerMessageField.setFont(FONT_CHAT);
        buyerMessageField.putClientProperty("JTextField.placeholderText", "Type Message");
        
        RoundedButton sendBtn = new RoundedButton("Send ➤", PINK_DARK, Color.WHITE, 25);
        sendBtn.addActionListener(e -> sendBuyerMessage());
        buyerMessageField.addActionListener(e -> sendBuyerMessage());

        inputPanel.add(buyerMessageField, BorderLayout.CENTER);
        inputPanel.add(sendBtn, BorderLayout.EAST);
        panel.add(inputPanel, BorderLayout.SOUTH);

        return panel;
    }


    private void saveBuyerProfile() {
        BuyerProfile bp = buyerProfiles.computeIfAbsent(currentBuyer, BuyerProfile::new);
        bp.phone = buyerPhoneField.getText().trim();
        bp.address = buyerAddressField.getText().trim();
        JOptionPane.showMessageDialog(buyerFrame, "Buyer profile saved");
    }

    private void sendBuyerMessage() {
        String text = buyerMessageField.getText().trim();
        if (text.isEmpty()) return;

        String lower = text.toLowerCase();

        if (lower.startsWith("i am ") || lower.startsWith("saya ") || lower.startsWith("nama saya ") || lower.startsWith("my name is ")) {
            String name = extractNameFromMessage(text);
            if (!name.isEmpty()) {
                // Jika buyer baru (berbeda dari currentBuyer dan bukan default)
                if (!buyerList.contains(name)) buyerList.add(name);
                
                // Jika buyer baru, bersihkan sesi aktif (tapi simpan history transaksi global)
                if (!currentBuyer.equals(name) && !currentBuyer.equals("Buyer")) {
                    clearActiveSession();
                }
                buyerOrderCount.putIfAbsent(name, 0);
                currentBuyer = name;
                buyerProfiles.putIfAbsent(name, new BuyerProfile(name));

                BuyerProfile bp = buyerProfiles.get(name);
                buyerPhoneField.setText(bp.phone);
                buyerAddressField.setText(bp.address);

                buyerChatView.addSystem("👤 Switched to: " + currentBuyer + " ✨");
                buyerMessageField.setText("");
                return;
            }
        }

        buyerChatView.addSelf("🌸 " + currentBuyer, text);

        ParsedResult parsed = parseSmartText(text);
        if (parsed.price > 0 || !parsed.itemName.equalsIgnoreCase("Unknown Menu")) {
            applySmartFilter(parsed.itemName, parsed.price);
            
            String log = "🔍 Filtering: ";
            if (!parsed.itemName.equalsIgnoreCase("Unknown Menu")) log += parsed.itemName + " ";
            if (parsed.price > 0) log += "under Rp " + String.format("%,.0f", parsed.price);
            buyerChatView.addSystem(log);
        }

        boolean wantsToBuy =
                lower.contains("i want") || lower.contains("i wanna") || lower.contains("i would like") ||
                        lower.contains("order") || lower.contains("beli") || lower.contains("pesan") ||
                        lower.contains("mau beli") || lower.contains("mau pesan");

        // Only start a new broadcast if it's a clear 'buying' intent and no active order
        // OR if it's a clear NEW request (e.g. contains specific price/qty)
        boolean isNewRequest = parsed.price > 0 || lower.contains("new") || lower.contains("lain");
        
        if (wantsToBuy && (currentOrderNumber == 0 || isNewRequest)) {
            startNewOrderBroadcast(text);
        } else {
            sellerInboxView.addSystem(getCurrentTime() + " 💬 Msg from " + currentBuyer + ": " + text);
        }

        buyerMessageField.setText("");
    }

    private void clearActiveSession() {
        // Bersihkan data sesi aktif (permintaan yang sedang berjalan)
        latestOfferMeta.clear();
        cartItems.clear();
        currentOrderNumber = 0;
        updateCartButton();
        
        // Bersihkan tampilan panel order di semua seller
        for (String seller : sellerList) {
            JPanel panel = sellerOrderPanels.get(seller);
            if (panel != null) {
                panel.removeAll();
                panel.revalidate();
                panel.repaint();
            }
            
            JLabel lbl = sellerOrderFromLabels.get(seller);
            if (lbl != null) {
                lbl.setText("📝 Waiting request...");
            }
            
            BuyerOfferCard card = buyerOfferCards.get(seller);
            if (card != null) {
                card.setWaiting("", 0, "");
            }
        }
        
        // Bersihkan chat view untuk sesi baru
        // buyerChatView.addSystem("✨ Sesi baru dimulai. Keranjang belanja telah dikosongkan.");
        
        refreshAllSellerRatingLabels();
        refreshAllBuyerCardRatings();
    }

    private JPanel createSellerPanel() {
        JPanel page = new JPanel(new BorderLayout(0, 10));
        page.setOpaque(false);

        JPanel topBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 5));
        topBar.setOpaque(false);
        topBar.setBorder(new EmptyBorder(5, 5, 10, 5));

        RoundedButton historyBtn = styledButton("📋 Order History", PURPLE, 20);
        RoundedButton revenueBtn = styledButton("💰 Total Revenue", ORANGE, 20);
        RoundedButton resetBtn = styledButton("🔄 Reset All", new Color(255, 71, 87), 20);

        historyBtn.addActionListener(e -> showPurchaseHistory());
        revenueBtn.addActionListener(e -> showTotalRevenue());
        resetBtn.addActionListener(e -> resetAllData());

        topBar.add(historyBtn);
        topBar.add(revenueBtn);
        topBar.add(resetBtn);
        page.add(topBar, BorderLayout.NORTH);

        RoundedPanel left = styledCard(ROSE_MEDIUM, Color.WHITE, 20);
        left.add(styledLabel("📩 Requests Inbox", FONT_TITLE, ROSE_DARK), BorderLayout.NORTH);
        left.add(sellerInboxView.getView(), BorderLayout.CENTER);

        JPanel list = new JPanel();
        list.setOpaque(false);
        list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));

        sellerOrderPanels.clear();
        sellerOrderFromLabels.clear();
        sellerRatingLabels.clear();

        for (String s : sellerList) {
            list.add(createSellerMarketplaceCard(s));
            list.add(Box.createRigidArea(new Dimension(0, 12)));
        }

        RoundedPanel right = styledCard(ROSE_MEDIUM, Color.WHITE, 20);
        right.add(styledLabel("🏪 My Restaurants", FONT_TITLE, ROSE_DARK), BorderLayout.NORTH);
        right.add(transparentScroll(list), BorderLayout.CENTER);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, left, right);
        split.setResizeWeight(0.30);
        split.setBorder(null);
        split.setDividerSize(8);

        page.add(split, BorderLayout.CENTER);
        return page;
    }

    private JComponent createSellerMarketplaceCard(String sellerName) {
        RoundedPanel card = new RoundedPanel(new BorderLayout(0, 10), 20, Color.WHITE);
        card.setShadow(3, new Color(0, 0, 0, 12));
        card.setBorder(new CompoundBorder(new LineBorder(new Color(240, 240, 240), 1, true), new EmptyBorder(15, 15, 15, 15)));

        SellerProfile sp = sellerProfiles.get(sellerName);

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);

        JLabel title = styledLabel("🏪 " + sellerName, FONT_TITLE, ROSE_DARK);
        JLabel rating = styledLabel("⭐ 0.0 (0)", FONT_BOLD, GOLD);
        sellerRatingLabels.put(sellerName, rating);

        header.add(title, BorderLayout.WEST);
        header.add(rating, BorderLayout.EAST);

        JPanel infoLine = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        infoLine.setOpaque(false);
        infoLine.add(styledLabel("ID: " + safe(sp.sellerId), FONT_SMALL, Color.GRAY));
        infoLine.add(styledLabel("📞 " + safe(sp.phone), FONT_SMALL, Color.GRAY));

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);

        RoundedButton waBtn = new RoundedButton("📲 WA", new Color(34, 139, 34), Color.WHITE, 12);
        RoundedButton videoBtn = new RoundedButton("🎬 Video", PURPLE, Color.WHITE, 12);
        RoundedButton editBtn = new RoundedButton("⚙ Edit", ORANGE, Color.WHITE, 12);

        for (RoundedButton b : new RoundedButton[]{waBtn, videoBtn, editBtn}) {
            b.setFont(FONT_SMALL);
            b.setPreferredSize(new Dimension(80, 28));
        }

        waBtn.addActionListener(e -> openWhatsApp(sp.phone, "Hi " + sellerName));
        videoBtn.addActionListener(e -> openUrl(buildYoutubeSearchUrl(null)));
        editBtn.addActionListener(e -> editSellerProfileDialog(sellerName));

        actions.add(waBtn);
        actions.add(videoBtn);
        actions.add(editBtn);

        JLabel orderLbl = styledLabel("📝 Waiting for request...", FONT_SMALL, Color.LIGHT_GRAY);
        sellerOrderFromLabels.put(sellerName, orderLbl);

        JPanel topWrap = new JPanel();
        topWrap.setOpaque(false);
        topWrap.setLayout(new BoxLayout(topWrap, BoxLayout.Y_AXIS));
        topWrap.add(header);
        topWrap.add(Box.createRigidArea(new Dimension(0, 4)));
        topWrap.add(infoLine);
        topWrap.add(Box.createRigidArea(new Dimension(0, 8)));
        topWrap.add(orderLbl);
        topWrap.add(Box.createRigidArea(new Dimension(0, 8)));
        topWrap.add(actions);

        card.add(topWrap, BorderLayout.NORTH);

        JPanel orders = new JPanel();
        orders.setOpaque(false);
        orders.setLayout(new BoxLayout(orders, BoxLayout.Y_AXIS));
        orders.setBorder(new EmptyBorder(5, 5, 5, 5));
        sellerOrderPanels.put(sellerName, orders);

        JScrollPane spn = new JScrollPane(orders);
        spn.setBorder(new LineBorder(new Color(245, 245, 245), 1, true));
        spn.getViewport().setBackground(new Color(250, 250, 250));
        spn.getVerticalScrollBar().setUnitIncrement(12);

        card.add(spn, BorderLayout.CENTER);
        return card;
    }

    private void startNewOrderBroadcast(String requestText) {
        latestBuyerRequest = requestText;

        int currentCount = buyerOrderCount.getOrDefault(currentBuyer, 0) + 1;
        buyerOrderCount.put(currentBuyer, currentCount);
        currentOrderNumber = currentCount;
        currentOrderStartTime = System.currentTimeMillis();
        latestOfferMeta.clear();

        for (String s : sellerList) {
            BuyerOfferCard card = buyerOfferCards.get(s);
            if (card != null) card.setWaiting(currentBuyer, currentOrderNumber, requestText);
        }

        buyerChatView.addSystem(getCurrentTime() + " ✅ Request broadcasted to ALL sellers.");
        sellerInboxView.addSystem(getCurrentTime() + " 📝 New request from " + currentBuyer +
                " (Order #" + currentOrderNumber + "): " + requestText);

        for (String sellerName : sellerList) {
            JLabel lbl = sellerOrderFromLabels.get(sellerName);
            if (lbl != null) {
                lbl.setText(getCurrentTime() + " 📝 " + currentBuyer + " (Order #" + currentOrderNumber + ")");
            }

            JPanel panel = sellerOrderPanels.get(sellerName);
            if (panel != null) {
                panel.add(createSellerOfferForm(sellerName, currentBuyer, currentOrderNumber, requestText));
                panel.add(Box.createRigidArea(new Dimension(0, 10)));
                panel.revalidate();
                panel.repaint();
            }
        }
    }

    private JComponent createSellerOfferForm(String sellerName, String buyerName, int orderNum, String requestText) {
        RoundedPanel bubble = new RoundedPanel(new BorderLayout(0, 10), 20, PINK_LIGHT);
        bubble.setBorder(new EmptyBorder(15, 15, 15, 15));
        bubble.setAlignmentX(Component.CENTER_ALIGNMENT);

        BuyerProfile bp = buyerProfiles.getOrDefault(buyerName, new BuyerProfile(buyerName));
        String buyerAddress = (bp.address == null || bp.address.isBlank()) ? "-" : bp.address;
        String buyerPhone = (bp.phone == null || bp.phone.isBlank()) ? "-" : bp.phone;

        JPanel top = new JPanel();
        top.setOpaque(false);
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));

        top.add(styledLabel(getCurrentTime() + " 🧾 Offer for " + buyerName + " (#" + orderNum + ")", FONT_TITLE, ROSE_DARK));
        top.add(Box.createRigidArea(new Dimension(0, 5)));
        top.add(styledLabel("Request: " + requestText, FONT_BOLD, new Color(80, 80, 80)));
        top.add(Box.createRigidArea(new Dimension(0, 4)));
        top.add(styledLabel("📞 Phone: " + buyerPhone, FONT_SMALL, new Color(100, 100, 100)));
        top.add(Box.createRigidArea(new Dimension(0, 2)));
        top.add(styledLabel("📍 Address: " + buyerAddress, FONT_SMALL, new Color(100, 100, 100)));

        RoundedButton buyerMapBtn = new RoundedButton("🗺 Open Map", PURPLE, Color.WHITE, 15);
        buyerMapBtn.setFont(FONT_BOLD);
        buyerMapBtn.setBorder(new EmptyBorder(8, 12, 8, 12));
        buyerMapBtn.addActionListener(e -> openBuyerMap(buyerName));

        RoundedButton buyerWaBtn = new RoundedButton("📲 Chat Buyer", new Color(46, 204, 113), Color.WHITE, 15);
        buyerWaBtn.setFont(FONT_BOLD);
        buyerWaBtn.setBorder(new EmptyBorder(8, 12, 8, 12));
        buyerWaBtn.addActionListener(e -> {
            BuyerProfile buyerProfile = buyerProfiles.get(buyerName);
            if (buyerProfile == null || buyerProfile.phone == null || buyerProfile.phone.isBlank()) {
                JOptionPane.showMessageDialog(
                        sellerFrame,
                        "Buyer phone belum diisi.",
                        "Info",
                        JOptionPane.INFORMATION_MESSAGE
                );
                return;
            }

            openWhatsApp(
                    buyerProfile.phone,
                    "Hi " + buyerName + ", saya " + sellerName + ". Saya ingin konfirmasi order #" + orderNum
            );
        });

        JPanel topButtons = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 5));
        topButtons.setOpaque(false);
        topButtons.add(buyerMapBtn);
        topButtons.add(buyerWaBtn);

        top.add(Box.createRigidArea(new Dimension(0, 4)));
        top.add(topButtons);

        bubble.add(top, BorderLayout.NORTH);

        ParsedResult parsed = parseSmartText(requestText);
        
        JPanel rows = new JPanel();
        rows.setOpaque(false);
        rows.setLayout(new BoxLayout(rows, BoxLayout.Y_AXIS));

        JTextField[] menuFields = new JTextField[3];
        JTextField[] unitPriceFields = new JTextField[3];
        JSpinner[] stockSpinners = new JSpinner[3];
        @SuppressWarnings("unchecked")
        JComboBox<Integer>[] ratingCombo = new JComboBox[3];

        JLabel totalLabel = styledLabel("Potential Value: Rp 0", FONT_HEADER, ORANGE);

        Runnable recalcTotal = () -> {
            double sum = 0;
            for (int i = 0; i < 3; i++) {
                double unit = parsePriceSafe(unitPriceFields[i].getText());
                int stock = (Integer) stockSpinners[i].getValue();
                if (unit > 0 && stock > 0) sum += unit * stock;
            }
            totalLabel.setText("Potential Value: Rp " + String.format("%,.0f", sum));
        };

        for (int i = 0; i < 3; i++) {
            rows.add(createSellerFormRowWithStock(i, menuFields, unitPriceFields, stockSpinners, ratingCombo, recalcTotal, (i == 0 ? parsed : null)));
            if (i < 2) rows.add(Box.createRigidArea(new Dimension(0, 8)));
        }

        bubble.add(rows, BorderLayout.CENTER);

        RoundedButton send = new RoundedButton("Send Offer ✔", ROSE_DARK, Color.WHITE, 22);
        send.setFont(FONT_HEADER);
        send.setBorder(new EmptyBorder(12, 20, 12, 20));

        send.addActionListener(e -> {
            boolean ok = true;
            String[] items = new String[3];
            String[] unitPrices = new String[3];
            int[] stock = new int[3];
            int[] ratings = new int[3];

            for (int i = 0; i < 3; i++) {
                items[i] = menuFields[i].getText().trim();
                unitPrices[i] = unitPriceFields[i].getText().trim();
                stock[i] = (Integer) stockSpinners[i].getValue();
                ratings[i] = (Integer) ratingCombo[i].getSelectedItem();
                if (items[i].isEmpty() || unitPrices[i].isEmpty() || stock[i] <= 0) ok = false;
            }

            if (!ok) {
                sellerInboxView.addSystem("⚠️ " + sellerName + ": Fill all menu, unit price, and stock!");
                return;
            }

            double total = 0;
            for (int i = 0; i < 3; i++) {
                // Use requested qty from the broadcast for comparison, 
                // but keep stock for inventory management
                total += parsePriceSafe(unitPrices[i]) * parsed.qty;
            }

            long responseMs = System.currentTimeMillis() - currentOrderStartTime;
            latestOfferMeta.put(sellerName, new OfferMeta(sellerName, total, responseMs));

            BuyerOfferCard card = buyerOfferCards.get(sellerName);
            if (card != null) {
                card.updateOffer(items, unitPrices, stock, ratings, total, responseMs);
            }

            addToOrderHistory(buyerName, sellerName, orderNum, items, unitPrices, stock, ratings);

            sellerInboxView.addSystem("✅ " + sellerName + " sent offer to " + buyerName + " (#" + orderNum + ") " + getCurrentTime());
            buyerChatView.addSystem("🔔 New offer received from " + sellerName + "! Click 'Best Offers' to see summary.");
            
            send.setEnabled(false);
            send.setText("✓ Sent");
            send.setBackground(new Color(200, 200, 200));

            highlightBestOffers(false);
        });

        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setOpaque(false);
        bottom.add(totalLabel, BorderLayout.WEST);
        bottom.add(send, BorderLayout.EAST);

        bubble.add(bottom, BorderLayout.SOUTH);
        return bubble;
    }

    private JPanel createSellerFormRowWithStock(int index, JTextField[] menuFields, JTextField[] unitPriceFields,
                                                JSpinner[] stockSpinners, JComboBox<Integer>[] ratingCombo,
                                                Runnable recalcTotal, ParsedResult autoFill) {
        JPanel row = new JPanel(new GridBagLayout());
        row.setOpaque(false);
        row.setBorder(new CompoundBorder(
            new LineBorder(new Color(245, 245, 245), 1, true),
            new EmptyBorder(10, 12, 10, 12)
        ));

        JLabel itemLabel = styledLabel((index + 1) + ".", FONT_BOLD, new Color(150, 150, 150));
        
        JTextField menuField = styledTextField(ROSE_LIGHT);
        menuField.putClientProperty("JTextField.placeholderText", "Menu Name...");
        menuField.setMinimumSize(new Dimension(150, 36));

        JTextField unitPriceField = styledTextField(ROSE_LIGHT);
        unitPriceField.putClientProperty("JTextField.placeholderText", "0");
        unitPriceField.setMinimumSize(new Dimension(80, 36));
        unitPriceField.getDocument().addDocumentListener(SimpleDocListener.onChange(recalcTotal));
        if (autoFill != null && autoFill.price > 0) {
            unitPriceField.setText(String.format("%.0f", autoFill.price));
        }

        JSpinner stockSpin = new JSpinner(new SpinnerNumberModel(1, 1, 999, 1));
        stockSpin.setMinimumSize(new Dimension(60, 36));
        stockSpin.addChangeListener(e -> recalcTotal.run());
        if (autoFill != null && autoFill.qty > 1) {
            stockSpin.setValue(autoFill.qty);
        }

        Integer[] ratings = {5, 4, 3, 2, 1};
        ratingCombo[index] = new JComboBox<>(ratings);
        ratingCombo[index].setFont(FONT_BOLD);
        ratingCombo[index].setMinimumSize(new Dimension(60, 36));

        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(0, 5, 0, 5);
        gc.fill = GridBagConstraints.BOTH;

        // Number
        gc.gridx = 0; gc.weightx = 0.05; row.add(itemLabel, gc);
        
        // Menu Field
        gc.gridx = 1; gc.weightx = 0.4; row.add(menuField, gc);
        
        // Price Field
        gc.gridx = 2; gc.weightx = 0.25; row.add(wrapPriceField(unitPriceField), gc);
        
        // Stock
        gc.gridx = 3; gc.weightx = 0.15; row.add(wrapStockSpinner(stockSpin), gc);
        
        // Rating
        gc.gridx = 4; gc.weightx = 0.15; row.add(wrapRatingCombo(ratingCombo[index]), gc);

        menuFields[index] = menuField;
        unitPriceFields[index] = unitPriceField;
        stockSpinners[index] = stockSpin;
        return row;
    }

    private JPanel wrapPriceField(JTextField field) {
        JPanel p = new JPanel(new BorderLayout(6, 0));
        p.setOpaque(false);
        p.add(styledLabel("Rp ", FONT_BOLD, ORANGE), BorderLayout.WEST);
        p.add(field, BorderLayout.CENTER);
        return p;
    }

    private JPanel wrapStockSpinner(JSpinner spinner) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        p.setOpaque(false);
        p.add(styledLabel("Stock:", FONT_BOLD, Color.BLACK));
        p.add(spinner);
        return p;
    }

    private JPanel wrapRatingCombo(JComboBox<Integer> combo) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        p.setOpaque(false);
        p.add(styledLabel("⭐", FONT_BOLD, GOLD));
        p.add(combo);
        return p;
    }

    private void applySmartFilter(String keyword, double maxPrice) {
        recommendationArea.removeAll();
        recommendationArea.setVisible(false);
        boolean hasMatch = false;

        // Reset all card borders
        for (BuyerOfferCard card : buyerOfferCards.values()) {
            card.root.setBorder(new CompoundBorder(new LineBorder(new Color(240, 240, 240), 1, true), new EmptyBorder(15, 15, 15, 15)));
        }

        for (BuyerOfferCard card : buyerOfferCards.values()) {
            for (int i = 0; i < 3; i++) {
                if (card.items[i] == null || card.items[i].equalsIgnoreCase("Waiting...")) continue;
                
                boolean nameMatch = keyword.equalsIgnoreCase("Unknown Menu") || 
                                   card.items[i].toLowerCase().contains(keyword.toLowerCase());
                
                double price = parsePriceSafe(card.unitPrices[i]);
                boolean priceMatch = maxPrice <= 0 || price <= maxPrice;
                
                if (nameMatch && priceMatch) {
                    hasMatch = true;
                    recommendationArea.add(createMiniSuggestedItem(card.sellerName, card.items[i], card.unitPrices[i], card.stock[i], i));
                    recommendationArea.add(Box.createRigidArea(new Dimension(0, 8)));
                }
            }
        }
        
        if (hasMatch) {
            recommendationArea.setVisible(true);
            JLabel header = styledLabel("✨ Suggested for Your Request", FONT_HEADER, PINK_DARK);
            header.setBorder(new EmptyBorder(10, 10, 10, 10));
            header.setAlignmentX(Component.LEFT_ALIGNMENT);
            recommendationArea.add(header, 0); 
            
            buyerChatView.addSystem("💡 Found specific matches for your request! See them at the top.");
        }

        buyerOffersGrid.revalidate();
        buyerOffersGrid.repaint();
    }

    private JPanel createMiniSuggestedItem(String seller, String item, String price, int stock, int itemIdx) {
        RoundedPanel p = new RoundedPanel(new BorderLayout(15, 0), 15, Color.WHITE);
        p.setBorder(new CompoundBorder(new LineBorder(PINK_DARK, 2, true), new EmptyBorder(10, 15, 10, 15)));
        p.setMaximumSize(new Dimension(1000, 70));

        JPanel left = new JPanel(new GridLayout(2, 1));
        left.setOpaque(false);
        left.add(styledLabel("🍴 " + item, FONT_BOLD, PINK_DARK));
        left.add(styledLabel("🏪 " + seller, FONT_SMALL, Color.GRAY));

        JPanel center = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 0));
        center.setOpaque(false);
        center.add(styledLabel("Rp " + price, FONT_BOLD, ORANGE));
        center.add(styledLabel("Stock: " + stock, FONT_SMALL, Color.BLACK));

        RoundedButton addBtn = new RoundedButton("Add to Cart 🛒", ROSE_DARK, Color.WHITE, 15);
        addBtn.setFont(FONT_SMALL);
        addBtn.setPreferredSize(new Dimension(120, 30));
        addBtn.addActionListener(e -> {
            int qty = askBuyerQty(item, stock, price);
            if (qty > 0) addItemToCart(seller, item, price, qty, currentOrderNumber, itemIdx);
        });

        p.add(left, BorderLayout.WEST);
        p.add(center, BorderLayout.CENTER);
        p.add(addBtn, BorderLayout.EAST);
        p.setAlignmentX(Component.LEFT_ALIGNMENT);
        return p;
    }

    private void addItemToCart(String seller, String item, String priceStr, int qty, int orderNum, int itemIdx) {
        double unit = parsePriceSafe(priceStr);
        cartItems.add(new CartItem(seller, item, unit, qty, orderNum, itemIdx));
        updateCartButton();
        buyerChatView.addSelf("🌸 " + currentBuyer, "Added " + item + " x" + qty + " to cart.");
    }

    private void highlightBestOffers(boolean showChatSummary) {
        if (latestOfferMeta.isEmpty()) {
            if (showChatSummary) buyerChatView.addSystem("⏳ No offers received yet. Please wait for sellers to respond...");
            return;
        }

        String cheapestSeller = null;
        String expensiveSeller = null;
        String fastestSeller = null;

        double cheapest = Double.MAX_VALUE;
        double expensive = -1;
        long fastest = Long.MAX_VALUE;

        for (OfferMeta meta : latestOfferMeta.values()) {
            if (meta.totalPrice < cheapest) {
                cheapest = meta.totalPrice;
                cheapestSeller = meta.sellerName;
            }
            if (meta.totalPrice > expensive) {
                expensive = meta.totalPrice;
                expensiveSeller = meta.sellerName;
            }
            if (meta.responseMs < fastest) {
                fastest = meta.responseMs;
                fastestSeller = meta.sellerName;
            }
        }

        for (String seller : sellerList) {
            BuyerOfferCard card = buyerOfferCards.get(seller);
            if (card == null) continue;

            card.setBestTags(
                    seller.equals(cheapestSeller),
                    seller.equals(expensiveSeller),
                    seller.equals(fastestSeller)
            );
        }

        if (showChatSummary) {
            StringBuilder sb = new StringBuilder("🏆 Best Offer Summary\n");
            if (cheapestSeller != null) sb.append("💸 Cheapest: ").append(cheapestSeller).append("\n");
            if (expensiveSeller != null) sb.append("💰 Most Expensive: ").append(expensiveSeller).append("\n");
            if (fastestSeller != null) sb.append("⚡ Fastest: ").append(fastestSeller).append("\n");
            buyerChatView.addSystem(sb.toString());
        }
    }


    private class BuyerOfferCard {
        final String sellerName;
        final RoundedPanel root;
        final JLabel statusLabel, ratingLabel, bestTagLabel, infoMetaLabel;
        final JLabel[] menuLbl = new JLabel[3];
        final JLabel[] unitPriceLbl = new JLabel[3];
        final JLabel[] stockLbl = new JLabel[3];
        final JLabel[] starsLbl = new JLabel[3];
        final RoundedButton[] chooseBtn = new RoundedButton[3];

        int orderNum = 0;
        String[] items = new String[3];
        String[] unitPrices = new String[3];
        int[] stock = new int[3];
        int[] ratings = new int[3];

        BuyerOfferCard(String sellerName) {
            this.sellerName = sellerName;

            root = new RoundedPanel(new BorderLayout(0, 10), 18, Color.WHITE);
            root.setShadow(3, new Color(0, 0, 0, 10));
            root.setAlignmentX(Component.LEFT_ALIGNMENT);
            root.setBorder(new CompoundBorder(
                    new LineBorder(new Color(240, 240, 240), 1, true),
                    new EmptyBorder(15, 15, 15, 15)
            ));

            SellerProfile sp = sellerProfiles.get(sellerName);

            JPanel header = new JPanel(new BorderLayout());
            header.setOpaque(false);

            JLabel title = styledLabel("🏪 " + sellerName, FONT_TITLE, PINK_DARK);
            ratingLabel = styledLabel("⭐ 0.0 (0)", FONT_BOLD, GOLD);
            
            header.add(title, BorderLayout.WEST);
            header.add(ratingLabel, BorderLayout.EAST);

            JPanel infoBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
            infoBar.setOpaque(false);
            infoBar.add(styledLabel("ID: " + safe(sp.sellerId), FONT_SMALL, Color.GRAY));
            infoBar.add(styledLabel("📞 " + safe(sp.phone), FONT_SMALL, Color.GRAY));

            RoundedButton waBtn = new RoundedButton("💬 Chat", new Color(37, 211, 102), Color.WHITE, 12);
            RoundedButton videoBtn = new RoundedButton("🎬 Video", PURPLE, Color.WHITE, 12);
            RoundedButton mapBtn = new RoundedButton("📍 Map", SKY, Color.WHITE, 12);

            for (RoundedButton b : new RoundedButton[]{waBtn, videoBtn, mapBtn}) {
                b.setFont(FONT_SMALL);
                b.setPreferredSize(new Dimension(80, 26));
            }

            waBtn.addActionListener(e -> openWhatsApp(sp.phone, "Hi " + sellerName + "!"));
            videoBtn.addActionListener(e -> {
                String q = (items[0] != null) ? items[0] : "food";
                openUrl(buildYoutubeSearchUrl(q));
            });
            mapBtn.addActionListener(e -> openSellerMap(sellerName));

            infoBar.add(waBtn);
            infoBar.add(videoBtn);
            infoBar.add(mapBtn);

            statusLabel = styledLabel("📝 Waiting for request...", FONT_SMALL, Color.LIGHT_GRAY);
            bestTagLabel = styledLabel("", FONT_BOLD, SKY);
            infoMetaLabel = styledLabel("", FONT_SMALL, Color.GRAY);

            JPanel north = new JPanel();
            north.setOpaque(false);
            north.setLayout(new BoxLayout(north, BoxLayout.Y_AXIS));
            north.add(header);
            north.add(Box.createRigidArea(new Dimension(0, 5)));
            north.add(infoBar);
            north.add(Box.createRigidArea(new Dimension(0, 8)));
            north.add(statusLabel);
            north.add(bestTagLabel);
            north.add(infoMetaLabel);
            north.add(Box.createRigidArea(new Dimension(0, 10)));

            root.add(north, BorderLayout.NORTH);

            JPanel rows = new JPanel();
            rows.setOpaque(false);
            rows.setLayout(new BoxLayout(rows, BoxLayout.Y_AXIS));

            for (int i = 0; i < 3; i++) {
                rows.add(buildRow(i));
                if (i < 2) rows.add(Box.createRigidArea(new Dimension(0, 8)));
            }

            root.add(rows, BorderLayout.CENTER);
            setWaiting("", 0, "");
        }

        private JPanel buildRow(int idx) {
            JPanel row = new JPanel(new GridBagLayout());
            row.setOpaque(false);
            row.setBorder(new CompoundBorder(
                new LineBorder(new Color(245, 245, 245), 1, true),
                new EmptyBorder(8, 10, 8, 10)
            ));

            menuLbl[idx] = new JLabel("Waiting...");
            menuLbl[idx].setFont(FONT_BOLD);
            
            unitPriceLbl[idx] = new JLabel("---");
            stockLbl[idx] = new JLabel("Stock: -");
            starsLbl[idx] = new JLabel("");
            
            chooseBtn[idx] = new RoundedButton("Add to Cart 🛒", ROSE_DARK, Color.WHITE, 15);
            chooseBtn[idx].setFont(FONT_BOLD);
            chooseBtn[idx].setPreferredSize(new Dimension(130, 32));
            chooseBtn[idx].setEnabled(false);

            GridBagConstraints gc = new GridBagConstraints();
            gc.fill = GridBagConstraints.HORIZONTAL;
            gc.insets = new Insets(2, 5, 2, 5);

            gc.gridx = 0; gc.weightx = 0.4; row.add(menuLbl[idx], gc);
            gc.gridx = 1; gc.weightx = 0.2; row.add(unitPriceLbl[idx], gc);
            gc.gridx = 2; gc.weightx = 0.15; row.add(stockLbl[idx], gc);
            gc.gridx = 3; gc.weightx = 0.15; row.add(starsLbl[idx], gc);
            gc.gridx = 4; gc.weightx = 0.1; row.add(chooseBtn[idx], gc);

            chooseBtn[idx].addActionListener(e -> addToCart(idx));

            return row;
        }

        void setWaiting(String buyerName, int orderNum, String requestText) {
            this.orderNum = orderNum;
            statusLabel.setText(orderNum <= 0 ? "📝 No request yet" : "⏳ Waiting for offer... (#" + orderNum + ")");
            statusLabel.setForeground(orderNum <= 0 ? Color.LIGHT_GRAY : ORANGE);
            
            bestTagLabel.setText("");
            infoMetaLabel.setText("");

            for (int i = 0; i < 3; i++) {
                items[i] = null;
                unitPrices[i] = null;
                stock[i] = 0;
                ratings[i] = 0;
                menuLbl[i].setText("Waiting...");
                menuLbl[i].setForeground(Color.LIGHT_GRAY);
                unitPriceLbl[i].setText("---");
                stockLbl[i].setText("-");
                starsLbl[i].setText("");
                chooseBtn[i].setEnabled(false);
                chooseBtn[i].setText("Order Now");
            }
        }

        void updateOffer(String[] items, String[] unitPrices, int[] stock, int[] ratings,
                         double grandTotal, long responseMs) {
            this.items = items.clone();
            this.unitPrices = unitPrices.clone();
            this.stock = stock.clone();
            this.ratings = ratings.clone();

            statusLabel.setText("✅ Offer Ready!");
            statusLabel.setForeground(GREEN.darker());
            
            infoMetaLabel.setText("Total: Rp " + String.format("%,.0f", grandTotal) + " | " + (responseMs/1000.0) + "s");

            for (int i = 0; i < 3; i++) {
                menuLbl[i].setText(items[i]);
                menuLbl[i].setForeground(Color.DARK_GRAY);
                unitPriceLbl[i].setText("Rp " + unitPrices[i]);
                stockLbl[i].setText("Stock: " + stock[i]);
                starsLbl[i].setText("⭐".repeat(Math.max(0, ratings[i])));
                chooseBtn[i].setEnabled(stock[i] > 0);
            }
            refreshRatingLabel();
        }

        void setBestTags(boolean cheapest, boolean expensive, boolean fastest) {
            List<String> tags = new ArrayList<>();
            if (cheapest) tags.add("💸 Cheapest");
            if (expensive) tags.add("💰 Most Expensive");
            if (fastest) tags.add("⚡ Fastest");

            bestTagLabel.setText(String.join("   ", tags));

            if (!tags.isEmpty()) {
                root.setBorder(new CompoundBorder(new LineBorder(SKY, 2, true), new EmptyBorder(15, 15, 15, 15)));
            } else {
                root.setBorder(new CompoundBorder(new LineBorder(new Color(240, 240, 240), 1, true), new EmptyBorder(15, 15, 15, 15)));
            }
            root.revalidate();
            root.repaint();
        }

        void refreshRatingLabel() {
            double r = sellerRatings.getOrDefault(sellerName, 0.0);
            int c = sellerRatingCount.getOrDefault(sellerName, 0);
            ratingLabel.setText("⭐ " + String.format("%.1f", r) + " (" + c + ")");
        }

        private void addToCart(int index) {
            if (items[index] == null) return;

            if (stock[index] <= 0) {
                JOptionPane.showMessageDialog(buyerFrame, "Stock untuk item ini sudah habis.", "Sold out", JOptionPane.WARNING_MESSAGE);
                return;
            }

            int selectedQty = askBuyerQty(items[index], stock[index], unitPrices[index]);
            if (selectedQty <= 0) return;

            addItemToCart(sellerName, items[index], unitPrices[index], selectedQty, orderNum, index);
            
            // Temporary visual feedback
            chooseBtn[index].setText("Added! ✅");
            javax.swing.Timer t = new javax.swing.Timer(1500, e -> {
                chooseBtn[index].setText("Add More ➕");
            });
            t.setRepeats(false);
            t.start();
        }
    }

    private void updateCartButton() {
        if (cartBtn != null) {
            cartBtn.setText("🛒 Cart (" + cartItems.size() + ")");
        }
    }

    private void showCartDialog() {
        if (cartItems.isEmpty()) {
            JOptionPane.showMessageDialog(buyerFrame, "Cart is empty! Let's go shopping! 🍕", "Empty Cart", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        JDialog dialog = new JDialog(buyerFrame, "My Shopping Cart 🛒", true);
        dialog.setSize(550, 600);
        dialog.setLocationRelativeTo(buyerFrame);

        JPanel main = new JPanel(new BorderLayout(15, 15));
        main.setBackground(Color.WHITE);
        main.setBorder(new EmptyBorder(20, 20, 20, 20));

        JLabel title = styledLabel("🛍️ Your Shopping Cart", FONT_TITLE, PINK_DARK);
        main.add(title, BorderLayout.NORTH);

        JPanel list = new JPanel();
        list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
        list.setOpaque(false);

        double grandTotal = 0;
        for (int i = 0; i < cartItems.size(); i++) {
            CartItem item = cartItems.get(i);
            double sub = item.unitPrice * item.qty;
            grandTotal += sub;

            JPanel row = new RoundedPanel(new BorderLayout(10, 0), 15, PINK_LIGHT);
            row.setBorder(new EmptyBorder(10, 15, 10, 15));
            row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));

            JLabel info = styledLabel("<html><b>" + item.itemName + "</b><br><font color='gray'>" + 
                                     item.sellerName + " | x" + item.qty + " @ Rp " + String.format("%,.0f", item.unitPrice) + "</font></html>", 
                                     FONT_NORMAL, Color.BLACK);
            JLabel price = styledLabel("Rp " + String.format("%,.0f", sub), FONT_BOLD, PINK_DARK);
            
            int finalI = i;
            JButton delBtn = new JButton("❌");
            delBtn.setBorderPainted(false);
            delBtn.setContentAreaFilled(false);
            delBtn.addActionListener(e -> {
                cartItems.remove(finalI);
                updateCartButton();
                dialog.dispose();
                showCartDialog();
            });

            row.add(info, BorderLayout.CENTER);
            row.add(price, BorderLayout.EAST);
            row.add(delBtn, BorderLayout.WEST);

            list.add(row);
            list.add(Box.createRigidArea(new Dimension(0, 10)));
        }

        JScrollPane scroll = transparentScroll(list);
        main.add(scroll, BorderLayout.CENTER);

        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);

        JLabel totalLbl = styledLabel("Total: Rp " + String.format("%,.0f", grandTotal), FONT_HEADER, PINK_DARK);
        RoundedButton checkoutBtn = new RoundedButton("Checkout & Pay 💳", PINK_DARK, Color.WHITE, 25);
        
        double finalGrandTotal = grandTotal;
        checkoutBtn.addActionListener(e -> {
            dialog.dispose();
            processCheckout(finalGrandTotal);
        });

        footer.add(totalLbl, BorderLayout.WEST);
        footer.add(checkoutBtn, BorderLayout.EAST);
        main.add(footer, BorderLayout.SOUTH);

        dialog.add(main);
        dialog.setVisible(true);
    }

    private void processCheckout(double total) {
        PaymentResult pay = showPaymentDialog("Bulk Checkout", total, 1, total);
        if (pay == null) return;

        String invoiceNo = generateInvoiceNo();
        for (CartItem item : cartItems) {
            double sub = item.unitPrice * item.qty;
            
            // Update UI Stock on the card if visible
            BuyerOfferCard card = buyerOfferCards.get(item.sellerName);
            if (card != null) {
                card.stock[item.itemIndex] -= item.qty;
                card.stockLbl[item.itemIndex].setText("Stock: " + card.stock[item.itemIndex]);
            }

            addTransaction(currentBuyer, item.sellerName, String.valueOf(item.orderNum), invoiceNo, item.itemName, 
                           item.unitPrice, item.qty, sub, pay.method, pay.status, pay.detail, pay.proofPath);
            saveOrderToDatabase(currentBuyer, item.sellerName, item.orderNum);

            // Update ratings
            double currentRating = sellerRatings.getOrDefault(item.sellerName, 0.0);
            int currentCount = sellerRatingCount.getOrDefault(item.sellerName, 0);
            double newRating = ((currentRating * currentCount) + 5) / (currentCount + 1); // Default 5 stars for checkout
            sellerRatings.put(item.sellerName, newRating);
            sellerRatingCount.put(item.sellerName, currentCount + 1);
        }

        buyerChatView.addSelf("🌸 " + currentBuyer, "✅ Checkout Successful! Invoice: " + invoiceNo);
        sellerInboxView.addSystem("🧾 Bulk Order received from " + currentBuyer + " (Inv: " + invoiceNo + ")");
        
        JOptionPane.showMessageDialog(buyerFrame, "Payment Successful! Thank you for ordering! ✨", "Success", JOptionPane.INFORMATION_MESSAGE);

        // Trigger bulk rating for all items in this checkout
        if (!cartItems.isEmpty()) {
            int itemCount = cartItems.size();
            List<Transaction> newTrans = allTransactions.subList(allTransactions.size() - itemCount, allTransactions.size());
            showBulkRatingDialog(newTrans);
        }

        clearActiveSession();
    }

    private int askBuyerQty(String item, int maxStock, String unitPrice) {
        JTextField qtyField = new JTextField("1");

        JPanel p = new JPanel(new GridLayout(0, 1, 8, 8));
        p.add(new JLabel("Item: " + item));
        p.add(new JLabel("Unit Price: Rp " + unitPrice));
        p.add(new JLabel("Available Stock: " + maxStock));
        p.add(new JLabel("Choose Quantity:"));
        p.add(qtyField);

        int ok = JOptionPane.showConfirmDialog(
                buyerFrame,
                p,
                "Choose Quantity",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE
        );

        if (ok != JOptionPane.OK_OPTION) return 0;

        int qty;
        try {
            qty = Integer.parseInt(qtyField.getText().trim());
        } catch (Exception e) {
            JOptionPane.showMessageDialog(
                    buyerFrame,
                    "Please input a valid number.",
                    "Invalid Quantity",
                    JOptionPane.ERROR_MESSAGE
            );
            return 0;
        }

        if (qty <= 0) {
            JOptionPane.showMessageDialog(
                    buyerFrame,
                    "Quantity must be more than 0.",
                    "Invalid Quantity",
                    JOptionPane.ERROR_MESSAGE
            );
            return 0;
        }

        if (qty > maxStock) {
            JOptionPane.showMessageDialog(
                    buyerFrame,
                    "Stock not available / not enough.\nAvailable stock: " + maxStock,
                    "Stock Warning",
                    JOptionPane.WARNING_MESSAGE
            );
            return 0;
        }

        return qty;
    }

    private void refreshAllSellerRatingLabels() {
        for (String s : sellerList) {
            JLabel lbl = sellerRatingLabels.get(s);
            if (lbl != null) {
                double r = sellerRatings.getOrDefault(s, 0.0);
                int c = sellerRatingCount.getOrDefault(s, 0);
                lbl.setText("⭐ " + String.format("%.1f", r) + " (" + c + ")");
            }
        }
    }

    private void refreshAllBuyerCardRatings() {
        for (String s : sellerList) {
            BuyerOfferCard card = buyerOfferCards.get(s);
            if (card != null) card.refreshRatingLabel();
        }
    }



    private void editSellerProfileDialog(String sellerName) {
        SellerProfile sp = sellerProfiles.get(sellerName);
        if (sp == null) return;
        JTextField idField = new JTextField(sp.sellerId);
        JTextField phoneField = new JTextField(sp.phone);
        JTextField videoField = new JTextField(sp.shortVideoUrl);
        JTextField addressField = new JTextField(sp.address);

        JPanel p = new JPanel(new GridLayout(0, 1, 8, 8));
        p.add(new JLabel("Seller ID:"));
        p.add(idField);
        p.add(new JLabel("Phone:"));
        p.add(phoneField);
        p.add(new JLabel("Short Video URL:"));
        p.add(videoField);
        p.add(new JLabel("Seller Address:"));
        p.add(addressField);

        int ok = JOptionPane.showConfirmDialog(
                sellerFrame,
                p,
                "Edit Seller Profile - " + sellerName,
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE
        );

        if (ok == JOptionPane.OK_OPTION) {
            sp.sellerId = idField.getText().trim();
            sp.phone = normalizeIndonesianPhone(phoneField.getText().trim());
            sp.shortVideoUrl = videoField.getText().trim();
            sp.address = addressField.getText().trim();
        }
    }

    private void openBuyerMap(String buyerName) {
        BuyerProfile bp = buyerProfiles.get(buyerName);

        if (bp == null || bp.address == null || bp.address.isBlank()) {
            JOptionPane.showMessageDialog(
                    buyerFrame,
                    "Buyer address belum diisi.",
                    "Info",
                    JOptionPane.INFORMATION_MESSAGE
            );
            return;
        }

        try {
            String q = URLEncoder.encode(bp.address, StandardCharsets.UTF_8);
            openUrl("https://www.google.com/maps/search/?api=1&query=" + q);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(
                    buyerFrame,
                    "Gagal membuka map: " + e.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }

    private void openSellerMap(String sellerName) {
        SellerProfile sp = sellerProfiles.get(sellerName);

        if (sp == null || sp.address == null || sp.address.isBlank()) {
            JOptionPane.showMessageDialog(null, "Seller address belum diisi");
            return;
        }

        try {
            String q = URLEncoder.encode(sp.address, StandardCharsets.UTF_8);
            openUrl("https://www.google.com/maps/search/?api=1&query=" + q);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void openWhatsApp(String phone, String message) {
        if (phone == null || phone.trim().isEmpty()) {
            JOptionPane.showMessageDialog(buyerFrame, "Phone belum di-set.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String cleaned = normalizeIndonesianPhone(phone);
        String text = URLEncoder.encode(message == null ? "" : message, StandardCharsets.UTF_8);
        openUrl("https://wa.me/" + cleaned + "?text=" + text);
    }
    private void openWhatsApp(String phone) {
        openWhatsApp(phone, "");
    }
    private String normalizeIndonesianPhone(String phone) {
        if (phone == null) return "";
        String cleaned = phone.replaceAll("[^0-9]", "");

        if (cleaned.startsWith("0")) {
            cleaned = "62" + cleaned.substring(1);
        } else if (!cleaned.startsWith("62") && !cleaned.isEmpty()) {
            cleaned = "62" + cleaned;
        }

        return cleaned;
    }

    private void openUrl(String url) {
        try {
            if (url == null || url.trim().isEmpty()) return;
            Desktop.getDesktop().browse(new URI(url.trim()));
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(buyerFrame, "Cannot open link:\n" + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    private String extractFoodFromRequest(String text) {
    if (text == null) return "food";

    text = text.toLowerCase();

    // hapus kata-kata tidak penting
    text = text.replaceAll("i want to buy", "")
               .replaceAll("i want", "")
               .replaceAll("i wanna", "")
               .replaceAll("i would like", "")
               .replaceAll("mau beli", "")
               .replaceAll("mau pesan", "")
               .replaceAll("beli", "")
               .replaceAll("pesan", "")
               .trim();

    return text.isEmpty() ? "food" : text;
}

    private String extractNameFromMessage(String message) {
        String[] patterns = {"i am ", "saya ", "nama saya ", "my name is "};
        String lower = message.toLowerCase();

        for (String p : patterns) {
            if (lower.startsWith(p)) {
                String name = message.substring(p.length()).trim().replaceAll("[.,!?;:]$", "").trim();
                if (!name.isEmpty()) {
                    return name.substring(0, 1).toUpperCase() + name.substring(1);
                }
            }
        }
        return "";
    }

    private String getCurrentTime() {
        return "[" + new SimpleDateFormat("HH:mm").format(new java.util.Date()) + "]";
    }

    private String getCurrentDate() {
        return new SimpleDateFormat("yyyy-MM-dd").format(new java.util.Date());
    }

    private ParsedResult parseSmartText(String text) {
        ParsedResult res = new ParsedResult();
        if (text == null || text.isEmpty()) return res;

        // 1. Detect Quantity (e.g. 2x, 3*)
        Pattern qtyPat = Pattern.compile("(\\d+)\\s*[x*]");
        Matcher qtyMat = qtyPat.matcher(text);
        if (qtyMat.find()) {
            res.qty = Integer.parseInt(qtyMat.group(1));
        }

        // 2. Detect Price (e.g. 15k, 15000, 15.000)
        Pattern pricePat = Pattern.compile("(\\d+)\\s*k|(\\d+(?:[.,]\\d{3})+)");
        Matcher priceMat = pricePat.matcher(text);
        if (priceMat.find()) {
            String p1 = priceMat.group(1);
            String p2 = priceMat.group(2);
            if (p1 != null) {
                res.price = Double.parseDouble(p1) * 1000;
            } else if (p2 != null) {
                res.price = parsePriceSafe(p2);
            }
        }

        // 3. Detect Size/Weight (e.g. 400gr, 250cc, 500ml)
        Pattern sizePat = Pattern.compile("(\\d+)\\s*(gr|gram|cc|ml|l|oz)", Pattern.CASE_INSENSITIVE);
        Matcher sizeMat = sizePat.matcher(text);
        if (sizeMat.find()) {
            res.size = sizeMat.group(1) + " " + sizeMat.group(2);
        }

        // 4. Detect Item Name (Cleaning up tags and common phrases)
        String clean = text.replaceAll("(?i)\\d+\\s*[x*]|\\d+\\s*k|\\d+(?:[.,]\\d{3})+|\\d+\\s*(gr|gram|cc|ml|l|oz)", "").trim();
        
        // Strip common "I want to buy" phrases
        String[] prefixes = {
            "(?i)i want to buy", "(?i)i wanna buy", "(?i)i want", "(?i)i would like to order",
            "(?i)mau beli", "(?i)mau pesan", "(?i)saya mau", "(?i)beli", "(?i)pesan", "(?i)order"
        };
        for (String p : prefixes) {
            clean = clean.replaceAll("^" + p + "\\s*", "");
        }
        
        clean = clean.replaceAll("[\\!\\?\\#\\@\\$\\^]", "").trim();
        res.itemName = clean.isEmpty() ? "Unknown Menu" : clean;

        return res;
    }

    private double parsePriceSafe(String raw) {
        if (raw == null) return -1;
        String cleaned = raw.replaceAll("[^0-9.,]", "");
        if (cleaned.isEmpty()) return -1;

        if (cleaned.contains(".") && !cleaned.contains(",")) {
            cleaned = cleaned.replace(".", "");
        } else if (cleaned.contains(",") && !cleaned.contains(".")) {
            cleaned = cleaned.replace(",", ".");
        } else {
            cleaned = cleaned.replace(".", "").replace(",", ".");
        }

        try {
            return Double.parseDouble(cleaned);
        } catch (Exception e) {
            return -1;
        }
    }

    private String safe(String s) {
        return (s == null || s.trim().isEmpty()) ? "-" : s.trim();
    }

    private String buildYoutubeSearchUrl(String query) {
        String q = (query == null || query.trim().isEmpty()) ? "food drink" : query.trim();
        return "https://www.youtube.com/results?search_query=" +
                URLEncoder.encode(q + " recipe food drink", StandardCharsets.UTF_8);
    }

    private String generateInvoiceNo() {
        String date = new SimpleDateFormat("yyyyMMdd").format(new java.util.Date());
        String no = String.format("INV-%s-%03d", date, invoiceCounter);
        invoiceCounter++;
        return no;
    }


 private void addToOrderHistory(String buyer, String seller, int orderNum,
                               String[] items, String[] unitPrices, int[] stock, int[] ratings) {
    allOrderHistory.add(new OrderRecord(buyer, seller, orderNum, items, unitPrices, stock, ratings));
    
    saveOrderToDatabase(buyer, seller, orderNum);
}


    private Transaction addTransaction(String buyer, String seller, String orderId, String invoiceNo, String itemName,
                                       double unitPrice, int qty, double total, String method, String status,
                                       String detail, String proofPath) {
        Transaction t = new Transaction(buyer, seller, orderId, invoiceNo, itemName, unitPrice, qty, total, method, status, detail, proofPath);
        allTransactions.add(t);
       saveTransactionToDatabase(t);
        if ("PAID".equalsIgnoreCase(status)) totalRevenue += total;
        return t;
    }

    private void showPurchaseHistory() {
        if (allTransactions.isEmpty()) {
            sellerInboxView.addSystem("🧾 No purchases yet.");
            return;
        }

        Map<String, List<Transaction>> byBuyer = new LinkedHashMap<>();
        for (Transaction t : allTransactions) {
            byBuyer.computeIfAbsent(t.buyerName, k -> new ArrayList<>()).add(t);
        }

        StringBuilder sb = new StringBuilder("🧾 ORDER HISTORY (Bought Items)\n====================================\n\n");
        double grandTotal = 0;

        for (Map.Entry<String, List<Transaction>> entry : byBuyer.entrySet()) {
            String buyer = entry.getKey();
            List<Transaction> list = entry.getValue();

            sb.append("👤 Buyer: ").append(buyer).append("\n");
            sb.append("------------------------------------\n");

            double buyerTotalPaid = 0;
            for (Transaction t : list) {
                sb.append("• ").append(t.date).append(" ").append(t.timestamp)
                        .append(" | ").append(t.invoiceNo)
                        .append(" | ").append(t.itemName)
                        .append(" | Seller: ").append(t.sellerName)
                        .append(" | unit Rp ").append(String.format("%,.0f", t.unitPrice))
                        .append(" | qty ").append(t.qty)
                        .append(" | total Rp ").append(String.format("%,.0f", t.totalAmount))
                        .append(" | ").append(t.paymentMethod).append(" (").append(t.paymentStatus).append(")\n");

                if ("PAID".equalsIgnoreCase(t.paymentStatus)) buyerTotalPaid += t.totalAmount;
            }

            sb.append("TOTAL PAID ").append(buyer).append(": Rp ")
                    .append(String.format("%,.0f", buyerTotalPaid)).append("\n\n");

            grandTotal += buyerTotalPaid;
        }

        sb.append("====================================\n");
        sb.append("GRAND TOTAL PAID: Rp ").append(String.format("%,.0f", grandTotal)).append("\n");

        showLargeDialog("Order History (with Payment)", sb.toString());
    }

    private void showBuyerOrders() {
        List<Transaction> myTrans = allTransactions.stream()
                .filter(t -> t.buyerName.equalsIgnoreCase(currentBuyer))
                .toList();

        if (myTrans.isEmpty()) {
            JOptionPane.showMessageDialog(buyerFrame, "You haven't ordered anything yet. Let's go! 🍕", "No Orders", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        StringBuilder sb = new StringBuilder("🛍️ YOUR ORDER HISTORY\n====================================\n\n");
        double totalSpent = 0;

        for (Transaction t : myTrans) {
            sb.append("• ").append(t.date).append(" ").append(t.timestamp)
                    .append("\n  Invoice: ").append(t.invoiceNo)
                    .append("\n  Item: ").append(t.itemName)
                    .append(" x").append(t.qty)
                    .append(" | Rp ").append(String.format("%,.0f", t.totalAmount))
                    .append("\n  Status: ").append(t.paymentStatus)
                    .append("\n  Seller: ").append(t.sellerName)
                    .append("\n\n");

            if ("PAID".equalsIgnoreCase(t.paymentStatus)) totalSpent += t.totalAmount;
        }

        sb.append("------------------------------------\n");
        sb.append("TOTAL SPENT: Rp ").append(String.format("%,.0f", totalSpent));

        showLargeDialogWithInvoice("My Orders", sb.toString(), myTrans);
    }

    private void showLargeDialogWithInvoice(String title, String content, List<Transaction> trans) {
        JTextArea area = new JTextArea(content);
        area.setFont(new Font("Monospaced", Font.PLAIN, 12));
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setEditable(false);

        JScrollPane sp = new JScrollPane(area);
        sp.setPreferredSize(new Dimension(500, 400));

        JPanel main = new JPanel(new BorderLayout());
        main.add(sp, BorderLayout.CENTER);

        if (!trans.isEmpty()) {
            // Group by invoice to show the latest bulk receipt
            String lastInv = trans.get(trans.size() - 1).invoiceNo;
            List<Transaction> lastInvItems = trans.stream()
                .filter(t -> t.invoiceNo.equals(lastInv))
                .toList();

            JButton invBtn = new JButton("📄 Download Receipt (" + lastInv + " - " + lastInvItems.size() + " items)");
            invBtn.addActionListener(e -> exportBulkInvoiceAsImage(lastInvItems));
            main.add(invBtn, BorderLayout.SOUTH);
        }

        JOptionPane.showMessageDialog(buyerFrame, main, title, JOptionPane.INFORMATION_MESSAGE);
    }

    private void showBulkRatingDialog(List<Transaction> items) {
        JPanel container = new JPanel();
        container.setLayout(new BoxLayout(container, BoxLayout.Y_AXIS));
        container.setOpaque(false);

        @SuppressWarnings("unchecked")
        JComboBox<Integer>[] starBoxes = new JComboBox[items.size()];
        JTextField[] commentFields = new JTextField[items.size()];

        for (int i = 0; i < items.size(); i++) {
            Transaction t = items.get(i);
            JPanel itemPanel = new RoundedPanel(new BorderLayout(10, 5), 15, PINK_LIGHT);
            itemPanel.setBorder(new EmptyBorder(10, 15, 10, 15));
            itemPanel.setMaximumSize(new Dimension(500, 120));

            JLabel nameLbl = styledLabel("🍴 " + t.itemName + " (from " + t.sellerName + ")", FONT_BOLD, PINK_DARK);
            
            JPanel ratingRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
            ratingRow.setOpaque(false);
            
            starBoxes[i] = new JComboBox<>(new Integer[]{5, 4, 3, 2, 1});
            ratingRow.add(new JLabel("Rating:"));
            ratingRow.add(starBoxes[i]);
            
            commentFields[i] = new JTextField(15);
            commentFields[i].putClientProperty("JTextField.placeholderText", "Write a review...");
            ratingRow.add(new JLabel("Review:"));
            ratingRow.add(commentFields[i]);

            itemPanel.add(nameLbl, BorderLayout.NORTH);
            itemPanel.add(ratingRow, BorderLayout.CENTER);

            container.add(itemPanel);
            container.add(Box.createRigidArea(new Dimension(0, 10)));
        }

        JScrollPane scroll = new JScrollPane(container);
        scroll.setPreferredSize(new Dimension(550, 400));
        scroll.setBorder(null);

        int ok = JOptionPane.showConfirmDialog(buyerFrame, scroll, "Rate your Meal ✨", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (ok == JOptionPane.OK_OPTION) {
            for (int i = 0; i < items.size(); i++) {
                Transaction t = items.get(i);
                t.rating = (Integer) starBoxes[i].getSelectedItem();
                t.reviewComment = commentFields[i].getText().trim();
                
                // Update seller rating
                double curR = sellerRatings.getOrDefault(t.sellerName, 0.0);
                int curC = sellerRatingCount.getOrDefault(t.sellerName, 0);
                double newR = ((curR * curC) + t.rating) / (curC + 1);
                sellerRatings.put(t.sellerName, newR);
                sellerRatingCount.put(t.sellerName, curC + 1);
                
                updateTransactionInDatabase(t);
            }
            
            refreshAllSellerRatingLabels();
            refreshAllBuyerCardRatings();
            buyerChatView.addSystem("💖 Thank you for the bulk reviews!");
        }
    }

    private void updateTransactionInDatabase(Transaction t) {
        try {
            Connection conn = DatabaseConnection.connect();
            String q = "UPDATE transactions SET rating = ?, review_comment = ? WHERE invoiceNo = ? AND itemName = ?";
            PreparedStatement ps = conn.prepareStatement(q);
            ps.setInt(1, t.rating);
            ps.setString(2, t.reviewComment);
            ps.setString(3, t.invoiceNo);
            ps.setString(4, t.itemName);
            ps.executeUpdate();
            conn.close();
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void exportBulkInvoiceAsImage(List<Transaction> items) {
        if (items.isEmpty()) return;
        Transaction first = items.get(0);
        
        int w = 450, h = 400 + (items.size() * 50); // Dynamic height
        java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(w, h, java.awt.image.BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, w, h);
        
        g.setColor(PINK_LIGHT);
        g.fillRect(0, 0, w, 80);
        
        g.setColor(PINK_DARK);
        g.setFont(FONT_HEADER);
        g.drawString("RTZ RESTAURANT", 120, 45);
        g.setFont(FONT_SMALL);
        g.drawString("Official Bulk Receipt", 170, 65);
        
        g.setColor(Color.BLACK);
        g.setFont(FONT_BOLD);
        int y = 120;
        g.drawString("INVOICE: " + first.invoiceNo, 40, y); y += 30;
        g.setFont(FONT_NORMAL);
        g.drawString("Date: " + first.date + " " + first.timestamp, 40, y); y += 40;
        
        g.drawLine(40, y, 410, y); y += 30;
        
        g.setFont(FONT_BOLD);
        g.drawString("Item", 40, y);
        g.drawString("Qty", 280, y);
        g.drawString("Total", 350, y); y += 25;
        
        double grandTotal = 0;
        g.setFont(FONT_NORMAL);
        for (Transaction t : items) {
            g.drawString(t.itemName, 40, y);
            g.drawString(String.valueOf(t.qty), 280, y);
            g.drawString(String.format("%,.0f", t.totalAmount), 350, y);
            y += 30;
            grandTotal += t.totalAmount;
        }
        
        y += 10;
        g.drawLine(40, y, 410, y); y += 40;
        
        g.setFont(FONT_BOLD);
        g.drawString("GRAND TOTAL", 40, y);
        g.setColor(PINK_DARK);
        g.drawString("Rp " + String.format("%,.0f", grandTotal), 320, y); y += 50;
        
        g.setColor(Color.GRAY);
        g.setFont(FONT_SMALL);
        g.drawString("Payment Method: " + first.paymentMethod, 40, y); y += 20;
        g.drawString("Buyer: " + first.buyerName, 40, y); y += 60;
        
        g.setColor(PINK_DARK);
        g.setFont(FONT_BOLD);
        g.drawString("Thank you for your bulk order! ✨", 100, y);
        
        g.dispose();
        
        try {
            File output = new File("Receipt_" + first.invoiceNo + ".png");
            javax.imageio.ImageIO.write(img, "png", output);
            JOptionPane.showMessageDialog(buyerFrame, "Bulk Receipt saved as: " + output.getAbsolutePath(), "Success", JOptionPane.INFORMATION_MESSAGE);
            Desktop.getDesktop().open(output);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showPaymentHistory() {
        if (allTransactions.isEmpty()) {
            showLargeDialog("Payment History", "💳 PAYMENT HISTORY\n\nNo payments yet.");
            return;
        }

        StringBuilder sb = new StringBuilder("💳 PAYMENT HISTORY\n====================================\n\n");
        double paid = 0;
        double pending = 0;

        for (Transaction t : allTransactions) {
            sb.append("• ").append(t.date).append(" ").append(t.timestamp)
                    .append(" | Invoice: ").append(t.invoiceNo)
                    .append(" | Buyer: ").append(t.buyerName)
                    .append(" | Seller: ").append(t.sellerName)
                    .append(" | ").append(t.itemName)
                    .append(" | total Rp ").append(String.format("%,.0f", t.totalAmount))
                    .append(" | ").append(t.paymentMethod)
                    .append(" (").append(t.paymentStatus).append(")")
                    .append(" | Detail: ").append(t.paymentDetail == null || t.paymentDetail.isBlank() ? "-" : t.paymentDetail)
                    .append("\n");

            if ("PAID".equalsIgnoreCase(t.paymentStatus)) paid += t.totalAmount;
            else pending += t.totalAmount;
        }

        sb.append("\n------------------------------------\n");
        sb.append("TOTAL PAID: Rp ").append(String.format("%,.0f", paid)).append("\n");
        sb.append("TOTAL PENDING: Rp ").append(String.format("%,.0f", pending)).append("\n");

        showLargeDialog("Payment History", sb.toString());
    }

    private void showTotalRevenue() {
        if (allTransactions.isEmpty()) {
            showLargeDialog("Total Revenue", "💰 TOTAL REVENUE\n\nNo sales yet.");
            return;
        }

        Map<String, Double> sellerRevenue = new LinkedHashMap<>();
        double grand = 0;

        for (Transaction t : allTransactions) {
            if (!"PAID".equalsIgnoreCase(t.paymentStatus)) continue;
            sellerRevenue.put(t.sellerName, sellerRevenue.getOrDefault(t.sellerName, 0.0) + t.totalAmount);
            grand += t.totalAmount;
        }

        StringBuilder sb = new StringBuilder("💰 TOTAL REVENUE (PAID only)\n================================\n\n");
        sb.append("Total Transactions: ").append(allTransactions.size()).append("\n");
        sb.append("Total Customers: ").append(new HashSet<>(buyerList).size()).append("\n");
        sb.append("Total Sellers: ").append(sellerList.size()).append("\n\n");
        sb.append("Revenue per Seller:\n");

        for (String s : sellerList) {
            sb.append(" - ").append(s).append(": Rp ")
                    .append(String.format("%,.0f", sellerRevenue.getOrDefault(s, 0.0))).append("\n");
        }

        sb.append("\n--------------------------------\n");
        sb.append("GRAND TOTAL REVENUE: Rp ").append(String.format("%,.0f", grand)).append("\n");

        showLargeDialog("Total Revenue", sb.toString());
    }

    private void resetAllData() {
        int confirm = JOptionPane.showConfirmDialog(
                sellerFrame,
                "⚠️ Reset ALL data?\nThis cannot be undone.",
                "Confirm Reset",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );

        if (confirm != JOptionPane.YES_OPTION) return;

        allTransactions.clear();
        allOrderHistory.clear();
        totalRevenue = 0;
        latestOfferMeta.clear();
        invoiceCounter = 1;

        for (String s : sellerList) {
            sellerRatings.put(s, 0.0);
            sellerRatingCount.put(s, 0);

            JPanel p = sellerOrderPanels.get(s);
            if (p != null) {
                p.removeAll();
                p.revalidate();
                p.repaint();
            }

            JLabel l = sellerOrderFromLabels.get(s);
            if (l != null) l.setText("📝 Waiting request...");

            BuyerOfferCard card = buyerOfferCards.get(s);
            if (card != null) card.setWaiting("", 0, "");
        }

        sellerInboxView.addSystem("🔄 All data reset.");
        buyerChatView.addSystem("🔄 All data reset.");

        refreshAllSellerRatingLabels();
        refreshAllBuyerCardRatings();
    }

    private void showLargeDialog(String title, String content) {
        JTextArea area = new JTextArea(content);
        area.setFont(new Font("Monospaced", Font.PLAIN, 12));
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setEditable(false);

        JScrollPane sp = new JScrollPane(area);
        sp.setPreferredSize(new Dimension(900, 560));

        JOptionPane.showMessageDialog(sellerFrame, sp, title, JOptionPane.INFORMATION_MESSAGE);
    }

    private static class PaymentResult {
        String method, status, detail, proofPath;

        PaymentResult(String method, String status, String detail, String proofPath) {
            this.method = method;
            this.status = status;
            this.detail = detail;
            this.proofPath = proofPath;
        }
    }

    private PaymentResult showPaymentDialog(String item, double unit, int qty, double total) {
        JComboBox<String> paymentBox = new JComboBox<>(
                new String[]{"Cash", "Transfer Bank", "E-Wallet"}
        );

        JCheckBox paid = new JCheckBox("Mark as PAID", true);

        JPanel bankPanel = new JPanel(new GridLayout(4, 1));
        bankPanel.add(new JLabel("Bank name (for Transfer):"));
        JTextField bankField = new JTextField();
        bankPanel.add(bankField);

        bankPanel.add(new JLabel("Transfer reference (for Transfer):"));
        JTextField refField = new JTextField();
        bankPanel.add(refField);

        bankPanel.setVisible(false);

        JPanel walletPanel = new JPanel(new GridLayout(4, 1));

        walletPanel.add(new JLabel("E-Wallet provider:"));
        JComboBox<String> ewalletBox = new JComboBox<>(new String[]{"OVO", "GoPay", "Dana"});
        walletPanel.add(ewalletBox);

        walletPanel.add(new JLabel("E-Wallet number:"));
        JTextField ewalletNumber = new JTextField();
        walletPanel.add(ewalletNumber);

        walletPanel.setVisible(false);

        paymentBox.addActionListener(e -> {
            String selected = (String) paymentBox.getSelectedItem();

            bankPanel.setVisible("Transfer Bank".equals(selected));
            walletPanel.setVisible("E-Wallet".equals(selected));

            // refresh ukuran dialog
            SwingUtilities.getWindowAncestor(paymentBox).pack();
        });

        JLabel proofLabel = new JLabel("No file selected");
        JButton uploadBtn = new JButton("Upload Proof");
        final String[] proofPath = {""};

        uploadBtn.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            int result = chooser.showOpenDialog(buyerFrame);
            if (result == JFileChooser.APPROVE_OPTION) {
                File file = chooser.getSelectedFile();
                proofPath[0] = file.getAbsolutePath();
                proofLabel.setText(file.getName());
            }
        });

        JLabel info = new JLabel("<html><b>Payment</b><br/>" +
                "Item: " + item + "<br/>" +
                "Unit: Rp " + String.format("%,.0f", unit) + "<br/>" +
                "Qty: " + qty + "<br/>" +
                "Total: Rp " + String.format("%,.0f", total) + "</html>");

        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));

        p.add(info);
        p.add(Box.createRigidArea(new Dimension(0, 10)));
        p.add(paymentBox);
        p.add(bankPanel);
        p.add(walletPanel);
        p.add(paid);
        p.add(uploadBtn);
        p.add(proofLabel);

        int ok = JOptionPane.showConfirmDialog(
                buyerFrame,
                p,
                "Payment",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE
        );

        if (ok != JOptionPane.OK_OPTION) return null;

        String selectedMethod = (String) paymentBox.getSelectedItem();
        String status = paid.isSelected() ? "PAID" : "PENDING";
        String detail = "";

        if ("Cash".equals(selectedMethod)) {
            detail = "Cash Payment";
        } else if ("Transfer Bank".equals(selectedMethod)) {
            String bank = bankField.getText().trim();
            String ref = refField.getText().trim();

            if (bank.isEmpty() || ref.isEmpty()) {
                JOptionPane.showMessageDialog(buyerFrame,
                        "Bank name dan transfer reference wajib diisi.",
                        "Validation Error",
                        JOptionPane.ERROR_MESSAGE);
                return null;
            }

            detail = bank + " - Ref: " + ref;
        } else if ("E-Wallet".equals(selectedMethod)) {
            String provider = (String) ewalletBox.getSelectedItem();
            String walletNo = ewalletNumber.getText().trim();

            if (walletNo.isEmpty()) {
                JOptionPane.showMessageDialog(buyerFrame,
                        "Nomor e-wallet wajib diisi.",
                        "Validation Error",
                        JOptionPane.ERROR_MESSAGE);
                return null;
            }

            detail = provider + " - " + walletNo;
        }

        return new PaymentResult(selectedMethod, status, detail, proofPath[0]);
    }

    private class ChatView {
        private final JPanel messages;
        private final JScrollPane scrollPane;
        private final Color selfColor, otherColor, selfTextColor, otherTextColor;
        private final Component bottomGlue;

        ChatView(Color selfColor, Color otherColor, Color selfTextColor, Color otherTextColor) {
            this.selfColor = selfColor;
            this.otherColor = otherColor;
            this.selfTextColor = selfTextColor;
            this.otherTextColor = otherTextColor;

            messages = new JPanel();
            messages.setLayout(new BoxLayout(messages, BoxLayout.Y_AXIS));
            messages.setBackground(Color.WHITE);
            messages.setBorder(new EmptyBorder(15, 15, 15, 15));

            bottomGlue = Box.createVerticalGlue();
            messages.add(bottomGlue);

            scrollPane = new JScrollPane(messages);
            scrollPane.setBorder(null);
            scrollPane.getViewport().setBackground(Color.WHITE);
            scrollPane.getVerticalScrollBar().setUnitIncrement(16);
            
            SwingUtilities.invokeLater(() -> scrollPane.getVerticalScrollBar().setValue(0));
        }

        JComponent getView() {
            return scrollPane;
        }

        void addSelf(String who, String text) {
            addTextBubble(text, true);
        }

        void addOther(String who, String text) {
            addTextBubble(text, false);
        }

        void addSystem(String text) {
            JPanel wrap = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 10));
            wrap.setOpaque(false);

            RoundedPanel bubble = new RoundedPanel(new BorderLayout(), 25, new Color(242, 245, 248));
            bubble.setBorder(new EmptyBorder(10, 16, 10, 16));

            JTextArea ta = bubbleText(text, FONT_SYSTEM, new Color(120, 120, 120));
            bubble.add(ta, BorderLayout.CENTER);
            wrap.add(bubble);

            insertBeforeGlue(wrap);
            refresh();
            scrollToBottom();
        }

        private void addTextBubble(String text, boolean isSelf) {
            ParsedResult parsed = parseSmartText(text);
            if (!isSelf && parsed.price > 0) {
                // If receiving an offer from someone else, log the detect
                addSystem("🏷️ Detected Offer: " + parsed.itemName + " @ Rp " + String.format("%,.0f", parsed.price));
            }

            JPanel wrap = new JPanel(new FlowLayout(isSelf ? FlowLayout.RIGHT : FlowLayout.LEFT, 0, 5));
            wrap.setOpaque(false);

            Color bg = isSelf ? selfColor : otherColor;
            RoundedPanel bubble = new RoundedPanel(new BorderLayout(), 25, bg);
            bubble.setShadow(3, new Color(0, 0, 0, 10));
            bubble.setBorder(new EmptyBorder(12, 18, 12, 18));

            JTextArea textArea = bubbleText(text, FONT_CHAT, isSelf ? selfTextColor : otherTextColor);
            bubble.add(textArea, BorderLayout.CENTER);
            
            // Add shadow-like border for bubbles
            bubble.setBorder(new CompoundBorder(
                new LineBorder(bg.darker().darker(), 0, true), // Hide border, just use shadow
                new EmptyBorder(12, 18, 12, 18)
            ));

            wrap.add(bubble);
            insertBeforeGlue(wrap);
            refresh();
            scrollToBottom();
        }

        private JTextArea bubbleText(String text, Font font, Color color) {
            JTextArea ta = new JTextArea(text);
            ta.setFont(font);
            ta.setForeground(color);
            ta.setOpaque(false);
            ta.setEditable(false);
            ta.setLineWrap(true);
            ta.setWrapStyleWord(true);
            ta.setBorder(null);
            ta.setColumns(20);
            int width = 350;
            ta.setSize(width, ta.getPreferredSize().height);
            return ta;
        }

        private void insertBeforeGlue(Component c) {
            messages.add(c, messages.getComponentCount() - 1);
        }

        private void refresh() {
            messages.revalidate();
            messages.repaint();
        }

        private void scrollToBottom() {
            SwingUtilities.invokeLater(() -> {
                JScrollBar verticalBar = scrollPane.getVerticalScrollBar();
                verticalBar.setValue(verticalBar.getMaximum());
            });
        }
    }

    private static class RoundedPanel extends JPanel {
        private final int radius;
        private final Color bgColor;
        private int shadowSize = 0;
        private Color shadowColor = new Color(0, 0, 0, 30);

        RoundedPanel(LayoutManager layout, int radius, Color bgColor) {
            super(layout);
            this.radius = radius;
            this.bgColor = bgColor;
            setOpaque(false);
        }

        void setShadow(int size, Color color) {
            this.shadowSize = size;
            this.shadowColor = color;
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            if (shadowSize > 0) {
                g2.setColor(shadowColor);
                for (int i = 0; i < shadowSize; i++) {
                    g2.fillRoundRect(i, i, getWidth() - (i * 2), getHeight() - (i * 2), radius, radius);
                }
            }

            g2.setColor(bgColor);
            g2.fillRoundRect(0, 0, getWidth() - shadowSize, getHeight() - shadowSize, radius, radius);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    private static class RoundedButton extends JButton {
        private Color bgColor;
        private final int radius;

        RoundedButton(String text, Color bgColor, Color fgColor, int radius) {
            super(text);
            this.bgColor = bgColor;
            this.radius = radius;

            setOpaque(false);
            setFocusPainted(false);
            setBorderPainted(false);
            setContentAreaFilled(false);
            setForeground(fgColor);
            setCursor(new Cursor(Cursor.HAND_CURSOR));
            
            // Add smooth hover transition property if needed, but FlatLaf handles some
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            Color fill = bgColor;
            if (!isEnabled()) fill = new Color(220, 220, 220);
            else if (getModel().isPressed()) fill = bgColor.darker();
            else if (getModel().isRollover()) fill = brighter(bgColor, 0.15f);

            g2.setColor(fill);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), radius, radius);
            
            // Optional: Add a subtle border
            g2.setColor(new Color(0, 0, 0, 20));
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, radius, radius);
            
            g2.dispose();
            super.paintComponent(g);
        }

        private Color brighter(Color color, float factor) {
            int r = Math.min(255, (int) (color.getRed() * (1 + factor)));
            int g = Math.min(255, (int) (color.getGreen() * (1 + factor)));
            int b = Math.min(255, (int) (color.getBlue() * (1 + factor)));
            return new Color(r, g, b);
        }
    }


    private static class SimpleDocListener implements DocumentListener {
        private final Runnable onChange;

        private SimpleDocListener(Runnable onChange) {
            this.onChange = onChange;
        }

        static SimpleDocListener onChange(Runnable r) {
            return new SimpleDocListener(r);
        }

        @Override
        public void insertUpdate(DocumentEvent e) {
            onChange.run();
        }

        @Override
        public void removeUpdate(DocumentEvent e) {
            onChange.run();
        }

        @Override
        public void changedUpdate(DocumentEvent e) {
            onChange.run();
        }
    }

    public static void main(String[] args) {
        FlatLightLaf.setup();
        UIManager.put("Button.arc", 999);
        UIManager.put("Component.arc", 12);
        UIManager.put("TextComponent.arc", 12);
        UIManager.put("ScrollBar.thumbArc", 999);
        UIManager.put("ScrollBar.trackArc", 999);
        
        SwingUtilities.invokeLater(MainApp::new);
    }
}