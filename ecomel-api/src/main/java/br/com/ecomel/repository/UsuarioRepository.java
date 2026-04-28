package br.com.ecomel.repository;

import br.com.ecomel.domain.entity.Usuario;
import br.com.ecomel.domain.enums.StatusUsuario;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    /**
     * Verifica se já existe um e-mail cadastrado para evitar duplicidade
     *  sem levar em consideração o próprio objeto.
     */
    
	boolean existsByEmailAndIdNot(String email, Long id);
	
	/*
	 * Busca o usuário pelo e-mail ou pedo codigo da carteira
	 */	
	@Query("SELECT u FROM Usuario u " +
	        "JOIN FETCH u.carteira c " +
	        "WHERE (u.email = :login OR c.codigoEndereco = :login) " +
	        "AND u.status = :statusAtivo") 
	Optional<Usuario> findByEmailOuCodigoCarteira(
	    @Param("login") String login, 
	    @Param("statusAtivo") StatusUsuario statusAtivo
	);

    /*
     * Busca o usuario por id 
     */    
    @Query("SELECT u FROM Usuario u " +
            "JOIN FETCH u.carteira c " +
            "WHERE u.id = :id " +
            "AND u.status = :statusAtivo")
    Optional<Usuario> findByPorId(
        @Param("id") Long id, 
        @Param("statusAtivo") StatusUsuario statusAtivo
    );
    
}