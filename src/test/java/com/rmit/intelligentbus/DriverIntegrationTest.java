package com.rmit.intelligentbus;

import com.rmit.intelligentbus.model.Driver;
import com.rmit.intelligentbus.repository.DriverRepository;
import com.rmit.intelligentbus.service.DomainValidationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for driver persistence using real JSON files.
 */
public class DriverIntegrationTest {

    @TempDir
    Path tempDir;

    private DriverRepository repository() {
        return new DriverRepository(tempDir.resolve("drivers.json"));
    }

    private Driver validDriver(String id, String name, int experience, String license) {
        return new Driver(
                id,
                name,
                experience,
                license,
                "12|King Street|Melbourne|VIC|Australia",
                "12-05-1990"
        );
    }

    @Test
    void validDriverShouldBeStoredAndRetrievedFromFile() {
        DriverRepository repository = repository();
        Driver driver = validDriver("23@#45ABCD", "Aman", 8, "Heavy");

        repository.add(driver);

        DriverRepository reloaded = repository();
        assertEquals(1, reloaded.count());
        Driver saved = reloaded.retrieve("23@#45ABCD");
        assertNotNull(saved);
        assertEquals("Aman", saved.getName());
        assertEquals("Heavy", saved.getLicenseType());
    }

    @Test
    void invalidDriverShouldBeRejectedAndFileShouldRemainEmpty() {
        DriverRepository repository = repository();
        Driver invalid = validDriver("12@#45ABCD", "Aman", 8, "Heavy");

        assertThrows(DomainValidationException.class, () -> repository.add(invalid));
        assertEquals(0, repository.count());
    }

    @Test
    void updateShouldBePersistedCorrectly() {
        DriverRepository repository = repository();
        repository.add(validDriver("23@#45ABCD", "Aman", 8, "Heavy"));

        Driver updated = validDriver("23@#45ABCD", "Aman", 9, "Heavy");
        updated.setAddress("34|Queen Street|Sydney|NSW|Australia");
        updated.setBirthdate("13-05-1990");

        repository.update("23@#45ABCD", updated);

        DriverRepository reloaded = repository();
        Driver saved = reloaded.retrieve("23@#45ABCD");
        assertEquals(1, reloaded.count());
        assertEquals(9, saved.getExperienceYears());
        assertEquals("34|Queen Street|Sydney|NSW|Australia", saved.getAddress());
        assertEquals("13-05-1990", saved.getBirthdate());
    }

    @Test
    void countShouldReflectMultipleStoredDrivers() {
        DriverRepository repository = repository();
        repository.add(validDriver("23@#45ABCD", "Aman", 8, "Heavy"));
        repository.add(validDriver("24@#56EFGH", "Ravi", 5, "Medium"));

        DriverRepository reloaded = repository();
        assertEquals(2, reloaded.count());
        assertNotNull(reloaded.retrieve("23@#45ABCD"));
        assertNotNull(reloaded.retrieve("24@#56EFGH"));
    }
}
