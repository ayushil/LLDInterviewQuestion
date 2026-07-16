import java.util.*;

enum BookingStatus {
    CONFIRMED,
    CANCELLED
}

class Station {
    private final String name;
    private final int sequenceNumber;

    public Station(String name, int sequenceNumber) {
        this.name = name;
        this.sequenceNumber = sequenceNumber;
    }

    public String getName() {
        return name;
    }

    public int getSequenceNumber() {
        return sequenceNumber;
    }
}

class Seat {
    private final String seatNumber;

    public Seat(String seatNumber) {
        this.seatNumber = seatNumber;
    }

    public String getSeatNumber() {
        return seatNumber;
    }
}

class Booking {
    private final String bookingId;
    private final String passengerName;
    private final String trainId;
    private final String seatNumber;
    private final String source;
    private final String destination;
    private BookingStatus status;

    public Booking(String bookingId, String passengerName, String trainId,
                   String seatNumber, String source, String destination) {
        this.bookingId = bookingId;
        this.passengerName = passengerName;
        this.trainId = trainId;
        this.seatNumber = seatNumber;
        this.source = source;
        this.destination = destination;
        this.status = BookingStatus.CONFIRMED;
    }

    public String getBookingId() {
        return bookingId;
    }

    public String getPassengerName() {
        return passengerName;
    }

    public String getTrainId() {
        return trainId;
    }

    public String getSeatNumber() {
        return seatNumber;
    }

    public String getSource() {
        return source;
    }

    public String getDestination() {
        return destination;
    }

    public BookingStatus getStatus() {
        return status;
    }

    public void cancel() {
        this.status = BookingStatus.CANCELLED;
    }

    @Override
    public String toString() {
        return "Booking{" +
                "bookingId='" + bookingId + '\'' +
                ", passenger='" + passengerName + '\'' +
                ", trainId='" + trainId + '\'' +
                ", seat='" + seatNumber + '\'' +
                ", from='" + source + '\'' +
                ", to='" + destination + '\'' +
                ", status=" + status +
                '}';
    }
}

class Train {
    private final String trainId;
    private final List<Station> stations;
    private final List<Seat> seats;
    private final Map<String, Integer> stationIndexMap;

    public Train(String trainId, List<Station> stations, List<Seat> seats) {
        this.trainId = trainId;
        this.stations = stations;
        this.seats = seats;
        this.stationIndexMap = new HashMap<>();

        for (Station station : stations) {
            stationIndexMap.put(station.getName(), station.getSequenceNumber());
        }
    }

    public String getTrainId() {
        return trainId;
    }

    public List<Seat> getSeats() {
        return seats;
    }

    public int getStationIndex(String stationName) {
        if (!stationIndexMap.containsKey(stationName)) {
            throw new IllegalArgumentException("Invalid station: " + stationName);
        }
        return stationIndexMap.get(stationName);
    }
}

class SeatAvailabilityService {

    public boolean isSeatAvailable(Train train,
                                   Seat seat,
                                   String source,
                                   String destination,
                                   List<Booking> existingBookings) {

        int newStart = train.getStationIndex(source);
        int newEnd = train.getStationIndex(destination);

        if (newStart >= newEnd) {
            throw new IllegalArgumentException("Source must come before destination");
        }

        for (Booking booking : existingBookings) {
            if (booking.getStatus() == BookingStatus.CANCELLED) {
                continue;
            }

            if (!booking.getSeatNumber().equals(seat.getSeatNumber())) {
                continue;
            }

            int existingStart = train.getStationIndex(booking.getSource());
            int existingEnd = train.getStationIndex(booking.getDestination());

            if (hasOverlap(newStart, newEnd, existingStart, existingEnd)) {
                return false;
            }
        }

        return true;
    }

    private boolean hasOverlap(int newStart, int newEnd,
                               int existingStart, int existingEnd) {
        return newStart < existingEnd && existingStart < newEnd;
    }
}

class BookingService {
    private final Map<String, Train> trains = new HashMap<>();
    private final Map<String, List<Booking>> bookingsByTrain = new HashMap<>();
    private final SeatAvailabilityService availabilityService;

    public BookingService(SeatAvailabilityService availabilityService) {
        this.availabilityService = availabilityService;
    }

    public void addTrain(Train train) {
        trains.put(train.getTrainId(), train);
        bookingsByTrain.put(train.getTrainId(), new ArrayList<>());
    }

    public Booking bookSeat(String passengerName,
                            String trainId,
                            String source,
                            String destination) {

        if (!trains.containsKey(trainId)) {
            throw new IllegalArgumentException("Train not found: " + trainId);
        }

        Train train = trains.get(trainId);
        List<Booking> existingBookings = bookingsByTrain.get(trainId);

        for (Seat seat : train.getSeats()) {
            boolean available = availabilityService.isSeatAvailable(
                    train,
                    seat,
                    source,
                    destination,
                    existingBookings
            );

            if (available) {
                Booking booking = new Booking(
                        UUID.randomUUID().toString(),
                        passengerName,
                        trainId,
                        seat.getSeatNumber(),
                        source,
                        destination
                );

                existingBookings.add(booking);
                return booking;
            }
        }

        throw new RuntimeException("No seat available from " + source + " to " + destination);
    }

    public void cancelBooking(String trainId, String bookingId) {
        List<Booking> bookings = bookingsByTrain.get(trainId);

        if (bookings == null) {
            throw new IllegalArgumentException("Train not found: " + trainId);
        }

        for (Booking booking : bookings) {
            if (booking.getBookingId().equals(bookingId)) {
                booking.cancel();
                return;
            }
        }

        throw new IllegalArgumentException("Booking not found: " + bookingId);
    }
}

public class RailwaySeatBookingSystem {
    public static void main(String[] args) {
        List<Station> stations = Arrays.asList(
                new Station("A", 0),
                new Station("B", 1),
                new Station("C", 2),
                new Station("D", 3)
        );

        List<Seat> seats = Arrays.asList(
                new Seat("S1"),
                new Seat("S2")
        );

        Train train = new Train("TRAIN_1", stations, seats);

        BookingService bookingService =
                new BookingService(new SeatAvailabilityService());

        bookingService.addTrain(train);

        Booking booking1 = bookingService.bookSeat("Ayushi", "TRAIN_1", "A", "B");
        System.out.println(booking1);

        Booking booking2 = bookingService.bookSeat("Rahul", "TRAIN_1", "B", "C");
        System.out.println(booking2);

        Booking booking3 = bookingService.bookSeat("Priya", "TRAIN_1", "A", "C");
        System.out.println(booking3);
    }
}