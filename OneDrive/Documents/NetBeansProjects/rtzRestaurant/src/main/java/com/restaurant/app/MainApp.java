package com.restaurant.app;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;

public class MainApp extends JFrame {

    private ChatView buyerChatView;
    private ChatView sellerChatView;

    private JTextField buyerMessageField;
    private JTextField sellerMessageField;

    private String currentBuyer = "Buyer";

    // Data untuk multiple buyers
    private java.util.List<String> buyerList = new ArrayList<>();
    private java.util.Map<String, Integer> buyerOrderCount = new HashMap<>();

    // Data menu dari seller untuk order yang sedang aktif
    private String[] sellerMenuItems = new String[3];
    private String[] sellerPrices = new String[3];

    // Reference ke form buyer (untuk di-update)
    private RoundedPanel currentBuyerForm = null;
    private JPanel[] buyerFormRows = new JPanel[3];
    private RoundedButton[] buyerSubmitButtons = new RoundedButton[3];
    private JLabel[] buyerMenuLabels = new JLabel[3];
    private JLabel[] buyerPriceLabels = new JLabel[3];
    private JLabel[] buyerStatusLabels = new JLabel[3];

    // Counter untuk berapa menu yang sudah dipilih
    private int itemsChosen = 0;

    // Colors
    private final Color PINK_LIGHT = new Color(255, 240, 247);
    private final Color PINK_MEDIUM = new Color(255, 182, 193);
    private final Color PINK_DARK = new Color(255, 105, 180);
    private final Color MINT_LIGHT = new Color(230, 255, 243);
    private final Color MINT_MEDIUM = new Color(166, 227, 214);
    private final Color MINT_DARK = new Color(79, 203, 141);
    private final Color BG_COLOR = new Color(255, 245, 250);
    private final Color GREEN = new Color(144, 238, 144);
    private final Color ORANGE = new Color(255, 165, 0);

    // Font biasa (Sans-serif)
    private final Font FONT_NORMAL = new Font("SansSerif", Font.PLAIN, 13);
    private final Font FONT_BOLD = new Font("SansSerif", Font.BOLD, 13);
    private final Font FONT_TITLE = new Font("SansSerif", Font.BOLD, 18);
    private final Font FONT_HEADER = new Font("SansSerif", Font.BOLD, 20);
    private final Font FONT_SMALL = new Font("SansSerif", Font.PLAIN, 11);
    private final Font FONT_BUTTON = new Font("SansSerif", Font.BOLD, 14);
    private final Font FONT_CHAT = new Font("SansSerif", Font.PLAIN, 14);

    // ✅ PERBESAR SYSTEM FONT
    private final Font FONT_SYSTEM = new Font("SansSerif", Font.PLAIN, 14);

    public MainApp() {
        setTitle("🍕✨ RTZ Chat - Cute Restaurant Order System ✨🏪");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1400, 800);
        setLocationRelativeTo(null);

        buyerList.add("Buyer");
        buyerOrderCount.put("Buyer", 0);

        JPanel root = new JPanel(new GridLayout(1, 2, 20, 0));
        root.setBorder(new EmptyBorder(20, 20, 20, 20));
        root.setBackground(BG_COLOR);

        buyerChatView = new ChatView(PINK_LIGHT, MINT_LIGHT, PINK_DARK, MINT_DARK);
        sellerChatView = new ChatView(MINT_LIGHT, PINK_LIGHT, MINT_DARK, PINK_DARK);

        root.add(createBuyerPanel());
        root.add(createSellerPanel());

        setContentPane(root);

        SwingUtilities.invokeLater(() -> {
            buyerChatView.addSystem("🌸 Welcome to RTZ Chat! Let's order some yummy food! 🍕");
            sellerChatView.addSystem("✨ Hello Seller! Ready to take delicious orders! 🏪");
        });
    }

    private JPanel createBuyerPanel() {
        RoundedPanel panel = new RoundedPanel(new BorderLayout(0, 15), 30, Color.WHITE);
        panel.setBorder(new CompoundBorder(
                new LineBorder(PINK_MEDIUM, 2, true),
                new EmptyBorder(15, 15, 15, 15)
        ));
        panel.setShadow(10, PINK_LIGHT);

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);

        JLabel title = new JLabel("  🍕✨ Buyer  ");
        title.setFont(FONT_HEADER);
        title.setForeground(PINK_DARK);

        JLabel buyerInfo = new JLabel("Active: " + currentBuyer);
        buyerInfo.setFont(FONT_SMALL);
        buyerInfo.setForeground(new Color(150, 150, 150));

        header.add(title, BorderLayout.WEST);
        header.add(buyerInfo, BorderLayout.EAST);
        panel.add(header, BorderLayout.NORTH);

        panel.add(buyerChatView.getView(), BorderLayout.CENTER);

        JPanel inputPanel = new JPanel(new BorderLayout(10, 0));
        inputPanel.setOpaque(false);
        inputPanel.setBorder(new EmptyBorder(10, 5, 5, 5));

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
        sendBtn.setFont(new Font("SansSerif", Font.BOLD, 16));
        sendBtn.setBorder(new EmptyBorder(0, 0, 0, 0));

        sendBtn.addActionListener(e -> sendBuyerMessage());
        buyerMessageField.addActionListener(e -> sendBuyerMessage());

        inputPanel.add(buyerMessageField, BorderLayout.CENTER);
        inputPanel.add(sendBtn, BorderLayout.EAST);

        panel.add(inputPanel, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel createSellerPanel() {
        RoundedPanel panel = new RoundedPanel(new BorderLayout(0, 15), 30, Color.WHITE);
        panel.setBorder(new CompoundBorder(
                new LineBorder(MINT_MEDIUM, 2, true),
                new EmptyBorder(15, 15, 15, 15)
        ));
        panel.setShadow(10, MINT_LIGHT);

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);

        JLabel title = new JLabel("  🏪✨ Seller  ");
        title.setFont(FONT_HEADER);
        title.setForeground(MINT_DARK);

        JLabel buyerCount = new JLabel("Total Buyers: " + buyerList.size());
        buyerCount.setFont(FONT_SMALL);
        buyerCount.setForeground(new Color(150, 150, 150));

        header.add(title, BorderLayout.WEST);
        header.add(buyerCount, BorderLayout.EAST);
        panel.add(header, BorderLayout.NORTH);

        panel.add(sellerChatView.getView(), BorderLayout.CENTER);

        JPanel inputPanel = new JPanel(new BorderLayout(10, 0));
        inputPanel.setOpaque(false);
        inputPanel.setBorder(new EmptyBorder(10, 5, 5, 5));

        sellerMessageField = new JTextField();
        sellerMessageField.setFont(FONT_CHAT);
        sellerMessageField.setBorder(new CompoundBorder(
                new LineBorder(MINT_MEDIUM, 2, true),
                new EmptyBorder(10, 15, 10, 15)
        ));
        sellerMessageField.setBackground(new Color(245, 255, 250));
        sellerMessageField.setForeground(new Color(60, 100, 80));
        sellerMessageField.setCaretColor(MINT_DARK);

        RoundedButton sendBtn = new RoundedButton("➤", MINT_DARK, Color.WHITE, 40);
        sendBtn.setFont(new Font("SansSerif", Font.BOLD, 16));
        sendBtn.setBorder(new EmptyBorder(0, 0, 0, 0));

        sendBtn.addActionListener(e -> sendSellerMessage());
        sellerMessageField.addActionListener(e -> sendSellerMessage());

        inputPanel.add(sellerMessageField, BorderLayout.CENTER);
        inputPanel.add(sendBtn, BorderLayout.EAST);

        panel.add(inputPanel, BorderLayout.SOUTH);
        return panel;
    }

    private void sendBuyerMessage() {
        String text = buyerMessageField.getText().trim();
        if (text.isEmpty()) return;

        String lowerText = text.toLowerCase();

        if (lowerText.startsWith("i am ") || lowerText.startsWith("saya ") ||
                lowerText.startsWith("nama saya ") || lowerText.startsWith("my name is ")) {

            String name = extractNameFromMessage(text);
            if (!name.isEmpty() && !buyerList.contains(name)) {
                currentBuyer = name;
                buyerList.add(name);
                buyerOrderCount.put(name, 0);

                buyerChatView.addSelf("🌸 " + name, "Hello! I'm " + name);
                sellerChatView.addOther("🌸 " + name, "Hello! I'm " + name);
                buyerMessageField.setText("");
                return;
            }
        }

        if (lowerText.contains("order") || lowerText.contains("menu") ||
                lowerText.contains("new") || lowerText.contains("makan") ||
                lowerText.contains("pesan") || lowerText.contains("mau")) {

            startNewOrder();
            buyerMessageField.setText("");

            if (!text.equalsIgnoreCase("order") && !text.equalsIgnoreCase("new")) {
                buyerChatView.addSelf("🌸 " + currentBuyer, text);
                sellerChatView.addOther("🌸 " + currentBuyer, text);
            }
            return;
        }

        buyerChatView.addSelf("🌸 " + currentBuyer, text);
        sellerChatView.addOther("🌸 " + currentBuyer, text);

        buyerMessageField.setText("");
    }

    private String extractNameFromMessage(String message) {
        String[] patterns = {"i am ", "saya ", "nama saya ", "my name is "};
        String lowerMessage = message.toLowerCase();

        for (String pattern : patterns) {
            if (lowerMessage.startsWith(pattern)) {
                String name = message.substring(pattern.length()).trim();
                name = name.replaceAll("[.,!?;:]$", "").trim();
                if (!name.isEmpty()) {
                    return name.substring(0, 1).toUpperCase() + name.substring(1);
                }
            }
        }
        return "";
    }

    private void sendSellerMessage() {
        String text = sellerMessageField.getText().trim();
        if (text.isEmpty()) return;

        sellerChatView.addSelf("✨ Seller", text);
        buyerChatView.addOther("✨ Seller", text);

        sellerMessageField.setText("");
    }

    private void startNewOrder() {
        itemsChosen = 0;

        sellerMenuItems = new String[3];
        sellerPrices = new String[3];
        buyerFormRows = new JPanel[3];
        buyerSubmitButtons = new RoundedButton[3];
        buyerMenuLabels = new JLabel[3];
        buyerPriceLabels = new JLabel[3];
        buyerStatusLabels = new JLabel[3];

        int orderCount = buyerOrderCount.getOrDefault(currentBuyer, 0) + 1;
        buyerOrderCount.put(currentBuyer, orderCount);

        currentBuyerForm = createEmptyBuyerForm();
        buyerChatView.addComponentBubble(currentBuyerForm, false);

        JComponent sellerForm = createSellerOrderForm();
        sellerChatView.addComponentBubble(sellerForm, true);

        sellerChatView.addSystem("📋 " + currentBuyer + " requested a new order (#" + orderCount + "). Please fill the form!");
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

    private void showCheapestAndMostExpensive() {
        double minPrice = Double.MAX_VALUE;
        double maxPrice = -1;
        int minIndex = -1;
        int maxIndex = -1;

        for (int i = 0; i < 3; i++) {
            double p = parsePriceSafe(sellerPrices[i]);
            if (p <= 0) continue;

            if (p < minPrice) {
                minPrice = p;
                minIndex = i;
            }
            if (p > maxPrice) {
                maxPrice = p;
                maxIndex = i;
            }
        }

        if (minIndex == -1 || maxIndex == -1) return;

        String msg =
                "💡 Price Summary\n\n" +
                "✅ Cheapest:\n" + sellerMenuItems[minIndex] + " (Rp " + sellerPrices[minIndex] + ")\n\n" +
                "🧡 Most Expensive:\n" + sellerMenuItems[maxIndex] + " (Rp " + sellerPrices[maxIndex] + ")";

        buyerChatView.addSystem(msg);
        sellerChatView.addSystem(msg);

        highlightBuyerRow(minIndex, new Color(240, 255, 240), new Color(0, 130, 0));
        highlightBuyerRow(maxIndex, new Color(255, 235, 235), new Color(160, 60, 60));
    }

    private void highlightBuyerRow(int index, Color bg, Color textColor) {
        if (index < 0 || index >= 3) return;
        if (buyerFormRows[index] == null) return;

        Component[] components = buyerFormRows[index].getComponents();
        if (components.length > 1 && components[1] instanceof JPanel) {
            JPanel placeholderPanel = (JPanel) components[1];
            placeholderPanel.setOpaque(true);
            placeholderPanel.setBackground(bg);
            placeholderPanel.setBorder(new CompoundBorder(
                    new LineBorder(new Color(220, 220, 220), 1, true),
                    new EmptyBorder(8, 8, 8, 8)
            ));

            if (buyerMenuLabels[index] != null) buyerMenuLabels[index].setForeground(textColor);
            if (buyerPriceLabels[index] != null) buyerPriceLabels[index].setForeground(textColor);

            placeholderPanel.revalidate();
            placeholderPanel.repaint();
        }
    }

    private RoundedPanel createEmptyBuyerForm() {
        RoundedPanel mainBubble = new RoundedPanel(new BorderLayout(0, 15), 25,
                new Color(255, 250, 245));

        mainBubble.setBorder(new EmptyBorder(20, 20, 20, 20));
        mainBubble.setMaximumSize(new Dimension(700, Integer.MAX_VALUE));

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);

        JLabel title = new JLabel("🍕 Order from: " + currentBuyer);
        title.setFont(FONT_TITLE);
        title.setForeground(PINK_DARK);

        int orderNum = buyerOrderCount.getOrDefault(currentBuyer, 0);
        JLabel count = new JLabel("✨ Order #" + orderNum + " • Choose 1-3 items");
        count.setFont(FONT_SMALL);
        count.setForeground(new Color(150, 150, 150));

        header.add(title, BorderLayout.WEST);
        header.add(count, BorderLayout.EAST);
        mainBubble.add(header, BorderLayout.NORTH);

        JPanel rowsPanel = new JPanel();
        rowsPanel.setLayout(new BoxLayout(rowsPanel, BoxLayout.Y_AXIS));
        rowsPanel.setOpaque(false);
        rowsPanel.setBorder(new EmptyBorder(15, 5, 20, 5));

        for (int i = 0; i < 3; i++) {
            buyerFormRows[i] = createEmptyBuyerFormRow(i);
            rowsPanel.add(buyerFormRows[i]);
            if (i < 2) rowsPanel.add(Box.createRigidArea(new Dimension(0, 15)));
        }

        mainBubble.add(rowsPanel, BorderLayout.CENTER);

        JPanel footer = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 5));
        footer.setOpaque(false);

        JLabel instruction = new JLabel("⏳ Seller is creating menu. Please wait...");
        instruction.setFont(FONT_SMALL);
        instruction.setForeground(new Color(255, 140, 0));

        footer.add(instruction);
        mainBubble.add(footer, BorderLayout.SOUTH);

        return mainBubble;
    }

    private JPanel createEmptyBuyerFormRow(int index) {
        JPanel mainRow = new JPanel(new BorderLayout(10, 0));
        mainRow.setOpaque(false);

        JLabel itemLabel = new JLabel((index + 1) + ". ");
        itemLabel.setFont(FONT_BOLD);
        itemLabel.setForeground(new Color(120, 120, 120));
        itemLabel.setPreferredSize(new Dimension(30, 50));

        JPanel placeholderPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        placeholderPanel.setOpaque(false);

        buyerMenuLabels[index] = new JLabel("Waiting for menu...");
        buyerMenuLabels[index].setFont(FONT_NORMAL);
        buyerMenuLabels[index].setForeground(new Color(180, 180, 180));

        JPanel menuPanel = new JPanel();
        menuPanel.setBackground(new Color(245, 245, 245));
        menuPanel.setBorder(new MatteBorder(0, 0, 1, 0, new Color(220, 220, 220)));
        menuPanel.setPreferredSize(new Dimension(220, 35));
        menuPanel.add(buyerMenuLabels[index]);

        buyerPriceLabels[index] = new JLabel("---");
        buyerPriceLabels[index].setFont(FONT_NORMAL);
        buyerPriceLabels[index].setForeground(new Color(180, 180, 180));

        JPanel pricePanel = new JPanel(new BorderLayout(5, 0));
        pricePanel.setOpaque(false);
        pricePanel.setBorder(new MatteBorder(0, 0, 1, 0, new Color(220, 220, 220)));
        pricePanel.setPreferredSize(new Dimension(150, 35));

        JLabel rpLabel = new JLabel("Rp ");
        rpLabel.setFont(FONT_BOLD);
        rpLabel.setForeground(new Color(180, 180, 180));
        rpLabel.setBorder(new EmptyBorder(0, 0, 0, 5));

        pricePanel.add(rpLabel, BorderLayout.WEST);
        pricePanel.add(buyerPriceLabels[index], BorderLayout.CENTER);

        buyerSubmitButtons[index] = new RoundedButton("Submit",
                new Color(200, 200, 200), Color.WHITE, 15);
        buyerSubmitButtons[index].setFont(new Font("SansSerif", Font.BOLD, 12));
        buyerSubmitButtons[index].setBorder(new EmptyBorder(6, 15, 6, 15));
        buyerSubmitButtons[index].setPreferredSize(new Dimension(90, 35));
        buyerSubmitButtons[index].setEnabled(false);

        buyerStatusLabels[index] = new JLabel("");
        buyerStatusLabels[index].setFont(FONT_SMALL);
        buyerStatusLabels[index].setForeground(new Color(180, 180, 180));

        placeholderPanel.add(menuPanel);
        placeholderPanel.add(pricePanel);
        placeholderPanel.add(buyerSubmitButtons[index]);

        mainRow.add(itemLabel, BorderLayout.WEST);
        mainRow.add(placeholderPanel, BorderLayout.CENTER);

        return mainRow;
    }

    private JComponent createSellerOrderForm() {
        RoundedPanel sellerBubble = new RoundedPanel(new BorderLayout(0, 15), 25,
                new Color(255, 245, 250));

        sellerBubble.setBorder(new EmptyBorder(20, 20, 20, 20));
        sellerBubble.setMaximumSize(new Dimension(520, Integer.MAX_VALUE));

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);

        JLabel title = new JLabel("📝 Create Menu for: " + currentBuyer);
        title.setFont(FONT_TITLE);
        title.setForeground(MINT_DARK);

        int orderNum = buyerOrderCount.getOrDefault(currentBuyer, 0);
        JLabel count = new JLabel("✨ Order #" + orderNum + " • 3 items (choose 1-3)");
        count.setFont(FONT_SMALL);
        count.setForeground(new Color(150, 150, 150));

        header.add(title, BorderLayout.WEST);
        header.add(count, BorderLayout.EAST);
        sellerBubble.add(header, BorderLayout.NORTH);

        JPanel rowsPanel = new JPanel();
        rowsPanel.setLayout(new BoxLayout(rowsPanel, BoxLayout.Y_AXIS));
        rowsPanel.setOpaque(false);
        rowsPanel.setBorder(new EmptyBorder(15, 5, 20, 5));

        final JTextField[] menuFields = new JTextField[3];
        final JTextField[] priceFields = new JTextField[3];

        for (int i = 0; i < 3; i++) {
            JPanel rowPanel = createSellerFormRow(i, menuFields, priceFields);
            rowsPanel.add(rowPanel);
            if (i < 2) rowsPanel.add(Box.createRigidArea(new Dimension(0, 15)));
        }

        sellerBubble.add(rowsPanel, BorderLayout.CENTER);

        RoundedButton sendBtn = new RoundedButton("🚀 Send Menu to " + currentBuyer,
                MINT_DARK, Color.WHITE, 30);
        sendBtn.setFont(FONT_BUTTON);
        sendBtn.setBorder(new EmptyBorder(15, 30, 15, 30));

        sendBtn.addActionListener(e -> {
            boolean allFilled = true;

            for (int i = 0; i < 3; i++) {
                String menu = menuFields[i].getText().trim();
                String price = priceFields[i].getText().trim();

                if (menu.isEmpty() || price.isEmpty()) {
                    allFilled = false;
                    menuFields[i].setBackground(new Color(255, 230, 230));
                    priceFields[i].setBackground(new Color(255, 230, 230));
                } else {
                    menuFields[i].setBackground(Color.WHITE);
                    priceFields[i].setBackground(Color.WHITE);

                    sellerMenuItems[i] = menu;
                    sellerPrices[i] = price;
                }
            }

            if (allFilled) {
                sendBtn.setEnabled(false);
                sendBtn.setText("✓ Menu Sent");
                sendBtn.setBackground(new Color(200, 200, 200));

                updateBuyerFormWithMenu();
                showCheapestAndMostExpensive();

                sellerChatView.addSystem("✅ Menu sent to " + currentBuyer + " successfully!");
            } else {
                sellerChatView.addSystem("⚠️ Please fill all menu and price fields first! 🎀");
            }
        });

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 10));
        btnPanel.setOpaque(false);
        btnPanel.add(sendBtn);

        sellerBubble.add(btnPanel, BorderLayout.SOUTH);

        return sellerBubble;
    }

    private JPanel createSellerFormRow(int index, JTextField[] menuFields, JTextField[] priceFields) {
        JPanel row = new JPanel(new BorderLayout(15, 0));
        row.setOpaque(false);

        JLabel itemLabel = new JLabel((index + 1) + ". ");
        itemLabel.setFont(FONT_BOLD);
        itemLabel.setForeground(new Color(120, 120, 120));
        itemLabel.setPreferredSize(new Dimension(30, 40));

        JPanel fieldsPanel = new JPanel(new GridLayout(1, 2, 15, 0));
        fieldsPanel.setOpaque(false);

        JTextField menuField = new JTextField();
        menuField.setFont(FONT_NORMAL);
        menuField.setBorder(new CompoundBorder(
                new LineBorder(MINT_LIGHT, 2, true),
                new EmptyBorder(8, 12, 8, 12)
        ));
        menuField.setBackground(Color.WHITE);
        menuField.setPreferredSize(new Dimension(200, 45));

        JPanel pricePanel = new JPanel(new BorderLayout(8, 0));
        pricePanel.setOpaque(false);

        JLabel rpLabel = new JLabel("Rp ");
        rpLabel.setFont(FONT_BOLD);
        rpLabel.setForeground(new Color(255, 140, 0));
        rpLabel.setBorder(new EmptyBorder(0, 0, 0, 5));

        JTextField priceField = new JTextField();
        priceField.setFont(FONT_NORMAL);
        priceField.setBorder(new CompoundBorder(
                new LineBorder(MINT_LIGHT, 2, true),
                new EmptyBorder(8, 12, 8, 12)
        ));
        priceField.setBackground(Color.WHITE);
        priceField.setPreferredSize(new Dimension(150, 45));

        pricePanel.add(rpLabel, BorderLayout.WEST);
        pricePanel.add(priceField, BorderLayout.CENTER);

        fieldsPanel.add(menuField);
        fieldsPanel.add(pricePanel);

        row.add(itemLabel, BorderLayout.WEST);
        row.add(fieldsPanel, BorderLayout.CENTER);

        menuFields[index] = menuField;
        priceFields[index] = priceField;

        return row;
    }

    private void updateBuyerFormWithMenu() {
        if (currentBuyerForm == null) return;

        for (int i = 0; i < 3; i++) {
            if (sellerMenuItems[i] != null && sellerPrices[i] != null) {
                buyerMenuLabels[i].setText(sellerMenuItems[i]);
                buyerMenuLabels[i].setForeground(new Color(0, 100, 0));

                buyerPriceLabels[i].setText(sellerPrices[i]);
                buyerPriceLabels[i].setForeground(new Color(0, 100, 0));

                buyerSubmitButtons[i].setEnabled(true);
                buyerSubmitButtons[i].setBackground(GREEN);

                final int index = i;
                for (ActionListener al : buyerSubmitButtons[i].getActionListeners()) {
                    buyerSubmitButtons[i].removeActionListener(al);
                }
                buyerSubmitButtons[i].addActionListener(e -> handleBuyerChoice(index));
            }
        }

        currentBuyerForm.revalidate();
        currentBuyerForm.repaint();
    }

    private void handleBuyerChoice(int index) {
        String menu = sellerMenuItems[index] != null ? sellerMenuItems[index] : "Menu " + (index + 1);
        String price = sellerPrices[index] != null ? sellerPrices[index] : "0";

        buyerSubmitButtons[index].setEnabled(false);
        buyerSubmitButtons[index].setText("✓ Chosen");
        buyerSubmitButtons[index].setBackground(new Color(200, 200, 200));

        itemsChosen++;

        String choiceMessage = "I choose item " + (index + 1) + ": " + menu + " (Rp " + price + ")";
        buyerChatView.addSelf("🌸 " + currentBuyer, choiceMessage);
        sellerChatView.addOther("🌸 " + currentBuyer, choiceMessage);
    }

    // -------------------- CHAT VIEW --------------------
    private class ChatView {
        private final JPanel messages;
        private final JScrollPane scrollPane;
        private final Color selfColor;
        private final Color otherColor;
        private final Color selfTextColor;
        private final Color otherTextColor;

        ChatView(Color selfColor, Color otherColor, Color selfTextColor, Color otherTextColor) {
            this.selfColor = selfColor;
            this.otherColor = otherColor;
            this.selfTextColor = selfTextColor;
            this.otherTextColor = otherTextColor;

            messages = new JPanel();
            messages.setLayout(new BoxLayout(messages, BoxLayout.Y_AXIS));
            messages.setBackground(Color.WHITE);
            messages.setBorder(new EmptyBorder(10, 10, 10, 10));

            scrollPane = new JScrollPane(messages);
            scrollPane.setBorder(BorderFactory.createEmptyBorder());
            scrollPane.getViewport().setBackground(Color.WHITE);
            scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
            scrollPane.getVerticalScrollBar().setUI(new CustomScrollBarUI());
            scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
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

        // ✅ DIBUAT LEBIH BESAR & LEBIH MUDAH DIBACA
        void addSystem(String text) {
            JPanel wrap = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 10));
            wrap.setOpaque(false);

            RoundedPanel bubble = new RoundedPanel(new BorderLayout(), 20, new Color(245, 245, 250));
            bubble.setBorder(new EmptyBorder(14, 18, 14, 18));

            JTextArea ta = new JTextArea(text);
            ta.setFont(FONT_SYSTEM); // 14
            ta.setForeground(new Color(90, 90, 90));
            ta.setOpaque(false);
            ta.setEditable(false);
            ta.setLineWrap(true);
            ta.setWrapStyleWord(true);
            ta.setBorder(BorderFactory.createEmptyBorder());

            // Biar tinggi baris lebih enak
            ta.setMargin(new Insets(0, 0, 0, 0));

            bubble.add(ta, BorderLayout.CENTER);

            // Lebarkan bubble biar gak kecil
            bubble.setMaximumSize(new Dimension(520, Integer.MAX_VALUE));
            bubble.setPreferredSize(new Dimension(420, bubble.getPreferredSize().height));

            wrap.add(bubble);
            messages.add(wrap);
            messages.add(Box.createRigidArea(new Dimension(0, 6)));

            refresh();
            scrollToBottom();
        }

        void addComponentBubble(JComponent component, boolean isSelf) {
            int align = isSelf ? FlowLayout.RIGHT : FlowLayout.LEFT;

            JPanel wrap = new JPanel(new FlowLayout(align, 0, 12));
            wrap.setOpaque(false);
            wrap.add(component);

            messages.add(wrap);
            messages.add(Box.createRigidArea(new Dimension(0, 15)));

            refresh();
            scrollToBottom();
        }

        private void addTextBubble(String text, boolean isSelf) {
    int align = isSelf ? FlowLayout.RIGHT : FlowLayout.LEFT;

    JPanel wrap = new JPanel(new FlowLayout(align, 0, 8));
    wrap.setOpaque(false);

    RoundedPanel bubble = new RoundedPanel(new BorderLayout(), 25,
            isSelf ? selfColor : otherColor);
    bubble.setBorder(new EmptyBorder(12, 15, 12, 15));

    JTextArea textArea = new JTextArea(text);
    textArea.setFont(FONT_CHAT);
    textArea.setForeground(isSelf ? selfTextColor : otherTextColor);
    textArea.setOpaque(false);
    textArea.setEditable(false);
    textArea.setLineWrap(true);
    textArea.setWrapStyleWord(true);
    textArea.setBorder(BorderFactory.createEmptyBorder());

    bubble.add(textArea, BorderLayout.CENTER);

    // ✅ LEBAR DINAMIS: pendek -> bubble melebar secukupnya
    // tapi tetap ada max biar gak kepanjangan
    int chars = Math.min(text.length(), 60);
    int dynamicWidth = 120 + (chars * 7); // kira-kira 7px per char
    int maxWidth = 520;                  // batas maksimal
    int width = Math.min(dynamicWidth, maxWidth);

    bubble.setPreferredSize(new Dimension(width, bubble.getPreferredSize().height));
    bubble.setMaximumSize(new Dimension(maxWidth, Integer.MAX_VALUE));

    wrap.add(bubble);
    messages.add(wrap);
    messages.add(Box.createRigidArea(new Dimension(0, 5)));

    refresh();
    scrollToBottom();
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

    // -------------------- CUSTOM COMPONENTS --------------------
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
                g2.setColor(new Color(shadowColor.getRed(), shadowColor.getGreen(), shadowColor.getBlue(), 50));
                g2.fillRoundRect(shadowSize, shadowSize, getWidth() - shadowSize, getHeight() - shadowSize, radius, radius);
            }

            g2.setColor(bgColor);
            g2.fillRoundRect(0, 0, getWidth() - shadowSize, getHeight() - shadowSize, radius, radius);

            g2.dispose();
            super.paintComponent(g);
        }
    }

    private static class RoundedButton extends JButton {
        private Color bgColor;
        private final Color fgColor;
        private final int radius;

        RoundedButton(String text, Color bgColor, Color fgColor, int radius) {
            super(text);
            this.bgColor = bgColor;
            this.fgColor = fgColor;
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

    private static class CustomScrollBarUI extends javax.swing.plaf.basic.BasicScrollBarUI {
        @Override
        protected void configureScrollBarColors() {
            this.thumbColor = new Color(255, 182, 193);
            this.trackColor = new Color(255, 240, 247);
        }

        @Override
        protected JButton createDecreaseButton(int orientation) {
            return createZeroButton();
        }

        @Override
        protected JButton createIncreaseButton(int orientation) {
            return createZeroButton();
        }

        private JButton createZeroButton() {
            JButton button = new JButton();
            button.setPreferredSize(new Dimension(0, 0));
            button.setMinimumSize(new Dimension(0, 0));
            button.setMaximumSize(new Dimension(0, 0));
            return button;
        }

        @Override
        protected void paintThumb(Graphics g, JComponent c, Rectangle thumbBounds) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(thumbColor);
            g2.fillRoundRect(thumbBounds.x + 2, thumbBounds.y + 2,
                    thumbBounds.width - 4, thumbBounds.height - 4, 10, 10);
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

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        SwingUtilities.invokeLater(() -> {
            MainApp app = new MainApp();
            app.setVisible(true);
        });
    }
}
