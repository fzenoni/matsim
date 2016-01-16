package floetteroed.opdyts.logging;

import floetteroed.opdyts.DecisionVariable;
import floetteroed.opdyts.trajectorysampling.SamplingStage;
import floetteroed.utilities.statisticslogging.Statistic;

/**
 * TODO This should become general-purpose.
 * 
 * @author Gunnar Flötteröd
 *
 */
public class FreeMemory<U extends DecisionVariable> implements
		Statistic<SamplingStage<U>> {

	@Override
	public String label() {
		return "Free Memory";
	}

	@Override
	public String value(final SamplingStage<U> samplingStage) {
		return Long.toString(Runtime.getRuntime().freeMemory());
	}

}
