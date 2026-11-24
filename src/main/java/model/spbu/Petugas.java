package model.spbu;

import java.time.LocalDate;

public final class Petugas extends User implements Login {
    public Petugas(int id, String name, String username, String gender, LocalDate birthday, boolean active) {
        super(id, name, username, gender, birthday, active);
    }

    @Override
    public String getRole() {
        return "PETUGAS";
    }

    @Override
    public void signIn() {
        requireActiveAccount();
    }
}
