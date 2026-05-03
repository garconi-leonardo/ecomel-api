package br.com.ecomel.security;

import br.com.ecomel.domain.entity.Carteira;
import br.com.ecomel.domain.entity.Usuario;
import br.com.ecomel.domain.enums.StatusUsuario;
import br.com.ecomel.exception.BusinessException;
import br.com.ecomel.repository.CarteiraRepository;
import br.com.ecomel.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;

import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.Collections;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    /*
    @Override
    public UserDetails loadUserByUsername(String codigoEndereco) throws UsernameNotFoundException {
        // login é realizdo pelo código AAA001
        Carteira carteira = carteiraRepository.findByCodigoEndereco(codigoEndereco)
                .orElseThrow(() -> new UsernameNotFoundException("Carteira não encontrada: " + codigoEndereco));

        return new User(
                carteira.getCodigoEndereco(), 
                carteira.getUsuario().getSenha(), 
                new ArrayList<>()
        );
    }
    */
    
    @Override
    public UserDetails loadUserByUsername(String login) throws UsernameNotFoundException {
        Usuario usuario = usuarioRepository.findByEmailOuCodigoCarteira(login, StatusUsuario.ATIVO)
                .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado com: " + login));

        return org.springframework.security.core.userdetails.User
                .withUsername(login) // Pode ser o e-mail ou o código
                .password(usuario.getSenha())
                .authorities(Collections.emptyList()) // Adicione roles se tiver
                .build();
    }
    
    public void executarComRetry(Runnable operacao) {
        int tentativas = 0;
        int maxTentativas = 3;

        while (true) {
            try {
                operacao.run();
                return;
            } catch (ObjectOptimisticLockingFailureException e) {
                tentativas++;

                if (tentativas >= maxTentativas) {
                    throw new BusinessException("Sistema ocupado. Tente novamente.");
                }

                try {
                    Thread.sleep(50); // pequeno backoff
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

}
