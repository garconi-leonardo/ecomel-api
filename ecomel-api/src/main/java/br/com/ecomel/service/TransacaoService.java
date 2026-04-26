package br.com.ecomel.service;

import br.com.ecomel.domain.entity.Carteira;
import br.com.ecomel.domain.entity.IndiceGlobal;
import br.com.ecomel.domain.entity.Transacao;
import br.com.ecomel.domain.enums.StatusTransacao;
import br.com.ecomel.domain.enums.TipoTransacao;
import br.com.ecomel.dto.request.TransferenciaRequest;
import br.com.ecomel.dto.response.TransacaoResponse;
import br.com.ecomel.exception.BusinessException;
import br.com.ecomel.repository.CarteiraRepository;
import br.com.ecomel.repository.IndiceGlobalRepository;
import br.com.ecomel.repository.TransacaoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@RequiredArgsConstructor
//Aqui implementamos a regra: 10% de taxa, sendo 5% direto para o Índice Global.
public class TransacaoService {

    private final IndiceGlobalRepository indiceRepository;
    private final CarteiraRepository carteiraRepository;
    private final TransacaoRepository transacaoRepository;

    @Transactional
    public void processarDeposito(Long usuarioId, BigDecimal valor) {
        // 1. Lock no Índice e na Carteira (Consistência)
        IndiceGlobal indice = indiceRepository.findFirstByAtivoTrue();
        Carteira carteira = carteiraRepository.findByUsuarioId(usuarioId);

        // 2. Cálculos de Taxas (10% total)
        BigDecimal taxaTotal = valor.multiply(new BigDecimal("0.10"));
        BigDecimal taxaIndice = valor.multiply(new BigDecimal("0.05")); // 5% p/ revalorização
        BigDecimal valorLiquido = valor.subtract(taxaTotal);

        // 3. Atualizar Índice Global (Fórmula: i = i * (1 + 0.05))
        // Nota: A regra diz que cresce com a transação externa
        BigDecimal fatorCrescimento = BigDecimal.ONE.add(new BigDecimal("0.05"));
        indice.setValor(indice.getValor().multiply(fatorCrescimento));
        
        // 4. Converter valor líquido para Saldo Base e atualizar carteira
        // balanceBase += valor_liquido / novo_indice
        BigDecimal incrementoBase = valorLiquido.divide(indice.getValor(), 18, RoundingMode.HALF_UP);
        carteira.setSaldoBase(carteira.getSaldoBase().add(incrementoBase));

        // 5. Salvar (Auditoria automática via JPA)
        indiceRepository.save(indice);
        carteiraRepository.save(carteira);
    }
    
    @Transactional
    public void processarSaque(Long usuarioId, BigDecimal valorSaqueReal) {
        IndiceGlobal indice = indiceRepository.findFirstByAtivoTrue();
        Carteira carteira = carteiraRepository.findByUsuarioId(usuarioId);

        // 1. Validar se tem saldo real suficiente
        BigDecimal saldoRealAtual = carteira.getSaldoReal(indice.getValor());
        if (saldoRealAtual.compareTo(valorSaqueReal) < 0) {
            throw new RuntimeException("Saldo insuficiente");
        }

        // 2. Atualizar Índice Global (Valorização por transação externa)
        BigDecimal fatorCrescimento = BigDecimal.ONE.add(new BigDecimal("0.05"));
        indice.setValor(indice.getValor().multiply(fatorCrescimento));

        // 3. Converter o valor do saque (Real) para o que deve ser removido do Base
        // quantidade_base = valor_saque_real / novo_indice
        BigDecimal debitoBase = valorSaqueReal.divide(indice.getValor(), 18, RoundingMode.HALF_UP);
        carteira.setSaldoBase(carteira.getSaldoBase().subtract(debitoBase));

        indiceRepository.save(indice);
        carteiraRepository.save(carteira);
    }
    
    @Transactional(readOnly = true)
    public List<TransacaoResponse> listarTransacoesPorUsuario(Long usuarioId) {
        return transacaoRepository.findByCarteiraUsuarioIdOrderByCriadoEmDesc(usuarioId)
            .stream()
            .map(t -> new TransacaoResponse(
                t.getId(),
                t.getTipo(),
                t.getValorBruto(),
                t.getValorLiquido(),
                t.getTaxaTotal(),
                t.getStatus(),
                t.getCriadoEm(),
                t.getCarteiraDestino() != null ? t.getCarteiraDestino().getCodigoEndereco() : null
            )).toList();
    }
    
    @Transactional
    public void transferirInterno(TransferenciaRequest request) {
        IndiceGlobal indice = indiceRepository.findFirstByAtivoTrue();
        
        // 1. Localiza a origem (por ID) e o destino (pelo Código AAA111)
        Carteira origem = carteiraRepository.findByUsuarioId(request.usuarioOrigemId());
        Carteira destino = carteiraRepository.findByCodigoEndereco(request.codigoDestino())
            .orElseThrow(() -> new BusinessException("Carteira destino não encontrada: " + request.codigoDestino()));

        if (origem.getId().equals(destino.getId())) {
            throw new BusinessException("Não é permitido transferir para a própria carteira.");
        }

        // 2. Valida saldo real disponível
        BigDecimal saldoRealOrigem = origem.getSaldoReal(indice.getValor());
        if (saldoRealOrigem.compareTo(request.valorReal()) < 0) {
            throw new BusinessException("Saldo insuficiente para transferência.");
        }

        // 3. Converte o valor real para valor base (Usa o índice atual sem alterá-lo)
        BigDecimal valorBase = request.valorReal().divide(indice.getValor(), 18, RoundingMode.HALF_UP);

        // 4. Executa a movimentação
        origem.setSaldoBase(origem.getSaldoBase().subtract(valorBase));
        destino.setSaldoBase(destino.getSaldoBase().add(valorBase));

        // 5. Registra a transação no histórico
        Transacao transacao = new Transacao();
        transacao.setCarteira(origem);
        transacao.setCarteiraDestino(destino);
        transacao.setTipo(TipoTransacao.TRANSFERENCIA_INTERNA);
        transacao.setValorBruto(request.valorReal());
        transacao.setValorLiquido(request.valorReal()); // Sem taxas na interna
        transacao.setTaxaTotal(BigDecimal.ZERO);
        transacao.setStatus(StatusTransacao.CONCLUIDA);

        carteiraRepository.save(origem);
        carteiraRepository.save(destino);
        transacaoRepository.save(transacao);
    }

}

