package com.infoway.infofolga.model;

public enum Role {
    FUNCIONARIO("FUNCIONARIO"),
    GERENTE("GERENTE"),
    // Acompanha a equipe (relatórios, pontuação), mas não aprova folgas.
    RH("RH"),
    CEO("CEO");

    private String role;

    Role(String role) {
        this.role = role;
    }

    public String getRole() {
        return role;
    }
}