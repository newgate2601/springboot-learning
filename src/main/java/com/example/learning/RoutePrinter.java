package com.example.learning;

import jakarta.annotation.PostConstruct;
import org.springframework.cloud.gateway.config.GatewayProperties;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class RoutePrinter {

    private final RouteLocator routeLocator;
    private final GatewayProperties gatewayProperties;

    public RoutePrinter(RouteLocator routeLocator, GatewayProperties gatewayProperties) {
        this.routeLocator = routeLocator;
        this.gatewayProperties = gatewayProperties;
    }

    @PostConstruct
    public void printRoutes() {
        List<Route> routes = routeLocator.getRoutes().collectList().block();
        if (routes != null) {
            for (Route route : routes) {
                System.out.println("Route ID: " + route.getId());
                System.out.println("Predicates: " + route.getPredicate());
                System.out.println("Filters: " + route.getFilters());
                System.out.println("Uri: " + route.getId());
                System.out.println();
            }
        } else {
            System.out.println("No routes found.");
        }
    }
}
