package com.bankingsystem.domain;

import java.util.Objects;

public class Customer {
    private final String customerId;
    private final String firstName;
    private final String surname;
    private final String address;
    private final String phoneNumber;
    private final String email;
    private final boolean employed;
    private final String employerName;
    private final String employerAddress;

    public Customer(String customerId, String firstName, String surname, String address,
                    String phoneNumber, String email, boolean employed,
                    String employerName, String employerAddress) {
        this.customerId = requireText(customerId, "Customer ID");
        this.firstName = requireText(firstName, "First name");
        this.surname = requireText(surname, "Surname");
        this.address = requireText(address, "Address");
        this.phoneNumber = requireText(phoneNumber, "Phone number");
        this.email = requireText(email, "Email");
        this.employed = employed;
        this.employerName = blankToNull(employerName);
        this.employerAddress = blankToNull(employerAddress);
        if (employed && (this.employerName == null || this.employerAddress == null)) {
            throw new IllegalArgumentException("Employer name and address are required for an employed customer.");
        }
    }

    private static String requireText(String value, String label) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(label + " is required.");
        return value.trim();
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    public String getFullName() { return firstName + " " + surname; }
    public String getCustomerId() { return customerId; }
    public String getFirstName() { return firstName; }
    public String getSurname() { return surname; }
    public String getAddress() { return address; }
    public String getPhoneNumber() { return phoneNumber; }
    public String getEmail() { return email; }
    public boolean isEmployed() { return employed; }
    public String getEmployerName() { return employerName; }
    public String getEmployerAddress() { return employerAddress; }

    @Override public boolean equals(Object other) {
        return other instanceof Customer c && customerId.equals(c.customerId);
    }
    @Override public int hashCode() { return Objects.hash(customerId); }
}
