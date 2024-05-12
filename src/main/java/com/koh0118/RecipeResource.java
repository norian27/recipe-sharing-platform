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
    @Path("delete/{id}")
    @Transactional
    public Response deleteRecipe(@PathParam("id") Long id) {
        Recipe recipe = Recipe.findById(id);
        if (recipe == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        List<Planner> planners = Planner.list("mondayRecipe", recipe);
        planners.addAll(Planner.list("tuesdayRecipe", recipe));
        planners.addAll(Planner.list("wednesdayRecipe", recipe));
        planners.addAll(Planner.list("thursdayRecipe", recipe));
        planners.addAll(Planner.list("fridayRecipe", recipe));
        for (Planner planner : planners) {
            if (planner.getMondayRecipe() != null && planner.getMondayRecipe().getId().equals(id)) {
                planner.setMondayRecipe(null);
            }
            if (planner.getTuesdayRecipe() != null && planner.getTuesdayRecipe().getId().equals(id)) {
                planner.setTuesdayRecipe(null);
            }
            if (planner.getWednesdayRecipe() != null && planner.getWednesdayRecipe().getId().equals(id)) {
                planner.setWednesdayRecipe(null);
            }
            if (planner.getThursdayRecipe() != null && planner.getThursdayRecipe().getId().equals(id)) {
                planner.setThursdayRecipe(null);
            }
            if (planner.getFridayRecipe() != null && planner.getFridayRecipe().getId().equals(id)) {
                planner.setFridayRecipe(null);
            }
        }

        List<Planner> plannersWithGeneralRecipe = Planner.find("select p from Planner p join p.recipes r where r.id = ?1", id).list();
        for (Planner planner : plannersWithGeneralRecipe) {
            planner.getRecipes().removeIf(r -> r.getId().equals(id));
        }

        Recipe.deleteById(id);

        return Response.ok().build();
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
