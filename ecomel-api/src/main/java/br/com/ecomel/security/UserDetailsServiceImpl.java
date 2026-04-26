package br.com.ecomel.security;

import br.com.ecomel.domain.entity.Carteira;
import br.com.ecomel.repository.CarteiraRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import java.util.ArrayList;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final CarteiraRepository carteiraRepository;

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
}
