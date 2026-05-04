package br.com.ecomel.repository;

import br.com.ecomel.domain.entity.Usuario;
import br.com.ecomel.domain.enums.StatusUsuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    // =====================================================
    // 🔐 IDENTIDADE PRINCIPAL (CRIPTO)
    // =====================================================

    /**
     * 🔥 Busca usuário pelo código da carteira (IDENTIDADE REAL DO SISTEMA)
     */
    @Query("""
        SELECT u FROM Usuario u
        JOIN FETCH u.carteira c
        WHERE c.codigoCarteira = :codigo
          AND u.status = :status
    """)
    Optional<Usuario> findByCodigoCarteira(
            @Param("codigo") String codigo,
            @Param("status") StatusUsuario status
    );

    /**
     * 🔥 Busca usuário por ID (uso interno seguro)
     */
    @Query("""
        SELECT u FROM Usuario u
        JOIN FETCH u.carteira c
        WHERE u.id = :id
          AND u.status = :status
    """)
    Optional<Usuario> findByIdAtivo(
            @Param("id") Long id,
            @Param("status") StatusUsuario status
    );

    // =====================================================
    // ⚙️ OPCIONAL (E-MAIL NÃO É IDENTIDADE)
    // =====================================================

    /**
     * ⚠️ Apenas para validação (não usar como chave de negócio)
     */
    boolean existsByEmail(String email);

    boolean existsByEmailAndIdNot(String email, Long id);

    // =====================================================
    // 🔄 LOGIN FLEXÍVEL (SE QUISER SUPORTAR AMBOS)
    // =====================================================

    /**
     * 🔥 Permite login por código da carteira OU e-mail (opcional)
     * Mas a identidade real continua sendo a carteira
     */
    @Query("""
        SELECT u FROM Usuario u
        JOIN FETCH u.carteira c
        WHERE (c.codigoCarteira = :login OR u.email = :login)
          AND u.status = :status
    """)
    Optional<Usuario> findByLogin(
            @Param("login") String login,
            @Param("status") StatusUsuario status
    );
}
