package com.rmit.intelligentbus.repository;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.rmit.intelligentbus.model.Bus;
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
 * Stores and manages buses in a JSON file.
 */
public class BusRepository {
    private static final DateTimeFormatter BIRTHDATE_FORMAT =
            new DateTimeFormatterBuilder()
                    .appendPattern("dd-MM-uuuu")
                    .toFormatter()
                    .withResolverStyle(ResolverStyle.STRICT);

    private final Path storageFile;
    private final Gson gson;
    private final Type listType = new TypeToken<List<Bus>>() {}.getType();

    public BusRepository() {
        this(Path.of("data", "buses.json"));
    }

    public BusRepository(Path storageFile) {
        this.storageFile = Objects.requireNonNull(storageFile, "storageFile");
        this.gson = new GsonBuilder().setPrettyPrinting().create();
    }

    public synchronized void add(Bus bus) {
        validateForAdd(bus);
        List<Bus> buses = loadAll();
        if (findIndexById(buses, bus.getBusID()) >= 0) {
            throw new DomainValidationException("Duplicate bus ID is not allowed.");
        }
        buses.add(new Bus(bus));
        saveAll(buses);
    }

    public synchronized Bus retrieve(String busID) {
        ensureBusIdPresent(busID);
        return loadAll().stream()
                .filter(bus -> busID.equals(bus.getBusID()))
                .findFirst()
                .map(Bus::new)
                .orElse(null);
    }

    public synchronized List<Bus> retrieveAll() {
        List<Bus> copy = new ArrayList<>();
        for (Bus bus : loadAll()) {
            copy.add(new Bus(bus));
        }
        return copy;
    }

    public synchronized void update(String busID, Bus updatedBus) {
        ensureBusIdPresent(busID);
        Objects.requireNonNull(updatedBus, "updatedBus");

        List<Bus> buses = loadAll();
        int index = findIndexById(buses, busID);
        if (index < 0) {
            throw new DomainValidationException("Bus not found.");
        }

        Bus existing = buses.get(index);

        if (!busID.equals(updatedBus.getBusID())) {
            throw new DomainValidationException("busID cannot be modified.");
        }
        if (updatedBus.getCapacity() > existing.getCapacity()) {
            throw new DomainValidationException("busCapacity cannot increase during update.");
        }

        validateBusFields(updatedBus);

        buses.set(index, new Bus(updatedBus));
        saveAll(buses);
    }

    public synchronized int count() {
        return loadAll().size();
    }

    private void validateForAdd(Bus bus) {
        Objects.requireNonNull(bus, "bus");
        validateBusFields(bus);
    }

    private void validateBusFields(Bus bus) {
        if (bus.getBusID() == null || !bus.getBusID().matches("\\d{8}")) {
            throw new DomainValidationException("busID must be exactly 8 digits.");
        }
        if (bus.getCapacity() < 0) {
            throw new DomainValidationException("capacity cannot be negative.");
        }
        if (bus.getFuelLevel() < 0 || bus.getFuelLevel() > 100) {
            throw new DomainValidationException("fuelLevel must be between 0 and 100.");
        }
        if (bus.getFuelType() == null
                || !(bus.getFuelType().equals("Diesel")
                || bus.getFuelType().equals("Hybrid")
                || bus.getFuelType().equals("Electricity"))) {
            throw new DomainValidationException("fuelType is invalid.");
        }
    }

    private List<Bus> loadAll() {
        try {
            if (!Files.exists(storageFile) || Files.size(storageFile) == 0) {
                return new ArrayList<>();
            }
            try (Reader reader = Files.newBufferedReader(storageFile, StandardCharsets.UTF_8)) {
                List<Bus> buses = gson.fromJson(reader, listType);
                return buses == null ? new ArrayList<>() : new ArrayList<>(buses);
            }
        } catch (IOException ex) {
            throw new DomainValidationException("Unable to read bus file: " + ex.getMessage());
        }
    }

    private void saveAll(List<Bus> buses) {
        try {
            Path parent = storageFile.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            try (Writer writer = Files.newBufferedWriter(storageFile, StandardCharsets.UTF_8)) {
                gson.toJson(buses, writer);
            }
        } catch (IOException ex) {
            throw new DomainValidationException("Unable to write bus file: " + ex.getMessage());
        }
    }

    private int findIndexById(List<Bus> buses, String busID) {
        for (int i = 0; i < buses.size(); i++) {
            if (busID.equals(buses.get(i).getBusID())) {
                return i;
            }
        }
        return -1;
    }

    private void ensureBusIdPresent(String busID) {
        if (busID == null || busID.isBlank()) {
            throw new DomainValidationException("busID is required.");
        }
    }

    public void validateDriverForBus(Driver driver, Bus bus) {
        Objects.requireNonNull(driver, "driver");
        Objects.requireNonNull(bus, "bus");

        int age = computeAge(driver.getBirthdate());
        String license = driver.getLicenseType();
        String fuel = bus.getFuelType();

        if (age > 50 && bus.getCapacity() >= 50) {
            throw new DomainValidationException("Drivers older than 50 cannot drive buses with capacity 50 or more.");
        }

        if ("Electricity".equals(fuel)) {
            if (driver.getExperienceYears() < 5) {
                throw new DomainValidationException("Electric buses require at least 5 years of experience.");
            }
            if (!isAllowedHighPrivilegeLicence(license)) {
                throw new DomainValidationException("Electric buses require Heavy or PublicTransport licence.");
            }
        }

        if ("Hybrid".equals(fuel)) {
            if (!isAllowedHighPrivilegeLicence(license)) {
                throw new DomainValidationException("Hybrid buses require Heavy or PublicTransport licence.");
            }
        }
    }

    private int computeAge(String birthdate) {
        if (birthdate == null || birthdate.isBlank()) {
            throw new DomainValidationException("Invalid birthdate for age check.");
        }
        try {
            LocalDate dob = LocalDate.parse(birthdate, BIRTHDATE_FORMAT);
            return Period.between(dob, LocalDate.now()).getYears();
        } catch (Exception ex) {
            throw new DomainValidationException("Invalid birthdate for age check.");
        }
    }

    private boolean isAllowedHighPrivilegeLicence(String license) {
        return "Heavy".equals(license) || "PublicTransport".equals(license);
    }
}
