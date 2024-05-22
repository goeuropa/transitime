/* (C)2023 */
package org.transitclock.api.data;

import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import org.transitclock.domain.structs.ExportTable;

import java.io.Serializable;
import java.util.List;
import java.util.stream.Collectors;

/**
 * For when have list of exports. By using this class can control the element name when data is
 * output.
 *
 * @author Hubert goEuropa
 */
@XmlRootElement
public class ApiExportsData implements Serializable {

    /**
     *
     */
    private static final long serialVersionUID = -9106451522038837974L;

    @XmlElement(name = "exports")
    private List<ApiExportData> apiExportData;

    /********************** Member Functions **************************/

    /**
     * Need a no-arg constructor for Jersey. Otherwise get really obtuse "MessageBodyWriter not
     * found for media type=application/json" exception.
     */
    public ApiExportsData() {
    }

    /**
     *
     */
    public ApiExportsData(List<ExportTable> exportData) {
        apiExportData = exportData.stream()
                .map(ApiExportData::new)
                .collect(Collectors.toList());
    }

    public List<ApiExportData> getExportsData() {
        return apiExportData;
    }

    public void setExportsData(List<ApiExportData> exportsData) {
        this.apiExportData = exportsData;
    }
}
