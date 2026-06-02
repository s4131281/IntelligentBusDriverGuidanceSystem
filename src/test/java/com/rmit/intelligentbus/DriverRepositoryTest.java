package com.rmit.intelligentbus;

import com.rmit.intelligentbus.model.Driver;
import com.rmit.intelligentbus.repository.DriverRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Basic repository test for drivers.
 */
public class DriverRepositoryTest {

    @TempDir
    Path tempDir;

    @Test
    void addAndRetrieveDriverShouldWork() {
        DriverRepository repository = new DriverRepository(tempDir.resolve("drivers.json"));
        Driver driver = new Driver(
                "23@#45ABCD",
                "Aman",
                8,
                "Heavy",
                "12|King Street|Melbourne|VIC|Australia",
                "12-05-1990"
        );

        repository.add(driver);

        assertEquals(1, repository.count());
        Driver saved = repository.retrieve("23@#45ABCD");
        assertNotNull(saved);
        assertEquals("Aman", saved.getName());
    }

    @Test
    void invalidDriverIdShouldBeRejected() {
        DriverRepository repository = new DriverRepository(tempDir.resolve("drivers.json"));
        Driver driver = new Driver(
                "12@#45ABCD",
                "Aman",
                8,
                "Heavy",
                "12|King Street|Melbourne|VIC|Australia",
                "12-05-1990"
        );

        assertThrows(RuntimeException.class, () -> repository.add(driver));
    }
}
