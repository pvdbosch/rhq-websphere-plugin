package be.fgov.kszbcss.rhq.websphere.component.xm;

public class UnixProcessComponent extends SingletonPMIModuleComponent {
    @Override
    protected String getPMIModuleName() {
        return "ProcStats";
    }
}
