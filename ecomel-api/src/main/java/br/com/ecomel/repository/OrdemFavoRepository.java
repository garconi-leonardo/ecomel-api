package br.com.ecomel.repository;

import br.com.ecomel.domain.entity.OrdemFavo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.math.BigDecimal;
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
}
