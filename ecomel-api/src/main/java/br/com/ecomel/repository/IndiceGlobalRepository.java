package br.com.ecomel.repository;

import br.com.ecomel.domain.entity.IndiceGlobal;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface IndiceGlobalRepository extends JpaRepository<IndiceGlobal, Long> {
    
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM IndiceGlobal i WHERE i.ativo = true")
    IndiceGlobal findFirstByAtivoTrue();
}