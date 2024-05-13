package com.koh0118;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.HashMap;
import java.util.Map;

@Path("/planners")
public class PlannerResource {
    private final UserRepository userRepository;
    private static final String USERNAME = "username";


    @Inject
    public PlannerResource(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @POST
    @Path("/{username}/addRecipe/{recipeId}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Transactional
    public Response addRecipeToPlanner(@PathParam("recipeId") String recipeId, @PathParam(USERNAME) String username) {
        User user = userRepository.find(USERNAME, username).firstResult();
        Recipe recipe = PanacheEntityBase.findById(recipeId);

        if (user == null || recipe == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        user.getPlanner().getRecipes().add(recipe);
        userRepository.persist(user);
        return Response.ok().build();
    }



    @GET
    @Path("/getAllPlanners")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAllPlanners() {
        return Response.ok(userRepository.listAll()).build();
    }

    @GET
    @Path("/getPlannerRecipes/{username}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getPlannerRecipes(@PathParam(USERNAME) String username) {
        User user = userRepository.find(USERNAME, username).firstResult();
        if (user == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.ok(user.getPlanner().getRecipes()).build();
    }

    @DELETE
    @Path("/{username}/deleteRecipe/{recipeId}")
    @Produces(MediaType.APPLICATION_JSON)
    @Transactional
    public Response deleteRecipeFromPlanner(@PathParam("recipeId") String recipeId, @PathParam(USERNAME) String username) {
        User user = userRepository.find(USERNAME, username).firstResult();
        Recipe recipe = PanacheEntityBase.findById(recipeId);

        if (user == null || recipe == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        user.getPlanner().getRecipes().remove(recipe);
        userRepository.persist(user);
        return Response.ok().build();
    }

    @POST
    @Path("/{username}/setRecipeForDay/{recipeId}/{day}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Transactional
    public Response setRecipeForDay(@PathParam(USERNAME) String username,
                                    @PathParam("recipeId") Long recipeId,
                                    @PathParam("day") String day) {
        User user = userRepository.find(USERNAME, username).firstResult();
        Recipe recipe = PanacheEntityBase.findById(recipeId);
        if (user == null || recipe == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        Planner planner = user.getPlanner();
        switch(day.toLowerCase()) {
            case "monday":
                planner.setMondayRecipe(recipe);
                break;
            case "tuesday":
                planner.setTuesdayRecipe(recipe);
                break;
            case "wednesday":
                planner.setWednesdayRecipe(recipe);
                break;
            case "thursday":
                planner.setThursdayRecipe(recipe);
                break;
            case "friday":
                planner.setFridayRecipe(recipe);
                break;
            default:
                return Response.status(Response.Status.BAD_REQUEST).entity("Invalid day").build();
        }

        if (!planner.getRecipes().contains(recipe)) {
            planner.getRecipes().add(recipe);
        }
        userRepository.persist(user);
        return Response.ok().build();
    }

    @GET
    @Path("/getRecipesForWeek/{username}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getRecipesForWeek(@PathParam(USERNAME) String username) {
        User user = userRepository.find(USERNAME, username).firstResult();
        if (user == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        Planner planner = user.getPlanner();
        if (planner == null) {
            return Response.status(Response.Status.NOT_FOUND).entity("Planner not found for user").build();
        }

        Map<String, Recipe> weeklyRecipes = new HashMap<>();
        weeklyRecipes.put("Monday", planner.getMondayRecipe());
        weeklyRecipes.put("Tuesday", planner.getTuesdayRecipe());
        weeklyRecipes.put("Wednesday", planner.getWednesdayRecipe());
        weeklyRecipes.put("Thursday", planner.getThursdayRecipe());
        weeklyRecipes.put("Friday", planner.getFridayRecipe());

        return Response.ok(weeklyRecipes).build();
    }

}
