/* *********************************************************************** *
 * project: org.matsim.*
 * ReRoutingTest.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package org.matsim.integration.replanning;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.ControlerConfigGroup.EventsFileFormat;
import org.matsim.core.config.groups.ControlerConfigGroup.RoutingAlgorithmType;
import org.matsim.core.controler.Controler;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.examples.ExamplesUtils;
import org.matsim.testcases.MatsimTestUtils;

import java.io.File;
import java.net.MalformedURLException;
import java.util.EnumSet;

public class ReRoutingIT {

	@Rule
	public MatsimTestUtils utils = new MatsimTestUtils();

	private Scenario loadScenario() {
		Config config = utils.loadConfig(IOUtils.newUrl(utils.classInputResourcePath(), "config.xml"));
		config.network().setInputFile(IOUtils.newUrl(ExamplesUtils.getTestScenarioURL("berlin"), "network.xml.gz").toString());
		config.plans().setInputFile(IOUtils.newUrl(ExamplesUtils.getTestScenarioURL("berlin"), "plans_hwh_1pct.xml.gz").toString());
		config.qsim().setTimeStepSize(10.0);
        config.qsim().setStuckTime(100.0);
        config.qsim().setRemoveStuckVehicles(true);
		config.controler().setEventsFileFormats(EnumSet.of(EventsFileFormat.xml));
		config.controler().setLastIteration(1);

		/*
		 * The input plans file is not sorted. After switching from TreeMap to LinkedHashMap
		 * to store the persons in the population, we have to sort the population manually.  
		 * cdobler, oct'11
		 */
		Scenario scenario = ScenarioUtils.loadScenario(config);
		PopulationUtils.sortPersons(scenario.getPopulation());
		return scenario;
	}

	@Test
	public void testReRoutingDijkstra() throws MalformedURLException {
		Scenario scenario = this.loadScenario();
		scenario.getConfig().controler().setRoutingAlgorithmType(RoutingAlgorithmType.Dijkstra);
		Controler controler = new Controler(scenario);
        controler.getConfig().controler().setCreateGraphs(false);
		controler.getConfig().controler().setDumpDataAtEnd(false);
		controler.run();
		this.evaluate();
	}

	@Test
	public void testReRoutingFastDijkstra() throws MalformedURLException {
		Scenario scenario = this.loadScenario();
		scenario.getConfig().controler().setRoutingAlgorithmType(RoutingAlgorithmType.FastDijkstra);
		Controler controler = new Controler(scenario);
        controler.getConfig().controler().setCreateGraphs(false);
		controler.getConfig().controler().setDumpDataAtEnd(false);
		controler.run();
		this.evaluate();
	}
	
	/**
	 * This test seems to have race conditions somewhere (i.e. it fails intermittently without code changes). kai, aug'13
	 */
	@Test
	public void testReRoutingAStarLandmarks() throws MalformedURLException {
		Scenario scenario = this.loadScenario();
		scenario.getConfig().controler().setRoutingAlgorithmType(RoutingAlgorithmType.AStarLandmarks);
		Controler controler = new Controler(scenario);
        controler.getConfig().controler().setCreateGraphs(false);
		controler.getConfig().controler().setDumpDataAtEnd(false);
		controler.run();
		this.evaluate();
	}

	@Test
	public void testReRoutingFastAStarLandmarks() throws MalformedURLException {
		Scenario scenario = this.loadScenario();
		scenario.getConfig().controler().setRoutingAlgorithmType(RoutingAlgorithmType.FastAStarLandmarks);
		Controler controler = new Controler(scenario);
        controler.getConfig().controler().setCreateGraphs(false);
		controler.getConfig().controler().setDumpDataAtEnd(false);
		controler.run();
		this.evaluate();
	}
	
	private void evaluate() throws MalformedURLException {
		Config config = utils.loadConfig(IOUtils.newUrl(utils.classInputResourcePath(), "config.xml"));
		config.network().setInputFile(IOUtils.newUrl(ExamplesUtils.getTestScenarioURL("berlin"), "network.xml.gz").toString());
		config.plans().setInputFile(IOUtils.newUrl(utils.inputResourcePath(), "1.plans.xml.gz").toString());
		Scenario referenceScenario = ScenarioUtils.loadScenario(config);

		config.plans().setInputFile(new File(utils.getOutputDirectory() + "ITERS/it.1/1.plans.xml.gz").toURI().toURL().toString());
		Scenario scenario = ScenarioUtils.loadScenario(config);

		final boolean isEqual = PopulationUtils.equalPopulation(referenceScenario.getPopulation(), scenario.getPopulation());
		if ( !isEqual ) {
			new PopulationWriter(referenceScenario.getPopulation(), scenario.getNetwork()).write(utils.getOutputDirectory() + "/reference_population.xml.gz");
			new PopulationWriter(scenario.getPopulation(), scenario.getNetwork()).write(utils.getOutputDirectory() + "/output_population.xml.gz");
		}
		Assert.assertTrue("different plans files.", isEqual);
	}

}
