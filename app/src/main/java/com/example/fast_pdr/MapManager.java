package com.example.fast_pdr;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;

import androidx.annotation.IdRes;


import com.amap.api.maps.AMap;
import com.amap.api.maps.AMapOptions;
import com.amap.api.maps.CameraUpdate;
import com.amap.api.maps.CameraUpdateFactory;
import com.amap.api.maps.MapView;
import com.amap.api.maps.model.CameraPosition;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.MarkerOptions;
import com.amap.api.maps.utils.overlay.MovingPointOverlay;

import java.util.ArrayList;
import java.util.List;

public class MapManager {

    private MapView mMapView;
    private AMap aMap;
    private MovingPointOverlay movingPointOverlay;

    public LatLng Last_Position;

    public MapManager(Context context, @IdRes int mapViewId) {


        mMapView = ((Activity) context).findViewById(mapViewId);
    }

    public void initializeMap(Bundle savedInstanceState) {
        //参数依次是：视角调整区域的中心点坐标、希望调整到的缩放级别、俯仰角0°~45°（垂直与地图时为0）、偏航角 0~360° (正北方为0)
        CameraUpdate mCameraUpdate = CameraUpdateFactory.newCameraPosition(new CameraPosition(new LatLng(30.592935,114.305215),16,0,0));

        mMapView.onCreate(savedInstanceState);
        aMap = mMapView.getMap();
        aMap.moveCamera(mCameraUpdate);
        // Initialize the MovingPointOverlay object
        movingPointOverlay = new MovingPointOverlay(aMap, aMap.addMarker(new MarkerOptions()));
    }

    public void setcentralpoint (double latitude, double longitude) {
        this.Last_Position = new LatLng(latitude,longitude);
        CameraUpdate mCameraUpdate = CameraUpdateFactory.newCameraPosition(new CameraPosition(new LatLng(latitude,longitude),16,0,0));
        aMap.moveCamera(mCameraUpdate);
    }
    /**
     * Moves the point on the map to a new position.
     *
     * @param latitude  The latitude of the new position, in degrees.
     * @param longitude The longitude of the new position, in degrees.
     * This method creates a path from the last position to the new position,
     * sets this path to the MovingPointOverlay object, and starts the move.
     * The total duration of the move is set to 1 second.
     * The last position is updated to the new position after the move.
     */

    public void moveToPoint(double latitude, double longitude) { // deg deg
        LatLng newPosition = new LatLng(latitude, longitude);
        List<LatLng> path = new ArrayList<>();
        path.add(Last_Position);  // Current position
        path.add(newPosition);  // New position

        movingPointOverlay.setPoints(path);  // Set the path
        movingPointOverlay.setTotalDuration(1);  // Set the duration of the move
        movingPointOverlay.startSmoothMove();  // Start the move

        Last_Position = newPosition;
    }

    public void onDestroy() {
        mMapView.onDestroy();
    }

    public void onResume() {
        mMapView.onResume();
    }

    public void onPause() {
        mMapView.onPause();
    }

    public void onSaveInstanceState(Bundle outState) {
        mMapView.onSaveInstanceState(outState);
    }
}
