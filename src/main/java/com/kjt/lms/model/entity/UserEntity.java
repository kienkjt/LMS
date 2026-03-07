package com.kjt.lms.model.entity;

import com.kjt.lms.common.base.BaseEntity;
import jakarta.persistence.Entity;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Entity
@AllArgsConstructor
@NoArgsConstructor
public class UserEntity extends BaseEntity {
    private String email;
    private String password;
    private String name;
    private String phoneNumber;
    private String role_id;

}
