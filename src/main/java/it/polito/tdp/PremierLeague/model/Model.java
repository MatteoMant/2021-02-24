package it.polito.tdp.PremierLeague.model;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jgrapht.Graph;
import org.jgrapht.Graphs;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleDirectedWeightedGraph;

import it.polito.tdp.PremierLeague.db.PremierLeagueDAO;

public class Model {

	PremierLeagueDAO dao;
	private List<Match> matches;
	private Graph<Player, DefaultWeightedEdge> grafo;
	
	public Model() {
		dao = new PremierLeagueDAO();
		matches = this.dao.listAllMatches();
	}
	
	public List<Match> getLista(){
		return matches;
	}
	
	public void creaGrafo(Match match) {
		List<Player> giocatori = dao.listAllPlayersWithMatch(match);
		grafo = new SimpleDirectedWeightedGraph<>(DefaultWeightedEdge.class);
		Graphs.addAllVertices(this.grafo, giocatori);
		
		List<Action> azioni = dao.listAllActionsWithMatch(match);
		Map<Player, Double> mappaEfficienzaLocale = new HashMap<>();
		Map<Player, Double> mappaEfficienzaOspite = new HashMap<>();
		List<Team> teams = dao.listAllTeamsWithMatch(match);
		if (teams.size() != 2) {
			return;
		}
		Team locale = teams.get(0);
		Team ospite = teams.get(1);
		
		for (Action a : azioni) {
			Player player = null;
			for (Player p : giocatori) {
				if (p.getPlayerID().equals(a.getPlayerID())) {
					player = p;
					break;
				}
			}
			if (a.getTeamID() == locale.getTeamID()) {
				mappaEfficienzaLocale.put(player, (double)(a.getTotalSuccessfulPassesAll()+a.getAssists())/a.getTimePlayed());
			} else if (a.getTeamID() == ospite.getTeamID()){
				mappaEfficienzaOspite.put(player, (double)(a.getTotalSuccessfulPassesAll()+a.getAssists())/a.getTimePlayed());
			}
		}
		
		for (Player i : mappaEfficienzaLocale.keySet()) {
			for (Player j : mappaEfficienzaOspite.keySet()) {
				if (mappaEfficienzaLocale.get(i) > mappaEfficienzaOspite.get(j)) {
					Graphs.addEdge(this.grafo, i, j, mappaEfficienzaLocale.get(i)-mappaEfficienzaOspite.get(j));
				}else {
					Graphs.addEdge(this.grafo, j, i, mappaEfficienzaOspite.get(j)-mappaEfficienzaLocale.get(i));
				}
			}
		}
		
	}
	
	public Player getGiocatoreMigliore() {
		double max = 0.0;
		Player best = null;
		for (Player p : this.grafo.vertexSet()) {
			double delta = this.getDeltaPlayer(p);
			if (delta > max) {
				max = delta;
				best = p;
			}
		}
		return best;
	}
	
	public double getDeltaPlayer(Player player) {
		double sommaUscenti = 0.0;
		for (DefaultWeightedEdge e : this.grafo.outgoingEdgesOf(player)) {
			sommaUscenti += this.grafo.getEdgeWeight(e);
		}
		double sommaEntranti = 0.0;
		for (DefaultWeightedEdge e : this.grafo.incomingEdgesOf(player)) {
			sommaEntranti += this.grafo.getEdgeWeight(e);
		}
		return sommaUscenti - sommaEntranti;
	}
	
	public int getNumeroVertici() {
		return this.grafo.vertexSet().size();
	}
	
	public int getNumeroArchi() {
		return this.grafo.edgeSet().size();
	}
}

