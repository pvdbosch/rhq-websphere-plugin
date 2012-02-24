package be.fgov.kszbcss.rhq.websphere.component.server;

import java.util.Set;

import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.measurement.MeasurementReport;
import org.rhq.core.domain.measurement.MeasurementScheduleRequest;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.measurement.MeasurementFacet;

import be.fgov.kszbcss.rhq.websphere.component.WebSphereServiceComponent;
import be.fgov.kszbcss.rhq.websphere.proxy.TransactionService;
import be.fgov.kszbcss.rhq.websphere.support.measurement.MeasurementFacetSupport;
import be.fgov.kszbcss.rhq.websphere.support.measurement.PMIMeasurementHandler;
import be.fgov.kszbcss.rhq.websphere.support.measurement.SimpleMeasurementHandler;

import com.ibm.websphere.pmi.PmiConstants;

public class TransactionServiceComponent extends WebSphereServiceComponent<WebSphereServerComponent> implements MeasurementFacet {
    private MeasurementFacetSupport measurementFacetSupport;
    
    @Override
    protected void start() throws InvalidPluginConfigurationException, Exception {
        measurementFacetSupport = new MeasurementFacetSupport(this);
        measurementFacetSupport.addHandler("stats", new PMIMeasurementHandler(getServer().getServerMBean(), PmiConstants.TRAN_MODULE));
        final TransactionService transactionService = getServer().getMBeanClient("WebSphere:type=TransactionService,*").getProxy(TransactionService.class);
        measurementFacetSupport.addHandler("IndoubtTransactions", new SimpleMeasurementHandler() {
            @Override
            protected Object getValue() throws Exception {
                return transactionService.listImportedPreparedTransactions().length
                        + transactionService.listManualTransactions().length
                        + transactionService.listRetryTransactions().length
                        + transactionService.listHeuristicTransactions().length;
            }
        });
    }

    @Override
    protected boolean isConfigured() throws Exception {
        return true;
    }

    @Override
    protected AvailabilityType doGetAvailability() {
        return AvailabilityType.UP;
    }

    public void getValues(MeasurementReport report, Set<MeasurementScheduleRequest> requests) throws Exception {
        measurementFacetSupport.getValues(report, requests);
    }

    public void stop() {
    }
}