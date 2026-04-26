package br.com.ecomel.repository;

import br.com.ecomel.domain.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    /**
     * Busca um usuário pelo e-mail (usado no login e validações).
     */
    Optional<Usuario> findByEmail(String email);

    /**
     * Verifica se já existe um e-mail cadastrado para evitar duplicidade.
     */
    boolean existsByEmail(String email);
}