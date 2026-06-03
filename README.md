# Intelligent Bus Driver Guidance System

This is a Maven-based Java implementation for Assignment 4 Activity 1.2.

## Run locally
```bash
mvn test
```

## Storage
The repositories store data in human-readable JSON files under `data/`.

## Project Setup

This repository contains the Maven-based Java implementation for the Intelligent Bus Driver Guidance System.

The project includes:
- Driver and Bus classes
- DriverRepository and BusRepository classes
- JSON file storage for drivers and buses
- JUnit 5 unit and integration tests
- GitHub Actions workflow to automatically run Maven tests on push and pull request

## My Contribution s4131281

* Uploaded the Maven Java project to GitHub.
* Added the GitHub Actions workflow file for automatic Maven test execution.
* Verified that the GitHub Actions workflow runs successfully.
* Reviewed the Activity 1 test case plan and suggested corrections for test case consistency, validation rules, and expected/actual results.
* Provided feedback to improve the implementation logic for driver and bus conditions before final submission.
* Me and my team members together did  User Story and Acceptance Criteria 

## My Contributions s4192792
* Implemented the core classes: Driver, Bus, DriverRepository, and BusRepository with full functionality.
* Developed strong validation logic for all Driver conditions (D1–D5) and Bus conditions (B1–B5).
* Created 18 unit tests for Driver class covering normal, edge, and invalid cases.
* Created 17 unit tests for Bus class focusing on all business rules and restrictions.
* Wrote 5 integration tests each for Driver and Bus operations using real JSON file persistence.
* Documented all test cases in the Test Case Table.
* Configured Maven project with JUnit 5 and implemented JSON-based data storage.