package br.com.ecomel.repository;

import br.com.ecomel.domain.entity.OrdemFavo;
import br.com.ecomel.domain.enums.StatusOrdem;
import br.com.ecomel.domain.enums.TipoOrdem;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;

public interface OrdemFavoRepository extends JpaRepository<OrdemFavo, Long> {

    // =====================================================
    // 🔥 MATCHING ENGINE (LOCK + PRICE-TIME PRIORITY)
    // =====================================================

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT o FROM OrdemFavo o
        WHERE o.tipo = br.com.ecomel.domain.enums.TipoOrdem.VENDA
          AND o.status IN (
                br.com.ecomel.domain.enums.StatusOrdem.ABERTA,
                br.com.ecomel.domain.enums.StatusOrdem.PARCIALMENTE_EXECUTADA
          )
          AND o.precoUnitario <= :preco
        ORDER BY o.precoUnitario ASC, o.criadoEm ASC
    """)
    List<OrdemFavo> findVendasParaMatch(@Param("preco") BigDecimal preco);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT o FROM OrdemFavo o
        WHERE o.tipo = br.com.ecomel.domain.enums.TipoOrdem.COMPRA
          AND o.status IN (
                br.com.ecomel.domain.enums.StatusOrdem.ABERTA,
                br.com.ecomel.domain.enums.StatusOrdem.PARCIALMENTE_EXECUTADA
          )
          AND o.precoUnitario >= :preco
        ORDER BY o.precoUnitario DESC, o.criadoEm ASC
    """)
    List<OrdemFavo> findComprasParaMatch(@Param("preco") BigDecimal preco);


    // =====================================================
    // 📊 BOOK (PAGINADO)
    // =====================================================

    Page<OrdemFavo> findByTipoAndStatus(
            TipoOrdem tipo,
            StatusOrdem status,
            Pageable pageable
    );


    // =====================================================
    // 📊 ORDENS POR CARTEIRA (PADRÃO CORRETO)
    // =====================================================

    List<OrdemFavo> findByCarteiraIdAndStatusIn(
            Long carteiraId,
            Collection<StatusOrdem> statuses
    );

    Page<OrdemFavo> findByCarteiraIdAndStatusIn(
            Long carteiraId,
            Collection<StatusOrdem> statuses,
            Pageable pageable
    );

    long countByCarteiraIdAndStatusIn(
            Long carteiraId,
            Collection<StatusOrdem> statuses
    );


    // =====================================================
    // 💰 SALDOS BLOQUEADOS (CRÍTICO FINANCEIRO)
    // =====================================================

    @Query("""
        SELECT COALESCE(SUM(o.quantidadeRestante), 0)
        FROM OrdemFavo o
        WHERE o.carteira.id = :carteiraId
          AND o.tipo = :tipo
          AND o.status IN :status
    """)
    BigDecimal sumQuantidadeRestanteByCarteiraAndTipoAndStatus(
            @Param("carteiraId") Long carteiraId,
            @Param("tipo") TipoOrdem tipo,
            @Param("status") Collection<StatusOrdem> status
    );

    @Query("""
        SELECT COALESCE(SUM(o.quantidadeRestante * o.precoUnitario), 0)
        FROM OrdemFavo o
        WHERE o.carteira.id = :carteiraId
          AND o.tipo = :tipo
          AND o.status IN :status
    """)
    BigDecimal sumValorEcmBloqueado(
            @Param("carteiraId") Long carteiraId,
            @Param("tipo") TipoOrdem tipo,
            @Param("status") Collection<StatusOrdem> status
    );


    // =====================================================
    // 🔥 IDEMPOTÊNCIA
    // =====================================================

    boolean existsByRequestKey(String requestKey);

}
