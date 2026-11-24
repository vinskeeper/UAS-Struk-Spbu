package model.spbu;

import java.util.Locale;

public class Pengisian implements CetakStruk {
    private int id;
    private String invoiceNumber;
    private String tanggal;
    private String attendant;
    private int petugasId;
    private String nomorKendaraan;
    private Product product;
    private double quantity;
    private double nominal;
    private double total;
    private MetodePembayaran metode;
    private double hargaSnapshot;
    private String namaProdukSnapshot;

    public Pengisian(String invoiceNumber, String tanggal, String attendant,
            String nomorKendaraan, Product product, double quantity, double nominal,
            MetodePembayaran metode, double hargaSnapshot, String namaProdukSnapshot) {
        this.invoiceNumber = invoiceNumber;
        this.tanggal = tanggal;
        this.attendant = attendant;
        this.nomorKendaraan = nomorKendaraan;
        this.product = product;
        this.quantity = quantity;
        this.nominal = nominal;
        this.metode = metode;
        this.hargaSnapshot = hargaSnapshot;
        this.namaProdukSnapshot = namaProdukSnapshot;
        hitungTotal();
    }

    public Pengisian(String invoiceNumber, String tanggal, String attendant,
            String nomorKendaraan, Product product, double quantity, double nominal,
            MetodePembayaran metode) {
        this(invoiceNumber, tanggal, attendant, nomorKendaraan, product, quantity,
                nominal, metode, product == null ? 0 : product.getHarga(),
                product == null ? "" : product.getNama());
    }

    public Pengisian() { this("", "", "", "", null, 0, 0, null, 0, ""); }

    public double hitungTotal() {
        total = product == null ? 0 : product.hitungHarga(quantity);
        if (metode != null) metode.setAmount(total);
        return total;
    }

    public String formatStruk() {
        Locale id = Locale.forLanguageTag("id-ID");
        String productName = namaProdukSnapshot == null || namaProdukSnapshot.isBlank()
                ? (product == null ? "-" : product.getNama()) : namaProdukSnapshot;
        StringBuilder s = new StringBuilder();
        s.append("                    POM BENSIN\n");
        s.append("                  STRUK PENGISIAN\n");
        s.append("------------------------------------------------\n");
        s.append(String.format(id, "Invoice      : %s%n", value(invoiceNumber)));
        s.append(String.format(id, "Tanggal      : %s%n", tanggal == null || tanggal.isBlank() ? "-" : tanggal));
        s.append(String.format(id, "Petugas      : %s%n", value(attendant)));
        s.append(String.format(id, "Kendaraan    : %s%n", value(nomorKendaraan)));
        s.append("------------------------------------------------\n");
        s.append(String.format(id, "Produk       : %s%n", productName));
        s.append(String.format(id, "Jumlah       : %.2f L%n", quantity));
        s.append(String.format(id, "Harga/Liter  : Rp %,.0f%n", hargaSnapshot));
        s.append(String.format(id, "Total        : Rp %,.0f%n", total));
        if (metode instanceof Tunai tunai) {
            s.append("Pembayaran   : TUNAI\n");
            s.append(String.format(id, "Diterima     : Rp %,.0f%n", tunai.getUangDiterima()));
            s.append(String.format(id, "Kembalian    : Rp %,.0f%n", Math.max(0, tunai.getUangDiterima() - total)));
        } else if (metode != null) {
            s.append(String.format(id, "Pembayaran   : %s%n", metode.getRincianPembayaran()));
        }
        s.append("------------------------------------------------\n");
        s.append("        Terima kasih, hati-hati di jalan!\n");
        return s.toString();
    }

    @Override public void cetak() { System.out.println(formatStruk()); }
    private static String value(String text) { return text == null || text.isBlank() ? "-" : text; }

    public String getInvoiceNumber() { return invoiceNumber; }
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public void setInvoiceNumber(String invoiceNumber) { this.invoiceNumber = invoiceNumber; }
    public String getTanggal() { return tanggal; }
    public void setTanggal(String tanggal) { this.tanggal = tanggal; }
    public String getAttendant() { return attendant; }
    public void setAttendant(String attendant) { this.attendant = attendant; }
    public String getNomorKendaraan() { return nomorKendaraan; }
    public void setNomorKendaraan(String nomorKendaraan) { this.nomorKendaraan = nomorKendaraan; }
    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }
    public double getQuantity() { return quantity; }
    public void setQuantity(double quantity) { this.quantity = quantity; }
    public double getNominal() { return nominal; }
    public void setNominal(double nominal) { this.nominal = nominal; }
    public double getTotal() { return total; }
    public void setTotal(double total) { this.total = total; }
    public MetodePembayaran getMetode() { return metode; }
    public void setMetode(MetodePembayaran metode) { this.metode = metode; }
    public int getPetugasId() { return petugasId; }
    public void setPetugasId(int petugasId) { this.petugasId = petugasId; }
    public double getHargaSnapshot() { return hargaSnapshot; }
    public void setHargaSnapshot(double hargaSnapshot) { this.hargaSnapshot = hargaSnapshot; }
    public String getNamaProdukSnapshot() { return namaProdukSnapshot; }
    public void setNamaProdukSnapshot(String namaProdukSnapshot) { this.namaProdukSnapshot = namaProdukSnapshot; }
}
