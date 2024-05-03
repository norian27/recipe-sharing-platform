package com.koh0118;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.inject.Inject;
import java.util.List;

@Path("/recipes")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class RecipeResource {

    @Inject
    RecipeService recipeService;

    @POST
    public Response createRecipe(Recipe recipe) {
        recipeService.addRecipe(recipe);
        return Response.status(Response.Status.CREATED).entity(recipe).build();
    }

    @GET
    public List<Recipe> getAllRecipes() {
        return recipeService.getAllRecipes();
    }
}
