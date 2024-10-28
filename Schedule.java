// Schedule.java
import java.io.Serializable;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Schedule implements Serializable {
    private LocalDate weekStartDate;
    private List<Event> events;
    private static final long serialVersionUID = 1L;

    public Schedule(LocalDate weekStartDate) {
        this.weekStartDate = weekStartDate;
        this.events = new ArrayList<>();
    }

    public LocalDate getWeekStartDate() { return weekStartDate; }
    public List<Event> getEvents() { return events; }

    public boolean addEvent(Event event) {
        // Validate event timing constraints
        if (!isValidEventTiming(event)) {
            return false;
        }
        
        // Validate event overlap
        if (!isValidEventOverlap(event)) {
            return false;
        }

        events.add(event);
        return true;
    }

    public void removeEvent(Event event) {
        events.remove(event);
    }

    private boolean isValidEventTiming(Event event) {
        long duration = event.getDurationMinutes();
        if (duration < 30 || duration > 180) {  // 30 minutes to 3 hours
            return false;
        }

        LocalDateTime startTime = event.getStartTime();
        DayOfWeek day = startTime.getDayOfWeek();
        int hour = startTime.getHour();

        if (day == DayOfWeek.SUNDAY) {
            return false;
        }

        if (day == DayOfWeek.SATURDAY && 
            (hour < 8 || startTime.plusMinutes(duration).getHour() > 15)) {
            return false;
        }

        if (day.getValue() <= 5 && 
            (hour < 8 || startTime.plusMinutes(duration).getHour() > 20)) {
            return false;
        }

        return true;
    }

    private boolean isValidEventOverlap(Event newEvent) {
        for (Event existingEvent : events) {
            if (eventsOverlap(existingEvent, newEvent)) {
                long overlapMinutes = calculateOverlap(existingEvent, newEvent);
                if (overlapMinutes > 30) {
                    return false;
                }
            }
        }
        return true;
    }

    private boolean eventsOverlap(Event e1, Event e2) {
        return !e1.getEndTime().isBefore(e2.getStartTime()) && 
               !e2.getEndTime().isBefore(e1.getStartTime());
    }

    private long calculateOverlap(Event e1, Event e2) {
        LocalDateTime overlapStart = e1.getStartTime().isBefore(e2.getStartTime()) ? 
                                   e2.getStartTime() : e1.getStartTime();
        LocalDateTime overlapEnd = e1.getEndTime().isBefore(e2.getEndTime()) ? 
                                 e1.getEndTime() : e2.getEndTime();
        return java.time.Duration.between(overlapStart, overlapEnd).toMinutes();
    }

    public List<Event> getEventsForDay(LocalDate date) {
        List<Event> dayEvents = new ArrayList<>();
        for (Event event : events) {
            if (event.getStartTime().toLocalDate().equals(date)) {
                dayEvents.add(event);
            }
        }
        return dayEvents;
    }
}

