package com.evogroup.minicrm.service;

import com.evogroup.minicrm.dto.AuthResponse;
import com.evogroup.minicrm.dto.LoginRequest;
import com.evogroup.minicrm.dto.RegisterRequest;

public interface AuthService {
    AuthResponse register(RegisterRequest request);
    AuthResponse login(LoginRequest request);
}
