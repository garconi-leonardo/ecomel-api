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
import br.com.ecomel.util.CalculoFinanceiroUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j; // Import para logs
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.List;

@Slf4j // Habilita o objeto 'log'
@Service
@RequiredArgsConstructor
public class TransacaoService {

    private final IndiceGlobalRepository indiceRepository;
    private final CarteiraRepository carteiraRepository;
    private final TransacaoRepository transacaoRepository;
    private final DistribuicaoService distribuicaoService;

    @Transactional
    public void processarDeposito(Long usuarioId, BigDecimal valorReal, String requestKey) {
        try {

            verificarIdempotencia(requestKey);

            IndiceGlobal indice = indiceRepository.findFirstByAtivoTrue();
            Carteira carteira = carteiraRepository.findByUsuarioId(usuarioId);

            validarCarteiraAtiva(carteira);

            //Taxa total (10%)
            BigDecimal taxaTotal = valorReal.multiply(new BigDecimal("0.10"));
            BigDecimal valorLiquido = valorReal.subtract(taxaTotal);

            //Distribuição da taxa
            BigDecimal valorParaLastro = valorReal.multiply(new BigDecimal("0.05"));   // ECOMEL
            BigDecimal valorFavos = valorReal.multiply(new BigDecimal("0.0401"));
            BigDecimal valorGateway = valorReal.multiply(new BigDecimal("0.0099"));

            //Estado atual
            BigDecimal indiceAtual = indice.getValor();
            BigDecimal liquidez = indice.getLiquidezTotal();
            BigDecimal totalEcm = obterTotalEcm();

            //Calcular tokens (ANTES de atualizar liquidez)
            BigDecimal tokensComprados = valorLiquido.divide(indiceAtual, 18, RoundingMode.DOWN);
            tokensComprados = CalculoFinanceiroUtils.formatarEcm(tokensComprados);

            //Atualizar liquidez (LASTRO REAL)
            liquidez = liquidez
                    .add(valorLiquido)     // entra dinheiro real
                    .add(valorParaLastro); // entra reserva de valorização

            indice.setLiquidezTotal(liquidez);

            //Creditar tokens
            carteira.setTokenEcomel(
                    carteira.getTokenEcomel().add(tokensComprados)
            );

            //Novo total de tokens
            BigDecimal novoTotalEcm = totalEcm.add(tokensComprados);

            //Recalcular índice (PREÇO REAL)
            if (novoTotalEcm.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal novoIndice = liquidez.divide(novoTotalEcm, 18, RoundingMode.DOWN);
                indice.setValor(novoIndice);
            }

            //Distribuir favos
            BigDecimal favosBase = valorFavos.divide(indice.getValor(), 18, RoundingMode.DOWN);
            distribuicaoService.distribuirTaxaFavos(favosBase);

            //Registrar transação
            salvarTransacao(
                    carteira,
                    valorReal,
                    valorLiquido,
                    taxaTotal,
                    TipoTransacao.DEPOSITO,
                    requestKey
            );

            indiceRepository.save(indice);
            carteiraRepository.save(carteira);

                log.info("Depósito sustentável realizado. Usuario: {}, Valor: {}", usuarioId, valorReal);

            } catch (Exception e) {
                log.error("Erro ao processar depósito. Usuario: {}, Valor: {}", usuarioId, valorReal);
                log.error("Erro: {}", e.getMessage());
                throw e;
            }
        }



    @Transactional
    public void processarSaque(Long usuarioId, BigDecimal valorSaqueReal, String requestKey) {
        try {

            verificarIdempotencia(requestKey);

            IndiceGlobal indice = indiceRepository.findFirstByAtivoTrue();
            Carteira carteira = carteiraRepository.findByUsuarioId(usuarioId);

            validarCarteiraAtiva(carteira);

            BigDecimal indiceAtual = indice.getValor();

            //Calcular tokens necessários (BASE DA VALIDAÇÃO)
            BigDecimal tokensNecessarios = valorSaqueReal.divide(indiceAtual, 18, RoundingMode.DOWN);
            tokensNecessarios = CalculoFinanceiroUtils.formatarEcm(tokensNecessarios);

            //Validar saldo em TOKEN (CORREÇÃO CRÍTICA)
            if (carteira.getTokenEcomel().compareTo(tokensNecessarios) < 0) {
                throw new BusinessException("Saldo insuficiente para saque.");
            }

            //Taxa total (10%)
            BigDecimal taxaTotal = valorSaqueReal.multiply(new BigDecimal("0.10"));
            BigDecimal valorLiquido = valorSaqueReal.subtract(taxaTotal);

            //Distribuição
            BigDecimal valorParaLastro = valorSaqueReal.multiply(new BigDecimal("0.05"));
            BigDecimal valorFavos = valorSaqueReal.multiply(new BigDecimal("0.0401"));
            BigDecimal valorGateway = valorSaqueReal.multiply(new BigDecimal("0.0099"));

            //Estado atual
            BigDecimal liquidez = indice.getLiquidezTotal();
            BigDecimal totalEcm = obterTotalEcm();

            //Atualizar tokens (REMOVER)
            carteira.setTokenEcomel(
                    carteira.getTokenEcomel().subtract(tokensNecessarios)
            );

            //Atualizar liquidez
            liquidez = liquidez
                    .subtract(valorSaqueReal) // sai dinheiro
                    .add(valorParaLastro);    // parte da taxa volta como lastro

            if (liquidez.compareTo(BigDecimal.ZERO) < 0) {
                throw new BusinessException("Liquidez insuficiente no sistema.");
            }

            indice.setLiquidezTotal(liquidez);

            //Novo total de tokens
            BigDecimal novoTotalEcm = totalEcm.subtract(tokensNecessarios);

            //Recalcular índice
            if (novoTotalEcm.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal novoIndice = liquidez.divide(novoTotalEcm, 18, RoundingMode.DOWN);
                indice.setValor(novoIndice);
            } else {
                indice.setValor(BigDecimal.ONE); // reset seguro
            }

            //Distribuir favos
            BigDecimal favosBase = valorFavos.divide(indice.getValor(), 18, RoundingMode.DOWN);
            distribuicaoService.distribuirTaxaFavos(favosBase);

            //Registrar transação
            salvarTransacao(
                    carteira,
                    valorSaqueReal,
                    valorLiquido,
                    taxaTotal,
                    TipoTransacao.SAQUE,
                    requestKey
            );

            indiceRepository.save(indice);
            carteiraRepository.save(carteira);

                log.info("Saque processado com sucesso: Usuario: {}, Valor: {}", usuarioId, valorSaqueReal);

            } catch (Exception e) {            
            	log.error("Erro ao processar saque. Usuario: {}, Valor: {}", usuarioId, valorSaqueReal);
            	log.error("Erro ao processar saque para o usuário {}: {}", usuarioId, e.getMessage());
                throw e;
            }
        }



    @Transactional
    public void transferirInterno(TransferenciaRequest request, String requestKey) {
        try {

            verificarIdempotencia(requestKey);

            //Validação básica de entrada
            if (request.codigoDestino() == null || request.codigoDestino().trim().isEmpty()) {
                throw new BusinessException("Informe uma carteira válida.");
            }

            if (request.valorReal() == null || request.valorReal().compareTo(BigDecimal.ZERO) <= 0) {
                throw new BusinessException("Valor de transferência inválido.");
            }

            //Buscar dados
            IndiceGlobal indice = indiceRepository.findFirstByAtivoTrue();

            Carteira origem = carteiraRepository.findByUsuarioId(request.usuarioOrigemId());

            Carteira destino = carteiraRepository.findByCodigoEndereco(request.codigoDestino().trim())
                    .orElseThrow(() -> new BusinessException("Informe uma carteira válida."));

            //Validar carteiras
            validarCarteiraAtiva(origem);
            validarCarteiraAtiva(destino);

            //NÃO PERMITIR TRANSFERÊNCIA PARA SI MESMO
            if (origem.getId().equals(destino.getId())) {
                throw new BusinessException("Informe uma carteira válida.");
            }

            //Índice atual
            BigDecimal indiceAtual = indice.getValor();

            if (indiceAtual.compareTo(BigDecimal.ZERO) <= 0) {
                throw new BusinessException("Índice inválido para operação.");
            }

            //Converter REAL → TOKEN (momento da execução)
            BigDecimal valorBase = request.valorReal().divide(indiceAtual, 18, RoundingMode.DOWN);
            BigDecimal valorToken = CalculoFinanceiroUtils.formatarEcm(valorBase);

            if (valorToken.compareTo(BigDecimal.ZERO) <= 0) {
                throw new BusinessException("Valor insuficiente para transferência.");
            }

            //Validar saldo em TOKEN
            if (origem.getTokenEcomel().compareTo(valorToken) < 0) {
                throw new BusinessException("Saldo insuficiente para transferência.");
            }

            //Movimentação (não altera índice nem liquidez)
            origem.setTokenEcomel(origem.getTokenEcomel().subtract(valorToken));
            destino.setTokenEcomel(destino.getTokenEcomel().add(valorToken));

            salvarTransacaoInterna(
                    origem,
                    destino,
                    request.valorReal(),
                    requestKey
            );

            carteiraRepository.save(origem);
            carteiraRepository.save(destino);

            log.info("Transferência interna realizada com sucesso: {} -> {}, Valor Real: {}",
                    request.usuarioOrigemId(),
                    request.codigoDestino(),
                    request.valorReal());

        } catch (Exception e) {
        		log.error("Erro na transferência interna: {} -> {}, Valor Real: {}",
                    request.usuarioOrigemId(),
                    request.codigoDestino(),
                    request.valorReal());
                log.error("Erro na transferência interna: {}", e.getMessage());
                throw e;
        }
    }

    @Transactional(readOnly = true)
    public List<TransacaoResponse> listarTransacoesPorUsuario(Long usuarioId) {
        try {
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
        } catch (Exception e) {
            log.error("Erro ao listar transações para o usuário {}: {}", usuarioId, e.getMessage());
            throw e;
        }
    }
    
    private void verificarIdempotencia(String requestKey) {
        if (requestKey != null && transacaoRepository.existsByRequestKey(requestKey)) {
            throw new BusinessException("Transação já processada.");
        }
    }
    
    private void validarCarteiraAtiva(Carteira carteira) {
        if (carteira == null || !carteira.isAtivo()) {
            throw new RuntimeException("Carteira não encontrada ou desativada: " + carteira.getCodigoEndereco());
        }
    }
    
    private BigDecimal obterTotalEcm() {
        BigDecimal total = carteiraRepository.somarTotalEcomelAtivo();

        if (total == null || total.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ONE; // evita divisão por zero no início do sistema
        }

        return total;
    }


    private void salvarTransacao(Carteira c, BigDecimal bruto, BigDecimal liq, BigDecimal taxa, TipoTransacao tipo, String requestKey) {
        Transacao t = new Transacao();
        t.setCarteira(c);
        t.setTipo(tipo);
        t.setValorBruto(bruto);
        t.setValorLiquido(liq);
        t.setTaxaTotal(taxa);
        t.setStatus(StatusTransacao.CONCLUIDA);
        t.setRequestKey(requestKey);
        transacaoRepository.save(t);
    }

    private void salvarTransacaoInterna(Carteira o, Carteira d, BigDecimal valor, String requestKey) {
        Transacao t = new Transacao();
        t.setCarteira(o);
        t.setCarteiraDestino(d);
        t.setTipo(TipoTransacao.TRANSFERENCIA_INTERNA);
        t.setValorBruto(valor);
        t.setValorLiquido(valor);
        t.setTaxaTotal(BigDecimal.ZERO);
        t.setStatus(StatusTransacao.CONCLUIDA);
        t.setRequestKey(requestKey);
        transacaoRepository.save(t);
    }
}
