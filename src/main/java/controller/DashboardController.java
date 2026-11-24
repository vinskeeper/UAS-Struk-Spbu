package controller;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;

import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.print.PrinterJob;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import javafx.stage.Window;
import dao.PengisianDAO;
import dao.ProdukDAO;
import model.spbu.Pengisian;
import model.spbu.Product;
import model.spbu.Tunai;

public class DashboardController {
    @FXML private TabPane mainTabs;
    private TabPane salesTabs;
    private ComboBox<Product> transactionProductBox;
    private VBox transactionPage;
    private Runnable refreshTransactionTotal = () -> {};
    @FXML private Label loggedUser;
    @FXML private Button peopleButton;
    @FXML private Button productsButton;

    private final ProdukDAO productDAO = new ProdukDAO();
    private final PengisianDAO pengisianDAO = new PengisianDAO();
    private String attendant = "";
    private String currentUsername = "";
    private String currentRole = "PETUGAS";
    private int currentPetugasId;
    private static final Locale ID = Locale.forLanguageTag("id-ID");

    public void initializeApp(String userDetail, String username, int petugasId, String role) {
        attendant = userDetail == null ? "" : userDetail.trim();
        currentUsername = username == null ? "" : username.trim();
        currentPetugasId = petugasId;
        currentRole = "ADMIN".equals(role) ? "ADMIN" : "PETUGAS";
        if (loggedUser != null) loggedUser.setText(attendant);
        boolean admin = "ADMIN".equals(currentRole);
        if (peopleButton != null) { peopleButton.setVisible(admin); peopleButton.setManaged(admin); }
        if (productsButton != null) { productsButton.setVisible(admin); productsButton.setManaged(admin); }
        if (!admin) mainTabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        if (admin) {
            mainTabs.getTabs().setAll(
                    createMainTab("User", buildStaffScreen()),
                    createMainTab("Produk BBM", buildProductScreen()),
                    createMainTab("Transaksi", buildSalesScreen()));
        } else {
            mainTabs.getTabs().setAll(createMainTab("Transaksi", buildSalesScreen()));
        }
        selectTab("Transaksi");
    }

    @FXML private void showTransaksi(ActionEvent event) {
        Tab selected = mainTabs.getSelectionModel().getSelectedItem();
        boolean alreadySelected = selected != null && "Transaksi".equals(selected.getText());
        selectTab("Transaksi");
        if (alreadySelected) refreshTransactionProducts();
        if (salesTabs != null) salesTabs.getSelectionModel().select(0);
    }

    @FXML private void showRiwayat(ActionEvent event) {
        showTransaksi(event);
        if (salesTabs != null) salesTabs.getSelectionModel().select(1);
    }

    @FXML private void showProduk(ActionEvent event) {
        selectTab("Produk BBM");
    }

    @FXML private void showPeople(ActionEvent event) {
        selectTab("User");
    }

    private Tab createMainTab(String title, Node content) {
        Tab tab = new Tab(title, content);
        tab.setClosable(true);
        tab.setOnSelectionChanged(event -> {
            if (tab.isSelected() && "Transaksi".equals(title)) refreshTransactionProducts();
        });
        tab.setOnClosed(event -> {
            if ("Transaksi".equals(title)) salesTabs = null;
        });
        return tab;
    }

    private VBox buildSalesScreen() {
        salesTabs = new TabPane();
        salesTabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        salesTabs.getTabs().setAll(
                new Tab("Transaksi Baru", buildTransactionScreen()),
                new Tab("Riwayat Transaksi", buildHistoryScreen()));
        VBox container = new VBox(salesTabs);
        VBox.setVgrow(salesTabs, Priority.ALWAYS);
        return container;
    }

    private void selectTab(String title) {
        if (!"ADMIN".equals(currentRole) && !"Transaksi".equals(title)) return;
        for (Tab tab : mainTabs.getTabs()) {
            if (title.equals(tab.getText())) {
                mainTabs.getSelectionModel().select(tab);
                return;
            }
        }
        Node content = switch (title) {
            case "User" -> buildStaffScreen();
            case "Produk BBM" -> buildProductScreen();
            default -> buildSalesScreen();
        };
        mainTabs.getTabs().add(createMainTab(title, content));
        mainTabs.getSelectionModel().selectLast();
    }

    @FXML
    private void logout(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/login.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();

            stage.setTitle("UAS Struk SPBU — Login");
            stage.setScene(new Scene(root, 470, 570));
            stage.setMinWidth(440);
            stage.setMinHeight(520);
            stage.centerOnScreen();
        } catch (IOException e) {
            showError("Gagal membuka halaman login", e.getMessage());
        }
    }

    private VBox buildTransactionScreen() {
        FXMLLoader loader = screenLoader("pengisian.fxml");
        VBox page = loadSalesPage(loader, "transaksi");
        if (page == null) return new VBox();

        var controls = loader.getNamespace();
        @SuppressWarnings("unchecked")
        ComboBox<Product> productBox = (ComboBox<Product>) controls.get("productBox");
        @SuppressWarnings("unchecked")
        ComboBox<String> paymentBox = (ComboBox<String>) controls.get("paymentBox");
        TextField vehicleField = (TextField) controls.get("vehicleField");
        TextField litersField = (TextField) controls.get("litersField");
        TextField detailField = (TextField) controls.get("detailField");
        TextField cashField = (TextField) controls.get("cashField");
        Label detailLabel = (Label) controls.get("detailLabel");
        Label cashLabel = (Label) controls.get("cashLabel");
        Label totalValue = (Label) controls.get("totalValue");
        Label changeValue = (Label) controls.get("changeValue");
        Label changeSummaryLabel = (Label) controls.get("changeSummaryLabel");
        Button submit = (Button) controls.get("submit");
        ((Label) controls.get("attendantLabel")).setText(attendant);
        transactionProductBox = productBox;
        transactionPage = page;
        paymentBox.getItems().setAll("TUNAI", "QRIS", "DEBIT");
        paymentBox.getSelectionModel().selectFirst();

        Runnable refreshTotal = () -> {
            double total = 0;
            Product selected = productBox.getValue();
            Double liters = parseOptional(litersField.getText());
            if (selected != null && liters != null && liters > 0) total = selected.hitungHarga(liters);
            totalValue.setText(money(total));
            Double cash = parseOptional(cashField.getText());
            changeValue.setText(money("TUNAI".equals(paymentBox.getValue()) && cash != null
                    ? Math.max(0, cash - total) : 0));
        };
        refreshTransactionTotal = refreshTotal;
        paymentBox.valueProperty().addListener((o, old, value) -> {
            boolean cash = "TUNAI".equals(value);
            cashLabel.setVisible(cash); cashLabel.setManaged(cash);
            cashField.setVisible(cash); cashField.setManaged(cash);
            changeSummaryLabel.setVisible(cash); changeSummaryLabel.setManaged(cash);
            changeValue.setVisible(cash); changeValue.setManaged(cash);
            boolean needsDetail = "QRIS".equals(value) || "DEBIT".equals(value);
            detailLabel.setVisible(needsDetail); detailLabel.setManaged(needsDetail);
            detailField.setVisible(needsDetail); detailField.setManaged(needsDetail);
            detailLabel.setText("QRIS".equals(value) ? "Provider QRIS" : "Bank");
            detailField.setPromptText("QRIS".equals(value) ? "Contoh: GoPay, DANA" : "Contoh: BCA, Mandiri");
            refreshTotal.run();
        });
        productBox.valueProperty().addListener((o, old, value) -> refreshTotal.run());
        litersField.textProperty().addListener((o, old, value) -> refreshTotal.run());
        cashField.textProperty().addListener((o, old, value) -> refreshTotal.run());

        submit.setOnAction(e -> {
            Product selected = productBox.getValue();
            Double liters = parseOptional(litersField.getText());
            String vehicle = vehicleField.getText() == null ? "" : vehicleField.getText().trim();
            String payment = paymentBox.getValue();
            String detail = detailField.getText() == null ? "" : detailField.getText().trim();
            Double cash = parseOptional(cashField.getText());
            if (attendant.isBlank() || vehicle.isBlank() || selected == null || liters == null || liters <= 0) {
                showWarning("Lengkapi petugas, nomor kendaraan, produk, dan jumlah liter yang valid."); return;
            }
            if (java.math.BigDecimal.valueOf(liters).stripTrailingZeros().scale() > 3) {
                showWarning("Jumlah liter maksimal tiga angka di belakang koma."); return;
            }
            if (("QRIS".equals(payment) || "DEBIT".equals(payment)) && detail.isBlank()) {
                showWarning("Isi " + ("QRIS".equals(payment) ? "provider QRIS" : "nama bank") + " terlebih dahulu."); return;
            }
            if ("TUNAI".equals(payment) && (cash == null || cash < selected.hitungHarga(liters))) {
                showWarning("Uang diterima harus berupa angka dan tidak boleh kurang dari total."); return;
            }
            try {
                Pengisian saved = pengisianDAO.save(currentPetugasId, attendant, vehicle, selected.getId(), liters,
                        payment, detail, cash == null ? 0 : cash);
                showReceipt(saved);
                vehicleField.clear(); litersField.clear(); cashField.clear(); detailField.clear();
                productBox.getSelectionModel().clearSelection(); refreshTotal.run();
            } catch (SQLException ex) {
                showError("Transaksi gagal disimpan", readableDatabaseError(ex));
            }
        });

        refreshTransactionProducts();

        return page;
    }

    /** Refresh transaction choices from the same active product records used by product management. */
    private void refreshTransactionProducts() {
        if (transactionProductBox == null) return;
        Product previous = transactionProductBox.getValue();
        try {
            List<Product> products = productDAO.getProducts(true);
            transactionProductBox.setItems(FXCollections.observableArrayList(products));
            Product stillAvailable = products.stream()
                    .filter(product -> product.getId() == (previous == null ? -1 : previous.getId()))
                    .findFirst().orElse(null);
            if (stillAvailable != null) transactionProductBox.setValue(stillAvailable);
            else if (!products.isEmpty()) transactionProductBox.getSelectionModel().selectFirst();
            else transactionProductBox.getSelectionModel().clearSelection();
            transactionPage.getChildren().removeIf(node -> node.getProperties().containsKey("product-load-message"));
            if (products.isEmpty()) {
                Node notice = infoBanner("Belum ada produk aktif. Tambahkan atau aktifkan produk di menu Produk BBM.");
                notice.getProperties().put("product-load-message", true);
                transactionPage.getChildren().add(2, notice);
            }
        } catch (SQLException e) {
            transactionProductBox.getItems().clear();
            Node notice = infoBanner("Data produk gagal dimuat. Periksa koneksi database.");
            notice.getProperties().put("product-load-message", true);
            transactionPage.getChildren().removeIf(node -> node.getProperties().containsKey("product-load-message"));
            transactionPage.getChildren().add(2, notice);
        }
        refreshTransactionTotal.run();
    }

    private VBox buildHistoryScreen() {
        FXMLLoader loader = screenLoader("riwayat.fxml");
        VBox page = loadSalesPage(loader, "riwayat");
        if (page == null) return new VBox();

        var controls = loader.getNamespace();
        TextField search = (TextField) controls.get("search");
        Button find = (Button) controls.get("find");
        Button reload = (Button) controls.get("reload");
        Button receipt = (Button) controls.get("receipt");
        Button delete = (Button) controls.get("delete");
        @SuppressWarnings("unchecked")
        TableView<Pengisian> table = (TableView<Pengisian>) controls.get("table");
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.getColumns().setAll(List.of(
                textColumn("Invoice", Pengisian::getInvoiceNumber),
                textColumn("Waktu", p -> p.getTanggal() == null ? "-" : p.getTanggal()),
                textColumn("Petugas", Pengisian::getAttendant),
                textColumn("Kendaraan", Pengisian::getNomorKendaraan),
                textColumn("Produk", Pengisian::getNamaProdukSnapshot),
                textColumn("Liter", p -> String.format(ID, "%.2f", p.getQuantity())),
                textColumn("Total", p -> money(p.getTotal())),
                textColumn("Bayar", p -> p.getMetode() == null ? "-" : p.getMetode().getRincianPembayaran())));
        Runnable refresh = () -> {
            try {
                table.setItems(FXCollections.observableArrayList(pengisianDAO.getHistory(search.getText())));
                fitTableHeight(table, table.getItems().size());
            }
            catch (SQLException e) { showError("Riwayat tidak dapat dimuat", readableDatabaseError(e)); }
        };
        find.setOnAction(e -> refresh.run()); reload.setOnAction(e -> { search.clear(); refresh.run(); });
        search.setOnAction(e -> refresh.run());
        receipt.setOnAction(e -> {
            Pengisian selected = table.getSelectionModel().getSelectedItem();
            if (selected == null) showWarning("Pilih transaksi yang ingin dilihat struknya.");
            else showReceipt(selected);
        });
        delete.setOnAction(e -> {
            Pengisian selected = table.getSelectionModel().getSelectedItem();
            if (selected == null) { showWarning("Pilih transaksi yang ingin dihapus."); return; }
            if (!confirmDelete("Hapus transaksi?", "Transaksi " + selected.getInvoiceNumber()
                    + " dan data pembayarannya akan dihapus permanen.")) return;
            try {
                pengisianDAO.delete(selected.getId());
                refresh.run();
            } catch (SQLException ex) {
                showError("Transaksi gagal dihapus", readableDatabaseError(ex));
            }
        });
        table.setRowFactory(tv -> {
            javafx.scene.control.TableRow<Pengisian> row = new javafx.scene.control.TableRow<>();
            row.setOnMouseClicked(e -> { if (e.getClickCount() == 2 && !row.isEmpty()) showReceipt(row.getItem()); });
            return row;
        });
        try { refresh.run(); }
        catch (RuntimeException e) { page.getChildren().add(infoBanner("Belum dapat membaca tabel SPBU. Pastikan skema sudah disiapkan.")); }
        return page;
    }

    private FXMLLoader screenLoader(String fileName) {
        return new FXMLLoader(getClass().getResource("/fxml/" + fileName));
    }

    private VBox loadSalesPage(FXMLLoader loader, String name) {
        try {
            return loader.load();
        } catch (IOException e) {
            showError("Gagal membuka halaman " + name, e.getMessage());
            return null;
        }
    }

    private VBox buildProductScreen() {
        try { return new FXMLLoader(getClass().getResource("/fxml/produk.fxml")).load(); }
        catch (IOException e) { showError("Gagal membuka halaman produk", e.getMessage()); return new VBox(); }
    }
    private VBox buildStaffScreen() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/petugas.fxml"));
            VBox view = loader.load();
            UserController controller = loader.getController();
            controller.setActiveUsername(currentUsername);
            return view;
        } catch (IOException e) {
            showError("Gagal membuka halaman User", e.getMessage());
            return new VBox();
        }
    }
    private static <T> TableColumn<T, String> textColumn(String title, Function<T, String> text) {
        TableColumn<T, String> column = new TableColumn<>(title);
        column.setCellValueFactory(data -> new ReadOnlyStringWrapper(text.apply(data.getValue())));
        return column;
    }

    private static void fitTableHeight(TableView<?> table, int itemCount) {
        table.setFixedCellSize(34);
        int visibleRows = Math.max(1, Math.min(itemCount, 8));
        table.setPrefHeight(42 + visibleRows * 34);
    }

    private Button primaryButton(String text) {
        Button button = new Button(text);
        button.getStyleClass().add("button-primary");
        return button;
    }

    private Button secondaryButton(String text) { Button b = new Button(text); b.getStyleClass().add("button-secondary"); return b; }
    private Node infoBanner(String text) { Label label = new Label(text); label.getStyleClass().add("info-banner"); label.setWrapText(true); return label; }

    private static Double parseOptional(String text) {
        if (text == null || text.isBlank()) return null;
        try {
            double value = Double.parseDouble(text.trim().replace(',', '.'));
            return Double.isFinite(value) ? value : null;
        }
        catch (NumberFormatException e) { return null; }
    }

    private static String money(double amount) { return "Rp " + String.format(ID, "%,.0f", amount); }

    private void showReceipt(Pengisian receipt) {
        WebView invoice = new WebView();
        invoice.setContextMenuEnabled(false);
        invoice.getEngine().loadContent(renderInvoiceHtml(receipt));

        Button print = primaryButton("Cetak Invoice");
        Button close = secondaryButton("Tutup");
        HBox toolbar = new HBox(10, print, close);
        toolbar.setPadding(new javafx.geometry.Insets(12, 18, 12, 18));
        toolbar.setStyle("-fx-background-color: #ffffff; -fx-border-color: #e2e7e4; -fx-border-width: 0 0 1 0;");

        BorderPane root = new BorderPane(invoice);
        root.setTop(toolbar);
        Stage stage = new Stage();
        stage.initOwner(mainTabs.getScene().getWindow());
        stage.setTitle("Invoice " + receipt.getInvoiceNumber());
        stage.setScene(new Scene(root, 1050, 740));
        stage.setMinWidth(760);
        stage.setMinHeight(560);
        print.setOnAction(e -> printInvoice(invoice, stage));
        close.setOnAction(e -> stage.close());
        stage.show();
    }

    private String renderInvoiceHtml(Pengisian receipt) {
        try (InputStream stream = getClass().getResourceAsStream("/invoice.html")) {
            if (stream == null) return "<html><body><h2>Template invoice tidak ditemukan.</h2></body></html>";
            String template = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            String paymentRows = "<tr><td>Metode pembayaran</td><td colspan='2'></td><td class='right'>"
                    + html(receipt.getMetode() == null ? "-" : receipt.getMetode().getRincianPembayaran()) + "</td></tr>";
            if (receipt.getMetode() instanceof Tunai tunai) {
                paymentRows += "<tr><td>Uang diterima</td><td colspan='2'></td><td class='right'>"
                        + html(money(tunai.getUangDiterima())) + "</td></tr>"
                        + "<tr><td>Kembalian</td><td colspan='2'></td><td class='right'>"
                        + html(money(Math.max(0, tunai.getUangDiterima() - receipt.getTotal()))) + "</td></tr>";
            }
            String product = receipt.getNamaProdukSnapshot() == null || receipt.getNamaProdukSnapshot().isBlank()
                    ? receipt.getProduct().getNama() : receipt.getNamaProdukSnapshot();
            return template.replace("{{invoiceNumber}}", html(receipt.getInvoiceNumber()))
                    .replace("{{date}}", html(receipt.getTanggal()))
                    .replace("{{attendant}}", html(receipt.getAttendant()))
                    .replace("{{vehicle}}", html(receipt.getNomorKendaraan()))
                    .replace("{{product}}", html(product))
                    .replace("{{quantity}}", html(String.format(ID, "%.2f L", receipt.getQuantity())))
                    .replace("{{unitPrice}}", html(money(receipt.getHargaSnapshot())))
                    .replace("{{total}}", html(money(receipt.getTotal())))
                    .replace("{{paymentRows}}", paymentRows);
        } catch (IOException e) {
            return "<html><body><h2>Gagal membaca template invoice.</h2><p>" + html(e.getMessage()) + "</p></body></html>";
        }
    }

    private void printInvoice(WebView invoice, Window owner) {
        PrinterJob job = PrinterJob.createPrinterJob();
        if (job == null) { showWarning("Tidak ada printer yang tersedia di komputer ini."); return; }
        if (job.showPrintDialog(owner)) {
            invoice.getEngine().print(job);
            if (!job.endJob()) showError("Cetak invoice gagal", "Periksa printer lalu coba lagi.");
        } else {
            job.cancelJob();
        }
    }

    private static String html(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }

    private void showWarning(String message) {
        Alert a = new Alert(Alert.AlertType.WARNING); a.setTitle("Periksa data"); a.setHeaderText(null); a.setContentText(message); a.showAndWait();
    }

    private boolean confirmDelete(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        ButtonType delete = new ButtonType("Hapus", javafx.scene.control.ButtonBar.ButtonData.OK_DONE);
        ButtonType cancel = new ButtonType("Batal", javafx.scene.control.ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(delete, cancel);
        return alert.showAndWait().filter(delete::equals).isPresent();
    }

    private void showError(String title, String message) {
        Alert a = new Alert(Alert.AlertType.ERROR); a.setTitle(title); a.setHeaderText(null); a.setContentText(message); a.showAndWait();
    }

    private String readableDatabaseError(SQLException e) {
        String message = e.getMessage() == null ? "Kesalahan database." : e.getMessage();
        if (message.contains("spbu_") || message.contains("does not exist"))
            return "Struktur database belum lengkap. Restore backup pombensin.sql ke database pombensin sesuai petunjuk di README.md.\n\n" + message;
        if (message.contains("duplicate key") || message.contains("unique constraint"))
            return "Nama produk tersebut sudah digunakan. Pilih nama lain.\n\n" + message;
        return message;
    }
}
