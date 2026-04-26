package br.com.ecomel.repository;

import br.com.ecomel.domain.entity.Transacao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransacaoRepository extends JpaRepository<Transacao, Long> {

    /**
     * Busca todas as transações de uma carteira específica pelo ID do usuário.
     * Ordena pela data de criação decrescente (mais recentes primeiro).
     */
    List<Transacao> findByCarteiraUsuarioIdOrderByCriadoEmDesc(Long usuarioId);

    /**
     * Busca transações onde a carteira do usuário foi o destino (recebimento de transferência).
     */
    List<Transacao> findByCarteiraDestinoUsuarioIdOrderByCriadoEmDesc(Long usuarioId);
}