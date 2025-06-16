package com.ideas2it.ecommerceapp.controller;

import com.ideas2it.ecommerceapp.dto.ApiResponse;
import com.ideas2it.ecommerceapp.dto.AuthRequest;
import com.ideas2it.ecommerceapp.dto.AuthResponse;
import com.ideas2it.ecommerceapp.dto.RegisterRequest;
import com.ideas2it.ecommerceapp.model.User;
import com.ideas2it.ecommerceapp.security.JwtTokenProvider;
import com.ideas2it.ecommerceapp.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AuthControllerTest {

    @InjectMocks
    private AuthController authController;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtTokenProvider tokenProvider;

    @Mock
    private UserService userService;

    @Mock
    private Authentication authentication;

    private User testUser;
    private RegisterRequest registerRequest;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        // Create test user
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setPassword("encodedPassword");
        Set<String> roles = new HashSet<>();
        roles.add("USER");
        testUser.setRoles(roles);

        // Create register request
        registerRequest = new RegisterRequest();
        registerRequest.setUsername("newuser");
        registerRequest.setEmail("new@example.com");
        registerRequest.setPassword("password123");
        registerRequest.setFirstName("New");
        registerRequest.setLastName("User");
    }

    @Test
    void testLogin_ValidCredentials_ReturnsToken() {
        // Arrange
        AuthRequest authRequest = new AuthRequest();
        authRequest.setUsername("testuser");
        authRequest.setPassword("password123");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
            .thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(new org.springframework.security.core.userdetails.User(
            "testuser", "password123", new ArrayList<>()));
        when(tokenProvider.generateToken(any(UserDetails.class))).thenReturn("valid.jwt.token");

        // Act
        ResponseEntity<ApiResponse<AuthResponse>> response = authController.login(authRequest);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().isSuccess());
        assertEquals("Login successful", response.getBody().getMessage());
        assertEquals("valid.jwt.token", response.getBody().getData().getToken());
    }

    @Test
    void testLogin_InvalidCredentials_ReturnsUnauthorized() {
        // Arrange
        AuthRequest authRequest = new AuthRequest();
        authRequest.setUsername("testuser");
        authRequest.setPassword("wrongpassword");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
            .thenThrow(new BadCredentialsException("Invalid username or password"));

        // Act
        ResponseEntity<ApiResponse<AuthResponse>> response = authController.login(authRequest);

        // Assert
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertFalse(response.getBody().isSuccess());
        assertEquals("Invalid username or password", response.getBody().getMessage());
        assertNull(response.getBody().getData());
    }

    @Test
    void testRegister_ValidRequest_CreatesUserAndReturnsSuccess() {
        // Arrange
        User newUser = new User();
        newUser.setId(2L);
        newUser.setUsername("newuser");
        newUser.setEmail("new@example.com");

        when(userService.registerUser(any(User.class))).thenReturn(newUser);

        // Act
        ResponseEntity<ApiResponse<User>> response = authController.register(registerRequest);

        // Assert
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertTrue(response.getBody().isSuccess());
        assertEquals("User registered successfully", response.getBody().getMessage());
        assertEquals("newuser", response.getBody().getData().getUsername());
        verify(userService).registerUser(any(User.class));
    }

    @Test
    void testRegister_DuplicateUsername_ReturnsBadRequest() {
        // Arrange
        when(userService.registerUser(any(User.class)))
            .thenThrow(new IllegalArgumentException("Username already exists"));

        // Act
        ResponseEntity<ApiResponse<User>> response = authController.register(registerRequest);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertFalse(response.getBody().isSuccess());
        assertEquals("Username already exists", response.getBody().getMessage());
        assertNull(response.getBody().getData());
    }

    @Test
    void testValidateToken_ValidToken_ReturnsSuccess() {
        // Arrange
        String token = "valid.jwt.token";
        when(tokenProvider.validateToken(token)).thenReturn(true);
        when(tokenProvider.getUsernameFromToken(token)).thenReturn("testuser");

        // Act
        ResponseEntity<ApiResponse<String>> response = authController.validateToken(token);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().isSuccess());
        assertEquals("Token is valid", response.getBody().getMessage());
        assertEquals("testuser", response.getBody().getData());
    }

    @Test
    void testValidateToken_InvalidToken_ReturnsBadRequest() {
        // Arrange
        String token = "invalid.jwt.token";
        when(tokenProvider.validateToken(token)).thenReturn(false);

        // Act
        ResponseEntity<ApiResponse<String>> response = authController.validateToken(token);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertFalse(response.getBody().isSuccess());
        assertEquals("Token is invalid or expired", response.getBody().getMessage());
        assertNull(response.getBody().getData());
    }

    @Test
    void testRefreshToken_ValidToken_ReturnsNewToken() {
        // Arrange
        String token = "valid.jwt.token";
        when(tokenProvider.validateToken(token)).thenReturn(true);
        when(tokenProvider.getUsernameFromToken(token)).thenReturn("testuser");
        when(tokenProvider.refreshToken(token)).thenReturn("new.jwt.token");

        // Act
        ResponseEntity<ApiResponse<AuthResponse>> response = authController.refreshToken(token);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().isSuccess());
        assertEquals("Token refreshed successfully", response.getBody().getMessage());
        assertEquals("new.jwt.token", response.getBody().getData().getToken());
        assertEquals("testuser", response.getBody().getData().getUsername());
    }

    @Test
    void testRefreshToken_InvalidToken_ReturnsBadRequest() {
        // Arrange
        String token = "invalid.jwt.token";
        when(tokenProvider.validateToken(token)).thenReturn(false);

        // Act
        ResponseEntity<ApiResponse<AuthResponse>> response = authController.refreshToken(token);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertFalse(response.getBody().isSuccess());
        assertEquals("Invalid or expired token", response.getBody().getMessage());
        assertNull(response.getBody().getData());
    }
}
