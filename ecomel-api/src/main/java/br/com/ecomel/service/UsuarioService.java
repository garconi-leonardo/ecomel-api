package br.com.ecomel.service;

import br.com.ecomel.domain.entity.Carteira;
import br.com.ecomel.domain.entity.Usuario;
import br.com.ecomel.domain.enums.StatusUsuario;
import br.com.ecomel.dto.request.UsuarioRequest;
import br.com.ecomel.exception.BusinessException;
import br.com.ecomel.repository.CarteiraRepository;
import br.com.ecomel.repository.UsuarioRepository;
import br.com.ecomel.util.GeradorCodigoCarteira;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final CarteiraRepository carteiraRepository;
    private final GeradorCodigoCarteira geradorCodigo;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public Usuario salvar(UsuarioRequest request) {
        Usuario usuario = new Usuario();
        usuario.setNome(request.nome());
        usuario.setEmail(request.email()); // Pode ser null
        usuario.setSenha(passwordEncoder.encode(request.senha()));

        String novoCodigo = gerarNovoCodigoValido();

        Carteira carteira = new Carteira();
        carteira.setUsuario(usuario);
        carteira.setCodigoEndereco(novoCodigo);
        
        usuario.setCarteira(carteira);
        return usuarioRepository.save(usuario);
    }


    private String gerarNovoCodigoValido() {
        // 1. Busca o maior código alfanumérico atual (ex: "AAA010")
        String ultimo = carteiraRepository.findMaxCodigoEndereco();
        
        // 2. O utilitário incrementa para o próximo candidato (ex: "AAA011")
        String candidato = geradorCodigo.incrementar(ultimo);

        // 3. ESTA É A VERIFICAÇÃO:
        // Ele consulta o banco. Se o código "AAA011" já existir por algum motivo,
        // ele entra no loop e pula para o "AAA012", e assim por diante.
        while (carteiraRepository.existsByCodigoEndereco(candidato)) {
            candidato = geradorCodigo.incrementar(candidato);
        }
        
        return candidato; // Só retorna um código que o banco confirmou não existir
    }
    
    @Transactional
    public void editar(Long id, UsuarioRequest request) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Usuário não encontrado"));
        
        usuario.setNome(request.nome());
        if (request.email() != null) usuario.setEmail(request.email());
        if (request.senha() != null) usuario.setSenha(passwordEncoder.encode(request.senha()));
        
        usuarioRepository.save(usuario);
    }

    @Transactional
    public void inativar(Long id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Usuário não encontrado"));
        Carteira carteira = usuario.getCarteira();

        // REGRA CRÍTICA: Só inativa se estiver zerado
        boolean possuiSaldoEcm = carteira.getSaldoBase().compareTo(BigDecimal.ZERO) > 0;
        boolean possuiSaldoFavos = carteira.getSaldoFavos().compareTo(BigDecimal.ZERO) > 0;

        if (possuiSaldoEcm || possuiSaldoFavos) {
            throw new BusinessException("Não é possível inativar conta com saldo de ECM ou FAVOS ativo.");
        }

        usuario.setAtivo(false);
        usuario.setStatus(StatusUsuario.BLOQUEADO);
        usuario.setDesativadoEm(LocalDateTime.now());
        usuarioRepository.save(usuario);
    }

}
