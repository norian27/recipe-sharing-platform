package com.koh0118;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped

public class PlannerRepository implements PanacheRepository<Planner> {
}
