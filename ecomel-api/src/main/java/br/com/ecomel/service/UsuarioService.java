package br.com.ecomel.service;

import br.com.ecomel.domain.entity.Carteira;
import br.com.ecomel.domain.entity.IndiceGlobal;
import br.com.ecomel.domain.entity.Usuario;
import br.com.ecomel.domain.enums.StatusUsuario;
import br.com.ecomel.dto.request.UsuarioRequest;
import br.com.ecomel.dto.response.CarteiraResponse;
import br.com.ecomel.dto.response.UsuarioResponse;
import br.com.ecomel.exception.BusinessException;
import br.com.ecomel.repository.CarteiraRepository;
import br.com.ecomel.repository.IndiceGlobalRepository;
import br.com.ecomel.repository.UsuarioRepository;
import br.com.ecomel.util.GeradorCodigoCarteira;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final CarteiraRepository carteiraRepository;
    private final GeradorCodigoCarteira geradorCodigo;
    private final PasswordEncoder passwordEncoder;
    private final CarteiraService carteiraService;
    private final IndiceGlobalRepository indiceRepository;

    // ===============================
    // 🔥 CRIAÇÃO DE USUÁRIO
    // ===============================
    @Transactional
    public UsuarioResponse salvar(UsuarioRequest request) {

        if (request.email() != null && !request.email().isBlank()) {
            if (usuarioRepository.existsByEmail(request.email())) {
                throw new BusinessException("E-mail já está em uso.");
            }
        }

        Usuario usuario = new Usuario();
        usuario.setNome(request.nome());
        usuario.setEmail(request.email());
        usuario.setSenha(passwordEncoder.encode(request.senha()));
        usuario.setStatus(StatusUsuario.ATIVO);
        usuario.setAtivo(true);

        // 🔥 Criar carteira vinculada
        Carteira carteira = new Carteira();
        carteira.setUsuario(usuario);
        carteira.setCodigoCarteira(gerarNovoCodigoValido());
        carteira.setTokenEcomel(BigDecimal.ZERO);
        carteira.setTokenEcomelBloqueado(BigDecimal.ZERO);
        carteira.setSaldoFavos(BigDecimal.ZERO);
        carteira.setSaldoFavosBloqueado(BigDecimal.ZERO);
        carteira.setUltimoIndiceFavo(BigDecimal.ZERO);
        carteira.setAtivo(true);

        usuario.setCarteira(carteira);

        Usuario salvo = usuarioRepository.save(usuario);

        return buscarPorId(salvo.getId());
    }

    // ===============================
    // 🔥 EDIÇÃO
    // ===============================
    @Transactional
    public void editar(Long id, UsuarioRequest request) {

        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Usuário não encontrado"));

        if (request.email() != null && !request.email().isBlank()) {
            if (usuarioRepository.existsByEmailAndIdNot(request.email(), id)) {
                throw new BusinessException("E-mail já em uso por outro usuário.");
            }
        }

        usuario.setNome(request.nome());
        usuario.setEmail(request.email());

        if (request.senha() != null && !request.senha().isBlank()) {
            usuario.setSenha(passwordEncoder.encode(request.senha()));
        }

        usuarioRepository.save(usuario);
    }

    // ===============================
    // 🔥 INATIVAÇÃO (REGRA FINANCEIRA)
    // ===============================
    @Transactional
    public void inativar(Long id) {

        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Usuário não encontrado"));

        Carteira carteira = usuario.getCarteira();

        if (carteira == null) {
            throw new BusinessException("Usuário não possui carteira.");
        }

        IndiceGlobal indice = indiceRepository.findFirstByAtivoTrueWithLock();

        BigDecimal limiteMinimo = new BigDecimal("0.01");

        BigDecimal saldoReal = carteira.getSaldoReal(indice.getValor());

        boolean possuiSaldo =
                saldoReal.compareTo(limiteMinimo) >= 0 ||
                carteira.getSaldoFavos().compareTo(limiteMinimo) >= 0 ||
                carteira.getTokenEcomelBloqueado().compareTo(BigDecimal.ZERO) > 0 ||
                carteira.getSaldoFavosBloqueado().compareTo(BigDecimal.ZERO) > 0;

        if (possuiSaldo) {
            throw new BusinessException("Conta possui ativos. Zere antes de desativar.");
        }

        usuario.setAtivo(false);
        usuario.setStatus(StatusUsuario.BLOQUEADO);

        carteira.setAtivo(false);

        usuarioRepository.save(usuario);
    }

    // ===============================
    // 🔥 BUSCA
    // ===============================
    @Transactional(readOnly = true)
    public UsuarioResponse buscarPorId(Long id) {

        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Usuário não encontrado"));

        if (!usuario.isAtivo()) {
            throw new BusinessException("Usuário inativo.");
        }

        CarteiraResponse carteiraResumo = carteiraService.obterExtratoPorUsuario(id);

        return new UsuarioResponse(
                usuario.getId(),
                usuario.getNome(),
                usuario.getEmail(),
                usuario.getCriadoEm(),
                carteiraResumo
        );
    }

    // ===============================
    // 🔥 GERADOR DE CARTEIRA
    // ===============================
    private String gerarNovoCodigoValido() {

        String ultimo = carteiraRepository.findMaxCodigoCarteira();
        String candidato = geradorCodigo.incrementar(ultimo);

        while (carteiraRepository.existsByCodigoCarteira(candidato)) {
            candidato = geradorCodigo.incrementar(candidato);
        }

        return candidato;
    }
}
