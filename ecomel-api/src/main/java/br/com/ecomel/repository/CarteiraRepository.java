package br.com.ecomel.repository;

import br.com.ecomel.domain.entity.Carteira;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface CarteiraRepository extends JpaRepository<Carteira, Long> {
    
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Carteira c WHERE c.usuario.id = :usuarioId")
    Carteira findByUsuarioId(Long usuarioId);
}