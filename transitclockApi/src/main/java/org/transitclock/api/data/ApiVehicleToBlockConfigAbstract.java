/*
 * This file is part of Transitime.org
 * 
 * Transitime.org is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License (GPL) as published by the
 * Free Software Foundation, either version 3 of the License, or any later
 * version.
 * 
 * Transitime.org is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * Transitime.org . If not, see <http://www.gnu.org/licenses/>.
 */

package org.transitclock.api.data;

import java.util.Date;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlTransient;

import org.transitclock.api.rootResources.TransitimeApi.UiMode;
import org.transitclock.ipc.data.IpcVehicle;
import org.transitclock.ipc.data.IpcVehicleToBlockConfig;

/**
 * This class exists so that can have multiple subclasses that inherent from
 * each other while still being able to set the propOrder for each class.
 * Specifically, ApiVehicleDetails is supposed to be a subclass of ApiVehicle.
 * But want the vehicle id to be output as the first attribute. But the
 * attributes for the subclass are output first and one can't normally set the
 * propOrder of parent class attributes. One gets an internal error if one tries
 * to do so.
 * <p>
 * The solution is to use the abstract class ApiVehicleAbstract. Then can
 * implement ApiVehicle and ApiVehicleDetails to inherit from ApiVehicleAbstract
 * and those classes can each set propOrder as desired. Yes, this is rather
 * complicated, but it works.
 *
 * @author SkiBu Smith
 *
 */
@XmlTransient
public abstract class ApiVehicleToBlockConfigAbstract {

	@XmlAttribute
	protected long id;

	@XmlAttribute
	protected String vehicleId;

	@XmlAttribute
	protected Date validFrom;

	@XmlAttribute
	protected Date validTo;

	@XmlAttribute
	protected Date assignmentDate;

	@XmlAttribute
	protected String tripId;

	@XmlAttribute
	protected String blockId;

	/**
	 * Need a no-arg constructor for Jersey. Otherwise get really obtuse
	 * "MessageBodyWriter not found for media type=application/json" exception.
	 */
	protected ApiVehicleToBlockConfigAbstract() {
	}

	/**
	 * Takes a Vehicle object for client/server communication and constructs a
	 * ApiVehicle object for the API.
	 * 
	 * @param vehicle
	 * @param uiType
	 *            If should be labeled as "minor" in output for UI.
	 */
	public ApiVehicleToBlockConfigAbstract(IpcVehicleToBlockConfig vTBC) {
		id = vTBC.getId();
		vehicleId = vTBC.getVehicleId();
		tripId = vTBC.getTripId();
		blockId = vTBC.getBlockId();
		validFrom = vTBC.getValidFrom();
		validTo = vTBC.getValidTo();
		assignmentDate = vTBC.getAssignmentDate();
	}
}
