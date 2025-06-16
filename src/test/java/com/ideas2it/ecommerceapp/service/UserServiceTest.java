package com.ideas2it.ecommerceapp.service;

import com.ideas2it.ecommerceapp.model.User;
import com.ideas2it.ecommerceapp.repository.UserRepository;
import com.ideas2it.ecommerceapp.service.impl.UserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UserServiceTest {

    @InjectMocks
    private UserServiceImpl userService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private User testUser;

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
    }

    @Test
    void testGetAllUsers_ReturnsAllUsers() {
        // Arrange
        List<User> users = Arrays.asList(testUser, new User());
        when(userRepository.findAll()).thenReturn(users);

        // Act
        List<User> result = userService.getAllUsers();

        // Assert
        assertEquals(2, result.size());
        verify(userRepository).findAll();
    }

    @Test
    void testGetUserById_ExistingUser_ReturnsUser() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        // Act
        User result = userService.getUserById(1L);

        // Assert
        assertNotNull(result);
        assertEquals("testuser", result.getUsername());
        verify(userRepository).findById(1L);
    }

    @Test
    void testGetUserById_NonExistingUser_ThrowsException() {
        // Arrange
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(NoSuchElementException.class, () -> {
            userService.getUserById(99L);
        });
        verify(userRepository).findById(99L);
    }

    @Test
    void testGetUserByUsername_ExistingUser_ReturnsUser() {
        // Arrange
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        // Act
        User result = userService.getUserByUsername("testuser");

        // Assert
        assertNotNull(result);
        assertEquals(1L, result.getId());
        verify(userRepository).findByUsername("testuser");
    }

    @Test
    void testGetUserByUsername_NonExistingUser_ThrowsException() {
        // Arrange
        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(UsernameNotFoundException.class, () -> {
            userService.getUserByUsername("nonexistent");
        });
        verify(userRepository).findByUsername("nonexistent");
    }

    @Test
    void testRegisterUser_EncodesPasswordAndSavesUser() {
        // Arrange
        User newUser = new User();
        newUser.setUsername("newuser");
        newUser.setEmail("new@example.com");
        newUser.setPassword("plainPassword");

        when(passwordEncoder.encode("plainPassword")).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User savedUser = invocation.getArgument(0);
            savedUser.setId(2L);
            savedUser.getRoles().add("USER"); // Add default USER role
            return savedUser;
        });

        // Act
        User result = userService.registerUser(newUser);

        // Assert
        assertNotNull(result);
        assertEquals("encodedPassword", result.getPassword());
        assertTrue(result.getRoles().contains("USER")); // Should add default USER role
        verify(passwordEncoder).encode("plainPassword");
        verify(userRepository).save(newUser);
    }

    @Test
    void testUpdateUser_ExistingUser_UpdatesUserDetails() {
        // Arrange
        User updatedDetails = new User();
        updatedDetails.setUsername("updateduser");
        updatedDetails.setEmail("updated@example.com");
        updatedDetails.setFirstName("Updated");
        updatedDetails.setLastName("User");

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User userToUpdate = invocation.getArgument(0);
            userToUpdate.setUsername(updatedDetails.getUsername());
            userToUpdate.setEmail(updatedDetails.getEmail());
            userToUpdate.setFirstName(updatedDetails.getFirstName());
            userToUpdate.setLastName(updatedDetails.getLastName());
            return userToUpdate;
        });

        // Act
        User result = userService.updateUser(1L, updatedDetails);

        // Assert
        assertEquals("updateduser", result.getUsername());
        assertEquals("updated@example.com", result.getEmail());
        assertEquals("Updated", result.getFirstName());
        assertEquals("User", result.getLastName());
        verify(userRepository).save(testUser);
    }

    @Test
    void testChangePassword_CorrectCurrentPassword_UpdatesPassword() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("currentPassword", "encodedPassword")).thenReturn(true);
        when(passwordEncoder.encode("newPassword")).thenReturn("newEncodedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        User result = userService.changePassword(1L, "currentPassword", "newPassword");

        // Assert
        assertEquals("newEncodedPassword", result.getPassword());
        verify(passwordEncoder).matches("currentPassword", "encodedPassword");
        verify(passwordEncoder).encode("newPassword");
        verify(userRepository).save(testUser);
    }

    @Test
    void testChangePassword_IncorrectCurrentPassword_ThrowsException() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("wrongPassword", "encodedPassword")).thenReturn(false);

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            userService.changePassword(1L, "wrongPassword", "newPassword");
        });
        verify(passwordEncoder).matches("wrongPassword", "encodedPassword");
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void testAddUserRole_AddsRoleToUser() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        User result = userService.addUserRole(1L, "ADMIN");

        // Assert
        assertTrue(result.getRoles().contains("ADMIN"));
        assertEquals(2, result.getRoles().size());
        verify(userRepository).save(testUser);
    }

    @Test
    void testAddUserRole_RoleAlreadyExists_NoChange() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        User result = userService.addUserRole(1L, "USER");

        // Assert
        assertTrue(result.getRoles().contains("USER"));
        assertEquals(1, result.getRoles().size());
        verify(userRepository).save(testUser);
    }

    @Test
    void testRemoveUserRole_RemovesRoleFromUser() {
        // Arrange
        testUser.getRoles().add("ADMIN"); // Add ADMIN role to test removal
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        User result = userService.removeUserRole(1L, "ADMIN");

        // Assert
        assertFalse(result.getRoles().contains("ADMIN"));
        assertEquals(1, result.getRoles().size());
        verify(userRepository).save(testUser);
    }

    @Test
    void testRemoveUserRole_RoleDoesNotExist_NoChange() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        User result = userService.removeUserRole(1L, "ADMIN");

        // Assert
        assertFalse(result.getRoles().contains("ADMIN"));
        assertEquals(1, result.getRoles().size());
        verify(userRepository).save(testUser);
    }
}
