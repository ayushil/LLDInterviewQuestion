/*
Search restaurants - done
Order food - add dish to cart, pay, success - order placed
*/

import java.util.ArrayList;
import java.util.List;

public class FoodDelivery {
    public static void main(String[] args) {
        Userr user = new Userr(1, "Sachin");
        Swiggy swiggy = Swiggy.getInstance(user);
        Menu menu = new Menu();
        Dish dish1 = new Dish(1, "PaneerTikka", 400);
        Dish dish2 = new Dish(2, "VadaPav", 120);
        Dish dish3 = new Dish(3, "Chole", 180);
        menu.addDish(dish1);
        menu.addDish(dish2);
        menu.addDish(dish3);

        Restaurant restaurant1 = new Restaurant(1, "AyushiDhaba", 50, 50, menu);

        menu.addDish(new Dish(4, "Biryani", 280));
        Restaurant restaurant2 = new Restaurant(2, "MaaKaDhaba", 30, 50, menu);

        swiggy.restaurants.add(restaurant1);
        swiggy.restaurants.add(restaurant2);

        SearchStrategy nameSearchStrategy = new NameSearchStrategy();
        SearchStrategy locationSearchStrategy = new LocationSearchStrategy();

        SearchQuery searchQuery = new SearchQuery("AyushiDhaba", new Location(0, 0));

        List<Restaurant> restaurantList = swiggy.searchRestaurant(nameSearchStrategy, searchQuery);
        List<Restaurant> restaurantList2 = swiggy.searchRestaurant(locationSearchStrategy, searchQuery);

        System.out.println("Search results: " + restaurantList.size() + " " + restaurantList2.size());

        // simulating order
        List<OrderItem> orderItemList = new ArrayList<>();
        Restaurant selectedRestaurant = restaurantList.get(0);
//        OrderItem orderItem1 = new OrderItem(restaurantList.searchDishByName("Chole*"), 2);
        Dish selectedDish1 = selectedRestaurant.findDish("PaneerTikka");
        OrderItem orderItem1 = new OrderItem(selectedDish1, 2);
        // add to cart
        orderItemList.add(orderItem1);

        Dish selectedDish2 = selectedRestaurant.findDish("Chole");
        OrderItem orderItem2 = new OrderItem(selectedDish2, 2);
        // add to cart
        orderItemList.add(orderItem2);

        // placing order
        Order order = swiggy.placeOrder(orderItemList);
        System.out.println("User order list " + user.orders.size());
    }
}

class Swiggy {
    public static Swiggy foodDelivery;
    public List<Restaurant> restaurants;
    public Userr user;

    public static Swiggy getInstance(Userr user) {
        if (foodDelivery == null) {
            foodDelivery = new Swiggy(user);
        }
        return foodDelivery;
    }

    private Swiggy(Userr user) {
        this.restaurants = new ArrayList<>();
        this.user = user;
    }

    public List<Restaurant> searchRestaurant(SearchStrategy searchStrategy, SearchQuery searchQuery) {
        return searchStrategy.searchRestaurant(searchQuery, restaurants);
    }

    public Order placeOrder(List<OrderItem> orderItemList) {
        Order order = new Order(orderItemList, user);
        System.out.println("Order placed successfully for amount: " + order.amount);

        // payment

        return order;
    }
}

class Restaurant {
    int restaurantId;
    String restaurantName;
    Location location;
    Menu menu;
    List<Order> orders;

    public Restaurant(int restaurantId, String restaurantName, int lat, int lng, Menu menu) {
        this.restaurantId = restaurantId;
        this.restaurantName = restaurantName;
        this.location = new Location(lat, lng);
        this.menu = menu;
        orders = new ArrayList<>();
    }

    public Dish findDish(String dishName) {
        List<Dish> dishes = menu.dishes;
        for (Dish dish : dishes) {
            if (dish.dishName.equals(dishName)) {
                return dish;
            }
        }
        return null;
    }
}

class Menu {
    List<Dish> dishes;

    public Menu() {
        dishes = new ArrayList<>();
    }

    public void addDish(Dish dish) {
        dishes.add(dish);
    }
}

class Dish {
    int dishId;
    String dishName;
    int price;
    // ingredients

    public Dish(int dishId, String dishName, int price) {
        this.dishId = dishId;
        this.dishName = dishName;
        this.price = price;
    }
}

class OrderItem {
    Dish dish;
    int quantity;

    public OrderItem(Dish dish, int quantity) {
        this.dish = dish;
        this.quantity = quantity;
    }
}

class Order {
    List<OrderItem> orderedDishes;
    int amount;
    Userr user;

    public Order(List<OrderItem> orderedDishes, Userr user) {
        this.orderedDishes = orderedDishes;
        this.user = user;
        this.amount = 0;
        for (int i = 0; i < orderedDishes.size(); i++) {
            amount += orderedDishes.get(i).dish.price * orderedDishes.get(i).quantity;
        }
    }
}

class Userr {
    int userId;
    String userName;
    List<Order> orders;

    public Userr(int userId, String userName) {
        this.userId = userId;
        this.userName = userName;
        this.orders = new ArrayList<>();
    }
}

class Location {
    int lat;
    int lng;

    public Location(int lat, int lng) {
        this.lat = lat;
        this.lng = lng;
    }
}

class SearchQuery {
    String restaurantName;
    Location location;

    public SearchQuery(String restaurantName, Location location) {
        this.restaurantName = restaurantName;
        this.location = location;
    }
}

interface SearchStrategy {
    public List<Restaurant> searchRestaurant(SearchQuery searchQuery, List<Restaurant> restaurants);
}

class NameSearchStrategy implements SearchStrategy {
    @Override
    public List<Restaurant> searchRestaurant(SearchQuery searchQuery, List<Restaurant> restaurants) {
        List<Restaurant> res = new ArrayList<>();
        for (Restaurant restaurant : restaurants) {
            if (restaurant.restaurantName.equals(searchQuery.restaurantName)) {
                res.add(restaurant);
            }
        }
        return res;
    }
}

class LocationSearchStrategy implements SearchStrategy {
    int delta = 10;
    @Override
    public List<Restaurant> searchRestaurant(SearchQuery searchQuery, List<Restaurant> restaurants) {
        List<Restaurant> res = new ArrayList<>();
        for (Restaurant restaurant : restaurants) {
            if (restaurant.location.lat <= (searchQuery.location.lat + delta) &&
                    restaurant.location.lat >= (searchQuery.location.lat - delta) &&
                    restaurant.location.lng <= (searchQuery.location.lng + delta) &&
                    restaurant.location.lng >= (searchQuery.location.lng - delta)) {
                res.add(restaurant);
            }
        }
        return res;
    }
}