package es.upm.idvehiculos;

public enum AgentModel {
    PERCEPTION("PerceptionAgent"),
    PROCESSING("ProcessingAgent"),
    UI("UIAgent"),
    DESCONOCIDO("Desconocido");

    private final String value;

    AgentModel(String value) {
        this.value = value;
    }

    public String getValue() {
        return this.value;
    }

    public static AgentModel getEnum(String value) {
        return switch (value) {
            case "PerceptionAgent" -> PERCEPTION;
            case "ProcessingAgent" -> PROCESSING;
            case "UIAgent" -> UI;
            default -> DESCONOCIDO;
        };
    }
}