package com.koh0118;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class UserDTO extends PanacheEntity {
    private String username;
    private String password;
}
