package br.com.acervodaatletabrasileira.acervoapi.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

public class FotoAcervo {

    private String id;
    private String url;
    private String legenda;

    @JsonProperty("ehDestaque")
    private Boolean ehDestaque;

    // ==========================
    // CONSTRUTORES
    // ==========================

    public FotoAcervo() {
        this.id = UUID.randomUUID().toString();
        this.ehDestaque = false;
    }

    public FotoAcervo(String url, String legenda) {
        this.id = UUID.randomUUID().toString();
        this.url = url;
        this.legenda = legenda;
        this.ehDestaque = false;
    }

    // ==========================
    // GETTERS / SETTERS
    // ==========================

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getLegenda() {
        return legenda;
    }

    public void setLegenda(String legenda) {
        this.legenda = legenda;
    }

    public Boolean getEhDestaque() {
        return ehDestaque;
    }

    public void setEhDestaque(Boolean ehDestaque) {
        this.ehDestaque = ehDestaque;
    }
}
