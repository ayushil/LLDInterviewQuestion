/*
Book a cab
User should see all the past rides
Driver should be able to accept rides
Driver should be able to see all rides
*/

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

public class Uber {
    static void main() {
        UberService uberService = new UberService();
        // create Rider
        Rider r = new Rider(1);

        // create drivers
        Driver driver1 = new Driver(1, 10, 10);
        Driver driver2 = new Driver(2, 5, 5);
        Driver driver3 = new Driver(3, 100, 100);

        driver1.setDriverStatus(OperationalStatus.ONLINE);
        driver2.setDriverStatus(OperationalStatus.IN_RIDE);
        driver3.setDriverStatus(OperationalStatus.ONLINE);

        uberService.drivers.put(driver1.driverId, driver1);
        uberService.drivers.put(driver2.driverId, driver2);
        uberService.drivers.put(driver3.driverId, driver3);


        Vehicle vehicle = new Vehicle(1, VehicleType.CAR);
        // create Journey
        Journey j = new Journey(1,2,3,4);
        // create Ride
        Ride ride = new Ride(1, j, r.riderId, vehicle, new KmPriceStrategy(10));

        // find drivers & notify
        List<Driver> drivers = uberService.findAllDrivers(new ClosestDriverStrategy(10), ride.journey);
        for (Driver driver : drivers) {
            driver.notify(ride);
        }


    }
}

class UberService {
    HashMap<Integer, Driver> drivers;
    HashMap<Integer, Rider> riders;
    HashMap<Integer, Ride> ride;

    public UberService() {
        this.drivers = new HashMap<>();
    }
    public List<Driver> findAllDrivers(FindDriverStrategy findDriverStrategy, Journey journey) {
        return findDriverStrategy.findDrivers(List.copyOf(drivers.values()), journey);
    }
}

enum OperationalStatus {
    IN_RIDE,
    ONLINE,
    OFFLINE
}

class Driver {
    int driverId;
    OperationalStatus driverStatus;
    int lat;
    int lng;
    Vehicle vehicle;
    List<Ride> rideNotifications;

    public Driver(int driverId, int lat, int lng) {
        this.driverId = driverId;
        this.driverStatus = OperationalStatus.OFFLINE;
        this.lat = lat;
        this.lng = lng;
        this.rideNotifications = new ArrayList<>();
    }

    public void setDriverStatus(OperationalStatus status) {
        this.driverStatus = status;
    }

    public void notify(Ride ride) {
        System.out.println("Ride notification : " + ride.rideId);
        rideNotifications.add(ride);
        if (driverStatus == OperationalStatus.ONLINE) {
            System.out.println("RIDE ACCEPTED BY : " + driverId);
            ride.setDriverId(driverId);
        }
    }
}

class Rider {
    int riderId;
    HashMap<Integer, Ride> rides;
    public Rider(int riderId) {
        this.riderId = riderId;
        this.rides = new HashMap<>();
    }
}

enum VehicleType {
    BIKE,
    CAR
}

class Vehicle {
    int vehicleId;
    VehicleType vehicleType;

    public Vehicle(int vehicleId, VehicleType vehicleType) {
        this.vehicleId = vehicleId;
        this.vehicleType = vehicleType;
    }
}

class Ride {
    int rideId;
    Journey journey;
    int riderId;
    int driverId;
    int price;
    Vehicle vehicle;
    UberPriceStrategy priceStrategy;
    ReentrantLock lock;

    public Ride(int rideId, Journey journey, int riderId, Vehicle vehicle, UberPriceStrategy priceStrategy) {
        this.rideId = rideId;
        this.journey = journey;
        this.riderId = riderId;
        this.vehicle = vehicle;
        this.priceStrategy = priceStrategy;
        this.price = priceStrategy.calculatePrice(this);
        this.lock = new ReentrantLock();
        this.driverId = -1;
    }

    public void setDriverId(int driverId) {
        if (driverId != -1) return;

        try {
            lock.lock();
            if (driverId == -1) {
                this.driverId = driverId;
            }
        } finally {
            lock.unlock();
        }
    }
}

class Journey {
    int startLng;
    int startLat;
    int destLng;
    int destLat;

    public Journey(int startLng, int startLat, int destLng, int destLat) {
        this.startLng = startLng;
        this.startLat = startLat;
        this.destLng = destLng;
        this.destLat = destLat;
    }
}

interface FindDriverStrategy {
    public List<Driver> findDrivers(List<Driver> drivers, Journey journey);
}

class ClosestDriverStrategy implements FindDriverStrategy {
    int delta;
    public ClosestDriverStrategy(int delta) {
        this.delta = delta;
    }
    public List<Driver> findDrivers(List<Driver> drivers, Journey journey) {
        List<Driver> res = new ArrayList<>();
        for (Driver driver : drivers) {
            if (driver.driverStatus == OperationalStatus.ONLINE && driver.lat <= (journey.startLat + delta) &&
                    driver.lat >= (journey.startLat - delta) &&
                    driver.lng <= (journey.startLng + delta) &&
                    driver.lng >= (journey.startLng - delta)) {
                res.add(driver);
            }
        }
        return res;
    }
}

interface UberPriceStrategy {
    public int calculatePrice(Ride ride);
}

class KmPriceStrategy implements UberPriceStrategy {
    public int baseCharge;
    public HashMap<VehicleType, Integer> vehicleTypeMultiplier;
    public KmPriceStrategy(int baseCharge) {
        this.baseCharge = baseCharge;
        vehicleTypeMultiplier = new HashMap<>();
        vehicleTypeMultiplier.put(VehicleType.BIKE, 2);
        vehicleTypeMultiplier.put(VehicleType.CAR, 4);
    }

    public int calculatePrice(Ride ride) {
        int dist = ride.journey.destLat + ride.journey.destLng - ride.journey.startLng - ride.journey.startLat;
        return 10 * dist * vehicleTypeMultiplier.get(ride.vehicle.vehicleType);
    }
}