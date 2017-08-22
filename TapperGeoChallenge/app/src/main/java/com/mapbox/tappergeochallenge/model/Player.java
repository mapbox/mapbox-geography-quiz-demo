package com.mapbox.tappergeochallenge.model;

import com.mapbox.mapboxsdk.annotations.Icon;

/**
 * Created by LangstonSmith on 2/4/17.
 */

public class Player {

    private String playerName;
    private Icon playerOneIcon;
    private double selectedLatitude;
    private double selectedLongitude;
    private int points;

    public Player() {
    }

    public String getPlayerName() {
        return playerName;
    }

    public void setPlayerName(String playerName) {
        this.playerName = playerName;
    }

    public Icon getPlayerOneIcon() {
        return playerOneIcon;
    }

    public void setPlayerOneIcon(Icon playerOneIcon) {
        this.playerOneIcon = playerOneIcon;
    }

    public double getSelectedLatitude() {
        return selectedLatitude;
    }

    public void setSelectedLatitude(double selectedLatitude) {
        this.selectedLatitude = selectedLatitude;
    }

    public double getSelectedLongitude() {
        return selectedLongitude;
    }

    public void setSelectedLongitude(double selectedLongitude) {
        this.selectedLongitude = selectedLongitude;
    }

    public int getPoints() {
        return points;
    }

    public void setPoints(int points) {
        this.points = points;
    }
}
