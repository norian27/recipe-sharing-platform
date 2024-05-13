package com.koh0118;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.transaction.Transactional;



@Path("/auth")
public class UserResource {
    private final UserRepository userRepository;

    @Inject
    public UserResource(UserRepository userRepository) {
        this.userRepository = userRepository;
    }
    @POST
    @Path("/login")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response login(UserDTO userDTO) {  // Using DTO to manage data transfer
        User foundUser = userRepository.find("username", userDTO.getUsername()).firstResult();

        if (foundUser == null) {
            return Response.status(Response.Status.UNAUTHORIZED).entity("User not found").build();
        } else if (!foundUser.checkPassword(userDTO.getPassword())) {  // Verify with the plain password from the DTO
            return Response.status(Response.Status.UNAUTHORIZED).entity("Invalid password").build();
        } else {
            return Response.ok(foundUser).build();
        }
    }

    @POST
    @Path("/register")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Transactional
    public Response register(UserDTO userDTO) {  // Using DTO to manage data transfer
        if (userDTO.getUsername() == null || userDTO.getPassword() == null) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Missing information").build();
        }

        if (userRepository.find("username", userDTO.getUsername()).firstResult() != null) {
            return Response.status(Response.Status.CONFLICT).entity("User already exists").build();
        }

        User newUser = new User();
        newUser.setUsername(userDTO.getUsername());
        newUser.setPassword(userDTO.getPassword());  // Set password hashes it
        userRepository.persist(newUser);
        return Response.status(Response.Status.CREATED).entity("User created successfully").build();
    }

    @GET
    @Path("/getAllUsers")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAllUsers() {
        return Response.ok(userRepository.listAll()).build();
    }
}
