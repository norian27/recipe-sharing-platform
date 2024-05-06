package com.koh0118;

import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;

@Path("/recipes")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class RecipeResource {

    @POST
    @Transactional
    public Response createRecipe(Recipe recipe) {
        recipe.persist();
        return Response.status(Response.Status.CREATED).entity(recipe).build();
    }

    @GET
    public List<Recipe> getAllRecipes() {
        return Recipe.listAll();
    }

    @GET
    @Path("/{id}")
    public Response getRecipe(@PathParam("id") Long id) {
        Recipe recipe = Recipe.findById(id);
        if (recipe != null) {
            return Response.ok(recipe).build();
        } else {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
    }

    @DELETE
    @Path("/{id}")
    @Transactional
    public Response deleteRecipe(@PathParam("id") Long id) {
        boolean deleted = Recipe.deleteById(id);
        if (deleted) {
            return Response.ok().build();
        } else {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
    }

    @PUT
    @Path("/{id}")
    @Transactional
    public Response updateRecipe(@PathParam("id") Long id, Recipe update) {
        Recipe recipe = Recipe.findById(id);
        if (recipe != null) {
            recipe.setTitle(update.getTitle());
            recipe.setDescription(update.getDescription());
            recipe.setIngredients(update.getIngredients());
            recipe.setInstructions(update.getInstructions());
            recipe.persist();
            return Response.ok(recipe).build();
        } else {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
    }
}
