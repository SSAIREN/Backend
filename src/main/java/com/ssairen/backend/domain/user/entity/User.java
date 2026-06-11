package com.ssairen.backend.domain.user.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;

@Entity
@Table(name = "users")
public class User {

    /*
     * users는 피해자와 보호자를 함께 담는 공용 테이블이다.
     * 실제 역할 차이는 role enum과 pairings 같은 연결 테이블에서 구분한다.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long id;

    @Column(nullable = false, length = 50)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserRole role;

    private Integer age;

    @Column(length = 20)
    private String phone;

    @Column(name = "fcm_token", length = 255)
    private String fcmToken;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    protected User() {
    }

    public User(String name, UserRole role, Integer age, String phone) {
        // 생성 시점에는 이름/역할/기본 프로필만 있어도 row를 만들 수 있게 둔다.
        this.name = name;
        this.role = role;
        this.age = age;
        this.phone = phone;
        this.createdAt = OffsetDateTime.now();
    }

    public void updateVictimProfile(Integer age, String phone) {
        // 같은 피해자가 다시 들어오면 최신 프로필 정보로 보정한다.
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
}
