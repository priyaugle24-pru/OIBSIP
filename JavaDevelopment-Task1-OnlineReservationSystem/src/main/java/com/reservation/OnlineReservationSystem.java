package com.reservation;

import javax.swing.*;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.UUID;

public class OnlineReservationSystem {
    private static final String DB_URL = "jdbc:sqlite:reservation.db";

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                Class.forName("org.sqlite.JDBC");
                createDatabase();
                new LoginFrame().setVisible(true);
            } catch (Exception ex) {
                showError("Could not start application: " + ex.getMessage());
            }
        });
    }

    private static Connection connect() throws SQLException {
        return DriverManager.getConnection(DB_URL);
    }

    private static void createDatabase() throws SQLException {
        try (Connection con = connect(); Statement st = con.createStatement()) {
            st.executeUpdate("""
                CREATE TABLE IF NOT EXISTS users (
                    username TEXT PRIMARY KEY,
                    password TEXT NOT NULL
                )
            """);
            st.executeUpdate("""
                CREATE TABLE IF NOT EXISTS trains (
                    train_no INTEGER PRIMARY KEY,
                    train_name TEXT NOT NULL
                )
            """);
            st.executeUpdate("""
                CREATE TABLE IF NOT EXISTS reservations (
                    pnr TEXT PRIMARY KEY,
                    passenger_name TEXT NOT NULL,
                    train_no INTEGER NOT NULL,
                    train_name TEXT NOT NULL,
                    class_type TEXT NOT NULL,
                    journey_date TEXT NOT NULL,
                    source TEXT NOT NULL,
                    destination TEXT NOT NULL
                )
            """);

            try (PreparedStatement ps = con.prepareStatement(
                    "INSERT OR IGNORE INTO users(username, password) VALUES(?, ?)")) {
                ps.setString(1, "admin");
                ps.setString(2, "admin123");
                ps.executeUpdate();
            }

            insertTrainIfMissing(con, 12123, "Deccan Queen");
            insertTrainIfMissing(con, 11010, "Sinhagad Express");
            insertTrainIfMissing(con, 12127, "Intercity Express");
            insertTrainIfMissing(con, 12025, "Shatabdi Express");
        }
    }

    private static void insertTrainIfMissing(Connection con, int number, String name)
            throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(
                "INSERT OR IGNORE INTO trains(train_no, train_name) VALUES(?, ?)")) {
            ps.setInt(1, number);
            ps.setString(2, name);
            ps.executeUpdate();
        }
    }

    private static void showError(String message) {
        JOptionPane.showMessageDialog(null, message, "Error", JOptionPane.ERROR_MESSAGE);
    }

    private static class LoginFrame extends JFrame {
        private final JTextField username = new JTextField(18);
        private final JPasswordField password = new JPasswordField(18);

        LoginFrame() {
            setTitle("Online Reservation System - Login");
            setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            setSize(390, 240);
            setLocationRelativeTo(null);

            JPanel panel = new JPanel(new GridBagLayout());
            panel.setBorder(BorderFactory.createEmptyBorder(18, 25, 18, 25));
            GridBagConstraints g = new GridBagConstraints();
            g.insets = new Insets(7, 7, 7, 7);
            g.fill = GridBagConstraints.HORIZONTAL;

            g.gridx = 0; g.gridy = 0; panel.add(new JLabel("Username:"), g);
            g.gridx = 1; panel.add(username, g);
            g.gridx = 0; g.gridy = 1; panel.add(new JLabel("Password:"), g);
            g.gridx = 1; panel.add(password, g);

            JButton login = new JButton("Login");
            g.gridx = 0; g.gridy = 2; g.gridwidth = 2;
            panel.add(login, g);
            login.addActionListener(e -> authenticate());
            password.addActionListener(e -> authenticate());

            add(panel);
        }

        private void authenticate() {
            String user = username.getText().trim();
            String pass = new String(password.getPassword());

            if (user.isEmpty() || pass.isEmpty()) {
                showError("Please enter both username and password.");
                return;
            }

            String sql = "SELECT username FROM users WHERE username = ? AND password = ?";
            try (Connection con = connect(); PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setString(1, user);
                ps.setString(2, pass);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        dispose();
                        new MainFrame().setVisible(true);
                    } else {
                        showError("Access denied. Invalid username or password.");
                    }
                }
            } catch (SQLException ex) {
                showError("Login failed: " + ex.getMessage());
            }
        }
    }

    private static class MainFrame extends JFrame {
        private final JTextField passenger = new JTextField(20);
        private final JTextField trainNo = new JTextField(20);
        private final JTextField trainName = new JTextField(20);
        private final JComboBox<String> classType = new JComboBox<>(
                new String[]{"Sleeper", "AC 3 Tier", "AC 2 Tier", "AC First Class", "General"});
        private final JTextField journeyDate = new JTextField(20);
        private final JTextField source = new JTextField(20);
        private final JTextField destination = new JTextField(20);

        private final JTextField pnrInput = new JTextField(18);
        private final JTextArea bookingDetails = new JTextArea(9, 34);
        private String fetchedPnr;

        MainFrame() {
            setTitle("Online Reservation System");
            setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            setSize(650, 560);
            setLocationRelativeTo(null);

            JTabbedPane tabs = new JTabbedPane();
            tabs.addTab("Book Ticket", createBookingPanel());
            tabs.addTab("Cancel Ticket", createCancellationPanel());
            add(tabs);

            trainName.setEditable(false);
            trainNo.addActionListener(e -> findTrainName());
            trainNo.addFocusListener(new FocusAdapter() {
                @Override public void focusLost(FocusEvent e) { findTrainName(); }
            });
        }

        private JPanel createBookingPanel() {
            JPanel panel = new JPanel(new GridBagLayout());
            panel.setBorder(BorderFactory.createEmptyBorder(15, 25, 15, 25));
            GridBagConstraints g = new GridBagConstraints();
            g.insets = new Insets(6, 6, 6, 6);
            g.fill = GridBagConstraints.HORIZONTAL;

            addField(panel, g, 0, "Passenger Name:", passenger);
            addField(panel, g, 1, "Train Number:", trainNo);
            addField(panel, g, 2, "Train Name:", trainName);
            addField(panel, g, 3, "Class Type:", classType);
            addField(panel, g, 4, "Journey Date (YYYY-MM-DD):", journeyDate);
            addField(panel, g, 5, "Source Station:", source);
            addField(panel, g, 6, "Destination Station:", destination);

            JButton book = new JButton("Book / Insert");
            g.gridx = 0; g.gridy = 7; g.gridwidth = 2;
            panel.add(book, g);
            book.addActionListener(e -> bookTicket());
            return panel;
        }

        private JPanel createCancellationPanel() {
            JPanel panel = new JPanel(new GridBagLayout());
            panel.setBorder(BorderFactory.createEmptyBorder(20, 25, 20, 25));
            GridBagConstraints g = new GridBagConstraints();
            g.insets = new Insets(7, 7, 7, 7);
            g.fill = GridBagConstraints.HORIZONTAL;

            g.gridx = 0; g.gridy = 0; panel.add(new JLabel("Enter PNR Number:"), g);
            g.gridx = 1; panel.add(pnrInput, g);

            JButton fetch = new JButton("Fetch Booking");
            g.gridx = 0; g.gridy = 1; g.gridwidth = 2;
            panel.add(fetch, g);
            fetch.addActionListener(e -> fetchBooking());

            bookingDetails.setEditable(false);
            bookingDetails.setLineWrap(true);
            bookingDetails.setWrapStyleWord(true);
            g.gridx = 0; g.gridy = 2; g.gridwidth = 2;
            panel.add(new JScrollPane(bookingDetails), g);

            JButton cancel = new JButton("Confirm Cancellation");
            g.gridx = 0; g.gridy = 3; g.gridwidth = 2;
            panel.add(cancel, g);
            cancel.addActionListener(e -> cancelBooking());
            return panel;
        }

        private void addField(JPanel panel, GridBagConstraints g, int row,
                              String label, JComponent field) {
            g.gridwidth = 1;
            g.gridx = 0; g.gridy = row;
            panel.add(new JLabel(label), g);
            g.gridx = 1;
            panel.add(field, g);
        }

        private void findTrainName() {
            String value = trainNo.getText().trim();
            if (value.isEmpty()) {
                trainName.setText("");
                return;
            }
            try {
                int number = Integer.parseInt(value);
                try (Connection con = connect();
                     PreparedStatement ps = con.prepareStatement(
                             "SELECT train_name FROM trains WHERE train_no = ?")) {
                    ps.setInt(1, number);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            trainName.setText(rs.getString("train_name"));
                        } else {
                            trainName.setText("");
                            showError("Train number not found. Try 12123, 11010, 12127 or 12025.");
                        }
                    }
                }
            } catch (NumberFormatException ex) {
                trainName.setText("");
                showError("Train number must contain digits only.");
            } catch (SQLException ex) {
                showError("Could not find train: " + ex.getMessage());
            }
        }

        private void bookTicket() {
            String passengerName = passenger.getText().trim();
            String trainNumberText = trainNo.getText().trim();
            String train = trainName.getText().trim();
            String dateText = journeyDate.getText().trim();
            String from = source.getText().trim();
            String to = destination.getText().trim();
            String selectedClass = (String) classType.getSelectedItem();

            if (passengerName.isEmpty() || trainNumberText.isEmpty() || train.isEmpty()
                    || dateText.isEmpty() || from.isEmpty() || to.isEmpty()) {
                showError("Please fill in all required fields and enter a valid train number.");
                return;
            }

            int number;
            try {
                number = Integer.parseInt(trainNumberText);
            } catch (NumberFormatException ex) {
                showError("Train number must be numeric.");
                return;
            }

            LocalDate date;
            try {
                date = LocalDate.parse(dateText);
            } catch (DateTimeParseException ex) {
                showError("Enter the date in YYYY-MM-DD format, for example 2026-12-15.");
                return;
            }

            if (date.isBefore(LocalDate.now())) {
                showError("Journey date cannot be in the past.");
                return;
            }
            if (from.equalsIgnoreCase(to)) {
                showError("Source and destination stations must be different.");
                return;
            }

            // Recheck train number against the database before inserting.
            String trainSql = "SELECT train_name FROM trains WHERE train_no = ?";
            String actualTrainName;
            try (Connection con = connect();
                 PreparedStatement check = con.prepareStatement(trainSql)) {
                check.setInt(1, number);
                try (ResultSet rs = check.executeQuery()) {
                    if (!rs.next()) {
                        showError("Train number does not exist.");
                        return;
                    }
                    actualTrainName = rs.getString("train_name");
                }
            } catch (SQLException ex) {
                showError("Train validation failed: " + ex.getMessage());
                return;
            }

            String pnr = "PNR" + UUID.randomUUID().toString()
                    .replace("-", "").substring(0, 10).toUpperCase();
            String sql = """
                INSERT INTO reservations
                (pnr, passenger_name, train_no, train_name, class_type,
                 journey_date, source, destination)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """;

            try (Connection con = connect(); PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setString(1, pnr);
                ps.setString(2, passengerName);
                ps.setInt(3, number);
                ps.setString(4, actualTrainName);
                ps.setString(5, selectedClass);
                ps.setString(6, date.toString());
                ps.setString(7, from);
                ps.setString(8, to);
                ps.executeUpdate();

                String confirmation = "Reservation successful!\n\n"
                        + "PNR: " + pnr + "\n"
                        + "Passenger: " + passengerName + "\n"
                        + "Train: " + number + " - " + actualTrainName + "\n"
                        + "Class: " + selectedClass + "\n"
                        + "Journey Date: " + date + "\n"
                        + "From: " + from + "\nTo: " + to;
                JOptionPane.showMessageDialog(this, confirmation,
                        "Booking Confirmation", JOptionPane.INFORMATION_MESSAGE);
                clearBookingForm();
            } catch (SQLException ex) {
                showError("Booking failed: " + ex.getMessage());
            }
        }

        private void clearBookingForm() {
            passenger.setText("");
            trainNo.setText("");
            trainName.setText("");
            journeyDate.setText("");
            source.setText("");
            destination.setText("");
            classType.setSelectedIndex(0);
        }

        private void fetchBooking() {
            String pnr = pnrInput.getText().trim().toUpperCase();
            if (pnr.isEmpty()) {
                showError("Please enter a PNR number.");
                return;
            }

            String sql = "SELECT * FROM reservations WHERE pnr = ?";
            try (Connection con = connect(); PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setString(1, pnr);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        fetchedPnr = rs.getString("pnr");
                        bookingDetails.setText(
                                "PNR: " + rs.getString("pnr") + "\n"
                                + "Passenger: " + rs.getString("passenger_name") + "\n"
                                + "Train Number: " + rs.getInt("train_no") + "\n"
                                + "Train Name: " + rs.getString("train_name") + "\n"
                                + "Class: " + rs.getString("class_type") + "\n"
                                + "Journey Date: " + rs.getString("journey_date") + "\n"
                                + "Source: " + rs.getString("source") + "\n"
                                + "Destination: " + rs.getString("destination"));
                    } else {
                        fetchedPnr = null;
                        bookingDetails.setText("");
                        showError("No booking found for this PNR.");
                    }
                }
            } catch (SQLException ex) {
                showError("Could not fetch booking: " + ex.getMessage());
            }
        }

        private void cancelBooking() {
            if (fetchedPnr == null || !fetchedPnr.equalsIgnoreCase(pnrInput.getText().trim())) {
                showError("Fetch a valid booking before cancelling.");
                return;
            }

            int choice = JOptionPane.showConfirmDialog(this,
                    "Are you sure you want to cancel booking " + fetchedPnr + "?",
                    "Confirm Cancellation", JOptionPane.YES_NO_OPTION);

            if (choice != JOptionPane.YES_OPTION) return;

            try (Connection con = connect();
                 PreparedStatement ps = con.prepareStatement(
                         "DELETE FROM reservations WHERE pnr = ?")) {
                ps.setString(1, fetchedPnr);
                int deleted = ps.executeUpdate();
                if (deleted > 0) {
                    JOptionPane.showMessageDialog(this, "Booking cancelled successfully.");
                    pnrInput.setText("");
                    bookingDetails.setText("");
                    fetchedPnr = null;
                } else {
                    showError("Booking was not found. It may already be cancelled.");
                }
            } catch (SQLException ex) {
                showError("Cancellation failed: " + ex.getMessage());
            }
        }
    }
}
