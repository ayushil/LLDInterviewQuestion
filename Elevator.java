import java.util.*;

enum Direction {
    UP,
    DOWN,
    IDLE
}

public class Elevator {
    private int currentFloor;
    private Direction direction;
    private Set<Request> requests;

    public Elevator() {
        this.currentFloor = 0;
        this.direction = Direction.IDLE;
        this.requests = new HashSet<>();
    }

    public static void main(String[] args) {

        ElevatorController controller = new ElevatorController();

        // User at floor 2 wants to go up
        controller.requestElevator(2, RequestType.PICKUP_UP);

        // User at floor 8 wants to go down
        controller.requestElevator(8, RequestType.PICKUP_DOWN);

        // Simulate elevator movement
        for (int step = 1; step <= 15; step++) {

            System.out.println("Step: " + step);

            controller.step();

            System.out.println("---------------------");
        }
    }
    public boolean addRequest(Request request) {
        if (request.getFloor() < 0 || request.getFloor() > 9) {
            return false;
        }
        if (request.getFloor() == currentFloor) {
            return true;
        }
        if (requests.contains(request)) {
            return false;
        }
        return requests.add(request);
    }

    public void step() {
        if (requests.isEmpty()) {
            direction = Direction.IDLE;
            return;
        }

        if (direction == Direction.IDLE) {
            // Find nearest request to establish initial direction (deterministic)
            Request nearest = null;
            int minDistance = Integer.MAX_VALUE;

            for (Request req : requests) {
                int distance = Math.abs(req.getFloor() - currentFloor);
                if (distance < minDistance ||
                        (distance == minDistance && (nearest == null || req.getFloor() < nearest.getFloor()))) {
                    minDistance = distance;
                    nearest = req;
                }
            }

            direction = (nearest.getFloor() > currentFloor) ? Direction.UP : Direction.DOWN;
        }

        RequestType pickupType = (direction == Direction.UP) ? RequestType.PICKUP_UP : RequestType.PICKUP_DOWN;
        Request pickupRequest = new Request(currentFloor, pickupType);
        Request destinationRequest = new Request(currentFloor, RequestType.DESTINATION);

        if (requests.contains(pickupRequest) || requests.contains(destinationRequest)) {
            requests.remove(pickupRequest);
            requests.remove(destinationRequest);
            if (requests.isEmpty()) {
                direction = Direction.IDLE;
            }
            return;
        }

        if (!hasRequestsAhead(direction)) {
            direction = (direction == Direction.UP) ? Direction.DOWN : Direction.UP;
            return;
        }

        if (direction == Direction.UP) {
            currentFloor++;
        } else if (direction == Direction.DOWN) {
            currentFloor--;
        }
    }

    public boolean hasRequestsAhead(Direction dir) {
        for (Request request : requests) {
            if (dir == Direction.UP && request.getFloor() > currentFloor) {
                return true;
            }
            if (dir == Direction.DOWN && request.getFloor() < currentFloor) {
                return true;
            }
        }
        return false;
    }

    public boolean hasRequestsAtOrBeyond(int floor, Direction dir) {
        for (Request request : requests) {
            if (dir == Direction.UP && request.getFloor() >= floor) {
                if (request.getType() == RequestType.PICKUP_UP || request.getType() == RequestType.DESTINATION) {
                    return true;
                }
            }
            if (dir == Direction.DOWN && request.getFloor() <= floor) {
                if (request.getType() == RequestType.PICKUP_DOWN || request.getType() == RequestType.DESTINATION) {
                    return true;
                }
            }
        }
        return false;
    }

    public int getCurrentFloor() {
        return currentFloor;
    }

    public Direction getDirection() {
        return direction;
    }
}

class Request {
    private final int floor;
    private final RequestType type;

    public Request(int floor, RequestType type) {
        this.floor = floor;
        this.type = type;
    }

    public int getFloor() {
        return floor;
    }

    public RequestType getType() {
        return type;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Request)) return false;
        Request request = (Request) o;
        return floor == request.floor && type == request.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(floor, type);
    }
}


 class ElevatorController {
    private List<Elevator> elevators;

    public ElevatorController() {
        elevators = new ArrayList<>();
        elevators.add(new Elevator());
        elevators.add(new Elevator());
        elevators.add(new Elevator());
    }

    public boolean requestElevator(int floor, RequestType type) {
        if (floor < 0 || floor > 9) {
            return false;
        }
        if (type == RequestType.DESTINATION) {
            return false;
        }

        Request request = new Request(floor, type);
        Elevator best = selectBestElevator(request);
        if (best == null) {
            return false;
        }
        return best.addRequest(request);
    }

    public void step() {
        for (Elevator elevator : elevators) {
            elevator.step();
        }
    }

    private Elevator selectBestElevator(Request request) {
        Elevator best = findCommittedToFloor(request);
        if (best != null) {
            return best;
        }

        best = findNearestIdle(request.getFloor());
        if (best != null) {
            return best;
        }

        return findNearest(request.getFloor());
    }

    private Elevator findCommittedToFloor(Request request) {
        int floor = request.getFloor();
        Direction direction = (request.getType() == RequestType.PICKUP_UP) ? Direction.UP : Direction.DOWN;

        Elevator nearest = null;
        int minDistance = Integer.MAX_VALUE;

        for (Elevator e : elevators) {
            if (e.getDirection() != direction) {
                continue;
            }

            if ((direction == Direction.UP && e.getCurrentFloor() > floor) ||
                    (direction == Direction.DOWN && e.getCurrentFloor() < floor)) {
                continue;
            }

            if (!e.hasRequestsAtOrBeyond(floor, direction)) {
                continue;
            }

            int distance = Math.abs(e.getCurrentFloor() - floor);
            if (distance < minDistance) {
                minDistance = distance;
                nearest = e;
            }
        }

        return nearest;
    }

    private Elevator findNearestIdle(int floor) {
        Elevator nearest = null;
        int minDistance = Integer.MAX_VALUE;

        for (Elevator e : elevators) {
            if (e.getDirection() != Direction.IDLE) {
                continue;
            }

            int distance = Math.abs(e.getCurrentFloor() - floor);
            if (distance < minDistance) {
                minDistance = distance;
                nearest = e;
            }
        }

        return nearest;
    }

    private Elevator findNearest(int floor) {
        Elevator nearest = elevators.get(0);
        int minDistance = Math.abs(elevators.get(0).getCurrentFloor() - floor);

        for (Elevator e : elevators) {
            int distance = Math.abs(e.getCurrentFloor() - floor);
            if (distance < minDistance) {
                minDistance = distance;
                nearest = e;
            }
        }

        return nearest;
    }
}

enum RequestType {
    PICKUP_UP,
    PICKUP_DOWN,
    DESTINATION
}
