package br.com.ecomel.service;

import br.com.ecomel.domain.entity.Carteira;
import br.com.ecomel.domain.entity.Usuario;
import br.com.ecomel.domain.enums.StatusUsuario;
import br.com.ecomel.dto.request.UsuarioRequest;
import br.com.ecomel.dto.response.CarteiraResponse;
import br.com.ecomel.dto.response.UsuarioResponse;
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
    private final CarteiraService carteiraService; // Usado para pegar a resposta da carteira com cálculos


    @Transactional
    public UsuarioResponse salvar(UsuarioRequest request) {
        // 1. Instância do usuário e criptografia de senha
        Usuario usuario = new Usuario();
        usuario.setNome(request.nome());
        usuario.setEmail(request.email());
        usuario.setSenha(passwordEncoder.encode(request.senha()));
        usuario.setStatus(StatusUsuario.ATIVO);

        // 2. Geração do código AAA000 e vínculo da Carteira
        String novoCodigo = gerarNovoCodigoValido();

        Carteira carteira = new Carteira();
        carteira.setUsuario(usuario);
        carteira.setCodigoEndereco(novoCodigo);
        usuario.setCarteira(carteira);

        // 3. Persistência no banco de dados
        Usuario salvo = usuarioRepository.save(usuario);

        // 4. Retorno do DTO unificado (reutilizando a lógica de busca completa)
        return buscarPorId(salvo.getId());
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
        
        // Regra de saldo zerado (Acessando o objeto carteira diretamente da entidade)
        if (usuario.getCarteira().getSaldoBase().compareTo(BigDecimal.ZERO) > 0 || 
            usuario.getCarteira().getSaldoFavos().compareTo(BigDecimal.ZERO) > 0) {
            throw new BusinessException("Conta possui ativos. Zere os saldos antes de desativar.");
        }

        // Desativa ambos simultaneamente
        usuario.setAtivo(false);
        usuario.setStatus(StatusUsuario.BLOQUEADO);
        usuario.getCarteira().setAtivo(false); // Desativa a carteira junto
        
        usuario.setDesativadoEm(LocalDateTime.now());
        usuarioRepository.save(usuario);
    }    
    @Transactional(readOnly = true)
    public UsuarioResponse buscarPorId(Long id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Usuário não encontrado"));
        
        // Buscamos a carteira através do service para garantir que saldos e dividendos venham calculados
        CarteiraResponse carteiraResumo = carteiraService.obterExtratoPorUsuario(id);
        
        return new UsuarioResponse(
            usuario.getId(),
            usuario.getNome(),
            usuario.getEmail(),
            usuario.getCriadoEm(),
            carteiraResumo
        );
    }
    


}
