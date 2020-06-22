package eu.h2020.helios_social.modules.socialgraphmining.examples;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;

import eu.h2020.helios_social.core.contextualegonetwork.ContextualEgoNetwork;
import eu.h2020.helios_social.core.contextualegonetwork.Interaction;
import eu.h2020.helios_social.core.contextualegonetwork.Node;
import eu.h2020.helios_social.core.contextualegonetwork.Utils;
import eu.h2020.helios_social.modules.socialgraphmining.Measure;
import eu.h2020.helios_social.modules.socialgraphmining.SocialGraphMiner;
import eu.h2020.helios_social.modules.socialgraphmining.GNN.GNNMiner;
import eu.h2020.helios_social.modules.socialgraphmining.SocialGraphMiner.InteractionType;
import eu.h2020.helios_social.modules.socialgraphmining.SwitchableMiner;
import eu.h2020.helios_social.modules.socialgraphmining.heuristics.DifferenceMiner;
import eu.h2020.helios_social.modules.socialgraphmining.heuristics.RepeatAndReplyMiner;
import eu.h2020.helios_social.modules.socialgraphmining.measures.Accumulate;
import eu.h2020.helios_social.modules.socialgraphmining.measures.HitRate;

public class Example {
	public static class Device {
		private ContextualEgoNetwork contextualEgoNetwork;
		private HashSet<Device> neighbors = new HashSet<Device>();
		private SocialGraphMiner miner;
		
		private static SocialGraphMiner staticGNN = new GNNMiner(ContextualEgoNetwork.createOrLoad("experiment_data\\", "all", null))
				.setTrainingExampleDegradation(1)
				.setDeniability(0, 0)
				.setRegularizationAbsorbsion(0)
				.setRegularizationWeight(0.1);
		
		public Device(String name) {
			contextualEgoNetwork = ContextualEgoNetwork.createOrLoad("experiment_data\\", name, null);
			
			
			SwitchableMiner miner = new SwitchableMiner(contextualEgoNetwork);
			miner.createMiner("repeat", RepeatAndReplyMiner.class);
			miner.createMiner("gnn", GNNMiner.class).setDeniability(0.1, 0.1);
			
			
			this.miner = miner;
			//this.miner = new AdditionalDiscoveryMiner(miner, miner.getMiner("repeat"), 3);
			
			miner.setActiveMiner("gnn");
			contextualEgoNetwork.setCurrent(contextualEgoNetwork.getOrCreateContext("default"));
		}
		public String getName() {
			return contextualEgoNetwork.getEgo().getId();
		}
		public void send(Device other) {
			Interaction interaction = contextualEgoNetwork
					.getCurrentContext()
					.getOrAddEdge(contextualEgoNetwork.getEgo(), contextualEgoNetwork.getOrCreateNode(other.getName(), null))
					.addDetectedInteraction(null);
			other.receive(this, miner.getModelParameters(interaction));
			/*neighbors.add(other);
			for(Device neighbor : neighbors)
			if(neighbor!=other){
				Interaction interaction = neighbor.contextualEgoNetwork
						.getCurrentContext()
						.getOrAddEdge(neighbor.contextualEgoNetwork.getOrCreateNode(other.getName(), null), 
								neighbor.contextualEgoNetwork.getOrCreateNode(getName(), null))
						.addDetectedInteraction(null);
				neighbor.miner.newInteraction(interaction, miner.getModelParameters(null), InteractionType.RECEIVE);
			}*/
		}
		protected void receive(Device other, String parameters) {
			Interaction interaction = contextualEgoNetwork
					.getCurrentContext()
					.getOrAddEdge(contextualEgoNetwork.getOrCreateNode(other.getName(), null), contextualEgoNetwork.getEgo())
					.addDetectedInteraction(null);
			miner.newInteraction(interaction, parameters, InteractionType.RECEIVE);
			other.receiveAck(this, miner.getModelParameters(interaction));
		}
		protected void receiveAck(Device other, String parameters) {
			ArrayList<Interaction> interactions = contextualEgoNetwork
					.getCurrentContext()
					.getOrAddEdge(contextualEgoNetwork.getEgo(), contextualEgoNetwork.getOrCreateNode(other.getName(), null))
					.getInteractions();
			miner.newInteraction(interactions.get(interactions.size()-1), parameters, InteractionType.RECEIVE_REPLY);
		}
		public Node recommendNextInteraction() {
			HashMap<Node, Double> interactionScores = miner.recommendInteractions(contextualEgoNetwork.getCurrentContext());
			double bestScore = 0;
			Node bestNode = null;
			for(Entry<Node, Double> interactionScore : interactionScores.entrySet()) 
				if(interactionScore.getValue() > bestScore) {
					bestScore = interactionScore.getValue();
					bestNode = interactionScore.getKey();
				}
			return bestNode;
		}
	}
	
	
	public static void main(String[] args) throws Exception {
		HashMap<String, Device> devices = new HashMap<String, Device>();
		BufferedReader edgeReader = new BufferedReader(new FileReader(new File("datasets/ia-enron-email-dynamic.edges")));
		String line = null;
		Measure measure = new Accumulate(new HitRate(3, 9));
		String result = "";
		int currentInteraction = 0;
		while((line=edgeReader.readLine())!=null) {
			if(line.startsWith("%") || line.startsWith("#") || line.isEmpty())
				continue;
			String[] splt = line.split(" ");
			if(splt.length<3)
				continue;
			String u = splt[0];
			String v = splt[1];
			if(u.equals(v))
				continue;
			if(!devices.containsKey(u))
				devices.put(u, new Device(u));
			if(!devices.containsKey(v))
				devices.put(v, new Device(v));
			double evaluation = measure.evaluateSend(devices.get(u).miner,
					devices.get(u).contextualEgoNetwork.getCurrentContext(),
					devices.get(u).contextualEgoNetwork.getOrCreateNode(v, null));
			if(currentInteraction%1000==0)
				System.out.println("#"+currentInteraction+": "+evaluation);
			if(currentInteraction%100==0)
				result += ","+evaluation;
			devices.get(u).send(devices.get(v));
			currentInteraction++;
		}
		edgeReader.close();
		result = "["+result.substring(1)+"];\n";
		System.out.print(result);
	}
}