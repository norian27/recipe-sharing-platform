package com.koh0118;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/auth")
public class UserResource {
    @Inject
    UserRepository userRepository;

    @POST
    @Path("/login")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response login(User user) {
        User foundUser = userRepository.find("username", user.getUsername()).firstResult();

        if (foundUser != null && foundUser.checkPassword(user.getPasswordHash())) {
            // Login success: For simplicity, returning the user details
            // In practice, you should return a JWT or session token
            return Response.ok(foundUser).build();
        } else {
            // Login failed
            return Response.status(Response.Status.UNAUTHORIZED).entity("Invalid credentials").build();
        }
    }
    @POST
    @Path("/register")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response register(User user) {
        if (user.getUsername() == null || user.getPasswordHash() == null) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Missing information").build();
        }

        // Check if the user already exists
        if (userRepository.find("username", user.getUsername()).firstResult() != null) {
            return Response.status(Response.Status.CONFLICT).entity("User already exists").build();
        }

        user.setPassword(user.getPasswordHash()); // Hash the password
        userRepository.persist(user);
        return Response.status(Response.Status.CREATED).entity("User created successfully with Planner").build();
    }
}
