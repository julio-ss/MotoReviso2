package br.jss.motoreviso.config;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Tipos de veículo suportados no cadastro e quais seções do formulário
 * cada um exibe (motor convencional, alimentação, dados elétricos).
 *
 * Os nomes são gravados em {@code Veiculo.tipo}; "Moto" e "Carro" mantêm
 * compatibilidade com veículos já cadastrados.
 */
public final class VehicleTypeConfig {

    private VehicleTypeConfig() {}

    public static final class VehicleType {
        public final String nome;
        /** Exibe seção de alimentação (e motor, se não for elétrico). */
        public final boolean possuiMotor;
        /** Exibe seção de bateria/autonomia/recarga. */
        public final boolean isEletrico;

        public VehicleType(String nome, boolean possuiMotor, boolean isEletrico) {
            this.nome = nome;
            this.possuiMotor = possuiMotor;
            this.isEletrico = isEletrico;
        }

        @Override
        public String toString() { return nome; }
    }

    public static final List<VehicleType> TODOS = Collections.unmodifiableList(Arrays.asList(
            new VehicleType("Moto", true, false),
            new VehicleType("Scooter", true, false),
            new VehicleType("Moto Elétrica", true, true),
            new VehicleType("Carro", true, false),
            new VehicleType("Carro Elétrico", true, true),
            new VehicleType("Caminhonete", true, false),
            new VehicleType("Quadriciclo", true, false),
            new VehicleType("Bicicleta", false, false),
            new VehicleType("Bicicleta Elétrica", true, true),
            new VehicleType("Patinete Elétrico", true, true)
    ));

    /** Mesmos valores documentados em {@code Veiculo.tipoAlimentacao}. */
    public static final List<String> TIPOS_ALIMENTACAO = Collections.unmodifiableList(Arrays.asList(
            "Gasolina", "Etanol", "Flex", "Diesel", "GNV", "Elétrico", "Híbrido", "Outro"
    ));

    public static VehicleType findByName(String nome) {
        if (nome == null) return null;
        String n = nome.trim();
        for (VehicleType t : TODOS) {
            if (t.nome.equalsIgnoreCase(n)) return t;
        }
        return null;
    }
}
