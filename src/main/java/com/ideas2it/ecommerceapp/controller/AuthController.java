package com.ideas2it.ecommerceapp.controller;

import jakarta.validation.Valid;
import com.ideas2it.ecommerceapp.dto.ApiResponse;
import com.ideas2it.ecommerceapp.dto.AuthRequest;
import com.ideas2it.ecommerceapp.dto.AuthResponse;
import com.ideas2it.ecommerceapp.dto.RegisterRequest;
import com.ideas2it.ecommerceapp.exception.GlobalExceptionHandler;
import com.ideas2it.ecommerceapp.model.User;
import com.ideas2it.ecommerceapp.security.JwtTokenProvider;
import com.ideas2it.ecommerceapp.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtTokenProvider tokenProvider;

    @Autowired
    private UserService userService;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody AuthRequest request) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);
            String jwt = tokenProvider.generateToken((UserDetails) authentication.getPrincipal());

            return ResponseEntity.ok(ApiResponse.success("Login successful", new AuthResponse(jwt)));
        } catch (Exception e) {
            return GlobalExceptionHandler.errorResponseEntity(e.getMessage(), HttpStatus.UNAUTHORIZED);
        }
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<User>> register(@Valid @RequestBody RegisterRequest request) {
        try {
            User user = new User();
            user.setUsername(request.getUsername());
            user.setPassword(request.getPassword());
            user.setEmail(request.getEmail());
            user.setFirstName(request.getFirstName());
            user.setLastName(request.getLastName());

            User registeredUser = userService.registerUser(user);

            return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("User registered successfully", registeredUser));
        } catch (Exception e) {
            return GlobalExceptionHandler.errorResponseEntity(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @PostMapping("/validate")
    public ResponseEntity<ApiResponse<String>> validateToken(@RequestParam String token) {
        try {
            if (tokenProvider.validateToken(token)) {
                String username = tokenProvider.getUsernameFromToken(token);
                return ResponseEntity.ok(ApiResponse.success("Token is valid", username));
            } else {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Token is invalid or expired", null));
            }
        } catch (Exception e) {
            return GlobalExceptionHandler.errorResponseEntity(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refreshToken(@RequestParam String token) {
        try {
            if (tokenProvider.validateToken(token)) {
                String username = tokenProvider.getUsernameFromToken(token);
                String newToken = tokenProvider.refreshToken(token);

                AuthResponse response = new AuthResponse(newToken);
                response.setUsername(username);

                return ResponseEntity.ok(ApiResponse.success("Token refreshed successfully", response));
            } else {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Invalid or expired token", null));
            }
        } catch (Exception e) {
            return GlobalExceptionHandler.errorResponseEntity(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }
}
