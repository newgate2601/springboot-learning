package com.example.learning.entity.querylab;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(
        name = "ql_user_account",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_ql_user_account_username", columnNames = "username"),
                @UniqueConstraint(name = "uk_ql_user_account_email", columnNames = "email")
        },
        indexes = {
                @Index(name = "idx_ql_user_account_bu", columnList = "business_unit_id"),
                @Index(name = "idx_ql_user_account_role", columnList = "role_name")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserAccountEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // PK của tài khoản người dùng hệ thống.

    @Column(name = "username", nullable = false, length = 60)
    private String username; // Tên đăng nhập duy nhất để truy cập backend.

    @Column(name = "email", nullable = false, length = 120)
    private String email; // Email công việc dùng cho thông báo và đối soát.

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash; // Mật khẩu đã băm hash, không lưu plain text.

    @Column(name = "role_name", nullable = false, length = 40)
    private String roleName; // Vai trò nghiệp vụ như BUYER, SALES, PLANNER.

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "business_unit_id", nullable = false, foreignKey = @ForeignKey(name = "fk_ql_user_account_bu"))
    private BusinessUnitEntity businessUnit; // Đơn vị mà user được phép thao tác.

    @Builder.Default
    @Column(name = "active", nullable = false)
    private Boolean active = true; // Trạng thái khóa/mở tài khoản.

    @Column(name = "created_at", nullable = false)
    private Instant createdAt; // Mốc tạo tài khoản để audit.

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}

