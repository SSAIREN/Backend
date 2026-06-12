package com.ssairen.backend.domain.user.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;

@Entity
@Table(name = "users")
public class User {

    /*
     * MVP 단계에서는 Flutter가 하드코딩된 userId를 보낼 수 있도록
     * user_id 를 서버 더미 데이터 기준으로 고정 주입 가능하게 설계한다.
     */
    @Id
    @Column(name = "user_id")
    private Long id;

    @Column(name = "name", nullable = false, length = 50)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private UserRole role;

    @Column(name = "age")
    private Integer age;

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "fcm_token", length = 255)
    private String fcmToken;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    protected User() {
    }

    public User(String name, UserRole role, Integer age, String phone) {
        this(null, name, role, age, phone, null, OffsetDateTime.now());
    }

    private User(
            Long id,
            String name,
            UserRole role,
            Integer age,
            String phone,
            String fcmToken,
            OffsetDateTime createdAt
    ) {
        this.id = id;
        this.name = name;
        this.role = role;
        this.age = age;
        this.phone = phone;
        this.fcmToken = fcmToken;
        this.createdAt = createdAt;
    }

    public static User mvpPreset(
            Long userId,
            String name,
            UserRole role,
            Integer age,
            String phone,
            String fcmToken,
            OffsetDateTime createdAt
    ) {
        return new User(userId, name, role, age, phone, fcmToken, createdAt);
    }

    public void updateVictimProfile(Integer age, String phone) {
        this.age = age;
        this.phone = phone;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public UserRole getRole() {
        return role;
    }

    public Integer getAge() {
        return age;
    }

    public String getPhone() {
        return phone;
    }

    public String getFcmToken() {
        return fcmToken;
    }
}
