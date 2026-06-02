package com.rmit.intelligentbus;

import com.rmit.intelligentbus.model.Bus;
import com.rmit.intelligentbus.model.Driver;
import com.rmit.intelligentbus.repository.BusRepository;
import com.rmit.intelligentbus.service.DomainValidationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for bus persistence using real JSON files.
 */
public class BusIntegrationTest {

    @TempDir
    Path tempDir;

    private BusRepository repository() {
        return new BusRepository(tempDir.resolve("buses.json"));
    }

    private Bus validBus(String id, int capacity, double fuelLevel, String fuelType) {
        return new Bus(id, capacity, fuelLevel, fuelType);
    }

    private Driver validDriver(String licenseType, int experienceYears, String birthdate) {
        return new Driver(
                "23@#45ABCD",
                "Aman",
                experienceYears,
                licenseType,
                "12|King Street|Melbourne|VIC|Australia",
                birthdate
        );
    }

    @Test
    void validBusShouldBeStoredAndRetrievedFromFile() {
        BusRepository repository = repository();
        Bus bus = validBus("12345678", 60, 80.0, "Diesel");

        repository.add(bus);

        BusRepository reloaded = repository();
        assertEquals(1, reloaded.count());
        Bus saved = reloaded.retrieve("12345678");
        assertNotNull(saved);
        assertEquals(60, saved.getCapacity());
        assertEquals("Diesel", saved.getFuelType());
    }

    @Test
    void invalidBusShouldBeRejectedAndFileShouldRemainEmpty() {
        BusRepository repository = repository();
        Bus invalid = validBus("1234567", 60, 80.0, "Diesel");

        assertThrows(DomainValidationException.class, () -> repository.add(invalid));
        assertEquals(0, repository.count());
    }

    @Test
    void updateShouldBePersistedCorrectly() {
        BusRepository repository = repository();
        repository.add(validBus("12345678", 60, 80.0, "Diesel"));

        Bus updated = validBus("12345678", 55, 75.0, "Diesel");
        repository.update("12345678", updated);

        BusRepository reloaded = repository();
        Bus saved = reloaded.retrieve("12345678");
        assertEquals(1, reloaded.count());
        assertEquals(55, saved.getCapacity());
        assertEquals(75.0, saved.getFuelLevel(), 0.0001);
    }

    @Test
    void countShouldReflectMultipleStoredBuses() {
        BusRepository repository = repository();
        repository.add(validBus("12345678", 60, 80.0, "Diesel"));
        repository.add(validBus("87654321", 45, 65.0, "Hybrid"));

        BusRepository reloaded = repository();
        assertEquals(2, reloaded.count());
        assertNotNull(reloaded.retrieve("12345678"));
        assertNotNull(reloaded.retrieve("87654321"));
    }
}
