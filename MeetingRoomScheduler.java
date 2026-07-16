import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;

public final class MeetingRoomScheduler {
    private final Map<String, RoomSchedule> roomsById = new LinkedHashMap<>();
    private final AtomicLong nextReservationCounter = new AtomicLong(1);
    private final MeetingRoomSelectionStrategy roomSelectionStrategy;

    public MeetingRoomScheduler(List<String> roomIds) {
        this(roomIds, new FirstAvailableMeetingRoomStrategy());
    }

    public MeetingRoomScheduler(List<String> roomIds, MeetingRoomSelectionStrategy roomSelectionStrategy) {
        if (roomIds == null || roomIds.isEmpty()) throw new IllegalArgumentException("roomIds");
        if (roomSelectionStrategy == null) throw new IllegalArgumentException("roomSelectionStrategy");
        this.roomSelectionStrategy = roomSelectionStrategy;
        for (String roomId : roomIds) {
            if (roomId == null || roomId.isEmpty()) throw new IllegalArgumentException("invalid roomId");
            roomsById.put(roomId, new RoomSchedule(roomId));
        }
    }

    public synchronized Meeting scheduleMeeting(LocalDateTime start, LocalDateTime end) {
        TimeSlot slot = new TimeSlot(start, end);
        RoomSchedule room = roomSelectionStrategy.selectRoom(new ArrayList<>(roomsById.values()), slot);
        if (room == null) throw new NoRoomAvailableException("No rooms available for " + start + " - " + end);
        return room.reserve(slot, buildReservationId(room.getRoomId()));
    }

    public synchronized RecurringMeetingResult scheduleRecurringMeeting(
            LocalDateTime start, LocalDateTime end, RecurrenceRule recurrenceRule) {
        if (recurrenceRule == null) throw new IllegalArgumentException("recurrenceRule");
        List<TimeSlot> slots = recurrenceRule.generate(new TimeSlot(start, end));
        List<RoomSchedule> eligibleRooms = new ArrayList<>();
        for (RoomSchedule room : roomsById.values()) {
            if (isRoomFreeForAll(room, slots)) eligibleRooms.add(room);
        }
        RoomSchedule room = roomSelectionStrategy.selectRoom(eligibleRooms, slots.get(0));
        if (room == null) throw new NoRoomAvailableException("No room can host all recurring occurrences");
        String seriesId = "SERIES-" + UUID.randomUUID();
        List<Meeting> meetings = new ArrayList<>(slots.size());
        for (TimeSlot slot : slots) {
            meetings.add(room.reserve(slot, buildReservationId(room.getRoomId()), seriesId));
        }
        return new RecurringMeetingResult(seriesId, meetings);
    }

    private boolean isRoomFreeForAll(RoomSchedule room, List<TimeSlot> slots) {
        for (TimeSlot slot : slots) {
            if (!room.isAvailable(slot)) return false;
        }
        return true;
    }

    private String buildReservationId(String roomId) {
        return roomId + "-R" + nextReservationCounter.getAndIncrement();
    }

    public static void main(String[] args) {
        MeetingRoomScheduler firstAvailableScheduler =
                new MeetingRoomScheduler(List.of("roomA", "roomB", "roomC"));

        MeetingRoomScheduler randomScheduler =
                new MeetingRoomScheduler(
                        List.of("roomA", "roomB", "roomC"),
                        new RandomMeetingRoomStrategy());

        System.out.println("=== One-time meetings (first available strategy) ===");
        Meeting a = firstAvailableScheduler.scheduleMeeting(
                LocalDateTime.of(2026, 4, 28, 10, 0), LocalDateTime.of(2026, 4, 28, 11, 0));
        Meeting b = firstAvailableScheduler.scheduleMeeting(
                LocalDateTime.of(2026, 4, 28, 10, 0), LocalDateTime.of(2026, 4, 28, 11, 0));
        Meeting c = firstAvailableScheduler.scheduleMeeting(
                LocalDateTime.of(2026, 4, 28, 10, 0), LocalDateTime.of(2026, 4, 28, 11, 0));
        System.out.println(a.getReservationId() + " -> " + a.getRoomId());
        System.out.println(b.getReservationId() + " -> " + b.getRoomId());
        System.out.println(c.getReservationId() + " -> " + c.getRoomId());

        System.out.println("\n=== Recurring meetings (weekly) ===");
        RecurringMeetingResult recurring = firstAvailableScheduler.scheduleRecurringMeeting(
                LocalDateTime.of(2026, 4, 29, 9, 0),
                LocalDateTime.of(2026, 4, 29, 10, 0),
                new WeeklyRecurrenceRule(3, 1));
        System.out.println("Series: " + recurring.getSeriesId());
        for (Meeting meeting : recurring.getMeetings()) {
            System.out.println(meeting.getReservationId() + " -> " + meeting.getRoomId()
                    + " | " + meeting.getSlot().getStart() + " to " + meeting.getSlot().getEnd());
        }

        System.out.println("\n=== One-time meeting (random strategy) ===");
        Meeting randomChoice = randomScheduler.scheduleMeeting(
                LocalDateTime.of(2026, 4, 28, 15, 0), LocalDateTime.of(2026, 4, 28, 16, 0));
        System.out.println(randomChoice.getReservationId() + " -> " + randomChoice.getRoomId());

        System.out.println("\n=== No room available case ===");
        try {
            firstAvailableScheduler.scheduleMeeting(
                    LocalDateTime.of(2026, 4, 28, 10, 0), LocalDateTime.of(2026, 4, 28, 11, 0));
        } catch (NoRoomAvailableException e) {
            System.out.println("Expected error: " + e.getMessage());
        }

        System.out.println("\n=== Recurring no-room case ===");
        try {
            firstAvailableScheduler.scheduleRecurringMeeting(
                    LocalDateTime.of(2026, 4, 28, 10, 0),
                    LocalDateTime.of(2026, 4, 28, 11, 0),
                    new DailyRecurrenceRule(2, 1));
            firstAvailableScheduler.scheduleRecurringMeeting(
                    LocalDateTime.of(2026, 4, 28, 10, 0),
                    LocalDateTime.of(2026, 4, 28, 11, 0),
                    new DailyRecurrenceRule(2, 1));
            firstAvailableScheduler.scheduleRecurringMeeting(
                    LocalDateTime.of(2026, 4, 28, 10, 0),
                    LocalDateTime.of(2026, 4, 28, 11, 0),
                    new DailyRecurrenceRule(2, 1));
            firstAvailableScheduler.scheduleRecurringMeeting(
                    LocalDateTime.of(2026, 4, 28, 10, 0),
                    LocalDateTime.of(2026, 4, 28, 11, 0),
                    new DailyRecurrenceRule(2, 1));
        } catch (NoRoomAvailableException e) {
            System.out.println("Expected recurring error: " + e.getMessage());
        }
    }
}

final class TimeSlot {
    private final LocalDateTime start;
    private final LocalDateTime end;

    TimeSlot(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null) throw new IllegalArgumentException("start/end");
        if (!start.isBefore(end)) throw new IllegalArgumentException("start must be before end");
        this.start = start;
        this.end = end;
    }

    LocalDateTime getStart() { return start; }
    LocalDateTime getEnd() { return end; }

    boolean overlaps(TimeSlot other) {
        return this.start.isBefore(other.end) && other.start.isBefore(this.end);
    }

    TimeSlot plusDays(long days) {
        return new TimeSlot(start.plusDays(days), end.plusDays(days));
    }
}

final class NoRoomAvailableException extends RuntimeException {
    NoRoomAvailableException(String message) { super(message); }
}

interface MeetingRoomSelectionStrategy {
    RoomSchedule selectRoom(List<RoomSchedule> roomSchedules, TimeSlot slot);
}

final class FirstAvailableMeetingRoomStrategy implements MeetingRoomSelectionStrategy {
    @Override
    public RoomSchedule selectRoom(List<RoomSchedule> roomSchedules, TimeSlot slot) {
        for (RoomSchedule room : roomSchedules) {
            if (room.isAvailable(slot)) return room;
        }
        return null;
    }
}

final class RandomMeetingRoomStrategy implements MeetingRoomSelectionStrategy {
    @Override
    public RoomSchedule selectRoom(List<RoomSchedule> roomSchedules, TimeSlot slot) {
        List<RoomSchedule> available = new ArrayList<>();
        for (RoomSchedule room : roomSchedules) {
            if (room.isAvailable(slot)) available.add(room);
        }
        if (available.isEmpty()) return null;
        int idx = ThreadLocalRandom.current().nextInt(available.size());
        return available.get(idx);
    }
}

final class Meeting {
    private final String reservationId;
    private final String roomId;
    private final TimeSlot slot;
    private final String recurringSeriesId;

    Meeting(String reservationId, String roomId, TimeSlot slot) {
        this(reservationId, roomId, slot, null);
    }

    Meeting(String reservationId, String roomId, TimeSlot slot, String recurringSeriesId) {
        this.reservationId = reservationId;
        this.roomId = roomId;
        this.slot = slot;
        this.recurringSeriesId = recurringSeriesId;
    }

    String getReservationId() { return reservationId; }
    String getRoomId() { return roomId; }
    TimeSlot getSlot() { return slot; }
    String getRecurringSeriesId() { return recurringSeriesId; }
}

interface RecurrenceRule {
    List<TimeSlot> generate(TimeSlot firstSlot);
}

final class DailyRecurrenceRule implements RecurrenceRule {
    private final int occurrences;
    private final int everyNDays;

    DailyRecurrenceRule(int occurrences, int everyNDays) {
        if (occurrences < 1 || everyNDays < 1)
            throw new IllegalArgumentException("occurrences/everyNDays must be >= 1");
        this.occurrences = occurrences;
        this.everyNDays = everyNDays;
    }

    @Override
    public List<TimeSlot> generate(TimeSlot firstSlot) {
        List<TimeSlot> out = new ArrayList<>(occurrences);
        TimeSlot cur = firstSlot;
        for (int i = 0; i < occurrences; i++) {
            out.add(cur);
            cur = cur.plusDays(everyNDays);
        }
        return out;
    }
}

final class WeeklyRecurrenceRule implements RecurrenceRule {
    private final int occurrences;
    private final int everyNWeeks;

    WeeklyRecurrenceRule(int occurrences, int everyNWeeks) {
        if (occurrences < 1 || everyNWeeks < 1)
            throw new IllegalArgumentException("occurrences/everyNWeeks must be >= 1");
        this.occurrences = occurrences;
        this.everyNWeeks = everyNWeeks;
    }

    @Override
    public List<TimeSlot> generate(TimeSlot firstSlot) {
        List<TimeSlot> out = new ArrayList<>(occurrences);
        TimeSlot cur = firstSlot;
        for (int i = 0; i < occurrences; i++) {
            out.add(cur);
            cur = cur.plusDays(7L * everyNWeeks);
        }
        return out;
    }
}

final class RecurringMeetingResult {
    private final String seriesId;
    private final List<Meeting> meetings;

    RecurringMeetingResult(String seriesId, List<Meeting> meetings) {
        this.seriesId = seriesId;
        this.meetings = new ArrayList<>(meetings);
    }

    String getSeriesId() { return seriesId; }
    List<Meeting> getMeetings() { return Collections.unmodifiableList(meetings); }
}

final class RoomSchedule {
    private final String roomId;
    private final List<Meeting> meetings = new ArrayList<>();

    RoomSchedule(String roomId) { this.roomId = roomId; }

    String getRoomId() { return roomId; }

    boolean isAvailable(TimeSlot slot) {
        for (Meeting r : meetings) {
            if (r.getSlot().overlaps(slot)) return false;
        }
        return true;
    }

    Meeting reserve(TimeSlot slot, String reservationId) {
        return reserve(slot, reservationId, null);
    }

    Meeting reserve(TimeSlot slot, String reservationId, String recurringSeriesId) {
        Meeting reservation = new Meeting(reservationId, roomId, slot, recurringSeriesId);
        meetings.add(reservation);
        meetings.sort(Comparator.comparing(r -> r.getSlot().getStart()));
        return reservation;
    }
}