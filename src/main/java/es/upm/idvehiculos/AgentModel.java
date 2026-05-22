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
        switch (value) {
            case "PerceptionAgent":
                return PERCEPTION;
            case "ProcessingAgent":
                return PROCESSING;
            case "UIAgent":
                return UI;
            default:
                return DESCONOCIDO;
        }
    }
}