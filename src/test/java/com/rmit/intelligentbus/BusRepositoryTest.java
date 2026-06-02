package com.rmit.intelligentbus;

import com.rmit.intelligentbus.model.Bus;
import com.rmit.intelligentbus.repository.BusRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Basic repository test for buses.
 */
public class BusRepositoryTest {

    @TempDir
    Path tempDir;

    @Test
    void addAndUpdateBusShouldWork() {
        BusRepository repository = new BusRepository(tempDir.resolve("buses.json"));
        Bus bus = new Bus("12345678", 60, 80.0, "Diesel");
        repository.add(bus);

        Bus updated = new Bus("12345678", 55, 75.0, "Diesel");
        repository.update("12345678", updated);

        assertEquals(1, repository.count());
        assertEquals(55, repository.retrieve("12345678").getCapacity());
    }

    @Test
    void increasingCapacityShouldBeRejected() {
        BusRepository repository = new BusRepository(tempDir.resolve("buses.json"));
        repository.add(new Bus("12345678", 60, 80.0, "Diesel"));

        Bus updated = new Bus("12345678", 70, 80.0, "Diesel");
        assertThrows(RuntimeException.class, () -> repository.update("12345678", updated));
    }
}
