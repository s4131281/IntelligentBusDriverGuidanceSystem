package com.rmit.intelligentbus;

import com.rmit.intelligentbus.model.Bus;
import com.rmit.intelligentbus.model.Driver;
import com.rmit.intelligentbus.repository.BusRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for driver-to-bus business rules.
 */
public class BusAssignmentRulesTest {

    @TempDir
    Path tempDir;

    @Test
    void olderDriverShouldNotDriveLargeBus() {
        BusRepository repository = new BusRepository(tempDir.resolve("buses.json"));

        Driver olderDriver = new Driver(
                "23@#45ABCD",
                "Suresh",
                12,
                "Heavy",
                "12|King Street|Melbourne|VIC|Australia",
                "01-01-1960"
        );
        Bus bus = new Bus("12345678", 55, 70.0, "Diesel");

        assertThrows(RuntimeException.class, () -> repository.validateDriverForBus(olderDriver, bus));
    }

    @Test
    void electricBusShouldRequireAtLeastFiveYearsExperience() {
        BusRepository repository = new BusRepository(tempDir.resolve("buses.json"));

        Driver driver = new Driver(
                "23@#45ABCD",
                "Aman",
                4,
                "Heavy",
                "12|King Street|Melbourne|VIC|Australia",
                "12-05-1990"
        );
        Bus bus = new Bus("12345678", 40, 70.0, "Electricity");

        assertThrows(RuntimeException.class, () -> repository.validateDriverForBus(driver, bus));
    }
}
