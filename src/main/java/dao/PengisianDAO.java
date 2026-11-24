package dao;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import model.spbu.Debit;
import model.spbu.MetodePembayaran;
import model.spbu.Pengisian;
import model.spbu.Product;
import model.spbu.QRIS;
import model.spbu.Tunai;

public class PengisianDAO {
    private final ProdukDAO productDAO = new ProdukDAO();

    public Pengisian save(int petugasId, String attendant, String vehicle, int productId, double liters,
            String paymentType, String paymentDetail, double cashReceived) throws SQLException {
        if (!Double.isFinite(liters) || liters <= 0
                || java.math.BigDecimal.valueOf(liters).stripTrailingZeros().scale() > 3) {
            throw new SQLException("Jumlah liter harus lebih dari nol dan maksimal tiga angka desimal.");
        }
        if (attendant == null || attendant.isBlank() || vehicle == null || vehicle.isBlank()) {
            throw new SQLException("Petugas dan nomor kendaraan harus diisi.");
        }
        try (Connection conn = PostgreSQLConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                Product product = productDAO.getActiveProduct(productId, conn);
                double total = product.hitungHarga(liters);
                MetodePembayaran payment;
                double nominal;
                if ("TUNAI".equals(paymentType)) {
                    if (cashReceived < total) throw new SQLException("Uang tunai kurang dari total transaksi.");
                    payment = new Tunai(total, cashReceived);
                    nominal = cashReceived;
                } else if ("QRIS".equals(paymentType)) {
                    payment = new QRIS(total, paymentDetail);
                    nominal = total;
                } else if ("DEBIT".equals(paymentType)) {
                    payment = new Debit(total, paymentDetail);
                    nominal = total;
                } else {
                    throw new SQLException("Metode pembayaran tidak dikenali.");
                }

                LocalDateTime now = LocalDateTime.now();
                int transactionId = nextTransactionId(conn);
                String invoice = formatInvoice(transactionId);
                String detail = "TUNAI".equals(paymentType) ? "Tunai" : paymentDetail.trim();
                String sql = "INSERT INTO public.pengisian "
                        + "(id, invoice_number, tanggal_waktu, petugas_id, attendant, nomor_kendaraan, produk_id, "
                        + "nama_produk_snapshot, harga_per_liter_snapshot, quantity_liter, total, nominal) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setInt(1, transactionId);
                    ps.setString(2, invoice);
                    ps.setObject(3, now);
                    ps.setInt(4, petugasId);
                    ps.setString(5, attendant.trim());
                    ps.setString(6, vehicle.trim().toUpperCase(java.util.Locale.ROOT));
                    ps.setInt(7, product.getId());
                    ps.setString(8, product.getNama());
                    ps.setBigDecimal(9, BigDecimal.valueOf(product.getHarga()));
                    ps.setBigDecimal(10, BigDecimal.valueOf(liters));
                    ps.setBigDecimal(11, BigDecimal.valueOf(total));
                    ps.setBigDecimal(12, BigDecimal.valueOf(nominal));
                    ps.executeUpdate();
                }
                savePayment(conn, transactionId, paymentType, total, nominal, detail);
                conn.commit();
                Pengisian saved = new Pengisian(invoice, now.format(DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm", java.util.Locale.forLanguageTag("id-ID"))),
                        attendant, vehicle.toUpperCase(java.util.Locale.ROOT), product,
                        liters, nominal, payment, product.getHarga(), product.getNama());
                saved.setId(transactionId);
                saved.setPetugasId(petugasId);
                payment.setPengisianId(transactionId);
                return saved;
            } catch (SQLException | RuntimeException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    private int nextTransactionId(Connection conn) throws SQLException {
        String sql = "SELECT nextval(pg_get_serial_sequence('public.pengisian', 'id'))";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new SQLException("Gagal membuat ID transaksi.");
                return rs.getInt(1);
            }
        }
    }

    private String formatInvoice(int transactionId) {
        return "SPBU-" + String.format("%06d", transactionId);
    }

    public List<Pengisian> getHistory(String filter) throws SQLException {
        String sql = "SELECT t.id, t.invoice_number, t.tanggal_waktu, t.petugas_id, t.attendant, t.nomor_kendaraan, t.produk_id, "
                + "t.nama_produk_snapshot, t.harga_per_liter_snapshot, t.quantity_liter, t.total, t.nominal, "
                + "CASE WHEN u.pengisian_id IS NOT NULL THEN 'TUNAI' "
                + "WHEN q.pengisian_id IS NOT NULL THEN 'QRIS' WHEN d.pengisian_id IS NOT NULL THEN 'DEBIT' "
                + "ELSE '' END AS metode_pembayaran, p.amount, u.uang_diterima, q.provider, d.bank "
                + "FROM public.pengisian t JOIN public.pembayaran p ON p.pengisian_id=t.id "
                + "LEFT JOIN public.tunai u ON u.pengisian_id=p.pengisian_id "
                + "LEFT JOIN public.qris q ON q.pengisian_id=p.pengisian_id "
                + "LEFT JOIN public.debit d ON d.pengisian_id=p.pengisian_id "
                + "WHERE (? = '' OR t.invoice_number ILIKE ? OR t.nomor_kendaraan ILIKE ?) "
                + "ORDER BY t.tanggal_waktu DESC";
        List<Pengisian> rows = new ArrayList<>();
        try (Connection conn = PostgreSQLConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            String q = filter == null ? "" : filter.trim();
            ps.setString(1, q);
            ps.setString(2, "%" + q + "%");
            ps.setString(3, "%" + q + "%");
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) rows.add(mapHistory(rs));
            }
        }
        return rows;
    }

    public void delete(int id) throws SQLException {
        try (Connection conn = PostgreSQLConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM public.pengisian WHERE id=?")) {
            ps.setInt(1, id);
            if (ps.executeUpdate() == 0) throw new SQLException("Transaksi tidak ditemukan.");
        }
    }

    private Pengisian mapHistory(ResultSet rs) throws SQLException {
        String type = rs.getString("metode_pembayaran");
        double amount = rs.getBigDecimal("amount").doubleValue();
        MetodePembayaran payment = switch (type) {
            case "TUNAI" -> new Tunai(amount, rs.getBigDecimal("uang_diterima").doubleValue());
            case "QRIS" -> new QRIS(amount, rs.getString("provider"));
            case "DEBIT" -> new Debit(amount, rs.getString("bank"));
            default -> throw new SQLException("Metode pembayaran pada riwayat tidak dikenal: " + type);
        };
        int productId = rs.getInt("produk_id");
        String name = rs.getString("nama_produk_snapshot");
        double price = rs.getBigDecimal("harga_per_liter_snapshot").doubleValue();
        Product product = productDAO.getProductForHistory(productId, name, price);
        Pengisian item = new Pengisian(rs.getString("invoice_number"),
                rs.getObject("tanggal_waktu", LocalDateTime.class).format(
                        DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm", java.util.Locale.forLanguageTag("id-ID"))),
                rs.getString("attendant"),
                rs.getString("nomor_kendaraan"), product,
                rs.getBigDecimal("quantity_liter").doubleValue(),
                rs.getBigDecimal("nominal").doubleValue(),
                payment, price, name);
        item.setId(rs.getInt("id"));
        item.setPetugasId(rs.getInt("petugas_id"));
        item.setTotal(rs.getBigDecimal("total").doubleValue());
        payment.setPengisianId(item.getId());
        return item;
    }

    private void savePayment(Connection conn, int pengisianId, String type, double total,
            double nominal, String detail) throws SQLException {
        try (PreparedStatement payment = conn.prepareStatement(
                "INSERT INTO public.pembayaran (pengisian_id, amount) VALUES (?, ?)")) {
            payment.setInt(1, pengisianId);
            payment.setBigDecimal(2, BigDecimal.valueOf(total));
            payment.executeUpdate();
        }
        String sql = switch (type) {
            case "TUNAI" -> "INSERT INTO public.tunai (pengisian_id, uang_diterima) VALUES (?, ?)";
            case "QRIS" -> "INSERT INTO public.qris (pengisian_id, provider) VALUES (?, ?)";
            case "DEBIT" -> "INSERT INTO public.debit (pengisian_id, bank) VALUES (?, ?)";
            default -> throw new SQLException("Metode pembayaran tidak dikenali.");
        };
        try (PreparedStatement subtype = conn.prepareStatement(sql)) {
            subtype.setInt(1, pengisianId);
            if ("TUNAI".equals(type)) subtype.setBigDecimal(2, BigDecimal.valueOf(nominal));
            else subtype.setString(2, detail);
            subtype.executeUpdate();
        }
    }
}
