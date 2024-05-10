package com.koh0118;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;
import lombok.Data;
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
    private Planner planner;
    public User() {
        this.planner = new Planner(); // Initialize a new planner when a user is created
        this.planner.setUser(this);  // Set the bi-directional relationship
    }
    public void setPassword(String password) {
        this.passwordHash = BCrypt.hashpw(password, BCrypt.gensalt());
    }

    public boolean checkPassword(String password) {
        return BCrypt.checkpw(password, this.passwordHash);
    }
}
