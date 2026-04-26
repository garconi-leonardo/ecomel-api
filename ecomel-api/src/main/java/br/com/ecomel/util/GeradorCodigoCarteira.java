package br.com.ecomel.util;

import org.springframework.stereotype.Component;

@Component
public class GeradorCodigoCarteira {
	
	//Esta classe gerencia a lógica de incremento (AAA999 -> AAB000).

    public String incrementar(String ultimoCodigo) {
        if (ultimoCodigo == null || ultimoCodigo.isBlank()) {
            return "AAA000";
        }

        char[] chars = ultimoCodigo.toCharArray();
        int i = chars.length - 1;

        while (i >= 0) {
            char c = chars[i];
            if (Character.isDigit(c)) {
                if (c < '9') {
                    chars[i]++;
                    return new String(chars);
                } else {
                    chars[i] = '0';
                }
            } else if (Character.isLetter(c)) {
                if (c < 'Z') {
                    chars[i]++;
                    return new String(chars);
                } else {
                    chars[i] = 'A';
                }
            }
            i--;
        }
        // Se esgotar (ex: ZZZ999), expande para AAAA0000
        return "A" + ultimoCodigo.replace('Z', 'A').replace('9', '0') + "0";
    }
}