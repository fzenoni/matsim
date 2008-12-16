/* *********************************************************************** *
 * project: org.matsim.*																															*
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
package playground.benjamin;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.basic.v01.BasicActImpl;
import org.matsim.basic.v01.BasicActivity;
import org.matsim.basic.v01.BasicKnowledge;
import org.matsim.basic.v01.BasicLeg;
import org.matsim.basic.v01.BasicLegImpl;
import org.matsim.basic.v01.BasicPerson;
import org.matsim.basic.v01.BasicPersonImpl;
import org.matsim.basic.v01.BasicPlan;
import org.matsim.basic.v01.BasicPlanImpl;
import org.matsim.basic.v01.BasicPopulation;
import org.matsim.basic.v01.BasicPopulationImpl;
import org.matsim.basic.v01.BasicPopulationReaderV5;
import org.matsim.basic.v01.BasicRouteImpl;
import org.matsim.basic.v01.Id;
import org.matsim.basic.v01.IdImpl;
import org.matsim.population.PopulationWriterV5;


/**
 * @author dgrether
 *
 */
public class BkTestPlansCreator {

	private static final Logger log = Logger.getLogger(BkTestPlansCreator.class);
	
	private Id id1 = new IdImpl(1);
	private Id id2 = new IdImpl(2);
	private Id id3 = new IdImpl(3);
	private Id id4 = new IdImpl(4);
	private Id id5 = new IdImpl(5);
	
	public BasicPopulation<BasicPerson<BasicPlan, BasicKnowledge<BasicActivity>>> createPlans() {
		double firstHomeEndTime = 6.0 * 3600.0;
		double homeEndTime = firstHomeEndTime;
		
		BasicPopulation<BasicPerson<BasicPlan, BasicKnowledge<BasicActivity>>> pop = new BasicPopulationImpl();
		
		for (int i = 1; i <= 1000; i++) {
			BasicPerson<BasicPlan, BasicKnowledge<BasicActivity>> p = new BasicPersonImpl(new IdImpl(i));
			BasicPlan plan = new BasicPlanImpl();
			plan.setSelected(true);
			p.addPlan(plan);
			
			BasicActImpl act1 = new BasicActImpl("h");
			act1.setLinkId(id1);
			act1.setEndTime(homeEndTime);
			plan.addAct(act1);
			
			BasicLeg leg = new BasicLegImpl(BasicLeg.Mode.car);
			BasicRouteImpl route = new BasicRouteImpl(id1, id4);
			List<Id> linkids = new ArrayList<Id>();
			linkids.add(id2);
			linkids.add(id3);
			route.setLinkIds(linkids);
			leg.setRoute(route);
			plan.addLeg(leg);
			
			BasicActImpl act2 = new BasicActImpl("w");
			act2.setLinkId(id3);
			plan.addAct(act2);
			
			BasicLeg leg2 = new BasicLegImpl(BasicLeg.Mode.car);
			route = new BasicRouteImpl(id4, id5);
			linkids.clear();
			linkids.add(id5);
			linkids.add(id5);
			route.setLinkIds(linkids);
			leg2.setRoute(route);
			plan.addLeg(leg);
			
			BasicActImpl act3 = new BasicActImpl("h");
			act3.setLinkId(id1);
			plan.addAct(act3);
			
			pop.addPerson(p);
			homeEndTime++;				
		}
		
		return pop;
	}
	
	
	/**
	 * @param args
	 * @throws IOException 
	 * @throws FileNotFoundException 
	 */
	public static void main(String[] args) throws FileNotFoundException, IOException {
		String outfile = "./output/myplans.xml";
		BkTestPlansCreator pc = new BkTestPlansCreator();
		BasicPopulation<BasicPerson<BasicPlan, BasicKnowledge<BasicActivity>>> pop = pc.createPlans();
		PopulationWriterV5 writer = new PopulationWriterV5(pop, null);
		writer.writeFile(outfile);
		
		//test if correct...
    pop = new BasicPopulationImpl();
		BasicPopulationReaderV5 reader = new BasicPopulationReaderV5(pop, null);
		reader.readFile(outfile);
	}

}
