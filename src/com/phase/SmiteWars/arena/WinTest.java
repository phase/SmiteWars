package com.phase.SmiteWars.arena;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

public class WinTest {

	
	static ArrayList<UUID> Players = new ArrayList<UUID>();
	static HashMap<UUID, Integer> TotalKills = new HashMap<UUID, Integer>();
	
	public static void main(String[] args) {
		UUID u1 = new UUID(0,0);
		UUID u2 = new UUID(1234, 1234);
		Players.add(u1);
		Players.add(u2);
		TotalKills.put(u1, 1234);
		TotalKills.put(u2, 2345);
		UUID w = getWinner();
		System.out.println(w.toString() + " " + TotalKills.get(w));
		
	}
	
	public static UUID getWinner() {
		UUID winner = null;
		for (UUID u : Players)
			if (TotalKills.containsKey(u))
				if (winner == null
						|| TotalKills.get(winner) < TotalKills.get(u))
					winner = u;
		return winner;
	}

}
