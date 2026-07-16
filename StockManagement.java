import java.util.*;

enum OrderType {
    BUY, SELL
}

class Orderr {
    String orderId;
    String symbol;
    OrderType type;
    double price;
    int quantity;
    long timestamp;

    public Orderr(String orderId, String symbol, OrderType type, double price, int quantity, long timestamp) {
        this.orderId = orderId;
        this.symbol = symbol;
        this.type = type;
        this.price = price;
        this.quantity = quantity;
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return orderId + " " + type + " " + symbol + " $" + price + " qty=" + quantity;
    }
}

class Trade {
    String buyOrderId;
    String sellOrderId;
    String symbol;
    double price;
    int quantity;

    public Trade(String buyOrderId, String sellOrderId, String symbol, double price, int quantity) {
        this.buyOrderId = buyOrderId;
        this.sellOrderId = sellOrderId;
        this.symbol = symbol;
        this.price = price;
        this.quantity = quantity;
    }

    @Override
    public String toString() {
        return "Trade{buy=" + buyOrderId +
                ", sell=" + sellOrderId +
                ", symbol=" + symbol +
                ", price=" + price +
                ", qty=" + quantity + "}";
    }
}

interface MatchingStrategy {
    List<Trade> match(Orderr incomingOrder, OrderBook orderBook);
}

class NearestPriceMatchingStrategy implements MatchingStrategy {

    @Override
    public List<Trade> match(Orderr incomingOrder, OrderBook orderBook) {
        List<Trade> trades = new ArrayList<>();

        List<Orderr> oppositeOrders = incomingOrder.type == OrderType.BUY
                ? orderBook.getSellOrders()
                : orderBook.getBuyOrders();

        while (incomingOrder.quantity > 0) {
            Orderr bestOrder = findNearestMatchingOrder(incomingOrder, oppositeOrders);

            if (bestOrder == null) {
                break;
            }

            int matchedQty = Math.min(incomingOrder.quantity, bestOrder.quantity);
            double tradePrice = bestOrder.price;

            String buyId = incomingOrder.type == OrderType.BUY ? incomingOrder.orderId : bestOrder.orderId;
            String sellId = incomingOrder.type == OrderType.SELL ? incomingOrder.orderId : bestOrder.orderId;

            trades.add(new Trade(buyId, sellId, incomingOrder.symbol, tradePrice, matchedQty));

            incomingOrder.quantity -= matchedQty;
            bestOrder.quantity -= matchedQty;

            if (bestOrder.quantity == 0) {
                oppositeOrders.remove(bestOrder);
            }
        }

        return trades;
    }

    private Orderr findNearestMatchingOrder(Orderr incomingOrder, List<Orderr> oppositeOrders) {
        Orderr best = null;

        for (Orderr order : oppositeOrders) {
            if (!isPriceMatch(incomingOrder, order)) {
                continue;
            }

            if (best == null ||
                    Math.abs(incomingOrder.price - order.price) < Math.abs(incomingOrder.price - best.price) ||
                    (Math.abs(incomingOrder.price - order.price) == Math.abs(incomingOrder.price - best.price)
                            && order.timestamp < best.timestamp)) {
                best = order;
            }
        }

        return best;
    }

    private boolean isPriceMatch(Orderr incomingOrder, Orderr oppositeOrder) {
        if (incomingOrder.type == OrderType.BUY) {
            return incomingOrder.price >= oppositeOrder.price;
        } else {
            return incomingOrder.price <= oppositeOrder.price;
        }
    }
}

class OrderBook {
    private final String symbol;
    private final List<Orderr> buyOrders = new ArrayList<>();
    private final List<Orderr> sellOrders = new ArrayList<>();

    public OrderBook(String symbol) {
        this.symbol = symbol;
    }

    public List<Orderr> getBuyOrders() {
        return buyOrders;
    }

    public List<Orderr> getSellOrders() {
        return sellOrders;
    }

    public void addOrder(Orderr order) {
        if (order.type == OrderType.BUY) {
            buyOrders.add(order);
        } else {
            sellOrders.add(order);
        }
    }
}

class StockExchange {
    private final Map<String, OrderBook> orderBooks = new HashMap<>();
    private final MatchingStrategy matchingStrategy;

    public StockExchange(MatchingStrategy matchingStrategy) {
        this.matchingStrategy = matchingStrategy;
    }

    public List<Trade> placeOrder(Orderr order) {
        OrderBook orderBook = orderBooks.computeIfAbsent(
                order.symbol,
                OrderBook::new
        );

        List<Trade> trades = matchingStrategy.match(order, orderBook);

        if (order.quantity > 0) {
            orderBook.addOrder(order);
        }

        return trades;
    }
}

public class StockManagement {
    public static void main(String[] args) {
        StockExchange exchange = new StockExchange(new NearestPriceMatchingStrategy());

        exchange.placeOrder(new Orderr("S1", "UBER", OrderType.SELL, 100.0, 10, 1));
        exchange.placeOrder(new Orderr("S2", "UBER", OrderType.SELL, 98.0, 5, 2));
        exchange.placeOrder(new Orderr("S3", "UBER", OrderType.SELL, 102.0, 7, 3));

        List<Trade> trades = exchange.placeOrder(
                new Orderr("B1", "UBER", OrderType.BUY, 101.0, 12, 4)
        );

        for (Trade trade : trades) {
            System.out.println(trade);
        }
    }
}