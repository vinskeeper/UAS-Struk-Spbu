package app;

import dao.UserDAO;
import java.io.Console;
import java.sql.SQLException;
import java.util.Arrays;

public final class CreateFirstAdmin {
    private CreateFirstAdmin() {}

    public static void main(String[] args) throws SQLException {
        Console console = System.console();
        if (console == null) {
            throw new IllegalStateException("Jalankan dari terminal interaktif.");
        }

        UserDAO users = new UserDAO();
        if (!users.getAll().isEmpty()) {
            throw new IllegalStateException("Database sudah memiliki akun.");
        }
        String name = console.readLine("Nama: ");
        String username = console.readLine("Username: ");
        String gender = console.readLine("Gender (Male/Female): ");
        char[] password = console.readPassword("Password: ");
        if (password == null) return;
        try {
            users.create(name, gender, null, username, new String(password), "ADMIN");
            console.printf("Akun Admin pertama berhasil dibuat.%n");
        } finally {
            Arrays.fill(password, '\0');
        }
    }
}
