package model.spbu;

public class Solar extends Product {
    private int cetane;
    public Solar(String nama, double harga, int cetane) { super(nama, harga); this.cetane = cetane; }
    public Solar(int id, String nama, double harga, int cetane, boolean aktif) {
        super(id, nama, harga, aktif); this.cetane = cetane;
    }
    public Solar() { this("", 0, 0); }
    public int getCetane() { return cetane; }
    public void setCetane(int cetane) { this.cetane = cetane; }
}
