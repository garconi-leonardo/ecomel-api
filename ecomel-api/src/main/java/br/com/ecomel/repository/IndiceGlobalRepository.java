package br.com.ecomel.repository;

import br.com.ecomel.domain.entity.IndiceGlobal;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface IndiceGlobalRepository extends JpaRepository<IndiceGlobal, Long> {

    /**
     * 🔥 Recupera o índice ativo com LOCK PESSIMISTA
     * Garante consistência em operações críticas (depósito/saque)
     */
	   @Lock(LockModeType.PESSIMISTIC_WRITE)
	   @Query("SELECT i FROM IndiceGlobal i WHERE i.ativo = true")
	   IndiceGlobal findFirstByAtivoTrueWithLock();

	   // Use este para Extratos e Consultas (Onde você APENAS lê)
	   @Query("SELECT i FROM IndiceGlobal i WHERE i.ativo = true")
	   IndiceGlobal findFirstByAtivoTrue();
}
