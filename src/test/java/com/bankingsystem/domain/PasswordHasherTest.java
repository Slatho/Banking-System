package com.bankingsystem.domain;

import com.bankingsystem.security.PasswordHasher;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PasswordHasherTest {
    @Test void correctPasswordVerifiesAndWrongPasswordDoesNot() {
        String hash = PasswordHasher.hash("ProjectPass@2026".toCharArray());
        assertTrue(PasswordHasher.verify("ProjectPass@2026".toCharArray(), hash));
        assertFalse(PasswordHasher.verify("incorrect".toCharArray(), hash));
        assertFalse(hash.contains("ProjectPass@2026"));
    }
}
