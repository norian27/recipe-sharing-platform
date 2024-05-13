package com.koh0118;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
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
    @Path("delete/{id}")
    @Transactional
    public Response deleteRecipe(@PathParam("id") Long id) {
        Recipe recipe = Recipe.findById(id);
        if (recipe == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        clearDailyRecipes(recipe);
        removeRecipeFromPlanners(recipe);

        PanacheEntityBase.deleteById(id);
        return Response.ok().build();
    }
    private void clearDailyRecipes(Recipe recipe) {
        List<Planner> planners = Planner.list("from Planner where mondayRecipe = ?1 or tuesdayRecipe = ?1 or wednesdayRecipe = ?1 or thursdayRecipe = ?1 or fridayRecipe = ?1", recipe);
        for (Planner planner : planners) {
            if (recipe.equals(planner.getMondayRecipe())) {
                planner.setMondayRecipe(null);
            }
            if (recipe.equals(planner.getTuesdayRecipe())) {
                planner.setTuesdayRecipe(null);
            }
            if (recipe.equals(planner.getWednesdayRecipe())) {
                planner.setWednesdayRecipe(null);
            }
            if (recipe.equals(planner.getThursdayRecipe())) {
                planner.setThursdayRecipe(null);
            }
            if (recipe.equals(planner.getFridayRecipe())) {
                planner.setFridayRecipe(null);
            }
        }
    }

    private void removeRecipeFromPlanners(Recipe recipe) {
        List<Planner> plannersWithGeneralRecipe = Planner.find("select p from Planner p join p.recipes r where r.id = ?1", recipe.getId()).list();
        for (Planner planner : plannersWithGeneralRecipe) {
            planner.getRecipes().removeIf(r -> r.getId().equals(recipe.getId()));
        }
    }
    @PUT
    @Path("update/{id}")
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
