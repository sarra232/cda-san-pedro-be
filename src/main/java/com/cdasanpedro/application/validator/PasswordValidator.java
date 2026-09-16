package com.cdasanpedro.application.validator;

import com.cdasanpedro.core.exception.BusinessException;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

@Component
public class PasswordValidator {

    private static final int MIN_LENGTH = 8;
    private static final Pattern HAS_UPPERCASE = Pattern.compile("[A-Z]");
    private static final Pattern HAS_SPECIAL_CHAR = Pattern.compile("[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?~`^]");

    public void validate(String password, String confirmPassword) {
        if (password == null || password.isBlank()) {
            throw new BusinessException("La contraseña no puede estar vacía");
        }

        if (confirmPassword == null || confirmPassword.isBlank()) {
            throw new BusinessException("Por favor confirme su nueva contraseña");
        }

        if (!password.equals(confirmPassword)) {
            throw new BusinessException("Las contraseñas ingresadas no coinciden");
        }

        if (password.length() < MIN_LENGTH) {
            throw new BusinessException("La contraseña debe tener al menos " + MIN_LENGTH + " caracteres");
        }

        if (!HAS_UPPERCASE.matcher(password).find()) {
            throw new BusinessException("La contraseña debe contener al menos 1 letra mayúscula (A-Z)");
        }

        if (!HAS_SPECIAL_CHAR.matcher(password).find()) {
            throw new BusinessException("La contraseña debe contener al menos 1 caracter especial (!@#$%^&*...)");
        }
    }

    public boolean isValid(String password) {
        if (password == null || password.length() < MIN_LENGTH) {
            return false;
        }
        return HAS_UPPERCASE.matcher(password).find() && HAS_SPECIAL_CHAR.matcher(password).find();
    }
}
