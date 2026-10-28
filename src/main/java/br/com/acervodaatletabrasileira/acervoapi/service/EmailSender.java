package br.com.acervodaatletabrasileira.acervoapi.service;

import reactor.core.publisher.Mono;

/** Envio de e-mails transacionais (a implementação atual usa o Resend). */
public interface EmailSender {

    Mono<Void> enviar(String para, String assunto, String html, String texto);
}
