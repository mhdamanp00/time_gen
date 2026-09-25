package com.timetable.model;

import jakarta.persistence.*;

/** Local administrator login. Passwords are stored as PBKDF2 hashes, never plaintext. */
@Entity
@Table(name = "admin_accounts")
public class AdminAccount {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 60)
    private String username;

    @Column(name = "password_salt", nullable = false, length = 64)
    private String passwordSalt;

    @Column(name = "password_hash", nullable = false, length = 128)
    private String passwordHash;

    public AdminAccount() {}
    public AdminAccount(String username, String passwordSalt, String passwordHash) {
        this.username = username;
        this.passwordSalt = passwordSalt;
        this.passwordHash = passwordHash;
    }
    public Long getId() { return id; }
    public String getUsername() { return username; }
    public String getPasswordSalt() { return passwordSalt; }
    public String getPasswordHash() { return passwordHash; }
}
