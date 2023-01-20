package cuchaz.enigma.source;

// todo make record
public class SourceSettings {
    public final boolean removeImports;
    public final boolean removeVariableFinal;

    public SourceSettings(boolean removeImports, boolean removeVariableFinal) {
        this.removeImports = removeImports;
        this.removeVariableFinal = removeVariableFinal;
    }
}
