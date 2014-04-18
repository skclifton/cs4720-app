package com.photointeering;

public class Tuple {
	
	public String gameOwner;
	public int number_players;
	
	public Tuple(String s, int n) {
		this.gameOwner = s;
		this.number_players = n;
	}
	
	public void increment_players() {
		this.number_players += 1;
	}
	
	public String getGameOwner(){
		return this.gameOwner;
	}
	
	public int getNumPlayers(){
		return this.number_players;
	}

}
