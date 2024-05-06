package com.koh0118;

public class UserService {

    public boolean authenticateUser(String username, String password) {
        // Here you'd interact with your database or another service to authenticate the user
        return username.equals("user") && password.equals("pass"); // Simplified for illustration
    }

    public User createUser(String username, String password) {
        // Logic to create a new user record
        User user = new User();
        user.setUsername(username);
        user.setPassword(password); // Make sure to hash the password in a real app
        // Save user to the database
        return user;
    }
}
