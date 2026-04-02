// UpdateProfileRequest.java
package com.smartexpense.server.controller;

import lombok.Data;

@Data
public class UpdateProfileRequest {
    private String phone;
    private String password;
    private String name;
}