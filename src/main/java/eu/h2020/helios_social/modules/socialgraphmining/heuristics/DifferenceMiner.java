package eu.h2020.helios_social.modules.socialgraphmining.heuristics;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import eu.h2020.helios_social.core.contextualegonetwork.Context;
import eu.h2020.helios_social.core.contextualegonetwork.Interaction;
import eu.h2020.helios_social.core.contextualegonetwork.Node;
import eu.h2020.helios_social.core.contextualegonetwork.Utils;
import eu.h2020.helios_social.modules.socialgraphmining.SocialGraphMiner;

/**
 * This class provides a {@link SocialGraphMiner} that wraps a miner predictions so as not to predict the top of a base miner's predictions
 * (e.g. DHR@k,withhold of a miner is obtained if HitRate@k is calculated over the outcome of <code>
 * new AdditionalDiscoveryMiner(miner, new RepeatAndReplyMiner(cen), withhold)</code>.
 * @author Emmanouil Krasanakis
 */
public class DifferenceMiner extends SocialGraphMiner {
	private SocialGraphMiner discoveryMiner, baseMiner;
	private int withholdTopOfBaseMiner;

	public DifferenceMiner(SocialGraphMiner discoveryMiner, SocialGraphMiner baseMiner, int withholdTopOfBaseMiner) {
		super(discoveryMiner.getContextualEgoNetwork());
		this.discoveryMiner = discoveryMiner;
		this.baseMiner = baseMiner;
		this.withholdTopOfBaseMiner = withholdTopOfBaseMiner;
	}

	@Override
	public double predictNewInteraction(Context context, Node destinationNode) {
		return Utils.error("This method should not be called for differences", 0);
		//return discoveryMiner.predictNewInteraction(context, destinationNode) / (0.1+baseMiner.predictNewInteraction(context, destinationNode));
	}
	
	public HashMap<Node, Double> recommendInteractions(Context context) {
    	HashMap<Node, Double> discoveredInteractions = discoveryMiner.recommendInteractions(context);
    	HashMap<Node, Double> baseInteractions = baseMiner.recommendInteractions(context);
    	for(Node node : sort(baseInteractions, withholdTopOfBaseMiner)) 
    		discoveredInteractions.remove(node);
    	return discoveredInteractions;
    }

	@SuppressWarnings({ "rawtypes", "unchecked" })
	protected static <T1, T2> List<T1> sort(Map<T1, T2> unsortedMap) {
		return unsortedMap
				.entrySet()
				.stream()
			    .sorted((e1, e2) -> -((Comparable) e1.getValue()).compareTo(e2.getValue()))
			    .map(e -> e.getKey())
			    .collect(Collectors.toList());
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	protected static <T1, T2> List<T1> sort(Map<T1, T2> unsortedMap, int topK) {
		return unsortedMap
				.entrySet()
				.stream()
			    .sorted((e1, e2) -> -((Comparable) e1.getValue()).compareTo(e2.getValue()))
			    .map(e -> e.getKey())
			    .limit(topK)
			    .collect(Collectors.toList());
	}

	@Override
	public void newInteractionParameters(Interaction interaction, SocialGraphMinerParameters neighborModelParameters, InteractionType interactionType) {
		discoveryMiner.newInteractionParameters(interaction, neighborModelParameters==null?null:neighborModelParameters.getNested("discovery_miner"), interactionType);
		if(baseMiner!=discoveryMiner)
			baseMiner.newInteractionParameters(interaction, neighborModelParameters==null?null:neighborModelParameters.getNested("base_miner"), interactionType);
	}

	@Override
	public SocialGraphMinerParameters constructModelParameterObject(Interaction interaction) {
		SocialGraphMinerParameters ret = new SocialGraphMinerParameters();
		ret.put("discovery_miner", discoveryMiner.getModelParameterObject(interaction));
		ret.put("base_miner", baseMiner.getModelParameterObject(interaction));
		return ret;
	}

}
