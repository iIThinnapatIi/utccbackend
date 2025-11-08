package com.example.backend1.User;

import jakarta.persistence.*;

@Entity
@Table(name="user")
public class login {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="id")
    private int id;
    @Column(name="username")
    private String username;
    @Column(name = "password")
    private String password;

    public login(){

    }

    public login(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    @Override
    public String toString() {
        return
                "login{"+
                        "id="+id+
                    ", username="+username+
                    ",password="+ password+
                        " }";
    }
}
