package com.rmit.intelligentbus.repository;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.rmit.intelligentbus.model.Driver;
import com.rmit.intelligentbus.service.DomainValidationException;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.ResolverStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Stores and manages drivers in a JSON file.
 */
public class DriverRepository {
    private static final List<String> ALLOWED_LICENSES = List.of("Light", "Medium", "Heavy", "PublicTransport");
    private static final DateTimeFormatter BIRTHDATE_FORMAT =
            new DateTimeFormatterBuilder()
                    .appendPattern("dd-MM-uuuu")
                    .toFormatter()
                    .withResolverStyle(ResolverStyle.STRICT);

    private final Path storageFile;
    private final Gson gson;
    private final Type listType = new TypeToken<List<Driver>>() {}.getType();

    public DriverRepository() {
        this(Path.of("data", "drivers.json"));
    }

    public DriverRepository(Path storageFile) {
        this.storageFile = Objects.requireNonNull(storageFile, "storageFile");
        this.gson = new GsonBuilder().setPrettyPrinting().create();
    }

    public synchronized void add(Driver driver) {
        validateForAdd(driver);
        List<Driver> drivers = loadAll();
        if (findIndexById(drivers, driver.getDriverID()) >= 0) {
            throw new DomainValidationException("Duplicate driver ID is not allowed.");
        }
        drivers.add(new Driver(driver));
        saveAll(drivers);
    }

    public synchronized Driver retrieve(String driverID) {
        ensureDriverIdPresent(driverID);
        return loadAll().stream()
                .filter(driver -> driverID.equals(driver.getDriverID()))
                .findFirst()
                .map(Driver::new)
                .orElse(null);
    }

    public synchronized List<Driver> retrieveAll() {
        List<Driver> copy = new ArrayList<>();
        for (Driver driver : loadAll()) {
            copy.add(new Driver(driver));
        }
        return copy;
    }

    public synchronized void update(String driverID, Driver updatedDriver) {
        ensureDriverIdPresent(driverID);
        Objects.requireNonNull(updatedDriver, "updatedDriver");

        List<Driver> drivers = loadAll();
        int index = findIndexById(drivers, driverID);
        if (index < 0) {
            throw new DomainValidationException("Driver not found.");
        }

        Driver existing = drivers.get(index);

        if (!driverID.equals(updatedDriver.getDriverID())) {
            throw new DomainValidationException("driverID cannot be modified.");
        }
        if (!Objects.equals(existing.getName(), updatedDriver.getName())) {
            throw new DomainValidationException("name cannot be modified.");
        }
        if (existing.getExperienceYears() > 10
                && !Objects.equals(existing.getLicenseType(), updatedDriver.getLicenseType())) {
            throw new DomainValidationException("licenseType cannot be changed for experienced drivers.");
        }

        validateDriverFields(updatedDriver, false);

        drivers.set(index, new Driver(updatedDriver));
        saveAll(drivers);
    }

    public synchronized int count() {
        return loadAll().size();
    }

    public synchronized boolean exists(String driverID) {
        return retrieve(driverID) != null;
    }

    private void validateForAdd(Driver driver) {
        Objects.requireNonNull(driver, "driver");
        validateDriverFields(driver, true);
    }

    private void validateDriverFields(Driver driver, boolean includeIdUniquenessCheck) {
        if (driver.getDriverID() == null || driver.getDriverID().isBlank()) {
            throw new DomainValidationException("driverID is required.");
        }
        if (includeIdUniquenessCheck && !isValidDriverID(driver.getDriverID())) {
            throw new DomainValidationException("driverID must be 10 characters, start with 2-9, contain at least two special characters, and end with two uppercase letters.");
        }

        if (driver.getName() == null || driver.getName().isBlank()) {
            throw new DomainValidationException("name is required.");
        }

        if (driver.getExperienceYears() < 0) {
            throw new DomainValidationException("experienceYears cannot be negative.");
        }

        if (driver.getLicenseType() == null || !ALLOWED_LICENSES.contains(driver.getLicenseType())) {
            throw new DomainValidationException("licenseType is invalid.");
        }

        if (!isValidAddress(driver.getAddress())) {
            throw new DomainValidationException("address must follow Street Number|Street Name|City|State|Country.");
        }

        if (!isValidBirthdate(driver.getBirthdate())) {
            throw new DomainValidationException("birthdate must follow DD-MM-YYYY.");
        }
    }

    private boolean isValidDriverID(String driverID) {
        if (driverID.length() != 10) {
            return false;
        }
        if (!Character.isDigit(driverID.charAt(0)) || !Character.isDigit(driverID.charAt(1))) {
            return false;
        }
        if (driverID.charAt(0) < '2' || driverID.charAt(0) > '9') {
            return false;
        }
        if (driverID.charAt(1) < '2' || driverID.charAt(1) > '9') {
            return false;
        }
        if (!Character.isUpperCase(driverID.charAt(8)) || !Character.isUpperCase(driverID.charAt(9))) {
            return false;
        }

        int specialCount = 0;
        for (int i = 2; i <= 7; i++) {
            char c = driverID.charAt(i);
            if (!Character.isLetterOrDigit(c)) {
                specialCount++;
            }
        }
        return specialCount >= 2;
    }

    private boolean isValidAddress(String address) {
        if (address == null || address.isBlank()) {
            return false;
        }
        String[] parts = address.split("\\|", -1);
        if (parts.length != 5) {
            return false;
        }
        for (String part : parts) {
            if (part == null || part.isBlank()) {
                return false;
            }
        }
        return true;
    }

    private boolean isValidBirthdate(String birthdate) {
        if (birthdate == null || birthdate.isBlank()) {
            return false;
        }
        try {
            LocalDate.parse(birthdate, BIRTHDATE_FORMAT);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    public int getAge(String birthdate) {
        LocalDate dob = LocalDate.parse(birthdate, BIRTHDATE_FORMAT);
        return Period.between(dob, LocalDate.now()).getYears();
    }

    private List<Driver> loadAll() {
        try {
            if (!Files.exists(storageFile) || Files.size(storageFile) == 0) {
                return new ArrayList<>();
            }
            try (Reader reader = Files.newBufferedReader(storageFile, StandardCharsets.UTF_8)) {
                List<Driver> drivers = gson.fromJson(reader, listType);
                return drivers == null ? new ArrayList<>() : new ArrayList<>(drivers);
            }
        } catch (IOException ex) {
            throw new DomainValidationException("Unable to read driver file: " + ex.getMessage());
        }
    }

    private void saveAll(List<Driver> drivers) {
        try {
            Path parent = storageFile.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            try (Writer writer = Files.newBufferedWriter(storageFile, StandardCharsets.UTF_8)) {
                gson.toJson(drivers, writer);
            }
        } catch (IOException ex) {
            throw new DomainValidationException("Unable to write driver file: " + ex.getMessage());
        }
    }

    private int findIndexById(List<Driver> drivers, String driverID) {
        for (int i = 0; i < drivers.size(); i++) {
            if (driverID.equals(drivers.get(i).getDriverID())) {
                return i;
            }
        }
        return -1;
    }

    private void ensureDriverIdPresent(String driverID) {
        if (driverID == null || driverID.isBlank()) {
            throw new DomainValidationException("driverID is required.");
        }
    }
}
