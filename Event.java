// Event.java
import java.awt.Color;
import java.io.Serializable;
import java.time.LocalDateTime;

public class Event implements Serializable {
    private String name;
    private String location;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Color color;
    private static final long serialVersionUID = 1L;

    public Event(String name, String location, LocalDateTime startTime, 
                 LocalDateTime endTime, Color color) {
        this.name = name;
        this.location = location;
        this.startTime = startTime;
        this.endTime = endTime;
        this.color = color;
    }

    // Getters and setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
    public Color getColor() { return color; }
    public void setColor(Color color) { this.color = color; }

    public long getDurationMinutes() {
        return java.time.Duration.between(startTime, endTime).toMinutes();
    }
}

