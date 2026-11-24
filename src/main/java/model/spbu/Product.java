package model.spbu;

public class Product {
    private int id;
    private String nama;
    private double harga;
    private boolean aktif;

    public Product(String nama, double harga) {
        this(0, nama, harga, true);
    }

    public Product(int id, String nama, double harga, boolean aktif) {
        this.id = id;
        this.nama = nama;
        this.harga = harga;
        this.aktif = aktif;
    }

    public Product() { this(0, "", 0, true); }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getNama() { return nama; }
    public void setNama(String nama) { this.nama = nama; }
    public double getHarga() { return harga; }
    public void setHarga(double harga) { this.harga = harga; }
    public boolean isAktif() { return aktif; }
    public void setAktif(boolean aktif) { this.aktif = aktif; }

    public double hitungHarga(double quantity) {
        return java.math.BigDecimal.valueOf(harga)
                .multiply(java.math.BigDecimal.valueOf(quantity))
                .setScale(2, java.math.RoundingMode.HALF_UP).doubleValue();
    }

    @Override public String toString() {
        return nama + " — Rp " + String.format(java.util.Locale.forLanguageTag("id-ID"), "%,.0f", harga) + "/L";
    }
}
