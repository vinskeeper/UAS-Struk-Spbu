package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import model.spbu.Bensin;
import model.spbu.Product;
import model.spbu.Solar;

public class ProdukDAO {
    public Product getProductForHistory(int id, String name, double price) {
        return new Product(id, name, price, true);
    }

    private static final String PRODUCT_SELECT =
            "SELECT p.id, p.nama, p.harga_per_liter, p.aktif, b.oktan, s.cetane "
            + "FROM public.produk p LEFT JOIN public.bensin b ON b.produk_id=p.id "
            + "LEFT JOIN public.solar s ON s.produk_id=p.id ";

    public List<Product> getProducts(boolean activeOnly) throws SQLException {
        String sql = PRODUCT_SELECT + (activeOnly ? "WHERE p.aktif = TRUE " : "") + "ORDER BY p.nama";
        List<Product> products = new ArrayList<>();
        try (Connection conn = PostgreSQLConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) products.add(mapProduct(rs));
        }
        return products;
    }

    public Product getActiveProduct(int id, Connection conn) throws SQLException {
        String sql = PRODUCT_SELECT + "WHERE p.id = ? AND p.aktif = TRUE FOR SHARE OF p";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new SQLException("Produk tidak ditemukan atau sudah dinonaktifkan.");
                return mapProduct(rs);
            }
        }
    }

    public void save(Product product) throws SQLException {
        write(product, false, "INSERT INTO public.produk (nama, harga_per_liter) VALUES (?, ?) RETURNING id");
    }

    public void update(Product product) throws SQLException {
        write(product, true, "UPDATE public.produk SET nama=?, harga_per_liter=? WHERE id=? RETURNING id");
    }

    private void write(Product product, boolean update, String sql) throws SQLException {
        try (Connection conn = PostgreSQLConnection.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement statement = conn.prepareStatement(sql)) {
                statement.setString(1, product.getNama().trim());
                statement.setBigDecimal(2, java.math.BigDecimal.valueOf(product.getHarga()));
                if (update) statement.setInt(3, product.getId());
                int id;
                try (ResultSet result = statement.executeQuery()) {
                    if (!result.next()) throw new SQLException("Produk tidak ditemukan.");
                    id = result.getInt(1);
                }
                if (product instanceof Bensin bensin) {
                    saveSubtype(conn, id, "solar", "bensin", "oktan", bensin.getOktan());
                } else if (product instanceof Solar solar) {
                    saveSubtype(conn, id, "bensin", "solar", "cetane", solar.getCetane());
                } else {
                    throw new SQLException("Jenis produk harus Bensin atau Solar.");
                }
                conn.commit();
            } catch (SQLException | RuntimeException e) {
                conn.rollback();
                throw e;
            }
        }
    }

    private void saveSubtype(Connection conn, int id, String otherTable, String table, String column, int value)
            throws SQLException {
        try (PreparedStatement delete = conn.prepareStatement("DELETE FROM public." + otherTable + " WHERE produk_id=?");
             PreparedStatement save = conn.prepareStatement("INSERT INTO public." + table + " (produk_id, " + column
                     + ") VALUES (?, ?) ON CONFLICT (produk_id) DO UPDATE SET " + column + "=EXCLUDED." + column)) {
            delete.setInt(1, id);
            delete.executeUpdate();
            save.setInt(1, id);
            save.setInt(2, value);
            save.executeUpdate();
        }
    }

    public void deactivate(int id) throws SQLException {
        try (Connection conn = PostgreSQLConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "UPDATE public.produk SET aktif=FALSE WHERE id=?")) {
            ps.setInt(1, id);
            if (ps.executeUpdate() == 0) throw new SQLException("Produk tidak ditemukan.");
        }
    }

    public void activate(int id) throws SQLException {
        try (Connection conn = PostgreSQLConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "UPDATE public.produk SET aktif=TRUE WHERE id=?")) {
            ps.setInt(1, id);
            if (ps.executeUpdate() == 0) throw new SQLException("Produk tidak ditemukan.");
        }
    }

    public void delete(int id) throws SQLException {
        try (Connection conn = PostgreSQLConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM public.produk WHERE id=?")) {
            ps.setInt(1, id);
            if (ps.executeUpdate() == 0) throw new SQLException("Produk tidak ditemukan.");
        } catch (SQLException e) {
            if ("23503".equals(e.getSQLState())) {
                throw new SQLException("Produk ini sudah dipakai dalam transaksi dan tidak bisa dihapus agar riwayat tetap utuh.", e);
            }
            throw e;
        }
    }

    private static Product mapProduct(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        String name = rs.getString("nama");
        double price = rs.getBigDecimal("harga_per_liter").doubleValue();
        boolean active = rs.getBoolean("aktif");
        Integer oktan = rs.getObject("oktan", Integer.class);
        Integer cetane = rs.getObject("cetane", Integer.class);
        if ((oktan == null) == (cetane == null)) throw new SQLException("Tipe produk tidak valid untuk ID " + id);
        if (oktan != null) return new Bensin(id, name, price, oktan, active);
        return new Solar(id, name, price, cetane, active);
    }
}
