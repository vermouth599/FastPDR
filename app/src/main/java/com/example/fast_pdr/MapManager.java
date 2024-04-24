package com.example.fast_pdr;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;

import androidx.annotation.IdRes;

import android.util.Log;

import com.amap.api.maps.AMap;
import com.amap.api.maps.AMapOptions;
import com.amap.api.maps.CameraUpdate;
import com.amap.api.maps.CameraUpdateFactory;
import com.amap.api.maps.CoordinateConverter;
import com.amap.api.maps.MapView;
import com.amap.api.maps.model.BitmapDescriptorFactory;
import com.amap.api.maps.model.CameraPosition;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.Marker;
import com.amap.api.maps.model.MarkerOptions;
import com.amap.api.maps.model.PolylineOptions;
import com.amap.api.maps.utils.overlay.MovingPointOverlay;

import java.util.ArrayList;
import java.util.List;

public class MapManager {

    private MapView mMapView;
    private AMap aMap;
    private MovingPointOverlay movingPointOverlay;

    public LatLng Last_Position;

    private Marker marker;
    private CoordinateConverter converter;

    public MapManager(Context context, @IdRes int mapViewId) {


        mMapView = ((Activity) context).findViewById(mapViewId);
        converter = new CoordinateConverter(context);
    }
    public void clearMap() {
        aMap.clear();
    }
    

    public void initializeMap(Bundle savedInstanceState) {
        // 参数依次是：视角调整区域的中心点坐标、希望调整到的缩放级别、俯仰角0°~45°（垂直与地图时为0）、偏航角 0~360° (正北方为0)
        // 设置为武汉大学的坐标
        CameraUpdate mCameraUpdate = CameraUpdateFactory.newCameraPosition(new CameraPosition(new LatLng(30.536031,114.364331),16,0,0));

        mMapView.onCreate(savedInstanceState);
        aMap = mMapView.getMap();
        aMap.moveCamera(mCameraUpdate);

    }

    public void setcentralpoint (double latitude, double longitude) {
        this.Last_Position = GPS2GAODE(latitude, longitude);
        marker = aMap.addMarker(new MarkerOptions().position(Last_Position));
        movingPointOverlay = new MovingPointOverlay(aMap, marker);
        CameraUpdate mCameraUpdate = CameraUpdateFactory.newCameraPosition(new CameraPosition(Last_Position,19,0,0));
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
        LatLng newPosition = GPS2GAODE(latitude, longitude); // Convert GPS coordinates to AMap coordinates (GAODE)
        List<LatLng> path = new ArrayList<>();
        path.add(Last_Position);  // Current position
        path.add(newPosition);  // New position

        movingPointOverlay.setPoints(path);  // Set the path
        movingPointOverlay.setTotalDuration(1);  // Set the duration of the move TODO:改为1，仅为测试用
        movingPointOverlay.startSmoothMove();  // Start the move

        // Draw the path on the map
        PolylineOptions polylineOptions = new PolylineOptions();
        polylineOptions.addAll(path);
        polylineOptions.color(Color.RED);
        aMap.addPolyline(polylineOptions);
        
        
        
        // 调整视角中心
        CameraUpdate mCameraUpdate = CameraUpdateFactory.newCameraPosition(new CameraPosition(newPosition,19,0,0));
        aMap.moveCamera(mCameraUpdate);


        Last_Position = newPosition;
    }

    // 测试 moveToPoint
    public void testMoveToPoint() {

        // Test 1: Move to the new position (30.536031, 114.364331)
        moveToPoint(30.528620, 114.361027);

        // Test 2: Move to the new position (30.536031, 114.364331)
        moveToPoint(30.528607, 114.360811);

        
    }

    public LatLng GPS2GAODE(double latitude, double longitude) {

        converter.from(CoordinateConverter.CoordType.GPS); //TODO:修改为GPS，只是测试的时候用谷歌
        converter.coord(new LatLng(latitude,longitude));
        LatLng desLatLng = converter.convert();
        Log.d("MapManager", "GPS" + latitude + " " + longitude);
        Log.d("MapManager", "GAODE: " + desLatLng.latitude + " " + desLatLng.longitude);
        return desLatLng;
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
