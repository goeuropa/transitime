/*
 * This file is part of Transitime.org
 * 
 * Transitime.org is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL) as published by
 * the Free Software Foundation, either version 3 of the License, or
 * any later version.
 *
 * Transitime.org is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Transitime.org .  If not, see <http://www.gnu.org/licenses/>.
 */

package org.transitime.api.data;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.transitime.ipc.data.IpcBlock;
import org.transitime.ipc.data.IpcRouteSummary;
import org.transitime.ipc.data.IpcTrip;
import org.transitime.utils.Time;

/**
 *
 *
 * @author SkiBu Smith
 *
 */
@XmlRootElement(name="block")
public class ApiBlock {

    @XmlAttribute
    private int configRev;
    
    @XmlAttribute
    private String id;
    
    @XmlAttribute
    private String serviceId;

    @XmlAttribute
    private String startTime; // In seconds from midnight

    @XmlAttribute
    private String endTime; // In seconds from midnight

    @XmlAttribute
    private String headwaySecs;

    @XmlElement
    private List<ApiTrip> trips;

    @XmlElement(name="route")
    private List<ApiRouteSummary> routeSummaries;

    /********************** Member Functions **************************/

    protected ApiBlock() {}
    
    public ApiBlock(IpcBlock ipcBlock) {
	configRev = ipcBlock.getConfigRev();
	id = ipcBlock.getId();
	serviceId = ipcBlock.getServiceId();
	startTime = Time.timeOfDayStr(ipcBlock.getStartTime());
	endTime = Time.timeOfDayStr(ipcBlock.getEndTime());
	headwaySecs = ipcBlock.getHeadwaySecs() != 0 ?
		Integer.toString(ipcBlock.getHeadwaySecs()) : null;
		
	trips = new ArrayList<ApiTrip>();
	for (IpcTrip ipcTrip : ipcBlock.getTrips()) {
	    // Note: not including stop paths in trip pattern output
	    // because that can bve really voluminous.
	    trips.add(new ApiTrip(ipcTrip, false));
	}
	
	routeSummaries = new ArrayList<ApiRouteSummary>();
	for (IpcRouteSummary ipcRouteSummary : ipcBlock.getRouteSummaries()) {
	    routeSummaries.add(new ApiRouteSummary(ipcRouteSummary));
	}
    }
}