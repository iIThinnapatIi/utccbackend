package com.example.backend1.User;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface Loginrepository extends JpaRepository<login, Integer> {

    /**
     * ใช้ค้นหาผู้ใช้จาก username + password
     * อนุญาตให้มีได้หลายแถว → คืนเป็น List
     */
    List<login> findByUsernameAndPassword(String username, String password);
}
