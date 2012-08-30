/* *********************************************************************** *
 * project: org.matsim.*
 * WarmEmissionEvent.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */
package playground.andreas.aas.modules.multiAnalyzer.emissions.events;

import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.events.Event;

import playground.andreas.aas.modules.multiAnalyzer.emissions.types.WarmPollutant;

/**
 * Event to indicate that warm emissions were produced.
 * @author benjamin
 *
 */
public interface WarmEmissionEvent extends Event{

	public final static String EVENT_TYPE = "warmEmissionEvent";
	
	public final static String ATTRIBUTE_LINK_ID = "linkId";
	public final static String ATTRIBUTE_VEHICLE_ID = "vehicleId";
	
	public Id getLinkId();
	
	public Id getVehicleId();

	public Map<WarmPollutant, Double> getWarmEmissions();
}