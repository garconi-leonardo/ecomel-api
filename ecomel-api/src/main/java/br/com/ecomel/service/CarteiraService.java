package br.com.ecomel.service;

import br.com.ecomel.domain.entity.Carteira;
import br.com.ecomel.domain.entity.IndiceGlobal;
import br.com.ecomel.dto.response.CarteiraResponse;
import br.com.ecomel.repository.CarteiraRepository;
import br.com.ecomel.repository.IndiceGlobalRepository;
import br.com.ecomel.repository.OrdemFavoRepository;
import br.com.ecomel.domain.enums.StatusOrdem;
import br.com.ecomel.domain.enums.TipoOrdem;
import br.com.ecomel.util.CalculoFinanceiroUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;

@Service
@RequiredArgsConstructor
public class CarteiraService {

    private final CarteiraRepository carteiraRepository;
    private final IndiceGlobalRepository indiceRepository;
    private final OrdemFavoRepository ordemRepository;

    @Transactional
    public CarteiraResponse obterExtratoPorUsuario(Long usuarioId) {
        // 1. Busca os dados atuais com Lock para garantir sincronia
        Carteira carteira = carteiraRepository.findByUsuarioId(usuarioId);
        IndiceGlobal indice = indiceRepository.findFirstByAtivoTrue();

        // 2. Sincroniza dividendos de FAVOS acumulados passivamente
        sincronizarDividendos(carteira, indice);

        // 3. Cálculo de valores bloqueados em ordens abertas/parciais
        BigDecimal favosEmOrdem = ordemRepository.sumQuantidadeRestanteByCarteiraAndTipoAndStatus(
            carteira.getId(), TipoOrdem.VENDA, StatusOrdem.ABERTA);
            
        BigDecimal ecmEmOrdem = ordemRepository.sumValorEcmBloqueado(
            carteira.getId(), TipoOrdem.COMPRA, StatusOrdem.ABERTA);

        // 4. Salva a carteira atualizada com os novos dividendos e novo marco de índice
        carteiraRepository.save(carteira);

        return new CarteiraResponse(
            carteira.getCodigoEndereco(),
            carteira.getTokenEcomel(),
            carteira.getSaldoReal(indice.getValor()),
            carteira.getSaldoFavos(),
            favosEmOrdem != null ? favosEmOrdem : BigDecimal.ZERO,
            ecmEmOrdem != null ? ecmEmOrdem : BigDecimal.ZERO,
            indice.getValor()
        );
    }

    /**
     * Calcula e credita dividendos de ECM baseados na posse de FAVOS.
     * Fórmula: Lucro = (IndiceGlobalFavo - UltimoIndiceUsuario) * SaldoFavosUsuario
     * Os dividendos creditados em tokenEcomel são SEMPRE arredondados para menos.
     */
    private void sincronizarDividendos(Carteira carteira, IndiceGlobal indiceGlobal) {
        BigDecimal indiceAtual = indiceGlobal.getIndiceFavoAcumulado();
        BigDecimal ultimoIndiceUsuario = carteira.getUltimoIndiceFavo();

        if (indiceAtual.compareTo(ultimoIndiceUsuario) > 0 && carteira.getSaldoFavos().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal diferencaIndice = indiceAtual.subtract(ultimoIndiceUsuario);
            BigDecimal dividendosEcm = carteira.getSaldoFavos().multiply(diferencaIndice)
                                               .setScale(18, RoundingMode.DOWN);

            // Credita os dividendos no tokenEcomel (truncado, sempre inteiro pra menos)
            BigDecimal dividendosToken = CalculoFinanceiroUtils.formatarEcm(dividendosEcm);
            carteira.setTokenEcomel(carteira.getTokenEcomel().add(dividendosToken));
        }
        
        // Atualiza o marco temporal do usuário para o índice atual do sistema
        carteira.setUltimoIndiceFavo(indiceAtual);
    }
}
