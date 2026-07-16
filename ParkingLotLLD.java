//import java.util.ArrayList;
//import java.util.HashMap;
//import java.util.List;
//
///*
//The parking lot consists of multiple levels, and each level contains multiple rows of parking spots.
//There are two types of vehicles: motorcycles and cars.
//A motorcycle can park in any available parking spot, regardless of the spot type.
//A car can only park in a designated car spot.
//Also implement park, unpark and search functions.
//
//Park -
//Unpark
//search - vehicle & parkingSlotId
//
//Singleton
//Strategy
//    First empty
//    First same type if not then first empty
//
//* */
//public class ParkingLotLLD {
//    public static void main(String[] args) {
//        ParkingStrategy parkingStrategy = new NearestParkingStrategy();
//        ParkingStrategy parkingStrategy2 = new SameTypeParkingStrategy();
//        ParkingLot parkingLot = ParkingLot.getInstance(1, 5, 2, parkingStrategy2);
//        Vehicle v1 = new Vehicle(1, VehicleType.CAR);
//        Vehicle v2 = new Vehicle(2, VehicleType.BIKE);
//        Vehicle v3 = new Vehicle(3, VehicleType.BIKE);
//        Vehicle v4 = new Vehicle(4, VehicleType.CAR);
//        Vehicle v5 = new Vehicle(5, VehicleType.CAR);
//        Vehicle v6 = new Vehicle(6, VehicleType.BIKE);
//        Vehicle v7 = new Vehicle(7, VehicleType.BIKE);
//
//        parkingLot.park(v1);
//        System.out.println(parkingLot.search(v1));
//        parkingLot.unPark(v1);
//        System.out.println(parkingLot.search(v1));
//        System.out.println(parkingLot.park(v1));
//        System.out.println(parkingLot.park(v2));
//        System.out.println(parkingLot.park(v3));
//        System.out.println(parkingLot.park(v4));
//        System.out.println(parkingLot.park(v5));
//        System.out.println(parkingLot.park(v6));
//        System.out.println(parkingLot.park(v7));
//
//    }
//}
//
//class Vehicle {
//    int vehicleId;
//    VehicleType vehicleType;
//
//    public Vehicle(int vehicleId, VehicleType vehicleType) {
//        this.vehicleType = vehicleType;
//        this.vehicleId = vehicleId;
//    }
//}
//
//class ParkingLot {
//    public static ParkingLot parkingLot;
//    List<ParkingLevel> parkingLevels;
//    int levels;
//    int capacityOfLevel;
//    int carCapacityOfLevel;
//    ParkingStrategy parkingStrategy;
//    HashMap<Vehicle, int[]> parkMap;
//
//    private ParkingLot(int levels, int capacityOfLevel, int carCapacityOfLevel, ParkingStrategy parkingStrategy) {
//        parkingLevels = new ArrayList<>();
//        this.parkingStrategy = parkingStrategy;
//        this.levels = levels;
//        this.capacityOfLevel = capacityOfLevel;
//        this.carCapacityOfLevel = carCapacityOfLevel;
//        this.parkMap = new HashMap<>();
//        for (int i = 0; i < levels; i++) {
//            parkingLevels.add(new ParkingLevel(capacityOfLevel, carCapacityOfLevel));
//        }
//    }
//
//    public static ParkingLot getInstance(int levels, int capacityOfLevel, int carCapacityOfLevel, ParkingStrategy parkingStrategy) {
//        if (parkingLot == null) {
//            parkingLot = new ParkingLot(levels, capacityOfLevel, carCapacityOfLevel, parkingStrategy);
//        }
//        return parkingLot;
//    }
//
//    public boolean park(Vehicle v) {
//        for (int i = 0; i < levels; i++) {
//            ParkingLevel parkingLevel = parkingLevels.get(i);
//            if (v.vehicleType == VehicleType.BIKE && parkingLevel.capacity == 0) {
//                continue;
//            }
//            if (v.vehicleType == VehicleType.CAR && parkingLevel.carCapacity == 0) {
//                continue;
//            }
//            int spotId = parkingStrategy.park(parkingLevel, v);
//            if (spotId != -1) {
//                ParkingSpot parkingSpot = parkingLevel.parkingSpots.get(spotId);
//                if (parkingSpot.vehicleType == VehicleType.CAR) {
//                    parkingLevel.carCapacity--;
//                    parkingLevel.capacity--;
//                } else {
//                    parkingLevel.capacity--;
//                }
//                parkMap.put(v, new int[]{i, spotId});
//                return true;
//            }
//        }
//        return false;
//    }
//
//    public boolean unPark(Vehicle v) {
//        if (parkMap.containsKey(v)) {
//            int[] parkedLocation = parkMap.get(v);
//            int level = parkedLocation[0], spotId = parkedLocation[1];
//
//            ParkingLevel parkingLevel = parkingLevels.get(level);
//            parkingLevel.unPark(spotId, v);
//            parkMap.remove(v);
//            ParkingSpot parkingSpot = parkingLevel.parkingSpots.get(spotId);
//            if (parkingSpot.vehicleType == VehicleType.CAR) {
//                parkingLevel.carCapacity++;
//                parkingLevel.capacity++;
//            } else {
//                parkingLevel.capacity++;
//            }
//        }
//        return false;
//    }
//
//    public int[] search(Vehicle v) {
//        return parkMap.get(v);
//    }
//}
//
//class ParkingLevel {
//    List<ParkingSpot> parkingSpots;
//    int capacity;
//    int carCapacity;
//
//    public ParkingLevel(int capacity, int carCapacity) {
//        parkingSpots = new ArrayList<>();
//        for (int i = 0; i < capacity; i++) {
//            if (i < carCapacity) {
//                parkingSpots.add(new ParkingSpot(i, VehicleType.CAR));
//            } else {
//                parkingSpots.add(new ParkingSpot(i, VehicleType.BIKE));
//            }
//        }
//        this.capacity = capacity;
//        this.carCapacity = carCapacity;
//    }
//
//    public boolean park(int spotId, Vehicle v) {
//        ParkingSpot parkingSpot = parkingSpots.get(spotId);
//        parkingSpot.v = v;
//        parkingSpot.isOccupied = true;
//        return true;
//    }
//    public boolean unPark(int spotId, Vehicle v) {
//        ParkingSpot parkingSpot = parkingSpots.get(spotId);
//        parkingSpot.isOccupied = false;
//        parkingSpot.v = null;
//        return true;
//    }
//}
//
//class ParkingSpot {
//    int spotId;
//    Vehicle v;
//    VehicleType vehicleType;
//    boolean isOccupied;
//
//    public ParkingSpot(int spotId, VehicleType vehicleType) {
//        this.spotId = spotId;
//        this.vehicleType = vehicleType;
//        isOccupied = false;
//    }
//}
//
//enum VehicleType {
//    CAR,
//    BIKE
//}
//
//interface ParkingStrategy {
//    public int park(ParkingLevel parkingLevel, Vehicle vehicle);
//}
//
//class NearestParkingStrategy implements ParkingStrategy {
//    public int park(ParkingLevel parkingLevel, Vehicle v) {
//        List<ParkingSpot> parkingSpots = parkingLevel.parkingSpots;
//        for (int i = 0; i < parkingSpots.size(); i++) {
//            ParkingSpot spot = parkingSpots.get(i);
//            if (!spot.isOccupied && (v.vehicleType == VehicleType.BIKE || v.vehicleType == spot.vehicleType)) {
//                parkingLevel.park(i, v);
//                return i;
//            }
//        }
//        return -1;
//    }
//}
//
//class SameTypeParkingStrategy implements ParkingStrategy {
//    public int park(ParkingLevel parkingLevel, Vehicle v) {
//        List<ParkingSpot> parkingSpots = parkingLevel.parkingSpots;
//        for (int i = 0; i < parkingLevel.parkingSpots.size(); i++) {
//            ParkingSpot spot = parkingSpots.get(i);
//            if (!spot.isOccupied && v.vehicleType == spot.vehicleType) {
//                parkingLevel.park(i, v);
////                System.out.println(v.vehicleId + " " + i);
//                return i;
//            }
//        }
//
//        if (v.vehicleType == VehicleType.BIKE) {
//            for (int i = 0; i < parkingSpots.size(); i++) {
//                ParkingSpot spot = parkingSpots.get(i);
//                if (!spot.isOccupied) {
//                    parkingLevel.park(i, v);
//                    return i;
//                }
//            }
//        }
//
//        return -1;
//    }
//}
