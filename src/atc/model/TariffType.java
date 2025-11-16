package atc.model;

public enum TariffType {
    REGULAR("Обычный"),
    PRIVILEGED("Льготный");

    private final String title;

    TariffType(String title) { this.title = title; }

    @Override public String toString() { return title; }
}