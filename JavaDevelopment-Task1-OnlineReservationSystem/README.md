# Online Reservation System (Java Swing + JDBC + SQLite)

## Requirements
- JDK 17 or later
- Maven 3.8+ (or an IDE such as IntelliJ IDEA / Eclipse with Maven support)
- Internet for the first Maven build, so Maven can download the SQLite JDBC driver

## Run in an IDE
1. Extract the ZIP.
2. Open/import the `OnlineReservationSystem` folder as an existing Maven project.
3. Wait for Maven to download dependencies.
4. Run `src/main/java/com/reservation/OnlineReservationSystem.java`.
5. A local `reservation.db` file is created automatically in the project's working directory.

## Run from terminal
Open a terminal in the project folder and run:
```bash
mvn clean compile
mvn exec:java -Dexec.mainClass="com.reservation.OnlineReservationSystem"
```
If your Maven setup does not have the exec plugin configured, run the main class from your IDE.

## Demo login
- Username: `admin`
- Password: `admin123`

## Demo train numbers
| Train number | Train name |
|---|---|
| 12123 | Deccan Queen |
| 11010 | Sinhagad Express |
| 12127 | Intercity Express |
| 12025 | Shatabdi Express |

Enter a train number and press Enter or move focus away; the train name is fetched automatically.

## Features
- Login checks credentials using a parameterized JDBC query.
- Booking validates required fields, numeric train number, ISO date (`YYYY-MM-DD`), future journey date, and different source/destination.
- PNR is generated automatically and stored as the reservation primary key.
- Booking confirmation displays all reservation details.
- Cancellation fetches the complete booking by PNR, asks for confirmation, then deletes it.
- SQLite tables are created automatically on first run.

## Database tables
- `users(username, password)`
- `trains(train_no, train_name)`
- `reservations(pnr, passenger_name, train_no, train_name, class_type, journey_date, source, destination)`

## Important note
This is a learning/demo project. The sample login stores a plain-text password to keep the JDBC task easy to understand. A real application must store salted password hashes, use proper user registration/roles, and add stronger reservation/business rules. The train list is sample data, not live railway data.
