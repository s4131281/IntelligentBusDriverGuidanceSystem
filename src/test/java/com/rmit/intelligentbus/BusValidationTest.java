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
 * Unit tests for bus rules B1 to B5.
 */
public class BusValidationTest {

    @TempDir
    Path tempDir;

    private BusRepository repository() {
        return new BusRepository(tempDir.resolve("buses.json"));
    }

    private Bus validBus() {
        return new Bus("12345678", 60, 80.0, "Diesel");
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
    void validBusShouldBeAddedSuccessfully() {
        BusRepository repository = repository();
        repository.add(validBus());

        assertEquals(1, repository.count());
        assertNotNull(repository.retrieve("12345678"));
    }

    @Test
    void busIdMustBeExactlyEightDigits() {
        BusRepository repository = repository();
        Bus invalid = new Bus("1234567", 60, 80.0, "Diesel");

        assertThrows(DomainValidationException.class, () -> repository.add(invalid));
    }

    @Test
    void busIdMustRejectLetters() {
        BusRepository repository = repository();
        Bus invalid = new Bus("1234AB78", 60, 80.0, "Diesel");

        assertThrows(DomainValidationException.class, () -> repository.add(invalid));
    }

    @Test
    void duplicateBusIdShouldBeRejected() {
        BusRepository repository = repository();
        repository.add(validBus());

        Bus duplicate = new Bus("12345678", 55, 75.0, "Hybrid");
        assertThrows(DomainValidationException.class, () -> repository.add(duplicate));
    }

    @Test
    void capacityCanDecreaseDuringUpdate() {
        BusRepository repository = repository();
        repository.add(validBus());

        Bus updated = new Bus("12345678", 50, 70.0, "Diesel");
        assertDoesNotThrow(() -> repository.update("12345678", updated));
        assertEquals(50, repository.retrieve("12345678").getCapacity());
    }

    @Test
    void capacityIncreaseShouldBeRejected() {
        BusRepository repository = repository();
        repository.add(validBus());

        Bus updated = new Bus("12345678", 70, 70.0, "Diesel");
        assertThrows(DomainValidationException.class, () -> repository.update("12345678", updated));
    }

    @Test
    void capacitySameShouldBeAllowed() {
        BusRepository repository = repository();
        repository.add(validBus());

        Bus updated = new Bus("12345678", 60, 65.0, "Diesel");
        assertDoesNotThrow(() -> repository.update("12345678", updated));
        assertEquals(60, repository.retrieve("12345678").getCapacity());
    }

    @Test
    void olderDriverShouldNotDriveLargeBus() {
        BusRepository repository = repository();
        Bus largeBus = new Bus("12345678", 50, 80.0, "Diesel");
        Driver olderDriver = validDriver("Heavy", 12, "01-01-1960");

        assertThrows(DomainValidationException.class, () -> repository.validateDriverForBus(olderDriver, largeBus));
    }

    @Test
    void olderDriverShouldDriveSmallBus() {
        BusRepository repository = repository();
        Bus smallBus = new Bus("12345678", 49, 80.0, "Diesel");
        Driver olderDriver = validDriver("Heavy", 12, "01-01-1960");

        assertDoesNotThrow(() -> repository.validateDriverForBus(olderDriver, smallBus));
    }

    @Test
    void youngerDriverShouldDriveLargeBus() {
        BusRepository repository = repository();
        Bus largeBus = new Bus("12345678", 50, 80.0, "Diesel");
        Driver youngerDriver = validDriver("Heavy", 12, "01-01-1990");

        assertDoesNotThrow(() -> repository.validateDriverForBus(youngerDriver, largeBus));
    }

    @Test
    void electricBusShouldRejectDriverWithLessThanFiveYearsExperience() {
        BusRepository repository = repository();
        Bus electricBus = new Bus("12345678", 40, 90.0, "Electricity");
        Driver driver = validDriver("Heavy", 4, "01-01-1990");

        assertThrows(DomainValidationException.class, () -> repository.validateDriverForBus(driver, electricBus));
    }

    @Test
    void electricBusShouldAllowDriverWithFiveYearsExperienceAndHeavyLicence() {
        BusRepository repository = repository();
        Bus electricBus = new Bus("12345678", 40, 90.0, "Electricity");
        Driver driver = validDriver("Heavy", 5, "01-01-1990");

        assertDoesNotThrow(() -> repository.validateDriverForBus(driver, electricBus));
    }

    @Test
    void electricBusShouldAllowDriverWithFiveYearsExperienceAndPublicTransportLicence() {
        BusRepository repository = repository();
        Bus electricBus = new Bus("12345678", 40, 90.0, "Electricity");
        Driver driver = validDriver("PublicTransport", 5, "01-01-1990");

        assertDoesNotThrow(() -> repository.validateDriverForBus(driver, electricBus));
    }

    @Test
    void hybridBusShouldRejectLightLicence() {
        BusRepository repository = repository();
        Bus hybridBus = new Bus("12345678", 40, 90.0, "Hybrid");
        Driver driver = validDriver("Light", 8, "01-01-1990");

        assertThrows(DomainValidationException.class, () -> repository.validateDriverForBus(driver, hybridBus));
    }

    @Test
    void hybridBusShouldAllowHeavyLicence() {
        BusRepository repository = repository();
        Bus hybridBus = new Bus("12345678", 40, 90.0, "Hybrid");
        Driver driver = validDriver("Heavy", 8, "01-01-1990");

        assertDoesNotThrow(() -> repository.validateDriverForBus(driver, hybridBus));
    }
}
