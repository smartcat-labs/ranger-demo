package io.smartcat.service;

public class AvgHeartBeatRateDTO {
	
	private String username;
	private double avgHeartBeatRate;
	
	public String getUsername() {
		return username;
	}
	public void setUsername(String username) {
		this.username = username;
	}
	public double getAvgHeartBeatRate() {
		return avgHeartBeatRate;
	}
	public void setAvgHeartBeatRate(double avgHb) {
		this.avgHeartBeatRate = avgHb;
	}

}
