package br.com.ecomel.repository;

import br.com.ecomel.domain.entity.OrdemExecucao;
import br.com.ecomel.domain.entity.OrdemFavo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrdemExecucaoRepository extends JpaRepository<OrdemExecucao, Long> {

    /**
     * Busca execuções de uma ordem de compra
     */
    List<OrdemExecucao> findByOrdemCompra(OrdemFavo ordemCompra);

    /**
     * Busca execuções de uma ordem de venda
     */
    List<OrdemExecucao> findByOrdemVenda(OrdemFavo ordemVenda);

    /**
     * Histórico completo de execuções de uma ordem (compra ou venda)
     */
    List<OrdemExecucao> findByOrdemCompraOrOrdemVenda(
            OrdemFavo ordemCompra,
            OrdemFavo ordemVenda
    );
    
    /**
     * Últimas execuções (order book / histórico de trades)
     */
    List<OrdemExecucao> findTop50ByOrderByCriadoEmDesc();

}

