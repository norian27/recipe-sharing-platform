package com.koh0118;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
@Table(name = "AppUser")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    private String name;
    private String email;

}
