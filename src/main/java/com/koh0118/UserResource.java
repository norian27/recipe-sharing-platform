package com.koh0118;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/users")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class UserResource {

    @POST
    @Path("/register")
    public Response registerUser(User user) {
        // Implement logic to save user to the database
        return Response.status(Response.Status.CREATED).entity(user).build();
    }

    @POST
    @Path("/login")
    public Response loginUser(@FormParam("username") String username, @FormParam("password") String password) {
        // Implement logic to authenticate user
        boolean isAuthenticated = checkUserCredentials(username, password);
        if (isAuthenticated) {
            return Response.ok().entity("User authenticated").build();
        } else {
            return Response.status(Response.Status.UNAUTHORIZED).entity("Invalid credentials").build();
        }
    }

    private boolean checkUserCredentials(String username, String password) {
        // Add logic to verify username and password with database records
        return true; // Simplified for example purposes
    }
}
