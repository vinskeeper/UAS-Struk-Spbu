package model.spbu;

import java.time.LocalDate;

public abstract sealed class User permits Petugas, Admin {
    private final int id;
    private final String name;
    private final String username;
    private final String gender;
    private final LocalDate birthday;
    private final boolean active;

    protected User(int id, String name, String username, String gender, LocalDate birthday, boolean active) {
        this.id = id;
        this.name = name;
        this.username = username;
        this.gender = gender;
        this.birthday = birthday;
        this.active = active;
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public String getUsername() { return username; }
    public String getGender() { return gender; }
    public LocalDate getBirthday() { return birthday; }
    public boolean isActive() { return active; }

    public abstract String getRole();

    protected final void requireActiveAccount() {
        if (!active) {
            throw new IllegalStateException(
                    "Akun Anda telah dinonaktifkan. Hubungi pengelola SPBU untuk mengaktifkannya kembali.");
        }
    }
}
