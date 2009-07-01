/* *********************************************************************** *
 * project: org.matsim.*
 * SelectRiskyPlan.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

/**
 * 
 */
package playground.johannes.itsc08;

import org.matsim.core.api.network.Link;
import org.matsim.core.api.population.NetworkRoute;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.replanning.selectors.PlanSelector;

/**
 * @author illenberger
 *
 */
public class ForceSelectPlan implements PlanSelector {

	private Link link;
	
	public ForceSelectPlan(Link link) {
		this.link = link;
	}
	/* (non-Javadoc)
	 * @see org.matsim.replanning.selectors.PlanSelector#selectPlan(org.matsim.population.Person)
	 */
	public PlanImpl selectPlan(PersonImpl person) {
		PlanImpl plan = null;
		for(PlanImpl p : person.getPlans()) {
			LegImpl leg = (LegImpl) p.getPlanElements().get(1);
			if(((NetworkRoute) leg.getRoute()).getLinkIds().contains(link.getId())) {
				plan = p;
				break;
			}
		}
		return plan;
	}

}
