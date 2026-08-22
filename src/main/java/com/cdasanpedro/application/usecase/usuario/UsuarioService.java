package com.cdasanpedro.application.usecase.usuario;

import com.cdasanpedro.application.dto.usuario.CreateUsuarioDto;
import com.cdasanpedro.application.dto.usuario.UpdateUsuarioDto;
import com.cdasanpedro.application.dto.usuario.UsuarioDto;
import com.cdasanpedro.core.exception.BusinessException;
import com.cdasanpedro.core.exception.ResourceNotFoundException;
import com.cdasanpedro.infrastructure.persistence.entity.UsuarioEntity;
import com.cdasanpedro.infrastructure.persistence.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public List<UsuarioDto> listarTodos() {
        return usuarioRepository.findAll().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public UsuarioDto obtenerPorId(UUID id) {
        UsuarioEntity entity = usuarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con ID: " + id));
        return mapToDto(entity);
    }

    @Transactional
    public UsuarioDto crearUsuario(CreateUsuarioDto dto) {
        String numDoc = dto.getNumeroDocumento().trim();
        if (usuarioRepository.existsByNumeroDocumento(numDoc)) {
            throw new BusinessException("Ya existe un usuario registrado con el documento: " + numDoc);
        }

        UsuarioEntity entity = UsuarioEntity.builder()
                .tipoDocumento(dto.getTipoDocumento())
                .numeroDocumento(numDoc)
                .nombresApellidos(dto.getNombresApellidos().trim())
                .passwordHash(passwordEncoder.encode(dto.getPassword()))
                .rol(dto.getRol())
                .activo(true)
                .build();

        UsuarioEntity saved = usuarioRepository.save(entity);
        return mapToDto(saved);
    }

    @Transactional
    public UsuarioDto actualizarUsuario(UUID id, UpdateUsuarioDto dto) {
        UsuarioEntity entity = usuarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con ID: " + id));

        if (dto.getNumeroDocumento() != null && !dto.getNumeroDocumento().isBlank()) {
            String newDoc = dto.getNumeroDocumento().trim();
            if (!newDoc.equals(entity.getNumeroDocumento()) && usuarioRepository.existsByNumeroDocumento(newDoc)) {
                throw new BusinessException("El documento " + newDoc + " ya está en uso por otro usuario");
            }
            entity.setNumeroDocumento(newDoc);
        }

        if (dto.getTipoDocumento() != null) {
            entity.setTipoDocumento(dto.getTipoDocumento());
        }

        if (dto.getNombresApellidos() != null && !dto.getNombresApellidos().isBlank()) {
            entity.setNombresApellidos(dto.getNombresApellidos().trim());
        }

        if (dto.getPassword() != null && !dto.getPassword().isBlank()) {
            entity.setPasswordHash(passwordEncoder.encode(dto.getPassword()));
        }

        if (dto.getRol() != null) {
            entity.setRol(dto.getRol());
        }

        if (dto.getActivo() != null) {
            entity.setActivo(dto.getActivo());
        }

        UsuarioEntity updated = usuarioRepository.save(entity);
        return mapToDto(updated);
    }

    @Transactional
    public UsuarioDto cambiarEstado(UUID id, boolean activo) {
        UsuarioEntity entity = usuarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con ID: " + id));
        entity.setActivo(activo);
        UsuarioEntity updated = usuarioRepository.save(entity);
        return mapToDto(updated);
    }

    @Transactional
    public void eliminarUsuario(UUID id) {
        UsuarioEntity entity = usuarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con ID: " + id));
        usuarioRepository.delete(entity);
    }

    private UsuarioDto mapToDto(UsuarioEntity entity) {
        return UsuarioDto.builder()
                .id(entity.getId())
                .tipoDocumento(entity.getTipoDocumento())
                .numeroDocumento(entity.getNumeroDocumento())
                .nombresApellidos(entity.getNombresApellidos())
                .rol(entity.getRol())
                .activo(entity.getActivo())
                .ultimoLogin(entity.getUltimoLogin())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
