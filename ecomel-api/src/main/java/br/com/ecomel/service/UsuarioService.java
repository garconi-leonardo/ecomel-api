package br.com.ecomel.service;

import br.com.ecomel.domain.entity.Carteira;
import br.com.ecomel.domain.entity.Usuario;
import br.com.ecomel.dto.request.UsuarioRequest;
import br.com.ecomel.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final UsuarioRepository repository;

    @Transactional
    public Usuario salvar(UsuarioRequest request) {
        Usuario usuario = new Usuario();
        usuario.setNome(request.nome());
        usuario.setEmail(request.email());
        usuario.setSenha(request.senha()); // Futuramente: passwordEncoder.encode(...)

        Carteira carteira = new Carteira();
        carteira.setUsuario(usuario);
        usuario.setCarteira(carteira);

        return repository.save(usuario);
    }
}