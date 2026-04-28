package br.com.ecomel.repository;

import br.com.ecomel.domain.entity.OrdemFavo;
import br.com.ecomel.domain.enums.StatusOrdem;
import br.com.ecomel.domain.enums.TipoOrdem;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;

public interface OrdemFavoRepository extends JpaRepository<OrdemFavo, Long> {

    // Busca ordens de VENDA com preço menor ou igual ao preço de COMPRA (Ordenado por preço ASC - mais barato primeiro)
    @Query("SELECT o FROM OrdemFavo o WHERE o.tipo = 'VENDA' " +
           "AND o.status IN ('ABERTA', 'PARCIAL') " +
           "AND o.precoUnitario <= :preco " +
           "ORDER BY o.precoUnitario ASC, o.criadoEm ASC")
    List<OrdemFavo> findVendasCompativeis(@Param("preco") BigDecimal preco);

    // Busca ordens de COMPRA com preço maior ou igual ao preço de VENDA (Ordenado por preço DESC - mais caro primeiro)
    @Query("SELECT o FROM OrdemFavo o WHERE o.tipo = 'COMPRA' " +
           "AND o.status IN ('ABERTA', 'PARCIAL') " +
           "AND o.precoUnitario >= :preco " +
           "ORDER BY o.precoUnitario DESC, o.criadoEm ASC")
    List<OrdemFavo> findComprasCompativeis(@Param("preco") BigDecimal preco);
    
    /**
     * Busca ordens de venda para o comprador. 
     * Ordena pelo menor preço primeiro (Melhor oferta de compra).
     */
    List<OrdemFavo> findByTipoAndStatusOrderByPrecoUnitarioAsc(TipoOrdem tipo, StatusOrdem status);

    /**
     * Busca ordens de compra para o vendedor. 
     * Ordena pelo maior preço primeiro (Melhor oferta de venda).
     */
    List<OrdemFavo> findByTipoAndStatusOrderByPrecoUnitarioDesc(TipoOrdem tipo, StatusOrdem status);
    
    @Query("SELECT SUM(o.quantidadeRestante) FROM OrdemFavo o WHERE o.carteira.id = :carteiraId AND o.tipo = :tipo AND o.status = :status")
    BigDecimal sumQuantidadeRestanteByCarteiraAndTipoAndStatus(Long carteiraId, TipoOrdem tipo, StatusOrdem status);

    @Query("SELECT SUM(o.quantidadeRestante * o.precoUnitario) FROM OrdemFavo o WHERE o.carteira.id = :carteiraId AND o.tipo = :tipo AND o.status = :status")
    BigDecimal sumValorEcmBloqueado(Long carteiraId, TipoOrdem tipo, StatusOrdem status);

    /**
     * Busca ordens de uma carteira específica filtrando por uma coleção de status.
     * Essencial para listar as 'Minhas Ordens' na Consulta Detalhada.
     */
    List<OrdemFavo> findByCarteiraUsuarioIdAndStatusIn(Long usuarioId, Collection<StatusOrdem> statuses);
}
