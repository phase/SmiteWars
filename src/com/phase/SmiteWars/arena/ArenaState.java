package com.phase.SmiteWars.arena;

public enum ArenaState {
	
	IN_GAME(false), OUT_OF_GAME(true);
	
	private boolean canJoin;
	
	ArenaState(boolean canJoin){
		this.canJoin = canJoin;
	}
	
	public boolean canJoin(){
		return canJoin;
	}
	
}
