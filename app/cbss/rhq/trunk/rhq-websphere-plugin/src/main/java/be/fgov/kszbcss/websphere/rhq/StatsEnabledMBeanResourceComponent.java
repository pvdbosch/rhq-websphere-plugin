package be.fgov.kszbcss.websphere.rhq;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.beanutils.PropertyUtilsBean;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mc4j.ems.connection.bean.EmsBean;
import org.rhq.core.domain.measurement.MeasurementDataNumeric;
import org.rhq.core.domain.measurement.MeasurementReport;
import org.rhq.core.domain.measurement.MeasurementScheduleRequest;
import org.rhq.plugins.jmx.MBeanResourceComponent;

public class StatsEnabledMBeanResourceComponent extends MBeanResourceComponent {
    private final Log log = LogFactory.getLog(StatsEnabledMBeanResourceComponent.class);
    
    @Override
    protected void getValues(MeasurementReport report, Set requests, EmsBean bean) {
        Set<MeasurementScheduleRequest> simpleRequests = new HashSet<MeasurementScheduleRequest>();
        // We create a new PropertyUtilsBean every time in order to avoid keeping references
        // to class loaders used by EMS (which could create a leak).
        PropertyUtilsBean propUtils = new PropertyUtilsBean();
        Object stats = null;
        Method getStatisticMethod = null;
        for (MeasurementScheduleRequest request : (Set<MeasurementScheduleRequest>)requests) {
            String name = request.getName();
            if (name.startsWith("stats.")) {
                if (stats == null) {
                    stats = bean.getAttribute("stats").getValue();
                    Class<?> statsClass = stats.getClass();
                    if (log.isDebugEnabled()) {
                        try {
                            String[] statsNames = (String[])statsClass.getMethod("getStatisticNames").invoke(stats);
                            log.debug("Loaded Stats object from MBean. Available statistics: " + Arrays.asList(statsNames));
                        } catch (Exception ex) {
                            log.debug("Loaded Stats object from MBean, but unable to get statistic names", ex);
                        }
                    }
                    try {
                        getStatisticMethod = statsClass.getMethod("getStatistic", String.class);
                    } catch (NoSuchMethodException ex) {
                        log.error(ex);
                    }
                }
                if (getStatisticMethod == null) {
                    log.error("Unable to retrieve data for " + name + " because of previous failure");
                } else {
                    int idx = name.indexOf('.', 6);
                    String statisticName = name.substring(6, idx);
                    String propertyName = name.substring(idx+1);
                    Object statistic;
                    try {
                        statistic = getStatisticMethod.invoke(stats, statisticName);
                    } catch (Exception ex) {
                        log.error("Unable to retrieve statistic for " + statisticName, ex);
                        continue;
                    }
                    Long value;
                    try {
                        value = (Long)propUtils.getNestedProperty(statistic, propertyName);
                    } catch (Exception ex) {
                        log.error("Failed to get the " + propertyName + " from the Statistic object for " + statisticName, ex);
                        continue;
                    }
                    if (log.isDebugEnabled()) {
                        log.debug("Adding measurement for " + name + "; value=" + value);
                    }
                    report.addData(new MeasurementDataNumeric(request, new Double(value)));
                }
            } else {
                simpleRequests.add(request);
            }
        }
        if (!simpleRequests.isEmpty()) {
            super.getValues(report, simpleRequests, bean);
        }
    }

}
