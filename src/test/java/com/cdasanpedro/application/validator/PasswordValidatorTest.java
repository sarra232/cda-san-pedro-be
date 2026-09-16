package com.cdasanpedro.application.validator;

import com.cdasanpedro.core.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PasswordValidatorTest {

    private PasswordValidator validator;

    @BeforeEach
    void setUp() {
        validator = new PasswordValidator();
    }

    @Test
    @DisplayName("Debe aceptar una contraseña válida con >=8 caracteres, 1 mayúscula y 1 caracter especial")
    void validate_Success() {
        assertDoesNotThrow(() -> validator.validate("Segura123*", "Segura123*"));
        assertTrue(validator.isValid("Segura123*"));
    }

    @Test
    @DisplayName("Debe fallar si las contraseñas no coinciden")
    void validate_Mismatch() {
        BusinessException ex = assertThrows(BusinessException.class, 
                () -> validator.validate("Segura123*", "OtraPassword123*"));
        assertTrue(ex.getMessage().contains("no coinciden"));
    }

    @Test
    @DisplayName("Debe fallar si tiene menos de 8 caracteres")
    void validate_LessThan8Chars() {
        BusinessException ex = assertThrows(BusinessException.class, 
                () -> validator.validate("Abc1*", "Abc1*"));
        assertTrue(ex.getMessage().contains("al menos 8 caracteres"));
        assertFalse(validator.isValid("Abc1*"));
    }

    @Test
    @DisplayName("Debe fallar si no tiene letras mayúsculas")
    void validate_NoUppercase() {
        BusinessException ex = assertThrows(BusinessException.class, 
                () -> validator.validate("segura123*", "segura123*"));
        assertTrue(ex.getMessage().contains("mayúscula"));
        assertFalse(validator.isValid("segura123*"));
    }

    @Test
    @DisplayName("Debe fallar si no tiene caracteres especiales")
    void validate_NoSpecialChar() {
        BusinessException ex = assertThrows(BusinessException.class, 
                () -> validator.validate("Segura12345", "Segura12345"));
        assertTrue(ex.getMessage().contains("caracter especial"));
        assertFalse(validator.isValid("Segura12345"));
    }

    @Test
    @DisplayName("Debe fallar con contraseñas nulas o vacías")
    void validate_NullOrEmpty() {
        assertThrows(BusinessException.class, () -> validator.validate("", ""));
        assertThrows(BusinessException.class, () -> validator.validate(null, null));
    }
}
