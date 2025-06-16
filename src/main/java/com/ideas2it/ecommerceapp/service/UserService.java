package com.ideas2it.ecommerceapp.service;

import com.ideas2it.ecommerceapp.model.User;
import java.util.List;

public interface UserService {
    List<User> getAllUsers();

    User getUserById(Long id);

    User getUserByUsername(String username);

    User registerUser(User user);

    User updateUser(Long id, User userDetails);

    User changePassword(Long id, String currentPassword, String newPassword);

    User addUserRole(Long id, String role);

    User removeUserRole(Long id, String role);
}
