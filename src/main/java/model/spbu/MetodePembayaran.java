package model.spbu;

public abstract class MetodePembayaran {
    private int pengisianId;
    private double amount;
    public MetodePembayaran(double amount) { this.amount = amount; }
    public MetodePembayaran() { this(0); }
    public int getPengisianId() { return pengisianId; }
    public void setPengisianId(int pengisianId) { this.pengisianId = pengisianId; }
    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }
    public abstract String getRincianPembayaran();
}
