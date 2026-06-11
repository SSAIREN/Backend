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
     * users 는 피해자/보호자를 함께 담는 공용 테이블이다.
     * 운영 DB에서 암묵적 네이밍 전략 차이로 column mismatch가 나지 않도록
     * 실제 컬럼명을 명시적으로 모두 고정한다.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
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
        this.name = name;
        this.role = role;
        this.age = age;
        this.phone = phone;
        this.createdAt = OffsetDateTime.now();
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
}
