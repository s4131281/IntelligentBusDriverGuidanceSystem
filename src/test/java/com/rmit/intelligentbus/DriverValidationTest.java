package com.rmit.intelligentbus;

import com.rmit.intelligentbus.model.Driver;
import com.rmit.intelligentbus.repository.DriverRepository;
import com.rmit.intelligentbus.service.DomainValidationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for driver rules D1 to D5.
 */
public class DriverValidationTest {

    @TempDir
    Path tempDir;

    private DriverRepository repository() {
        return new DriverRepository(tempDir.resolve("drivers.json"));
    }

    private Driver validDriver() {
        return new Driver(
                "23@#45ABCD",
                "Aman",
                8,
                "Heavy",
                "12|King Street|Melbourne|VIC|Australia",
                "12-05-1990"
        );
    }

    @Test
    void validDriverShouldBeAddedSuccessfully() {
        DriverRepository repository = repository();
        repository.add(validDriver());

        assertEquals(1, repository.count());
        assertNotNull(repository.retrieve("23@#45ABCD"));
    }

    @Test
    void driverIdMustStartWithDigitsBetweenTwoAndNine() {
        DriverRepository repository = repository();
        Driver invalid = new Driver(
                "13@#45ABCD",
                "Aman",
                8,
                "Heavy",
                "12|King Street|Melbourne|VIC|Australia",
                "12-05-1990"
        );

        assertThrows(DomainValidationException.class, () -> repository.add(invalid));
    }

    @Test
    void driverIdMustHaveAtLeastTwoSpecialCharacters() {
        DriverRepository repository = repository();
        Driver invalid = new Driver(
                "23A345ABCD",
                "Aman",
                8,
                "Heavy",
                "12|King Street|Melbourne|VIC|Australia",
                "12-05-1990"
        );

        assertThrows(DomainValidationException.class, () -> repository.add(invalid));
    }

    @Test
    void driverIdMustHaveExactlyTenCharacters() {
        DriverRepository repository = repository();
        Driver invalid = new Driver(
                "23@#45ABC",
                "Aman",
                8,
                "Heavy",
                "12|King Street|Melbourne|VIC|Australia",
                "12-05-1990"
        );

        assertThrows(DomainValidationException.class, () -> repository.add(invalid));
    }

    @Test
    void driverIdMustEndWithTwoUppercaseLetters() {
        DriverRepository repository = repository();
        Driver invalid = new Driver(
                "23@#45ABcD",
                "Aman",
                8,
                "Heavy",
                "12|King Street|Melbourne|VIC|Australia",
                "12-05-1990"
        );

        assertThrows(DomainValidationException.class, () -> repository.add(invalid));
    }

    @Test
    void addressMustContainFivePipeSeparatedParts() {
        DriverRepository repository = repository();
        Driver invalid = new Driver(
                "23@#45ABCD",
                "Aman",
                8,
                "Heavy",
                "12|King Street|Melbourne|VIC",
                "12-05-1990"
        );

        assertThrows(DomainValidationException.class, () -> repository.add(invalid));
    }

    @Test
    void addressMustRejectBlankPart() {
        DriverRepository repository = repository();
        Driver invalid = new Driver(
                "23@#45ABCD",
                "Aman",
                8,
                "Heavy",
                "12| |Melbourne|VIC|Australia",
                "12-05-1990"
        );

        assertThrows(DomainValidationException.class, () -> repository.add(invalid));
    }

    @Test
    void validBirthdateShouldBeAccepted() {
        DriverRepository repository = repository();
        Driver driver = validDriver();

        repository.add(driver);
        assertEquals(1, repository.count());
    }

    @Test
    void invalidBirthdateShouldBeRejected() {
        DriverRepository repository = repository();
        Driver invalid = new Driver(
                "23@#45ABCD",
                "Aman",
                8,
                "Heavy",
                "12|King Street|Melbourne|VIC|Australia",
                "2020-05-12"
        );

        assertThrows(DomainValidationException.class, () -> repository.add(invalid));
    }

    @Test
    void updateMustRejectChangingLicenseForExperiencedDriver() {
        DriverRepository repository = repository();
        repository.add(new Driver(
                "23@#45ABCD",
                "Aman",
                11,
                "Heavy",
                "12|King Street|Melbourne|VIC|Australia",
                "12-05-1990"
        ));

        Driver updated = new Driver(
                "23@#45ABCD",
                "Aman",
                11,
                "Light",
                "12|King Street|Melbourne|VIC|Australia",
                "12-05-1990"
        );

        assertThrows(DomainValidationException.class, () -> repository.update("23@#45ABCD", updated));
    }

    @Test
    void updateShouldAllowLicenseChangeWhenExperienceIsExactlyTen() {
        DriverRepository repository = repository();
        repository.add(new Driver(
                "23@#45ABCD",
                "Aman",
                10,
                "Heavy",
                "12|King Street|Melbourne|VIC|Australia",
                "12-05-1990"
        ));

        Driver updated = new Driver(
                "23@#45ABCD",
                "Aman",
                10,
                "Light",
                "12|King Street|Melbourne|VIC|Australia",
                "12-05-1990"
        );

        assertDoesNotThrow(() -> repository.update("23@#45ABCD", updated));
        assertEquals("Light", repository.retrieve("23@#45ABCD").getLicenseType());
    }

    @Test
    void updateMustRejectChangingDriverId() {
        DriverRepository repository = repository();
        repository.add(validDriver());

        Driver updated = new Driver(
                "29@#45ABCD",
                "Aman",
                8,
                "Heavy",
                "12|King Street|Melbourne|VIC|Australia",
                "12-05-1990"
        );

        assertThrows(DomainValidationException.class, () -> repository.update("23@#45ABCD", updated));
    }

    @Test
    void updateMustRejectChangingName() {
        DriverRepository repository = repository();
        repository.add(validDriver());

        Driver updated = new Driver(
                "23@#45ABCD",
                "Rahul",
                8,
                "Heavy",
                "12|King Street|Melbourne|VIC|Australia",
                "12-05-1990"
        );

        assertThrows(DomainValidationException.class, () -> repository.update("23@#45ABCD", updated));
    }

    @Test
    void updateShouldAllowOtherChangesWhenImmutableFieldsStaySame() {
        DriverRepository repository = repository();
        repository.add(validDriver());

        Driver updated = new Driver(
                "23@#45ABCD",
                "Aman",
                9,
                "Heavy",
                "34|Queen Street|Sydney|NSW|Australia",
                "13-05-1990"
        );

        assertDoesNotThrow(() -> repository.update("23@#45ABCD", updated));
        Driver saved = repository.retrieve("23@#45ABCD");
        assertEquals(9, saved.getExperienceYears());
        assertEquals("34|Queen Street|Sydney|NSW|Australia", saved.getAddress());
        assertEquals("13-05-1990", saved.getBirthdate());
    }

    @Test
    void duplicateDriverIdShouldBeRejected() {
        DriverRepository repository = repository();
        repository.add(validDriver());

        Driver duplicate = new Driver(
                "23@#45ABCD",
                "Karan",
                5,
                "Medium",
                "14|Station Road|Melbourne|VIC|Australia",
                "01-01-1995"
        );

        assertThrows(DomainValidationException.class, () -> repository.add(duplicate));
    }
}
