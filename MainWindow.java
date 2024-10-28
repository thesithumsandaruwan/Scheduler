import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.time.*;
import java.time.format.*;
import java.util.*;
import java.util.List;
import javax.swing.*;

public class MainWindow extends JFrame {
    private Schedule schedule;
    private JPanel weekPanel;
    private static final String SCHEDULE_FILE = "schedule.dat";
    private static final String[] COLOR_NAMES = {"Red", "Green", "Yellow", "Blue", "Orange", "Gray"};

    public MainWindow() {
        setTitle("CEO Weekly Scheduler");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1300, 800);

        // Try to load existing schedule
        schedule = loadSchedule();
        if (schedule == null) {
            requestInitialMonday();
        }

        createMenuBar();
        createWeekView();

        setLocationRelativeTo(null);
    }

    private void createMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        JMenu fileMenu = new JMenu("File");

        JMenuItem newEventItem = new JMenuItem("New Event");
        newEventItem.addActionListener(e -> showNewEventDialog(schedule.getWeekStartDate(), LocalTime.of(8, 0)));

        JMenuItem saveItem = new JMenuItem("Save Schedule");
        saveItem.addActionListener(e -> saveSchedule());

        fileMenu.add(newEventItem);
        fileMenu.add(saveItem);
        menuBar.add(fileMenu);
        setJMenuBar(menuBar);
    }

    private void createWeekView() {
        JPanel topPanel = new JPanel(new BorderLayout());
        LocalDate startDate = schedule.getWeekStartDate();
        JLabel weekLabel = new JLabel("Week: " + startDate + " to " + startDate.plusDays(6));
        weekLabel.setFont(new Font("Arial", Font.BOLD, 16));
        weekLabel.setHorizontalAlignment(SwingConstants.CENTER);
        topPanel.add(weekLabel, BorderLayout.CENTER);
        topPanel.setBackground(new Color(60, 179, 113));
        getContentPane().add(topPanel, BorderLayout.NORTH);

        weekPanel = new JPanel(new GridLayout(0, 8, 5, 5)); // Added padding
        add(new JScrollPane(weekPanel), BorderLayout.CENTER);
        updateWeekView();
    }

    private void updateWeekView() {
        weekPanel.removeAll();

        // Add header cells
        weekPanel.add(new JLabel("Time", SwingConstants.CENTER));
        String[] days = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"};
        for (String day : days) {
            JLabel dayLabel = new JLabel(day, SwingConstants.CENTER);
            dayLabel.setFont(new Font("Arial", Font.BOLD, 14));
            dayLabel.setBorder(BorderFactory.createMatteBorder(0, 0, 2, 0, Color.BLACK));
            weekPanel.add(dayLabel);
        }

        // Add time slots
        LocalTime time = LocalTime.of(8, 0);
        while (time.isBefore(LocalTime.of(20, 30))) {
            JLabel timeLabel = new JLabel(time.format(DateTimeFormatter.ofPattern("HH:mm")), SwingConstants.CENTER);
            timeLabel.setFont(new Font("Arial", Font.PLAIN, 12));
            if (time.getMinute() == 0) {
                timeLabel.setBorder(BorderFactory.createMatteBorder(2, 0, 0, 0, Color.BLACK));
            }
            weekPanel.add(timeLabel);

            for (int day = 1; day <= 7; day++) {
                JPanel slot = createTimeSlot(day, time);
                weekPanel.add(slot);
            }

            time = time.plusMinutes(30);
        }

        weekPanel.revalidate();
        weekPanel.repaint();
    }

    private JPanel createTimeSlot(int dayOfWeek, LocalTime time) {
        JPanel slot = new JPanel();
        slot.setLayout(new BoxLayout(slot, BoxLayout.Y_AXIS)); // Vertical stacking for overlapping events
        slot.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
        slot.setBackground(new Color(240, 240, 240));

        // Grey out Saturday after 3 PM
        if (dayOfWeek == 6 && time.isAfter(LocalTime.of(15, 0))) {
            slot.setBackground(Color.LIGHT_GRAY);
            slot.setEnabled(false);
            return slot;
        }

        LocalDate date = schedule.getWeekStartDate().plusDays(dayOfWeek - 1);
        LocalDateTime dateTime = LocalDateTime.of(date, time);

        // Find events for this time slot
        List<Event> overlappingEvents = new ArrayList<>();
        for (Event event : schedule.getEvents()) {
            if (isTimeInEvent(dateTime, event)) {
                overlappingEvents.add(event);
            }
        }

        if (!overlappingEvents.isEmpty()) {
            for (Event event : overlappingEvents) {
                JButton eventButton = createEventButton(event);
                eventButton.setAlignmentX(Component.CENTER_ALIGNMENT);
                slot.add(eventButton);
            }
        } else {
            slot.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    showNewEventDialog(date, time);
                }
            });
        }

        return slot;
    }

    private boolean isTimeInEvent(LocalDateTime time, Event event) {
        return !time.isBefore(event.getStartTime()) &&
               !time.isAfter(event.getEndTime());
    }

    private JButton createEventButton(Event event) {
        JButton button = new JButton("<html>" + event.getName() + "<br>(" + event.getLocation() + ")</html>");
        button.setBackground(event.getColor());
        button.setOpaque(true);
        button.setForeground(Color.WHITE);
        button.setFont(new Font("Arial", Font.PLAIN, 12));
        button.setToolTipText("Location: " + event.getLocation() + ", Time: " +
                event.getStartTime().toLocalTime() + " - " + event.getEndTime().toLocalTime());

        // Rounded corners for event buttons
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(event.getColor().darker(), 2),
                BorderFactory.createEmptyBorder(5, 10, 5, 10)));

        button.addActionListener(e -> showEventDetails(event));
        return button;
    }

    private void showEventDetails(Event event) {
        JDialog dialog = new JDialog(this, "Event Details", true);
        dialog.setLayout(new BorderLayout());

        JPanel details = new JPanel(new GridLayout(0, 2));
        details.add(new JLabel("Name:"));
        JTextField nameField = new JTextField(event.getName());
        details.add(nameField);

        details.add(new JLabel("Location:"));
        JTextField locationField = new JTextField(event.getLocation());
        details.add(locationField);

        JComboBox<String> colorBox = new JComboBox<>(COLOR_NAMES);
        colorBox.setSelectedItem(getColorNameFromColor(event.getColor()));
        details.add(new JLabel("Color:"));
        details.add(colorBox);

        JComboBox<String> startTimeBox = new JComboBox<>(generateTimeSlots());
        JComboBox<String> endTimeBox = new JComboBox<>(generateTimeSlots());
        startTimeBox.setSelectedItem(event.getStartTime().toLocalTime().toString());
        endTimeBox.setSelectedItem(event.getEndTime().toLocalTime().toString());

        details.add(new JLabel("Start Time:"));
        details.add(startTimeBox);
        details.add(new JLabel("End Time:"));
        details.add(endTimeBox);

        JPanel buttons = new JPanel();
        JButton saveButton = new JButton("Save");
        saveButton.addActionListener(e -> {
            LocalTime startTime = LocalTime.parse((String) startTimeBox.getSelectedItem());
            LocalTime endTime = LocalTime.parse((String) endTimeBox.getSelectedItem());
            if (startTime.isBefore(endTime)) {
                updateEvent(event, nameField.getText(), locationField.getText(), getColorFromName((String) colorBox.getSelectedItem()), startTime, endTime);
                dialog.dispose();
                updateWeekView();
            } else {
                JOptionPane.showMessageDialog(this, "End time must be after start time!", "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        JButton deleteButton = new JButton("Delete");
        deleteButton.addActionListener(e -> {
            if (confirmDelete()) {
                schedule.removeEvent(event);
                dialog.dispose();
                updateWeekView();
            }
        });

        buttons.add(saveButton);
        buttons.add(deleteButton);

        dialog.add(details, BorderLayout.CENTER);
        dialog.add(buttons, BorderLayout.SOUTH);

        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private boolean confirmDelete() {
        return JOptionPane.showConfirmDialog(this,
            "Are you sure you want to delete this event?",
            "Confirm Delete",
            JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION;
    }

    private void updateEvent(Event event, String name, String location, Color color, LocalTime startTime, LocalTime endTime) {
        event.setName(name);
        event.setLocation(location);
        event.setColor(color);
        event.setStartTime(LocalDateTime.of(event.getStartTime().toLocalDate(), startTime));
        event.setEndTime(LocalDateTime.of(event.getEndTime().toLocalDate(), endTime));
    }

    private void showNewEventDialog(LocalDate defaultDate, LocalTime defaultTime) {
        JDialog dialog = new JDialog(this, "New Event", true);
        dialog.setLayout(new BorderLayout());
    
        JPanel form = new JPanel(new GridLayout(0, 2));
    
        JTextField nameField = new JTextField(32);
        JTextField locationField = new JTextField(32);
        JComboBox<String> colorBox = new JComboBox<>(COLOR_NAMES);
    
        // Add time selection for start and end time
        JComboBox<String> startTimeBox = new JComboBox<>(generateTimeSlots());
        JComboBox<String> endTimeBox = new JComboBox<>(generateTimeSlots());
    
        // Add day selection (from Monday to Sunday)
        JComboBox<String> dayBox = new JComboBox<>(new String[] {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"});
    
        form.add(new JLabel("Name:"));
        form.add(nameField);
        form.add(new JLabel("Location:"));
        form.add(locationField);
        form.add(new JLabel("Day:"));
        form.add(dayBox);
        form.add(new JLabel("Start Time:"));
        form.add(startTimeBox);
        form.add(new JLabel("End Time:"));
        form.add(endTimeBox);
        form.add(new JLabel("Color:"));
        form.add(colorBox);
    
        JButton saveButton = new JButton("Create");
        saveButton.addActionListener(e -> {
            LocalTime startTime = LocalTime.parse((String) startTimeBox.getSelectedItem());
            LocalTime endTime = LocalTime.parse((String) endTimeBox.getSelectedItem());
            if (startTime.isBefore(endTime)) {
                // Calculate the correct date based on selected day of the week
                LocalDate selectedDate = schedule.getWeekStartDate().plusDays(dayBox.getSelectedIndex());
                createNewEvent(nameField.getText(), locationField.getText(), selectedDate, startTime, endTime, getColorFromName((String) colorBox.getSelectedItem()));
                dialog.dispose();
                updateWeekView();
            } else {
                JOptionPane.showMessageDialog(this, "End time must be after start time!", "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
    
        dialog.add(form, BorderLayout.CENTER);
        dialog.add(saveButton, BorderLayout.SOUTH);
    
        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }
    

    // Helper function to generate 30-minute interval time slots
    private String[] generateTimeSlots() {
        java.util.List<String> slots = new ArrayList<>();
        LocalTime time = LocalTime.of(8, 0);
        while (time.isBefore(LocalTime.of(20, 0))) {
            slots.add(time.toString());
            time = time.plusMinutes(30);
        }
        return slots.toArray(new String[0]);
    }

    // Helper function to convert color name to Color object
    private Color getColorFromName(String colorName) {
        switch (colorName) {
            case "Red":
                return Color.RED;
            case "Green":
                return Color.GREEN;
            case "Yellow":
                return Color.YELLOW;
            case "Blue":
                return Color.BLUE;
            case "Orange":
                return Color.ORANGE;
            case "Gray":
                return Color.GRAY;
            default:
                return Color.BLACK;
        }
    }

    // Helper function to convert Color object to color name
    private String getColorNameFromColor(Color color) {
        if (color.equals(Color.RED)) return "Red";
        if (color.equals(Color.GREEN)) return "Green";
        if (color.equals(Color.YELLOW)) return "Yellow";
        if (color.equals(Color.BLUE)) return "Blue";
        if (color.equals(Color.ORANGE)) return "Orange";
        if (color.equals(Color.GRAY)) return "Gray";
        return "Unknown";
    }

    private void createNewEvent(String name, String location, LocalDate date, LocalTime startTime, LocalTime endTime, Color color) {
        Event newEvent = new Event(name, location, LocalDateTime.of(date, startTime), LocalDateTime.of(date, endTime), color);
        schedule.addEvent(newEvent);
    }

    private void requestInitialMonday() {
        String input = JOptionPane.showInputDialog(this, "Enter the start date (Monday) of the first week (YYYY-MM-DD):");
        LocalDate monday = LocalDate.parse(input);
        schedule = new Schedule(monday);
    }

    private void saveSchedule() {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(SCHEDULE_FILE))) {
            oos.writeObject(schedule);
            JOptionPane.showMessageDialog(this, "Schedule saved successfully.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Schedule loadSchedule() {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(SCHEDULE_FILE))) {
            return (Schedule) ois.readObject();
        } catch (IOException | ClassNotFoundException e) {
            return null;
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new MainWindow().setVisible(true));
    }
}
