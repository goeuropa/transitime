package org.transitclock.core.avl;

import org.transitclock.domain.structs.AvlReport;

public interface AvlReportProcessorFactory {
    AvlReportProcessor createClient(AvlReport report);
}
