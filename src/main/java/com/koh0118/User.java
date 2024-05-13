package com.koh0118;


import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.json.bind.annotation.JsonbTransient;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.mindrot.jbcrypt.BCrypt;

@Entity
@Getter
@Setter
@Table(name = "AppUser")
public class User extends PanacheEntity {
    private String username;
    private String passwordHash;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonbTransient
    private Planner planner;

    public User() {
        this.planner = new Planner();
        this.planner.setUser(this);
    }

    public void setPassword(String password) {
        this.passwordHash = BCrypt.hashpw(password, BCrypt.gensalt());
    }

    public boolean checkPassword(String plainPassword) {
        return BCrypt.checkpw(plainPassword, this.passwordHash);
    }
}
