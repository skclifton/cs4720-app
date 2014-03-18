package com.photointeering;

public class GameMap {
	private String url = "";
	private String photoLat = "";
	private String photoLon = "";
	
	public GameMap(String url, String photoLat, String photoLon,
			String currentLat, String currentLon) {
		super();
		this.url = url;
		this.photoLat = photoLat;
		this.photoLon = photoLon;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getPhotoLat() {
		return photoLat;
	}

	public void setPhotoLat(String photoLat) {
		this.photoLat = photoLat;
	}

	public String getPhotoLon() {
		return photoLon;
	}

	public void setPhotoLon(String photoLon) {
		this.photoLon = photoLon;
	}

}
