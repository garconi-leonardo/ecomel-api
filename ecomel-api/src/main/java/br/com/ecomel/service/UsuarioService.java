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
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final CarteiraRepository carteiraRepository;
    private final GeradorCodigoCarteira geradorCodigo;
    private final PasswordEncoder passwordEncoder;
    private final CarteiraService carteiraService;
    private final AuditorAware<String> auditorProvider;
    private final IndiceGlobalRepository indiceRepository;

    /**
     * Encapsula os dados de auditoria atuais.
     */
    private record AuditContext(LocalDateTime data, String usuario) {}

    private AuditContext getAuditContext() {
        return new AuditContext(
            LocalDateTime.now(), 
            auditorProvider.getCurrentAuditor().orElse("ECOMEL")
        );
    }

    @Transactional
    public UsuarioResponse salvar(UsuarioRequest request) {
        if (request.email() != null && !request.email().isBlank()) {
            if (usuarioRepository.existsByEmail(request.email())) {
                throw new BusinessException("Este e-mail já está em uso.");
            }
        }

        AuditContext audit = getAuditContext();

        Usuario usuario = new Usuario();
        usuario.setNome(request.nome());
        usuario.setEmail(request.email());
        usuario.setSenha(passwordEncoder.encode(request.senha()));
        usuario.setStatus(StatusUsuario.ATIVO);
        usuario.setAtivo(true);
        usuario.setCriadoEm(audit.data());
        usuario.setCriadoPor(audit.usuario());

        Carteira carteira = new Carteira();
        carteira.setUsuario(usuario);
        carteira.setCodigoEndereco(gerarNovoCodigoValido());
        carteira.setAtivo(true);
        carteira.setTokenEcomel(BigDecimal.ZERO);
        carteira.setSaldoFavos(BigDecimal.ZERO);
        carteira.setCriadoEm(audit.data());
        carteira.setCriadoPor(audit.usuario());
        
        usuario.setCarteira(carteira);

        Usuario salvo = usuarioRepository.save(usuario);
        return buscarPorId(salvo.getId());
    }

    @Transactional
    public void editar(Long id, UsuarioRequest request) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Usuário não encontrado"));

        if (request.email() != null && !request.email().isBlank()) {
            if (usuarioRepository.existsByEmailAndIdNot(request.email(), id)) {
                throw new BusinessException("Este e-mail já está em uso por outro usuário.");
            }
        }

        AuditContext audit = getAuditContext();
        usuario.setNome(request.nome());
        usuario.setEmail(request.email());
        usuario.setAtualizadoEm(audit.data());
        usuario.setAtualizadoPor(audit.usuario());
        
        if (request.senha() != null && !request.senha().isBlank()) {
            usuario.setSenha(passwordEncoder.encode(request.senha()));
        }

        usuarioRepository.save(usuario);
    }

    @Transactional
    public void inativar(Long id) {

        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Usuário não encontrado"));

        if (usuario.getCarteira() == null) {
            throw new BusinessException("Usuário não possui carteira associada.");
        }

        Carteira carteira = usuario.getCarteira();

        //Buscar índice atual (necessário para converter token -> real)
        IndiceGlobal indice = indiceRepository.findFirstByAtivoTrue();

        //Definir limite mínimo (1 centavo)
        BigDecimal limiteMinimo = new BigDecimal("0.01");

        //Calcular saldo em REAL
        BigDecimal saldoReal = carteira.getSaldoReal(indice.getValor());

        //só bloqueia se tiver valor relevante
        boolean possuiSaldoRelevante =
                saldoReal.compareTo(limiteMinimo) >= 0 ||
                carteira.getSaldoFavos().compareTo(limiteMinimo) >= 0;

        if (possuiSaldoRelevante) {
            throw new BusinessException("Conta possui ativos relevantes. Zere os saldos antes de desativar.");
        }

        //Auditoria
        AuditContext audit = getAuditContext();

        usuario.setAtivo(false);
        usuario.setStatus(StatusUsuario.BLOQUEADO);
        usuario.setDesativadoEm(audit.data());
        usuario.setDesativadoPor(audit.usuario());

        //Desativar carteira
        carteira.setAtivo(false);
        carteira.setDesativadoEm(audit.data());
        carteira.setDesativadoPor(audit.usuario());

        usuarioRepository.save(usuario);
    }

    @Transactional
    public UsuarioResponse buscarPorId(Long id) {
        // Nome do método ajustado para coincidir com o Repository discutido anteriormente
        Usuario usuario = usuarioRepository.findByPorId(id, StatusUsuario.ATIVO)
                .orElseThrow(() -> new BusinessException("Usuário não encontrado"));
        
        CarteiraResponse carteiraResumo = carteiraService.obterExtratoPorUsuario(id);
        
        return new UsuarioResponse(
            usuario.getId(),
            usuario.getNome(),
            usuario.getEmail(),
            usuario.getCriadoEm(),
            carteiraResumo
        );
    }

    private String gerarNovoCodigoValido() {
        String ultimo = carteiraRepository.findMaxCodigoEndereco();
        String candidato = geradorCodigo.incrementar(ultimo);

        while (carteiraRepository.existsByCodigoEndereco(candidato)) {
            candidato = geradorCodigo.incrementar(candidato);
        }
        return candidato;
    }
}
