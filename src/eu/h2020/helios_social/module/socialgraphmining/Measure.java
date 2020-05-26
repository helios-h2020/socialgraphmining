package eu.h2020.helios_social.module.socialgraphmining;

import eu.h2020.helios_social.core.contextualegonetwork.Context;
import eu.h2020.helios_social.core.contextualegonetwork.Node;

/**
 * Provides an abstraction of evaluation measures used to assess implementations of {@link SocialGraphMiner}.
 * 
 * @author Emmanouil Krasanakis
 */
public interface Measure {
	/**
	 * Supervised evaluation of interactions send by each node.
	 * @param socialGraphMiner The social graph miner used to perform the prediction.
	 * @param contexct The context in which new interactions are predicted.
	 * @param dst The destination node.
	 * @return The evaluation score - typically a value in the range [0,1], but NaN can be returned
	 * 	if the prediction cannot be assessed (e.g. due to invalid parameters of the measure).
	 */
	public abstract double evaluateSend(SocialGraphMiner socialGraphMiner, Context context, Node dst);
}
