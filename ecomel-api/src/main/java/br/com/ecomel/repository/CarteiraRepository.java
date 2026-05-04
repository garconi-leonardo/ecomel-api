package br.com.ecomel.repository;

import br.com.ecomel.domain.entity.Carteira;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface CarteiraRepository extends JpaRepository<Carteira, Long> {
    
    @Query("SELECT MAX(c.codigoCarteira) FROM Carteira c")
    String findMaxCodigoCarteira();

    boolean existsByCodigoCarteira(String codigoCarteira);

    Optional<Carteira> findByCodigoCarteira(String codigoCarteira);
    
    @Query("SELECT c FROM Carteira c WHERE c.usuario.id = :usuarioId")
    Optional<Carteira> findByUsuarioId(Long usuarioId);
    
    @Query("SELECT SUM(c.saldoFavos) FROM Carteira c")
    BigDecimal sumTotalFavos();

    @Query("SELECT c FROM Carteira c WHERE c.saldoFavos > 0")
    List<Carteira> findAllBySaldoFavosGreaterThanZero();
    
    @Query("SELECT COALESCE(SUM(c.tokenEcomel), 0) FROM Carteira c WHERE c.ativo = true")
    BigDecimal somarTotalEcomelAtivo();

}
