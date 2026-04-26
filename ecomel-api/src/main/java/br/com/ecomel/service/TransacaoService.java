package br.com.ecomel.service;

import br.com.ecomel.domain.entity.Carteira;
import br.com.ecomel.domain.entity.IndiceGlobal;
import br.com.ecomel.repository.CarteiraRepository;
import br.com.ecomel.repository.IndiceGlobalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
@RequiredArgsConstructor
//Aqui implementamos a regra: 10% de taxa, sendo 5% direto para o Índice Global.
public class TransacaoService {

    private final IndiceGlobalRepository indiceRepository;
    private final CarteiraRepository carteiraRepository;

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

}

