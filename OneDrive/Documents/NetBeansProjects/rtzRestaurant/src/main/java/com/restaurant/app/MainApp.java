package com.restaurant.app;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.plaf.basic.BasicScrollBarUI;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainApp {

    private static final String API_BASE_URL = "http://localhost:8080";
    private final HttpClient httpClient = HttpClient.newHttpClient();

    private JFrame buyerFrame, sellerFrame;
    private ChatView buyerChatView, sellerInboxView;
    private JTextField buyerMessageField, buyerPhoneField, buyerAddressField;

    private String currentBuyer = "Buyer";
    private int currentOrderNumber = 0;
    private long currentOrderStartTime = 0;
    private String latestBuyerRequest = "";
    private int invoiceCounter = 1;

    private final List<String> buyerList = new ArrayList<>();
    private final Map<String, Integer> buyerOrderCount = new HashMap<>();
    private final Map<String, BuyerProfile> buyerProfiles = new HashMap<>();

    private final List<String> sellerList = new ArrayList<>();
    private final Map<String, SellerProfile> sellerProfiles = new HashMap<>();
    private final Map<String, Double> sellerRatings = new HashMap<>();
    private final Map<String, Integer> sellerRatingCount = new HashMap<>();

    private JPanel buyerOffersGrid;
    private final Map<String, BuyerOfferCard> buyerOfferCards = new LinkedHashMap<>();
    private final Map<String, JPanel> sellerOrderPanels = new LinkedHashMap<>();
    private final Map<String, JLabel> sellerOrderFromLabels = new LinkedHashMap<>();
    private final Map<String, JLabel> sellerRatingLabels = new LinkedHashMap<>();

    private final List<OrderRecord> allOrderHistory = new ArrayList<>();
    private final List<Transaction> allTransactions = new ArrayList<>();
    private double totalRevenue = 0;

    private final Map<String, OfferMeta> latestOfferMeta = new HashMap<>();

    private final Color PINK_LIGHT = new Color(255, 240, 247);
    private final Color PINK_MEDIUM = new Color(255, 182, 193);
    private final Color PINK_DARK = new Color(255, 105, 180);

    private final Color MINT_LIGHT = new Color(230, 255, 243);
    private final Color MINT_MEDIUM = new Color(166, 227, 214);
    private final Color MINT_DARK = new Color(79, 203, 141);

    private final Color BG_COLOR = new Color(255, 245, 250);
    private final Color GREEN = new Color(144, 238, 144);
    private final Color ORANGE = new Color(255, 165, 0);
    private final Color PURPLE = new Color(147, 112, 219);
    private final Color GOLD = new Color(255, 215, 0);
    private final Color SKY = new Color(135, 206, 250);

    private final Font FONT_NORMAL = new Font("SansSerif", Font.PLAIN, 13);
    private final Font FONT_BOLD = new Font("SansSerif", Font.BOLD, 13);
    private final Font FONT_TITLE = new Font("SansSerif", Font.BOLD, 18);
    private final Font FONT_HEADER = new Font("SansSerif", Font.BOLD, 20);
    private final Font FONT_SMALL = new Font("SansSerif", Font.PLAIN, 11);
    private final Font FONT_BUTTON = new Font("SansSerif", Font.BOLD, 14);
    private final Font FONT_CHAT = new Font("SansSerif", Font.PLAIN, 14);
    private final Font FONT_SYSTEM = new Font("SansSerif", Font.PLAIN, 14);

    public MainApp() {
        initData();

        buyerChatView = new ChatView(PINK_LIGHT, MINT_LIGHT, PINK_DARK, MINT_DARK);
        sellerInboxView = new ChatView(
                new Color(245, 245, 250),
                new Color(245, 245, 250),
                new Color(80, 80, 80),
                new Color(80, 80, 80)
        );

        buildBuyerWindow();
        buildSellerWindow();

        SwingUtilities.invokeLater(() -> {
            buyerChatView.addSystem("🌸 Welcome! ");
            sellerInboxView.addSystem("📥 Incoming Requests ready.");
            refreshAllSellerRatingLabels();
            refreshAllBuyerCardRatings();
        });
    }

    private void initData() {
        buyerList.add("Buyer");
        buyerOrderCount.put("Buyer", 0);
        buyerProfiles.put("Buyer", new BuyerProfile("Buyer"));

        boolean apiLoaded = loadSellersFromApi();
        if (!apiLoaded) loadDefaultSellers();
    }

    private boolean loadSellersFromApi() {
        try {
            String json = sendGet(API_BASE_URL + "/sellers");
            List<SellerProfile> sellersFromApi = parseSellerJson(json);
            if (sellersFromApi.isEmpty()) return false;

            sellerList.clear();
            sellerProfiles.clear();
            sellerRatings.clear();
            sellerRatingCount.clear();

            for (SellerProfile sp : sellersFromApi) {
                sp.phone = normalizeIndonesianPhone(sp.phone);
                sellerList.add(sp.name);
                sellerProfiles.put(sp.name, sp);
                sellerRatings.put(sp.name, 0.0);
                sellerRatingCount.put(sp.name, 0);
            }
            return true;
        } catch (Exception e) {
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

    private String sendGet(String url) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        return response.body();
    }

    private void sendPostAsync(String url, String jsonBody) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenAccept(res -> System.out.println("POST " + url + " -> " + res.statusCode()))
                    .exceptionally(ex -> {
                        System.out.println("POST gagal: " + ex.getMessage());
                        return null;
                    });
        } catch (Exception e) {
            System.out.println("POST error: " + e.getMessage());
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

    private void postOrderToApi(String buyer, String seller, int orderNum,
                                String[] items, String[] unitPrices, int[] stock, int[] ratings) {
        StringBuilder itemArray = new StringBuilder("[");
        for (int i = 0; i < items.length; i++) {
            if (i > 0) itemArray.append(",");
            itemArray.append("{")
                    .append("\"name\":\"").append(escapeJson(items[i])).append("\",")
                    .append("\"unitPrice\":\"").append(escapeJson(unitPrices[i])).append("\",")
                    .append("\"stock\":").append(stock[i]).append(",")
                    .append("\"rating\":").append(ratings[i])
                    .append("}");
        }
        itemArray.append("]");

        String json = "{"
                + "\"buyer\":\"" + escapeJson(buyer) + "\","
                + "\"seller\":\"" + escapeJson(seller) + "\","
                + "\"orderNumber\":" + orderNum + ","
                + "\"date\":\"" + escapeJson(getCurrentDate()) + "\","
                + "\"time\":\"" + escapeJson(getCurrentTime()) + "\","
                + "\"items\":" + itemArray
                + "}";

        sendPostAsync(API_BASE_URL + "/orders", json);
    }

    private void postTransactionToApi(Transaction t) {
        String json = "{"
                + "\"buyer\":\"" + escapeJson(t.buyerName) + "\","
                + "\"seller\":\"" + escapeJson(t.sellerName) + "\","
                + "\"orderId\":\"" + escapeJson(t.orderId) + "\","
                + "\"invoiceNo\":\"" + escapeJson(t.invoiceNo) + "\","
                + "\"itemName\":\"" + escapeJson(t.itemName) + "\","
                + "\"unitPrice\":" + t.unitPrice + ","
                + "\"qty\":" + t.qty + ","
                + "\"totalAmount\":" + t.totalAmount + ","
                + "\"paymentMethod\":\"" + escapeJson(t.paymentMethod) + "\","
                + "\"paymentStatus\":\"" + escapeJson(t.paymentStatus) + "\","
                + "\"paymentDetail\":\"" + escapeJson(t.paymentDetail) + "\","
                + "\"proofPath\":\"" + escapeJson(t.proofPath) + "\","
                + "\"date\":\"" + escapeJson(t.date) + "\","
                + "\"time\":\"" + escapeJson(t.timestamp) + "\""
                + "}";

        sendPostAsync(API_BASE_URL + "/transactions", json);
    }

    private JLabel styledLabel(String text, Font font, Color color) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(font);
        lbl.setForeground(color);
        return lbl;
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
        sp.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        sp.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        sp.getVerticalScrollBar().setUI(new CustomScrollBarUI());
        sp.getHorizontalScrollBar().setUI(new CustomScrollBarUI());
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
        RoundedPanel panel = styledCard(PINK_MEDIUM, Color.WHITE, 30);
        panel.setShadow(10, PINK_LIGHT);

        JLabel title = styledLabel("Buyer Dashboard", FONT_HEADER, PINK_DARK);

        JPanel profileBar = new JPanel(new GridBagLayout());
        profileBar.setOpaque(false);
        profileBar.setBorder(new EmptyBorder(8, 0, 8, 0));

        JLabel buyerName = styledLabel(currentBuyer, FONT_BOLD, PINK_DARK);
        buyerPhoneField = styledTextField(PINK_MEDIUM);
        buyerPhoneField.setToolTipText("Nomor WA (contoh: 62812xxxx)");
        buyerAddressField = styledTextField(PINK_MEDIUM);
        buyerAddressField.setToolTipText("Alamat buyer");

        BuyerProfile bp = buyerProfiles.get(currentBuyer);
        if (bp != null) {
            buyerPhoneField.setText(bp.phone);
            buyerAddressField.setText(bp.address);
        }

        RoundedButton saveProfile = styledButton("Save Profile", PINK_DARK, 16);
        saveProfile.addActionListener(e -> saveBuyerProfile());

        RoundedButton openMyMapBtn = styledButton("🗺 Open My Map", PURPLE, 16);
        openMyMapBtn.addActionListener(e -> openBuyerMap(currentBuyer));

        RoundedButton payHistoryBtn = styledButton("💳 Payment History", ORANGE, 16);
        payHistoryBtn.addActionListener(e -> showPaymentHistory());

        RoundedButton bestBtn = styledButton("🏆 Highlight Best Offers", MINT_DARK, 16);
        bestBtn.addActionListener(e -> highlightBestOffers());

        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(4, 4, 4, 4);
        gc.fill = GridBagConstraints.HORIZONTAL;

        add(profileBar, styledLabel("👤 Buyer: ", FONT_BOLD, Color.BLACK), gc, 0, 0, 0);
        add(profileBar, buyerName, gc, 1, 0, 0.2);
        add(profileBar, new JLabel("📲 Phone:"), gc, 2, 0, 0);
        add(profileBar, buyerPhoneField, gc, 3, 0, 0.35);
        add(profileBar, new JLabel("🏠 Address:"), gc, 4, 0, 0);
        add(profileBar, buyerAddressField, gc, 5, 0, 0.65);
        add(profileBar, saveProfile, gc, 6, 0, 0);
        add(profileBar, openMyMapBtn, gc, 7, 0, 0);
        add(profileBar, payHistoryBtn, gc, 8, 0, 0);
        add(profileBar, bestBtn, gc, 9, 0, 0);

        JPanel north = new JPanel();
        north.setOpaque(false);
        north.setLayout(new BoxLayout(north, BoxLayout.Y_AXIS));
        north.add(title);
        north.add(profileBar);
        panel.add(north, BorderLayout.NORTH);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        split.setBorder(null);
        split.setDividerSize(10);
        split.setResizeWeight(0.35);

        RoundedPanel left = styledCard(PINK_MEDIUM, Color.WHITE, 25);
        left.setShadow(8, PINK_LIGHT);
        left.add(styledLabel("Buyer Chat", FONT_TITLE, PINK_DARK), BorderLayout.NORTH);
        left.add(buyerChatView.getView(), BorderLayout.CENTER);

        RoundedPanel right = styledCard(new Color(200, 230, 230), Color.WHITE, 19);
        right.setShadow(8, new Color(220, 240, 240));
        right.add(styledLabel("Found offers! ✨", FONT_TITLE, new Color(60, 60, 60)), BorderLayout.NORTH);

        buyerOffersGrid = new JPanel();
        buyerOffersGrid.setOpaque(false);
        buyerOffersGrid.setLayout(new BoxLayout(buyerOffersGrid, BoxLayout.Y_AXIS));

        buyerOfferCards.clear();
        for (String s : sellerList) {
            BuyerOfferCard card = new BuyerOfferCard(s);
            buyerOfferCards.put(s, card);
            card.root.setMaximumSize(new Dimension(Integer.MAX_VALUE, 280));
            card.root.setAlignmentX(Component.LEFT_ALIGNMENT);
            buyerOffersGrid.add(card.root);
            buyerOffersGrid.add(Box.createRigidArea(new Dimension(0, 8)));
        }

        right.add(transparentScroll(buyerOffersGrid), BorderLayout.CENTER);

        split.setLeftComponent(left);
        split.setRightComponent(right);
        panel.add(split, BorderLayout.CENTER);

        JPanel inputPanel = new JPanel(new BorderLayout(10, 0));
        inputPanel.setOpaque(false);
        inputPanel.setBorder(new EmptyBorder(8, 5, 5, 5));

        buyerMessageField = new JTextField();
        buyerMessageField.setFont(FONT_CHAT);
        buyerMessageField.setBorder(new CompoundBorder(
                new LineBorder(PINK_MEDIUM, 2, true),
                new EmptyBorder(10, 15, 10, 15)
        ));
        buyerMessageField.setBackground(new Color(255, 250, 252));
        buyerMessageField.setForeground(new Color(100, 60, 80));
        buyerMessageField.setCaretColor(PINK_DARK);

        RoundedButton sendBtn = new RoundedButton("➤", PINK_DARK, Color.WHITE, 40);
        sendBtn.setFont(new Font("SansSerif", Font.BOLD, 20));
        sendBtn.setBorder(new EmptyBorder(10, 10, 10, 10));
        sendBtn.addActionListener(e -> sendBuyerMessage());
        buyerMessageField.addActionListener(e -> sendBuyerMessage());

        inputPanel.add(buyerMessageField, BorderLayout.CENTER);
        inputPanel.add(sendBtn, BorderLayout.EAST);
        panel.add(inputPanel, BorderLayout.SOUTH);

        return panel;
    }

    private void add(JPanel panel, Component c, GridBagConstraints gc, int x, int y, double weight) {
        gc.gridx = x;
        gc.gridy = y;
        gc.weightx = weight;
        panel.add(c, gc);
    }

   private void saveBuyerProfile() {

    BuyerProfile bp = buyerProfiles.computeIfAbsent(currentBuyer, BuyerProfile::new);

    bp.phone = buyerPhoneField.getText().trim();
    bp.address = buyerAddressField.getText().trim();

    JOptionPane.showMessageDialog(buyerFrame,"Buyer profile saved");
}

    private void sendBuyerMessage() {
        String text = buyerMessageField.getText().trim();
        if (text.isEmpty()) return;

        String lower = text.toLowerCase();

        if (lower.startsWith("i am ") || lower.startsWith("saya ") || lower.startsWith("nama saya ") || lower.startsWith("my name is ")) {
            String name = extractNameFromMessage(text);
            if (!name.isEmpty()) {
                if (!buyerList.contains(name)) buyerList.add(name);
                buyerOrderCount.putIfAbsent(name, 0);
                currentBuyer = name;
                buyerProfiles.putIfAbsent(name, new BuyerProfile(name));

                BuyerProfile bp = buyerProfiles.get(name);
                buyerPhoneField.setText(bp.phone);
                buyerAddressField.setText(bp.address);

                buyerChatView.addSystem("👤 Buyer set to: " + currentBuyer);
                buyerMessageField.setText("");
                return;
            }
        }

        buyerChatView.addSelf("🌸 " + currentBuyer, text);

        boolean wantsToBuy =
                lower.contains("i want") || lower.contains("i wanna") || lower.contains("i would like") ||
                        lower.contains("order") || lower.contains("beli") || lower.contains("pesan") ||
                        lower.contains("mau beli") || lower.contains("mau pesan");

        if (wantsToBuy) {
            startNewOrderBroadcast(text);
        } else {
            sellerInboxView.addSystem(getCurrentTime() + " 💬 Msg from " + currentBuyer + ": " + text);
        }

        buyerMessageField.setText("");
    }

    private JPanel createSellerPanel() {
        JPanel page = new JPanel(new BorderLayout(0, 6));
        page.setOpaque(false);

        JPanel topBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        topBar.setOpaque(false);

        RoundedButton historyBtn = styledButton("📋 Complete Order History", PURPLE, 20);
        RoundedButton revenueBtn = styledButton("💰 Total Revenue", ORANGE, 20);
        RoundedButton resetBtn = styledButton("🔄 Reset Data", new Color(255, 99, 71), 20);

        historyBtn.setFont(FONT_BUTTON);
        revenueBtn.setFont(FONT_BUTTON);
        resetBtn.setFont(FONT_BUTTON);

        historyBtn.addActionListener(e -> showPurchaseHistory());
        revenueBtn.addActionListener(e -> showTotalRevenue());
        resetBtn.addActionListener(e -> resetAllData());

        topBar.add(historyBtn);
        topBar.add(revenueBtn);
        topBar.add(resetBtn);
        page.add(topBar, BorderLayout.NORTH);

        RoundedPanel left = styledCard(MINT_MEDIUM, Color.WHITE, 25);
        left.setShadow(8, MINT_LIGHT);
        left.add(styledLabel("Incoming Requests", FONT_TITLE, MINT_DARK), BorderLayout.NORTH);
        left.add(sellerInboxView.getView(), BorderLayout.CENTER);

        JPanel list = new JPanel();
        list.setOpaque(false);
        list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));

        sellerOrderPanels.clear();
        sellerOrderFromLabels.clear();
        sellerRatingLabels.clear();

        for (String s : sellerList) {
            list.add(createSellerMarketplaceCard(s));
            list.add(Box.createRigidArea(new Dimension(0, 8)));
        }

        RoundedPanel right = styledCard(MINT_MEDIUM, Color.WHITE, 25);
        right.setShadow(8, MINT_LIGHT);
        right.add(transparentScroll(list), BorderLayout.CENTER);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, left, right);
        split.setResizeWeight(0.30);
        split.setBorder(null);
        split.setDividerSize(10);

        page.add(split, BorderLayout.CENTER);
        return page;
    }

    private JComponent createSellerMarketplaceCard(String sellerName) {
        RoundedPanel card = new RoundedPanel(new BorderLayout(0, 10), 22, new Color(250, 255, 252));
        card.setBorder(new CompoundBorder(new LineBorder(MINT_LIGHT, 2, true), new EmptyBorder(10, 10, 10, 10)));

        SellerProfile sp = sellerProfiles.get(sellerName);

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);

        JLabel title = styledLabel("🏪 " + sellerName + "  •  ID: " + safe(sp.sellerId), FONT_TITLE, MINT_DARK);
        JLabel rating = styledLabel("⭐ 0.0 (0)", FONT_BOLD, GOLD);
        sellerRatingLabels.put(sellerName, rating);

        header.add(title, BorderLayout.WEST);
        header.add(rating, BorderLayout.EAST);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        actions.setOpaque(false);

        RoundedButton waBtn = new RoundedButton("📲 Chat (WA)", new Color(34, 139, 34), Color.WHITE, 14);
        RoundedButton videoBtn = new RoundedButton("🎬 Short Video", PURPLE, Color.WHITE, 14);
        RoundedButton editBtn = new RoundedButton("⚙ Edit Profile", ORANGE, Color.WHITE, 14);

        for (RoundedButton b : new RoundedButton[]{waBtn, videoBtn, editBtn}) {
            b.setFont(FONT_SMALL);
            b.setBorder(new EmptyBorder(6, 10, 6, 10));
        }

        waBtn.addActionListener(e -> openWhatsApp(sp.phone, "Hi " + sellerName + ", I want to ask about your menu."));
        videoBtn.addActionListener(e -> openUrl(buildYoutubeSearchUrl(latestBuyerRequest)));
        editBtn.addActionListener(e -> editSellerProfileDialog(sellerName));

        actions.add(new JLabel("Phone: " + safe(sp.phone) + "  "));
        actions.add(waBtn);
        actions.add(videoBtn);
        actions.add(editBtn);

        JLabel orderLbl = styledLabel("📝 Waiting request...", FONT_SMALL, new Color(120, 120, 120));
        sellerOrderFromLabels.put(sellerName, orderLbl);

        JPanel headerWrap = new JPanel();
        headerWrap.setOpaque(false);
        headerWrap.setLayout(new BoxLayout(headerWrap, BoxLayout.Y_AXIS));
        headerWrap.add(header);
        headerWrap.add(Box.createRigidArea(new Dimension(0, 4)));
        headerWrap.add(actions);
        headerWrap.add(Box.createRigidArea(new Dimension(0, 4)));
        headerWrap.add(orderLbl);

        card.add(headerWrap, BorderLayout.NORTH);

        JPanel orders = new JPanel();
        orders.setOpaque(false);
        orders.setLayout(new BoxLayout(orders, BoxLayout.Y_AXIS));
        orders.setBorder(new EmptyBorder(6, 6, 6, 6));
        sellerOrderPanels.put(sellerName, orders);

        JScrollPane spn = new JScrollPane(orders);
        spn.setBorder(new LineBorder(new Color(235, 235, 235), 1, true));
        spn.getViewport().setBackground(Color.WHITE);
        spn.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        spn.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        spn.getVerticalScrollBar().setUI(new CustomScrollBarUI());
        spn.getHorizontalScrollBar().setUI(new CustomScrollBarUI());

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
    RoundedPanel bubble = new RoundedPanel(new BorderLayout(0, 10), 20, new Color(255, 245, 250));
    bubble.setBorder(new EmptyBorder(12, 12, 12, 12));
    bubble.setMaximumSize(new Dimension(450, Integer.MAX_VALUE));

    BuyerProfile bp = buyerProfiles.getOrDefault(buyerName, new BuyerProfile(buyerName));
    String buyerAddress = (bp.address == null || bp.address.isBlank()) ? "-" : bp.address;
    String buyerPhone = (bp.phone == null || bp.phone.isBlank()) ? "-" : bp.phone;

    JPanel top = new JPanel();
    top.setOpaque(false);
    top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));

    top.add(styledLabel(getCurrentTime() + " 🧾 Offer for " + buyerName + " (#" + orderNum + ")", FONT_BOLD, MINT_DARK));
    top.add(Box.createRigidArea(new Dimension(0, 3)));
    top.add(styledLabel("Request: " + requestText, FONT_SMALL, new Color(100, 100, 100)));
    top.add(Box.createRigidArea(new Dimension(0, 3)));
    top.add(styledLabel("Buyer Phone: " + buyerPhone, FONT_SMALL, new Color(70, 70, 70)));
    top.add(Box.createRigidArea(new Dimension(0, 2)));
    top.add(styledLabel("Buyer Address: " + buyerAddress, FONT_SMALL, new Color(70, 70, 70)));

    RoundedButton buyerMapBtn = new RoundedButton("🗺 Open Buyer Map", PURPLE, Color.WHITE, 14);
    buyerMapBtn.setFont(FONT_SMALL);
    buyerMapBtn.setBorder(new EmptyBorder(6, 10, 6, 10));
    buyerMapBtn.addActionListener(e -> openBuyerMap(buyerName));

    RoundedButton buyerWaBtn = new RoundedButton("📲 Chat Buyer", new Color(34, 139, 34), Color.WHITE, 14);
    buyerWaBtn.setFont(FONT_SMALL);
    buyerWaBtn.setBorder(new EmptyBorder(6, 10, 6, 10));
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

    JPanel rows = new JPanel();
    rows.setOpaque(false);
    rows.setLayout(new BoxLayout(rows, BoxLayout.Y_AXIS));

    JTextField[] menuFields = new JTextField[3];
    JTextField[] unitPriceFields = new JTextField[3];
    JSpinner[] stockSpinners = new JSpinner[3];
    @SuppressWarnings("unchecked")
    JComboBox<Integer>[] ratingCombo = new JComboBox[3];

    JLabel totalLabel = styledLabel("Potential Value: Rp 0", FONT_BOLD, ORANGE);

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
        rows.add(createSellerFormRowWithStock(i, menuFields, unitPriceFields, stockSpinners, ratingCombo, recalcTotal));
        if (i < 2) rows.add(Box.createRigidArea(new Dimension(0, 8)));
    }

    bubble.add(rows, BorderLayout.CENTER);

    RoundedButton send = new RoundedButton("Send Offer ✔", MINT_DARK, Color.WHITE, 18);
    send.setFont(FONT_BOLD);
    send.setBorder(new EmptyBorder(10, 14, 10, 14));

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
        int avgStars = 0;
        for (int i = 0; i < 3; i++) {
            total += parsePriceSafe(unitPrices[i]) * stock[i];
            avgStars += ratings[i];
        }
        avgStars /= 3;

        long responseMs = System.currentTimeMillis() - currentOrderStartTime;
        latestOfferMeta.put(sellerName, new OfferMeta(sellerName, total, avgStars, responseMs));

        BuyerOfferCard card = buyerOfferCards.get(sellerName);
        if (card != null) {
            card.updateOffer(items, unitPrices, stock, ratings, total, avgStars, responseMs);
        }

        addToOrderHistory(buyerName, sellerName, orderNum, items, unitPrices, stock, ratings);
        postOrderToApi(buyerName, sellerName, orderNum, items, unitPrices, stock, ratings);

        sellerInboxView.addSystem("✅ " + sellerName + " sent offer to " + buyerName + " (#" + orderNum + ") " + getCurrentTime());

        send.setEnabled(false);
        send.setText("✓ Sent");
        send.setBackground(new Color(200, 200, 200));

        highlightBestOffers();
    });

    JPanel bottom = new JPanel(new BorderLayout());
    bottom.setOpaque(false);
    bottom.add(totalLabel, BorderLayout.WEST);
    bottom.add(send, BorderLayout.EAST);

    bubble.add(bottom, BorderLayout.SOUTH);
    return bubble;
}
    private JPanel createSellerFormRowWithStock(
            int index,
            JTextField[] menuFields,
            JTextField[] unitPriceFields,
            JSpinner[] stockSpinners,
            JComboBox<Integer>[] ratingCombo,
            Runnable recalcTotal
    ) {
        JPanel row = new JPanel(new BorderLayout(10, 0));
        row.setOpaque(false);

        JLabel itemLabel = styledLabel((index + 1) + ". ", FONT_BOLD, new Color(120, 120, 120));
        itemLabel.setPreferredSize(new Dimension(30, 36));

        JTextField menuField = styledTextField(MINT_LIGHT);
        JTextField unitPriceField = styledTextField(MINT_LIGHT);
        unitPriceField.getDocument().addDocumentListener(SimpleDocListener.onChange(recalcTotal));

        JSpinner stockSpin = new JSpinner(new SpinnerNumberModel(1, 1, 999, 1));
        ((JSpinner.DefaultEditor) stockSpin.getEditor()).getTextField().setColumns(3);
        stockSpin.addChangeListener(e -> recalcTotal.run());

        Integer[] ratings = {1, 2, 3, 4, 5};
        ratingCombo[index] = new JComboBox<>(ratings);
        ratingCombo[index].setFont(FONT_SMALL);
        ratingCombo[index].setBackground(Color.WHITE);
        ratingCombo[index].setPreferredSize(new Dimension(60, 30));

        JPanel fieldsPanel = new JPanel(new GridLayout(1, 4, 10, 0));
        fieldsPanel.setOpaque(false);
        fieldsPanel.add(menuField);
        fieldsPanel.add(wrapPriceField(unitPriceField));
        fieldsPanel.add(wrapStockSpinner(stockSpin));
        fieldsPanel.add(wrapRatingCombo(ratingCombo[index]));
        

        row.add(itemLabel, BorderLayout.WEST);
        row.add(fieldsPanel, BorderLayout.CENTER);

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

    private void highlightBestOffers() {
        if (latestOfferMeta.isEmpty()) return;

        String cheapestSeller = null;
        String easiestSeller = null;
        String fastestSeller = null;

        double cheapest = Double.MAX_VALUE;
        int easiest = -1;
        long fastest = Long.MAX_VALUE;

        for (OfferMeta meta : latestOfferMeta.values()) {
            if (meta.totalPrice < cheapest) {
                cheapest = meta.totalPrice;
                cheapestSeller = meta.sellerName;
            }
            if (meta.avgRating > easiest) {
                easiest = meta.avgRating;
                easiestSeller = meta.sellerName;
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
                    seller.equals(easiestSeller),
                    seller.equals(fastestSeller)
            );
        }

        StringBuilder sb = new StringBuilder("🏆 Best Offer Summary\n");
        if (cheapestSeller != null) sb.append("💸 Cheapest: ").append(cheapestSeller).append("\n");
        if (easiestSeller != null) sb.append("⭐ Easiest: ").append(easiestSeller).append("\n");
        if (fastestSeller != null) sb.append("⚡ Fastest: ").append(fastestSeller).append("\n");

        buyerChatView.addSystem(sb.toString());
    }

    private static class OfferMeta {
        String sellerName;
        double totalPrice;
        int avgRating;
        long responseMs;

        OfferMeta(String sellerName, double totalPrice, int avgRating, long responseMs) {
            this.sellerName = sellerName;
            this.totalPrice = totalPrice;
            this.avgRating = avgRating;
            this.responseMs = responseMs;
        }
    }

    private class BuyerOfferCard {
        final String sellerName;
        final RoundedPanel root;
        final JLabel statusLabel, ratingLabel, bestTagLabel, infoMetaLabel;
        final JLabel[] menuLbl = new JLabel[3];
        final JLabel[] unitPriceLbl = new JLabel[3];
        final JLabel[] stockLbl = new JLabel[3];
        final JLabel[] chosenQtyLbl = new JLabel[3];
        final JLabel[] totalLbl = new JLabel[3];
        final JLabel[] starsLbl = new JLabel[3];
        final RoundedButton[] chooseBtn = new RoundedButton[3];

        int orderNum = 0;
        String[] items = new String[3];
        String[] unitPrices = new String[3];
        int[] stock = new int[3];
        int[] ratings = new int[3];

        BuyerOfferCard(String sellerName) {
            this.sellerName = sellerName;

            root = new RoundedPanel(new BorderLayout(0, 8), 20, Color.WHITE);
            root.setBorder(new CompoundBorder(
                    new LineBorder(PINK_MEDIUM, 2, true),
                    new EmptyBorder(10, 10, 10, 10)
            ));

            SellerProfile sp = sellerProfiles.get(sellerName);

            JPanel headerTop = new JPanel(new BorderLayout());
            headerTop.setOpaque(false);

            JLabel title = styledLabel("🏪 " + sellerName + "  •  ID: " + safe(sp.sellerId), FONT_BOLD, PINK_DARK);
            ratingLabel = styledLabel("⭐ 0.0 (0)", FONT_SMALL, GOLD);

            headerTop.add(title, BorderLayout.WEST);
            headerTop.add(ratingLabel, BorderLayout.EAST);

            JPanel contactRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
            contactRow.setOpaque(false);

            BuyerProfile bp = buyerProfiles.getOrDefault(currentBuyer, new BuyerProfile(currentBuyer));

            contactRow.add(new JLabel("Buyer Address: " + safe(bp.address)));

            JButton buyerMapBtn = new JButton("Open Buyer Map");
            buyerMapBtn.addActionListener(e -> openBuyerMap(currentBuyer));

            contactRow.add(buyerMapBtn);

            // tombol seller
            RoundedButton waBtn = new RoundedButton("📲 Chat Seller", new Color(34, 139, 34), Color.WHITE, 14);
            RoundedButton videoBtn = new RoundedButton("🎬 Video", PURPLE, Color.WHITE, 14);
            RoundedButton mapBtn = new RoundedButton("🗺 Seller Map", PURPLE, Color.WHITE, 14);

            for (RoundedButton b : new RoundedButton[]{waBtn, videoBtn}) {
                b.setFont(FONT_SMALL);
                b.setBorder(new EmptyBorder(5, 10, 5, 10));
            }
            root.add(contactRow, BorderLayout.NORTH);
            waBtn.addActionListener(e -> openWhatsApp(
                    sp.phone,
                    "Hi " + sellerName + ", I'm " + currentBuyer + ". I want to ask about my order #" + orderNum
            ));

            videoBtn.addActionListener(e -> {
                String query = latestBuyerRequest;
                if (items[0] != null && !items[0].trim().isEmpty()) {
                    query = items[0];
                }
                openUrl(buildYoutubeSearchUrl(query));
            });

            mapBtn.addActionListener(e -> openSellerMap(sellerName));

            contactRow.add(new JLabel("Seller Phone: " + safe(sp.phone)));
            contactRow.add(waBtn);
            contactRow.add(videoBtn);
            contactRow.add(mapBtn);

            statusLabel = styledLabel("📝 No request yet", FONT_SMALL, new Color(120, 120, 120));
            bestTagLabel = styledLabel("", FONT_BOLD, new Color(0, 102, 204));
            infoMetaLabel = styledLabel("", FONT_SMALL, new Color(90, 90, 90));

            JPanel header = new JPanel();
            header.setOpaque(false);
            header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
            header.add(headerTop);
            header.add(Box.createRigidArea(new Dimension(0, 4)));
            header.add(contactRow);
            header.add(Box.createRigidArea(new Dimension(0, 4)));
            header.add(statusLabel);
            header.add(Box.createRigidArea(new Dimension(0, 3)));
            header.add(bestTagLabel);
            header.add(infoMetaLabel);

            root.add(header, BorderLayout.NORTH);

            JPanel rows = new JPanel();
            rows.setOpaque(false);
            rows.setLayout(new BoxLayout(rows, BoxLayout.Y_AXIS));

            for (int i = 0; i < 3; i++) {
                rows.add(buildRow(i));
                if (i < 2) rows.add(Box.createRigidArea(new Dimension(0, 6)));
            }

            root.add(rows, BorderLayout.CENTER);
            setWaiting("", 0, "");
            refreshRatingLabel();
        }

        private JPanel buildRow(int idx) {
            JPanel row = new JPanel(new BorderLayout(8, 0));
            row.setOpaque(false);
            row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
            row.setAlignmentX(Component.LEFT_ALIGNMENT);

            JLabel no = styledLabel((idx + 1) + ".", FONT_BOLD, new Color(120, 120, 120));
            no.setPreferredSize(new Dimension(22, 30));

            menuLbl[idx] = new JLabel("Waiting...");
            menuLbl[idx].setFont(FONT_NORMAL);

            unitPriceLbl[idx] = new JLabel("---");
            unitPriceLbl[idx].setFont(FONT_SMALL);

            stockLbl[idx] = new JLabel("Stock: -");
            stockLbl[idx].setFont(FONT_SMALL);

            chosenQtyLbl[idx] = new JLabel("Buy Qty: -");
            chosenQtyLbl[idx].setFont(FONT_SMALL);

            totalLbl[idx] = styledLabel("Total: ---", FONT_SMALL, ORANGE);
            starsLbl[idx] = styledLabel("", FONT_SMALL, GOLD);

            JPanel mid = new JPanel(new GridLayout(1, 6, 6, 0));
            mid.setOpaque(false);
            mid.add(box(menuLbl[idx], 110));
            mid.add(box(unitPriceLbl[idx], 85));
            mid.add(box(stockLbl[idx], 70));
            mid.add(box(chosenQtyLbl[idx], 80));
            mid.add(box(totalLbl[idx], 95));
            mid.add(box(starsLbl[idx], 60));

            chooseBtn[idx] = new RoundedButton("Order", GREEN, Color.WHITE, 14);
            chooseBtn[idx].setFont(new Font("SansSerif", Font.BOLD, 11));
            chooseBtn[idx].setBorder(new EmptyBorder(6, 10, 6, 10));
            chooseBtn[idx].setPreferredSize(new Dimension(85, 30));
            chooseBtn[idx].setEnabled(false);

            int index = idx;
            chooseBtn[idx].addActionListener(e -> chooseAndPay(index));

            row.add(no, BorderLayout.WEST);
            row.add(mid, BorderLayout.CENTER);
            row.add(chooseBtn[idx], BorderLayout.EAST);

            return row;
        }

        private JPanel box(JComponent c, int w) {
            JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
            p.setOpaque(false);
            p.setBorder(new CompoundBorder(
                    new LineBorder(new Color(235, 235, 235), 1, true),
                    new EmptyBorder(4, 6, 4, 6)
            ));
            p.setPreferredSize(new Dimension(w, 30));
            p.setMinimumSize(new Dimension(w, 30));
            p.setMaximumSize(new Dimension(w, 30));
            p.add(c);
            return p;
        }

        void setWaiting(String buyerName, int orderNum, String requestText) {
            this.orderNum = orderNum;

            if (orderNum <= 0) {
                statusLabel.setText("📝 No request yet");
                statusLabel.setForeground(new Color(120, 120, 120));
            } else {
                statusLabel.setText("⏳ Waiting offer... (Order #" + orderNum + ")");
                statusLabel.setForeground(new Color(255, 140, 0));
            }

            bestTagLabel.setText("");
            infoMetaLabel.setText("");

            for (int i = 0; i < 3; i++) {
                items[i] = null;
                unitPrices[i] = null;
                stock[i] = 0;
                ratings[i] = 0;

                menuLbl[i].setText("Waiting...");
                menuLbl[i].setForeground(new Color(170, 170, 170));
                unitPriceLbl[i].setText("---");
                stockLbl[i].setText("Stock: -");
                chosenQtyLbl[i].setText("Buy Qty: -");
                totalLbl[i].setText("Total: ---");
                starsLbl[i].setText("");
                chooseBtn[i].setEnabled(false);
                chooseBtn[i].setBackground(new Color(200, 200, 200));
                chooseBtn[i].setText("Order");
            }

            refreshRatingLabel();
            root.setBorder(new CompoundBorder(
                    new LineBorder(PINK_MEDIUM, 2, true),
                    new EmptyBorder(10, 10, 10, 10)
            ));
        }

        void updateOffer(String[] items, String[] unitPrices, int[] stock, int[] ratings,
                         double grandTotal, int avgStars, long responseMs) {
            this.items = items.clone();
            this.unitPrices = unitPrices.clone();
            this.stock = stock.clone();
            this.ratings = ratings.clone();

            statusLabel.setText("✅ Offer ready! (Order #" + orderNum + ")");
            statusLabel.setForeground(new Color(0, 130, 0));

            infoMetaLabel.setText(
                    "Potential Value: Rp " + String.format("%,.0f", grandTotal) +
                            " | Avg Rating: " + avgStars +
                            " | Response: " + (responseMs / 1000.0) + " sec"
            );

            for (int i = 0; i < 3; i++) {
                menuLbl[i].setText(items[i]);
                menuLbl[i].setForeground(new Color(0, 100, 0));

                unitPriceLbl[i].setText("Rp " + unitPrices[i]);
                stockLbl[i].setText("Stock: " + stock[i]);
                chosenQtyLbl[i].setText("Buy Qty: ?");
                totalLbl[i].setText("Total: Choose qty");
                starsLbl[i].setText("⭐".repeat(Math.max(0, ratings[i])));

                chooseBtn[i].setEnabled(stock[i] > 0);
                chooseBtn[i].setBackground(stock[i] > 0 ? GREEN : new Color(200, 200, 200));
            }

            refreshRatingLabel();
        }

        void setBestTags(boolean cheapest, boolean easiest, boolean fastest) {
            List<String> tags = new ArrayList<>();
            if (cheapest) tags.add("💸 Cheapest");
            if (easiest) tags.add("⭐ Easiest");
            if (fastest) tags.add("⚡ Fastest");

            bestTagLabel.setText(String.join("   ", tags));

            if (!tags.isEmpty()) {
                root.setBorder(new CompoundBorder(
                        new LineBorder(SKY, 3, true),
                        new EmptyBorder(10, 10, 10, 10)
                ));
            } else {
                root.setBorder(new CompoundBorder(
                        new LineBorder(PINK_MEDIUM, 2, true),
                        new EmptyBorder(10, 10, 10, 10)
                ));
            }

            root.revalidate();
            root.repaint();
        }

        void refreshRatingLabel() {
            double r = sellerRatings.getOrDefault(sellerName, 0.0);
            int c = sellerRatingCount.getOrDefault(sellerName, 0);
            ratingLabel.setText("⭐ " + String.format("%.1f", r) + " (" + c + ")");
        }

        private void chooseAndPay(int index) {
            if (items[index] == null) return;

            if (stock[index] <= 0) {
                JOptionPane.showMessageDialog(
                        buyerFrame,
                        "Stock untuk item ini sudah habis.",
                        "Stock Habis",
                        JOptionPane.WARNING_MESSAGE
                );
                return;
            }

            int selectedQty = askBuyerQty(items[index], stock[index], unitPrices[index]);
            if (selectedQty <= 0) return;

            if (selectedQty > stock[index]) {
                JOptionPane.showMessageDialog(
                        buyerFrame,
                        "Quantity yang dipilih melebihi stock tersedia.\nStock tersedia: " + stock[index],
                        "Tidak Bisa Membeli",
                        JOptionPane.ERROR_MESSAGE
                );
                return;
            }

            double unit = parsePriceSafe(unitPrices[index]);
            double total = unit * selectedQty;
            int stars = ratings[index];

            PaymentResult pay = showPaymentDialog(items[index], unit, selectedQty, total);
            if (pay == null) return;

            stock[index] -= selectedQty;
            stockLbl[index].setText("Stock: " + stock[index]);
            chosenQtyLbl[index].setText("Buy Qty: " + selectedQty);
            totalLbl[index].setText("Total: Rp " + String.format("%,.0f", total));

            if (stock[index] <= 0) {
                chooseBtn[index].setEnabled(false);
                chooseBtn[index].setText("Sold Out");
                chooseBtn[index].setBackground(new Color(200, 200, 200));
            } else {
                chooseBtn[index].setText("Order Again");
            }
            if (selectedQty > stock[index]) {
                JOptionPane.showMessageDialog(
                        buyerFrame,
                        "Stock not available / not enough.",
                        "Stock Warning",
                        JOptionPane.WARNING_MESSAGE
                );
                return;
            }

            String invoiceNo = generateInvoiceNo();
            String msg = getCurrentTime() + " ✅ Paid & choose from " + sellerName + ": " + items[index] +
                    " | unit Rp " + String.format("%,.0f", unit) +
                    " | qty " + selectedQty +
                    " | total Rp " + String.format("%,.0f", total) +
                    " | " + pay.method + " (" + pay.status + ")" +
                    " | Invoice: " + invoiceNo +
                    " " + "⭐".repeat(stars);

            buyerChatView.addSelf("🌸 " + currentBuyer, msg);
            sellerInboxView.addSystem("🧾 " + currentBuyer + " ordered: " + items[index] + " x" + selectedQty + " (from " + sellerName + ")");

            Transaction tx = addTransaction(
                    currentBuyer, sellerName, String.valueOf(orderNum), invoiceNo, items[index],
                    unit, selectedQty, total, pay.method, pay.status, pay.detail, pay.proofPath
            );

            double currentRating = sellerRatings.getOrDefault(sellerName, 0.0);
            int currentCount = sellerRatingCount.getOrDefault(sellerName, 0);
            double newRating = ((currentRating * currentCount) + stars) / (currentCount + 1);
            sellerRatings.put(sellerName, newRating);
            sellerRatingCount.put(sellerName, currentCount + 1);

            refreshRatingLabel();
            refreshAllSellerRatingLabels();
        }
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

    private static class BuyerProfile {
        String name;
        String phone = "";
        String address = "";

        BuyerProfile(String name) {
            this.name = name;
        }
    }

    private static class SellerProfile {
        String name;
        String sellerId = "";
        String phone = "";
        String shortVideoUrl = "";
        String address = "";

        SellerProfile(String name) {
            this.name = name;
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
        return "[" + new SimpleDateFormat("HH:mm").format(new Date()) + "]";
    }

    private String getCurrentDate() {
        return new SimpleDateFormat("yyyy-MM-dd").format(new Date());
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
        String date = new SimpleDateFormat("yyyyMMdd").format(new Date());
        String no = String.format("INV-%s-%03d", date, invoiceCounter);
        invoiceCounter++;
        return no;
    }

    private class OrderRecord {
        String buyerName, sellerName, timestamp, date;
        int orderNumber;
        String[] items, unitPrices;
        int[] stock, ratings;

        OrderRecord(String buyerName, String sellerName, int orderNumber,
                    String[] items, String[] unitPrices, int[] stock, int[] ratings) {
            this.buyerName = buyerName;
            this.sellerName = sellerName;
            this.orderNumber = orderNumber;
            this.items = items.clone();
            this.unitPrices = unitPrices.clone();
            this.stock = stock.clone();
            this.ratings = ratings.clone();
            this.timestamp = getCurrentTime();
            this.date = getCurrentDate();
        }
    }

    private void addToOrderHistory(String buyer, String seller, int orderNum,
                                   String[] items, String[] unitPrices, int[] stock, int[] ratings) {
        allOrderHistory.add(new OrderRecord(buyer, seller, orderNum, items, unitPrices, stock, ratings));
    }

    private class Transaction {
        String buyerName, sellerName, orderId, invoiceNo, itemName;
        String paymentMethod, paymentStatus, paymentDetail, proofPath, timestamp, date;
        double unitPrice, totalAmount;
        int qty;

        Transaction(String buyerName, String sellerName, String orderId, String invoiceNo, String itemName,
                    double unitPrice, int qty, double totalAmount, String paymentMethod, String paymentStatus,
                    String paymentDetail, String proofPath) {
            this.buyerName = buyerName;
            this.sellerName = sellerName;
            this.orderId = orderId;
            this.invoiceNo = invoiceNo;
            this.itemName = itemName;
            this.unitPrice = unitPrice;
            this.qty = qty;
            this.totalAmount = totalAmount;
            this.paymentMethod = paymentMethod;
            this.paymentStatus = paymentStatus;
            this.paymentDetail = paymentDetail;
            this.proofPath = proofPath;
            this.timestamp = getCurrentTime();
            this.date = getCurrentDate();
        }
    }

    private Transaction addTransaction(String buyer, String seller, String orderId, String invoiceNo, String itemName,
                                       double unitPrice, int qty, double total, String method, String status,
                                       String detail, String proofPath) {
        Transaction t = new Transaction(buyer, seller, orderId, invoiceNo, itemName, unitPrice, qty, total, method, status, detail, proofPath);
        allTransactions.add(t);
        postTransactionToApi(t);
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
            new String[]{"Cash","Transfer Bank","E-Wallet"}
    );

            JCheckBox paid = new JCheckBox("Mark as PAID", true);

            JPanel bankPanel = new JPanel(new GridLayout(4,1));
            bankPanel.add(new JLabel("Bank name (for Transfer):"));
            JTextField bankField = new JTextField();
            bankPanel.add(bankField);

            bankPanel.add(new JLabel("Transfer reference (for Transfer):"));
            JTextField refField = new JTextField();
            bankPanel.add(refField);

            bankPanel.setVisible(false);

            JPanel walletPanel = new JPanel(new GridLayout(4,1));

            walletPanel.add(new JLabel("E-Wallet provider:"));
            JComboBox<String> ewalletBox = new JComboBox<>(new String[]{"OVO","GoPay","Dana"});
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
            p.add(Box.createRigidArea(new Dimension(0,10)));
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
            messages.setBorder(new EmptyBorder(10, 10, 10, 10));

            bottomGlue = Box.createVerticalGlue();
            messages.add(bottomGlue);

            scrollPane = new JScrollPane(messages);
            scrollPane.setBorder(BorderFactory.createEmptyBorder());
            scrollPane.getViewport().setBackground(Color.WHITE);
            scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
            scrollPane.getVerticalScrollBar().setUI(new CustomScrollBarUI());
            scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

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

            RoundedPanel bubble = new RoundedPanel(new BorderLayout(), 20, new Color(245, 245, 250));
            bubble.setBorder(new EmptyBorder(14, 18, 14, 18));

            JTextArea ta = bubbleText(text, FONT_SYSTEM, new Color(90, 90, 90));
            bubble.add(ta, BorderLayout.CENTER);
            bubble.setMaximumSize(new Dimension(700, Integer.MAX_VALUE));
            wrap.add(bubble);

            insertBeforeGlue(wrap);
            insertBeforeGlue(Box.createRigidArea(new Dimension(0, 6)));

            refresh();
            scrollToBottom();
        }

        private void addTextBubble(String text, boolean isSelf) {
            JPanel wrap = new JPanel(new FlowLayout(isSelf ? FlowLayout.RIGHT : FlowLayout.LEFT, 0, 8));
            wrap.setOpaque(false);

            RoundedPanel bubble = new RoundedPanel(new BorderLayout(), 25, isSelf ? selfColor : otherColor);
            bubble.setBorder(new EmptyBorder(12, 15, 12, 15));

            JTextArea textArea = bubbleText(text, FONT_CHAT, isSelf ? selfTextColor : otherTextColor);
            bubble.add(textArea, BorderLayout.CENTER);
            bubble.setMaximumSize(new Dimension(700, Integer.MAX_VALUE));
            wrap.add(bubble);

            insertBeforeGlue(wrap);
            insertBeforeGlue(Box.createRigidArea(new Dimension(0, 5)));

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
            ta.setBorder(BorderFactory.createEmptyBorder());
            ta.setPreferredSize(new Dimension(520, ta.getPreferredSize().height));
            ta.setMaximumSize(new Dimension(650, Integer.MAX_VALUE));
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
        private Color shadowColor = Color.GRAY;

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
                g2.setColor(new Color(
                        shadowColor.getRed(),
                        shadowColor.getGreen(),
                        shadowColor.getBlue(),
                        50
                ));
                g2.fillRoundRect(
                        shadowSize,
                        shadowSize,
                        getWidth() - shadowSize,
                        getHeight() - shadowSize,
                        radius,
                        radius
                );
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
        }

        @Override
        public void setBackground(Color bg) {
            this.bgColor = bg;
            super.setBackground(bg);
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            Color fill = bgColor;
            if (!isEnabled()) fill = new Color(200, 200, 200);
            else if (getModel().isPressed()) fill = bgColor.darker();
            else if (getModel().isRollover()) fill = brighter(bgColor, 0.2f);

            g2.setColor(fill);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), radius, radius);
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

    private static class CustomScrollBarUI extends BasicScrollBarUI {
        @Override
        protected void configureScrollBarColors() {
            thumbColor = new Color(255, 182, 193);
            trackColor = new Color(255, 240, 247);
        }

        @Override
        protected JButton createDecreaseButton(int orientation) {
            return zeroButton();
        }

        @Override
        protected JButton createIncreaseButton(int orientation) {
            return zeroButton();
        }

        private JButton zeroButton() {
            JButton button = new JButton();
            button.setPreferredSize(new Dimension(0, 0));
            return button;
        }

        @Override
        protected void paintThumb(Graphics g, JComponent c, Rectangle thumbBounds) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(thumbColor);
            g2.fillRoundRect(
                    thumbBounds.x + 2,
                    thumbBounds.y + 2,
                    thumbBounds.width - 4,
                    thumbBounds.height - 4,
                    10,
                    10
            );
            g2.dispose();
        }

        @Override
        protected void paintTrack(Graphics g, JComponent c, Rectangle trackBounds) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(trackColor);
            g2.fillRect(trackBounds.x, trackBounds.y, trackBounds.width, trackBounds.height);
            g2.dispose();
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
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }
        SwingUtilities.invokeLater(MainApp::new);
    }
}
