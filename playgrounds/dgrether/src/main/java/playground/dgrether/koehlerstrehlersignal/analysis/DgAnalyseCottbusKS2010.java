/* *********************************************************************** *
 * project: org.matsim.*
 * DgAnalyseCottbusBasecase
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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
package playground.dgrether.koehlerstrehlersignal.analysis;

import java.io.BufferedWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.matsim.analysis.VolumesAnalyzer;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.Event;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Time;
import org.matsim.counts.CountSimComparison;
import org.matsim.signalsystems.data.SignalsData;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.geometry.BoundingBox;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;

import playground.dgrether.DgPaths;
import playground.dgrether.analysis.RunResultsLoader;
import playground.dgrether.analysis.simsimanalyser.CountsShapefileWriter;
import playground.dgrether.analysis.simsimanalyser.SimSimAnalysis;
import playground.dgrether.analysis.simsimanalyser.SimSimShapefileWriter;
import playground.dgrether.events.DgNetShrinkImproved;
import playground.dgrether.events.EventsFilterManager;
import playground.dgrether.events.EventsFilterManagerImpl;
import playground.dgrether.events.InMemoryEventsManager;
import playground.dgrether.events.filters.TimeEventFilter;
import playground.dgrether.koehlerstrehlersignal.run.Cottbus2KS2010;
import playground.dgrether.signalsystems.cottbus.CottbusUtils;

import com.vividsolutions.jts.geom.Envelope;


public class DgAnalyseCottbusKS2010 {

	private static final Logger log = Logger.getLogger(DgAnalyseCottbusKS2010.class);

	private static class TimeConfig {
		String name;
		double startTime; 
		double endTime; 
	}

	private static class Extent {
		String name;
		Envelope envelope;
	}

	private static class RunInfo {
		String runId;
		String remark;
		public boolean baseCase = false;
		public Integer iteration;
	}
	
	private static class Result {
		public RunInfo runInfo;
		public RunResultsLoader runLoader;
		public Extent extent;
		public TimeConfig timeConfig;
		public double travelTime;
		public double travelTimeDelta;
		public double averageTravelTime;
		public double numberOfPersons;
		public double travelTimePercent;
		public double personsDelta;
		public VolumesAnalyzer volumes;
		public Network network;
		public DgMfd mfd;
		public double totalDelay;
	}
	
	private static class Results {
		private Map<RunInfo, Map<Extent, Map<TimeConfig,Result>>> resultMap = new HashMap();  
		
		private void addResult(Result result) {
			Map<Extent, Map<TimeConfig, Result>> m = resultMap.get(result.runInfo);
			if (m == null) {
				m = new HashMap();
				resultMap.put(result.runInfo, m);
			}
			Map<TimeConfig, Result> m2 = m.get(result.extent);
			if (m2 == null) {
				m2 = new HashMap();
				m.put(result.extent, m2);
			}
			m2.put(result.timeConfig, result);
		}
		
		public List<Result> getResults() {
			List<Result> ret = new ArrayList<Result>();
			for (Map<Extent, Map<TimeConfig, Result>> m : resultMap.values()) {
				for (Map<TimeConfig, Result> m2 : m.values()) {
					ret.addAll(m2.values());
				}
			}
			return ret;
		}
		
	}
	
	private Results results = new Results();
	private boolean useInMemoryEvents; 

	private void setUseInMemoryEvents(boolean useInMemoryEvents) {
		this.useInMemoryEvents = useInMemoryEvents;
	}

	
	private void analyseResults() {
		Map<Extent, Map<TimeConfig, Result>> baseCase = null;
		for (RunInfo r : results.resultMap.keySet()) {
			if (r.baseCase) {
				baseCase = results.resultMap.get(r);
			}
		}
		for (Result r : results.getResults()) {
			double averageTT = r.travelTime / r.numberOfPersons;
			r.averageTravelTime = averageTT;
			Result baseResult = baseCase.get(r.extent).get(r.timeConfig);
			r.travelTimeDelta = r.travelTime - baseResult.travelTime;
			r.travelTimePercent = r.travelTime / baseResult.travelTime * 100.0;
			r.personsDelta = r.numberOfPersons - baseResult.numberOfPersons;
			if (! r.runInfo.baseCase) {
				this.createAndWriteSimSimComparison(baseResult, r);
			}
			this.writeMfd(r);
		}
	}
	
	private void writeMfd(Result result) {
		String filename = result.runLoader.getIterationFilename(result.runInfo.iteration, "mfd_"+ result.timeConfig.name+ "_" + result.extent.name + ".txt");
		result.mfd.writeFile(filename);
	}
	
	private void createAndWriteSimSimComparison(Result baseResult, Result result) {
		SimSimAnalysis countsAnalysis = new SimSimAnalysis();
		Map<Id, List<CountSimComparison>> countSimCompMap = countsAnalysis.createCountSimComparisonByLinkId(result.network, baseResult.volumes, result.volumes);
		String shapeBase = baseResult.runInfo.runId + "_it_" + baseResult.runInfo.iteration + "_vs_";
		shapeBase += result.runInfo.runId + "_it_" + result.runInfo.iteration;
		
		String shapefile = shapeBase + "_simcountcomparison";
		shapefile = result.runLoader.getIterationFilename(result.runInfo.iteration, shapefile);
		new CountsShapefileWriter(result.network, Cottbus2KS2010.CRS).writeShape(shapefile + ".shp", countSimCompMap, baseResult.runInfo.runId, result.runInfo.runId);

		shapefile = shapeBase + "_simsimcomparison";
		shapefile = result.runLoader.getIterationFilename(result.runInfo.iteration, shapefile);
		new SimSimShapefileWriter(result.network, Cottbus2KS2010.CRS).writeShape(shapefile + ".shp", countSimCompMap, baseResult.runInfo.runId, result.runInfo.runId);
	}
	
	private void writeAverageTravelTimesToFile(String file) {
		List<String> lines = new ArrayList<String>();
		StringBuilder header = new StringBuilder();
		header.append("runId");
		header.append("\t");
		header.append("Iteration");
		header.append("\t");
		header.append("run?");
		header.append("\t");
		header.append("extent");
		header.append("\t");
		header.append("time interval");
		header.append("\t");
		header.append("travel time [s]");
		header.append("\t");
		header.append("travel time [hh:mm:ss]");
		header.append("\t");
		header.append("delta travel time [s]");
		header.append("\t");
		header.append("delta travel time [hh:mm:ss]");
		header.append("\t");
		header.append("delta travel time [%]");
		header.append("\t");
		header.append("number of drivers");
		header.append("\t");
		header.append("delta drivers");
		header.append("\t");
		header.append("average travel time");
		header.append("\t");
		header.append("total delay");
		header.append("\t");
		lines.add(header.toString());
		for (Result r : results.getResults()) {
			StringBuilder out = new StringBuilder();
			out.append(r.runInfo.runId);
			out.append("\t");		
			out.append(Integer.toString(r.runInfo.iteration));
			out.append("\t");
			out.append(r.runInfo.remark);
			out.append("\t");
			out.append(r.extent.name);
			out.append("\t");
			out.append(r.timeConfig.name);
			out.append("\t");
			out.append(formatDouble(r.travelTime));
			out.append("\t");
			out.append(Time.writeTime(r.travelTime));
			out.append("\t");
			out.append(formatDouble(r.travelTimeDelta));
			out.append("\t");
			out.append(Time.writeTime(r.travelTimeDelta));
			out.append("\t");
			out.append(formatDouble(r.travelTimePercent));
			out.append("\t");
			out.append(formatDouble(r.numberOfPersons));
			out.append("\t");
			out.append(formatDouble(r.personsDelta));
			out.append("\t");
			out.append(formatDouble(r.averageTravelTime));
			out.append("\t");
			out.append(formatDouble(r.totalDelay));
			out.append("\t");
				lines.add(out.toString());
			log.info(out.toString());
		}
		
		BufferedWriter bw = IOUtils.getBufferedWriter(file);
		try {
			log.info("Result");
			for (String l : lines) {
				System.out.println(l);
				bw.write(l);
				bw.newLine();
			}
			bw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private String formatDouble(double d){
		DecimalFormat format = new DecimalFormat("#.#");
		return format.format(d);
	}
	
	//Average traveltime
	// Average speed
	// Macroscopic fundamental diagram
	// Leg histogram, see DgCottbusLegHistogram or LHI
	// Traffic difference qgis

	private void calculateResults(List<RunInfo> runInfos, List<TimeConfig> times, List<Extent> extents) {
		for (RunInfo runInfo: runInfos) {
			String runId = runInfo.runId;
			String runDirectory = DgPaths.REPOS + "runs-svn/run"+runId+"/";
//			String runDirectory = "C:/Users/Atany/Desktop/SHK/SVN/runs-svn/run"+runId+"/";
			RunResultsLoader runDir = new RunResultsLoader(runDirectory, runId);
			
			InMemoryEventsManager inMemoryEvents = null;
			if (useInMemoryEvents) {
				inMemoryEvents = new InMemoryEventsManager();
				MatsimEventsReader reader = new MatsimEventsReader(inMemoryEvents);
				reader.readFile(runDir.getEventsFilename(runInfo.iteration));
			}

			
			for (Extent extent : extents) {
				Network net; 
				if (extent.envelope != null) {
					net = new DgNetShrinkImproved().createSmallNetwork(runDir.getNetwork(), extent.envelope);
				}
				else {
					net = runDir.getNetwork();
				}
				for (TimeConfig time : times) {
					
					Result result = new Result();
					result.runInfo = runInfo;
					result.runLoader = runDir;
					result.extent = extent;
					result.timeConfig = time;
					result.network = net;
					results.addResult(result);
					
					EventsFilterManager eventsManager = new EventsFilterManagerImpl();
					TimeEventFilter tef = new TimeEventFilter();
					tef.setStartTime(time.startTime);
					tef.setEndTime(time.endTime);
					eventsManager.addFilter(tef);
					
					DgAverageTravelTimeSpeed avgTtSpeed = new DgAverageTravelTimeSpeed(net);
					eventsManager.addHandler(avgTtSpeed);
					
					VolumesAnalyzer volumes = new VolumesAnalyzer(3600, 24 * 3600, net);
					eventsManager.addHandler(volumes);
					
					DgMfd mfd = new DgMfd(net);
					eventsManager.addHandler(mfd);
					
					SignalsData signals = runDir.getSignals();
					TotalDelay totalDelay = new TotalDelay(net, signals);
					eventsManager.addHandler(totalDelay);
					
					if (useInMemoryEvents){
						for (Event e : inMemoryEvents.getEvents()){
							eventsManager.processEvent(e);
						}
					}
					else {
						MatsimEventsReader reader = new MatsimEventsReader(eventsManager);
						reader.readFile(runDir.getEventsFilename(runInfo.iteration));
					}

					result.volumes = volumes;
					result.travelTime = avgTtSpeed.getTravelTime();
					result.numberOfPersons = avgTtSpeed.getNumberOfPersons();
					result.mfd = mfd;
					result.totalDelay = totalDelay.getTotalDelay();
					
					log.info("Total travel time : " + avgTtSpeed.getTravelTime() + " number of persons: " + avgTtSpeed.getNumberOfPersons());
				}
			}
		}
		log.info("Calculated results.");
	}


	private static List<TimeConfig> createTimeConfig(){
		TimeConfig morning = new TimeConfig();
		morning.startTime = 5.0 * 3600.0;
		morning.endTime  = 10.0 * 3600.0;
		morning.name = "morning";
		TimeConfig evening = new TimeConfig();
		evening.startTime = 13.0 * 3600.0;
		evening.endTime = 20.0 * 3600.0;
		evening.name = "afternoon";
		TimeConfig all = new TimeConfig();
		all.startTime = 0.0;
		all.endTime = 24.0 * 3600.0;
		all.name = "all_day";
		List<TimeConfig> list = new ArrayList<TimeConfig>();
		list.add(morning);
		list.add(evening);
		list.add(all);
		return list;
	}

	private static List<RunInfo> createRunsIdList(){
		List<RunInfo> l = new ArrayList<RunInfo>();
		RunInfo ri = null;
		
//		ri = new RunInfo();
//		ri.runId = "1712";
//		ri.remark = "base case";
//		ri.baseCase  = true;
//		ri.iteration = 1000;
//		l.add(ri);
		
//		ri = new RunInfo();
//		ri.runId = "1722";
//		ri.remark = "base_case";
//		ri.baseCase  = true;
//		ri.iteration = 1000;
//		l.add(ri);
//
//		ri = new RunInfo();
//		ri.runId = "1724";
//		ri.remark = "0.5 flow cap";
//		ri.iteration = 1000;
//		l.add(ri);
//
//		ri = new RunInfo();
//		ri.runId = "1725";
//		ri.remark = "0.3 flow cap";
//		ri.iteration = 1000;
//		l.add(ri);

		
//		ri = new RunInfo();
//		ri.runId = "1726";
//		ri.remark = "base_case_more_learning";
//		ri.baseCase = true;
//		ri.iteration = 1000;
//		l.add(ri);
//		
//		ri = new RunInfo();
//		ri.runId = "1727";
//		ri.remark = "base_case_new";
//		ri.iteration = 2000;
//		l.add(ri);

		
		//		ri = new RunInfo();
//		ri.runId = "1730";
//		ri.remark = "from scratch, com > 50";
//		l.add(ri);
//		ri = new RunInfo();
//		ri.runId = "1731";
//		ri.remark  = "from scratch, com > 10";
//		l.add(ri);

//		ri = new RunInfo();
//		ri.runId = "1734";
//		ri.baseCase = true;
//		ri.iteration = 1000;
//		ri.remark  = "base case, continue 1712";
//		l.add(ri);
//		
//		ri = new RunInfo();
//		ri.runId = "1732";
//		ri.iteration = 1000;
//		ri.remark = "continue 1712, com > 50";
//		l.add(ri);
//		
//		ri = new RunInfo();
//		ri.runId = "1733";
//		ri.iteration = 1000;
//		ri.remark  = "continue 1712, com > 10";
//		l.add(ri);
		
////
//		ri = new RunInfo();
//		ri.runId = "1735";
//		ri.iteration = 1500;
//		ri.remark = "continue 1722, com > 50";
//		l.add(ri);
//
//		ri = new RunInfo();
//		ri.runId = "1736";
//		ri.iteration = 1500;
//		ri.remark  = "continue 1722, com > 10";
//		l.add(ri);
//
		
//		ri = new RunInfo();
//		ri.runId = "1740";
//		ri.iteration = 2000;
//		ri.baseCase = true;
//		ri.remark  = "base case 1722 it 2000";
//		l.add(ri);
//
//		ri = new RunInfo();
//		ri.runId = "1737";
//		ri.iteration = 2000;
//		ri.remark  = "continue 1722, com > 10, new";
//		l.add(ri);
////		
//		ri = new RunInfo();
//		ri.runId = "1741";
//		ri.iteration = 2000;
//		ri.remark  = "sylvia: continue base case 1722 for 1000 iterations";
//		l.add(ri);

		ri = new RunInfo();
		ri.runId = "1900";
		ri.iteration = 1500;
		ri.baseCase = true;
		ri.remark  = "base case 1722 it 2000, no time choice";
		l.add(ri);

		ri = new RunInfo();
		ri.runId = "1901";
		ri.iteration = 1500;
		ri.remark  = "continue 1722, com > 10, new, no time choice";
		l.add(ri);

		ri = new RunInfo();
		ri.runId = "1902";
		ri.iteration = 1500;
		ri.remark  = "sylvia: continue base case 1722 for 1000 iterations, no time choice";
		l.add(ri);
		
		
//		ri = new RunInfo();
//		ri.runId = "1742";
//		ri.iteration = 1000;
//		ri.remark  = "start it 0: continue base case 1722 for 1000 iterations";
//		l.add(ri);

//		ri = new RunInfo();
//		ri.runId = "1743";
//		ri.iteration = 2000;
//		ri.baseCase = true;
//		ri.remark  = "continue base case 1726 for 1000 iterations";
//		l.add(ri);
//
//		ri = new RunInfo();
//		ri.runId = "1744";
//		ri.iteration = 1000;
//		ri.remark  = "start it 0: continue base case 1726 for 1000 iterations";
//		l.add(ri);

		
//		ri = new RunInfo();
//		ri.runId = "1745";
//		ri.iteration = 2000;
//		ri.baseCase = true;
//		ri.remark  = "base case 1712 it 2000";
//		l.add(ri);
//
//		ri = new RunInfo();
//		ri.runId = "1746";
//		ri.iteration = 2000;
//		ri.remark = "continue 1712, com > 10";
//		l.add(ri);
//
//		ri = new RunInfo();
//		ri.runId = "1747";
//		ri.iteration = 2000;
//		ri.remark  = "continue 1712, com > 50";
//		l.add(ri);
//
//		ri = new RunInfo();
//		ri.runId = "1748";
//		ri.iteration = 2000;
//		ri.remark  = "sylvia: continue base case 1712 for 1000 iterations";
//		l.add(ri);
		
//		ri = new RunInfo();
//		ri.runId = "1745";
//		ri.iteration = 2000;
//		ri.baseCase = true;
//		ri.remark  = "base case 1712 it 2000";
//		l.add(ri);
//
//		ri = new RunInfo();
//		ri.runId = "1746";
//		ri.iteration = 2000;
//		ri.remark = "continue 1712, com > 10";
//		l.add(ri);
//
//		ri = new RunInfo();
//		ri.runId = "1747";
//		ri.iteration = 2000;
//		ri.remark  = "continue 1712, com > 50";
//		l.add(ri);
//
//		ri = new RunInfo();
//		ri.runId = "1748";
//		ri.iteration = 2000;
//		ri.remark  = "sylvia: continue base case 1712 for 1000 iterations";
//		l.add(ri);

//		ri = new RunInfo();
//		ri.runId = "1722";
//		ri.iteration = 1000;
//		ri.baseCase = true;
//		ri.remark  = "base case, 0.7 cap";
//		l.add(ri);
//		
//		ri = new RunInfo();
//		ri.runId = "1940";
//		ri.iteration = 1000;
//		ri.remark  = "base case, 0.5 cap";
//		l.add(ri);
//
//		ri = new RunInfo();
//		ri.runId = "1941";
//		ri.iteration = 1000;
//		ri.remark  = "base case, 0.3 cap";
//		l.add(ri);
//
//		ri = new RunInfo();
//		ri.runId = "1942";
//		ri.iteration = 1000;
//		ri.remark  = "base case, 0.1 cap";
//		l.add(ri);

		
		return l;
	}

	private static Envelope transform(Envelope env, CoordinateReferenceSystem from, CoordinateReferenceSystem to) {
		RuntimeException ex; 
		try {
			MathTransform transformation = CRS.findMathTransform(from, to, true);
			Envelope transEnv = JTS.transform(env, transformation);
			return transEnv;
		} catch (TransformException e) {
			e.printStackTrace();
			ex = new RuntimeException(e);
		}
		catch (FactoryException e) {
			e.printStackTrace();
			ex = new RuntimeException(e);
		}
		throw ex;
	}

	private static Envelope getTransformedEnvelope(Tuple<CoordinateReferenceSystem, SimpleFeature> featureTuple) {
		BoundingBox bounds = featureTuple.getSecond().getBounds();
		Envelope env = new Envelope(bounds.getMinX(), bounds.getMaxX(), bounds.getMinY(), bounds.getMaxY());
		env = transform(env, featureTuple.getFirst(), Cottbus2KS2010.CRS);
		return env;
	}

	private static List<Extent> createExtentList(){
		List<Extent> l = new ArrayList<Extent>();
		String filterFeatureFilename = DgPaths.REPOS
				+ "shared-svn/studies/countries/de/brandenburg_gemeinde_kreisgrenzen/kreise/dlm_kreis.shp";
//		String filterFeatureFilename = "C:/Users/Atany/Desktop/SHK/SVN/"
//				+ "shared-svn/studies/countries/de/brandenburg_gemeinde_kreisgrenzen/kreise/dlm_kreis.shp";
		Tuple<CoordinateReferenceSystem, SimpleFeature> featureTuple = CottbusUtils.loadCottbusFeature(filterFeatureFilename);
		Envelope env = getTransformedEnvelope(featureTuple);
		Extent e = new Extent();
//		e.name = "Cottbus Kreis BB";
//		e.envelope = env;
//		l.add(e);
		
		filterFeatureFilename = DgPaths.REPOS
				+ "shared-svn/projects/cottbus/cb2ks2010/2013-07-31_minflow_10_evening_peak/shapes/bounding_box.shp";
//		filterFeatureFilename = "C:/Users/Atany/Desktop/SHK/SVN/"
//				+ "shared-svn/projects/cottbus/cb2ks2010/2013-07-31_minflow_10_evening_peak/shapes/bounding_box.shp";
		featureTuple = CottbusUtils.loadFeature(filterFeatureFilename);
		
		env = getTransformedEnvelope(featureTuple);
		e = new Extent();
		e.name = "signals_bb";
		e.envelope = env;
		l.add(e);
		
		e = new Extent();
		e.name = "all";
		l.add(e);
		
		return l;
	}

	public static void main(String[] args) {
		List<RunInfo> runIds = createRunsIdList();
		List<TimeConfig> times = createTimeConfig();
		List<Extent> extents = createExtentList();
		DgAnalyseCottbusKS2010 ana = new DgAnalyseCottbusKS2010();
		ana.setUseInMemoryEvents(true);
		ana.calculateResults(runIds, times, extents);
		ana.analyseResults();
		String outputDirectory = DgPaths.SHAREDSVN + "projects/cottbus/cb2ks2010/results/";
//		String outputDirectory = "C:/Users/Atany/Desktop/SHK/SVN/shared-svn/projects/cottbus/cb2ks2010/results/";
		String outputFilename = outputDirectory + "2013-08-28_travel_times_extent_1740_1737_1741.txt";
		ana.writeAverageTravelTimesToFile(outputFilename);

	}



}
