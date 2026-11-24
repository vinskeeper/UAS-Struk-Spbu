package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.mindrot.jbcrypt.BCrypt;

import model.spbu.Admin;
import model.spbu.Petugas;
import model.spbu.User;

public class UserDAO {
    public User findByCredentials(String username, String password) throws SQLException {
        String sql = "SELECT id,nama,username,password,gender,tanggal_lahir,aktif,role FROM public.users "
                + "WHERE username=?";
        try (Connection conn = PostgreSQLConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                String hashedPassword = rs.getString("password");
                if (password == null || !BCrypt.checkpw(password, hashedPassword)) return null;
                return mapUser(rs);
            }
        }
    }

    public List<User> getAll() throws SQLException {
        List<User> rows = new ArrayList<>();
        try (Connection c=PostgreSQLConnection.getConnection(); PreparedStatement p=c.prepareStatement(
                "SELECT id,nama,username,gender,tanggal_lahir,aktif,role FROM public.users ORDER BY nama");
             ResultSet r=p.executeQuery()) {
            while(r.next()) rows.add(mapUser(r));
        }
        return rows;
    }

    public void create(String name,String gender,LocalDate birthday,String username,String password,String role) throws SQLException {
        validate(name,gender,username,password,role,true);
        try(Connection c=PostgreSQLConnection.getConnection();PreparedStatement p=c.prepareStatement(
                "INSERT INTO public.users(nama,username,password,gender,tanggal_lahir,aktif,role) VALUES(?,?,?,?,?,TRUE,?)")) {
            p.setString(1,name.trim());p.setString(2,username.trim());p.setString(3,BCrypt.hashpw(password,BCrypt.gensalt(12)));p.setString(4,gender);p.setObject(5,birthday);p.setString(6,role);p.executeUpdate();
        }
    }

    public void update(int id,String name,String gender,LocalDate birthday,String username,String password,String role) throws SQLException {
        validate(name,gender,username,password,role,false);
        User existing = getById(id);
        if (existing == null) throw new SQLException("Data petugas tidak ditemukan.");
        if ("ADMIN".equals(existing.getRole()) && "PETUGAS".equals(role)
                && existing.isActive() && countActiveAdmins() <= 1) {
            throw new SQLException("Tidak dapat mengubah role admin terakhir yang aktif menjadi petugas.");
        }
        String sql=password==null||password.isBlank()
                ? "UPDATE public.users SET nama=?,gender=?,tanggal_lahir=?,username=?,role=? WHERE id=?"
                : "UPDATE public.users SET nama=?,gender=?,tanggal_lahir=?,username=?,password=?,role=? WHERE id=?";
        try(Connection c=PostgreSQLConnection.getConnection();PreparedStatement p=c.prepareStatement(sql)) {
            p.setString(1,name.trim());p.setString(2,gender);p.setObject(3,birthday);p.setString(4,username.trim());
            if(password==null||password.isBlank()){p.setString(5,role);p.setInt(6,id);}
            else{p.setString(5,BCrypt.hashpw(password,BCrypt.gensalt(12)));p.setString(6,role);p.setInt(7,id);}
            if(p.executeUpdate()==0)throw new SQLException("Data petugas tidak ditemukan.");
        }
    }

    public void setActive(int id,boolean active) throws SQLException {
        User existing = getById(id);
        if (existing == null) throw new SQLException("Akun petugas tidak ditemukan.");
        if (!active && existing.isActive() && "ADMIN".equals(existing.getRole()) && countActiveAdmins() <= 1)
            throw new SQLException("Admin terakhir yang aktif tidak dapat dinonaktifkan.");
        try(Connection c=PostgreSQLConnection.getConnection();PreparedStatement p=c.prepareStatement("UPDATE public.users SET aktif=? WHERE id=?")){
            p.setBoolean(1,active);p.setInt(2,id);if(p.executeUpdate()==0)throw new SQLException("Akun petugas tidak ditemukan.");
        }
    }

    public void delete(int id) throws SQLException {
        User existing = getById(id);
        if (existing == null) throw new SQLException("Akun petugas tidak ditemukan.");
        if ("ADMIN".equals(existing.getRole()) && countAdmins() <= 1)
            throw new SQLException("Akun Admin terakhir tidak dapat dihapus.");
        try (Connection c = PostgreSQLConnection.getConnection();
             PreparedStatement p = c.prepareStatement("DELETE FROM public.users WHERE id=?")) {
            p.setInt(1, id);
            if (p.executeUpdate() == 0) throw new SQLException("Akun petugas tidak ditemukan.");
        } catch (SQLException e) {
            if ("23503".equals(e.getSQLState())) {
                throw new SQLException("Akun ini sudah memiliki riwayat transaksi. Nonaktifkan akun jika petugas tidak boleh login lagi.", e);
            }
            throw e;
        }
    }

    private User getById(int id) throws SQLException {
        try (Connection c=PostgreSQLConnection.getConnection();
             PreparedStatement p=c.prepareStatement("SELECT id,nama,username,gender,tanggal_lahir,aktif,role FROM public.users WHERE id=?")) {
            p.setInt(1,id);
            try(ResultSet r=p.executeQuery()) {
                if(!r.next()) return null;
                return mapUser(r);
            }
        }
    }

    private static User mapUser(ResultSet row) throws SQLException {
        int id = row.getInt("id");
        String name = row.getString("nama");
        String username = row.getString("username");
        String gender = row.getString("gender");
        LocalDate birthday = row.getObject("tanggal_lahir", LocalDate.class);
        boolean active = row.getBoolean("aktif");
        String role = row.getString("role");

        return switch (role) {
            case "ADMIN" -> new Admin(id, name, username, gender, birthday, active);
            case "PETUGAS" -> new Petugas(id, name, username, gender, birthday, active);
            default -> throw new SQLException("Role akun tidak dikenali.");
        };
    }

    private int countActiveAdmins() throws SQLException {
        try (Connection c=PostgreSQLConnection.getConnection();
             PreparedStatement p=c.prepareStatement("SELECT COUNT(*) FROM public.users WHERE role='ADMIN' AND aktif=TRUE");
             ResultSet r=p.executeQuery()) {
            r.next();
            return r.getInt(1);
        }
    }

    private int countAdmins() throws SQLException {
        try (Connection c=PostgreSQLConnection.getConnection();
             PreparedStatement p=c.prepareStatement("SELECT COUNT(*) FROM public.users WHERE role='ADMIN'");
             ResultSet r=p.executeQuery()) {
            r.next();
            return r.getInt(1);
        }
    }

    private static void validate(String n,String g,String u,String p,String role,boolean required)throws SQLException{
        if(n==null||n.isBlank()||n.trim().length()>100)throw new SQLException("Nama petugas wajib diisi (maksimal 100 karakter).");
        if(!"Male".equals(g)&&!"Female".equals(g))throw new SQLException("Pilih gender petugas.");
        if(u==null||u.isBlank()||u.trim().length()>50)throw new SQLException("Username wajib diisi (maksimal 50 karakter).");
        if(!"ADMIN".equals(role)&&!"PETUGAS".equals(role))throw new SQLException("Pilih role Admin atau Petugas.");
        if(required&&(p==null||p.isBlank()))throw new SQLException("Password wajib diisi untuk akun baru.");
        if(p!=null&&p.length()>64)throw new SQLException("Password maksimal 64 karakter.");
    }
}
