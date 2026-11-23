package com.bankingsystem.persistence;

import com.bankingsystem.domain.Customer;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public class CustomerDAO {
    public Customer create(Customer customer) throws SQLException {
        String sql = "INSERT INTO customers(customer_id,first_name,surname,address,phone_number,email,employed,employer_name,employer_address,created_at) VALUES(?,?,?,?,?,?,?,?,?,?)";
        try (Connection c = Database.getConnection(); PreparedStatement p = c.prepareStatement(sql)) {
            bindCustomer(p, customer);
            p.executeUpdate();
        }
        return customer;
    }

    public Optional<Customer> findById(String id) throws SQLException {
        try (Connection c = Database.getConnection(); PreparedStatement p = c.prepareStatement("SELECT * FROM customers WHERE customer_id=?")) {
            p.setString(1, id);
            try (ResultSet rs = p.executeQuery()) { return rs.next() ? Optional.of(map(rs)) : Optional.empty(); }
        }
    }

    public long count() throws SQLException {
        try (Connection c = Database.getConnection(); Statement s = c.createStatement(); ResultSet rs = s.executeQuery("SELECT COUNT(*) FROM customers")) {
            return rs.next() ? rs.getLong(1) : 0;
        }
    }

    public static Customer map(ResultSet rs) throws SQLException {
        return new Customer(rs.getString("customer_id"), rs.getString("first_name"), rs.getString("surname"),
                rs.getString("address"), rs.getString("phone_number"), rs.getString("email"),
                rs.getInt("employed") == 1, rs.getString("employer_name"), rs.getString("employer_address"));
    }

    private static void bindCustomer(PreparedStatement p, Customer x) throws SQLException {
        p.setString(1, x.getCustomerId()); p.setString(2, x.getFirstName()); p.setString(3, x.getSurname());
        p.setString(4, x.getAddress()); p.setString(5, x.getPhoneNumber()); p.setString(6, x.getEmail());
        p.setInt(7, x.isEmployed() ? 1 : 0); p.setString(8, x.getEmployerName()); p.setString(9, x.getEmployerAddress());
        p.setString(10, LocalDateTime.now().toString());
    }
}
