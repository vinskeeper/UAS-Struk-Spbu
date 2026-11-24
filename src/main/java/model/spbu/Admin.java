package model.spbu;

import java.time.LocalDate;

public final class Admin extends User implements Login {
    public Admin(int id, String name, String username, String gender, LocalDate birthday, boolean active) {
        super(id, name, username, gender, birthday, active);
    }

    @Override
    public String getRole() {
        return "ADMIN";
    }

    @Override
    public void signIn() {
        requireActiveAccount();
    }
}
