package com.example.pizzamania;

import java.util.ArrayList;
import java.util.List;

public class MenuDBHelper {

    public List<MainMenuActivity.Pizza> getDefaultPizzas(int limit) {
        List<MainMenuActivity.Pizza> pizzas = new ArrayList<>();

        pizzas.add(new MainMenuActivity.Pizza("Margherita", "Small", 750.0, "Classic cheese & tomato", ""));
        pizzas.add(new MainMenuActivity.Pizza("Veggie Delight", "Small", 600.0, "Mixed veggies & cheese", ""));
        pizzas.add(new MainMenuActivity.Pizza("Hawaiian", "Medium", 900.0, "Ham & pineapple", ""));
        pizzas.add(new MainMenuActivity.Pizza("Seafood Special", "Medium", 1500.0, "Shrimp & cheese", ""));
        pizzas.add(new MainMenuActivity.Pizza("Pepperoni", "Large", 1200.0, "Pepperoni & cheese", ""));
        pizzas.add(new MainMenuActivity.Pizza("BBQ Chicken", "Large", 1300.0, "BBQ chicken & cheese", ""));

        // Apply limit safely
        List<MainMenuActivity.Pizza> result = new ArrayList<>();
        if (limit <= 0 || limit >= pizzas.size()) {
            result.addAll(pizzas);
        } else {
            for (int i = 0; i < limit; i++) {
                result.add(pizzas.get(i));
            }
        }

        return result;
    }
}
