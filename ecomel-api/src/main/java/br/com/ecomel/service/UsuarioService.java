package br.com.ecomel.service;

import br.com.ecomel.domain.entity.Carteira;
import br.com.ecomel.domain.entity.Usuario;
import br.com.ecomel.dto.request.UsuarioRequest;
import br.com.ecomel.repository.CarteiraRepository;
import br.com.ecomel.repository.UsuarioRepository;
import br.com.ecomel.util.GeradorCodigoCarteira;
import lombok.RequiredArgsConstructor;
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

}
