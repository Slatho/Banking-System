package com.bankingsystem.persistence;

import com.bankingsystem.domain.Customer;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.UUID;


public class RegistrationDAO {
    public void register(Customer customer, String username, String passwordHash) throws SQLException {
        String customerSql = "INSERT INTO customers(customer_id,first_name,surname,address,phone_number,email,employed,employer_name,employer_address,created_at) VALUES(?,?,?,?,?,?,?,?,?,?)";
        String loginSql = "INSERT INTO user_credentials(user_id,username,password_hash,role,customer_id,active,created_at) VALUES(?,?,?,'CUSTOMER',?,1,?)";
        try (Connection c = Database.getConnection()) {
            c.setAutoCommit(false);
            try {
                try (PreparedStatement p = c.prepareStatement(customerSql)) {
                    p.setString(1, customer.getCustomerId()); p.setString(2, customer.getFirstName()); p.setString(3, customer.getSurname());
                    p.setString(4, customer.getAddress()); p.setString(5, customer.getPhoneNumber()); p.setString(6, customer.getEmail());
                    p.setInt(7, customer.isEmployed() ? 1 : 0); p.setString(8, customer.getEmployerName()); p.setString(9, customer.getEmployerAddress());
                    p.setString(10, LocalDateTime.now().toString()); p.executeUpdate();
                }
                try (PreparedStatement p = c.prepareStatement(loginSql)) {
                    p.setString(1, UUID.randomUUID().toString()); p.setString(2, username); p.setString(3, passwordHash);
                    p.setString(4, customer.getCustomerId()); p.setString(5, LocalDateTime.now().toString()); p.executeUpdate();
                }
                c.commit();
            } catch (SQLException | RuntimeException ex) {
                try {
                    c.rollback();
                } catch (SQLException rollbackFailure) {
                    ex.addSuppressed(rollbackFailure);
                }
                throw ex;
            }
        }
    }
}
