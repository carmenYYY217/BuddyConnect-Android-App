package edu.cuhk.csci3310.buddyconnect.Map;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import edu.cuhk.csci3310.buddyconnect.R;

public class MapFragment extends Fragment {

    private MapView mapView;
    private MyLocationNewOverlay myLocationOverlay;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Set osmdroid configuration (important for caching and tile loading)
        Configuration.getInstance().load(requireContext(), requireActivity().getPreferences(0));
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_map, container, false);
        mapView = view.findViewById(R.id.map_view);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Set the tile source to OpenStreetMap's default tiles
        mapView.setTileSource(TileSourceFactory.MAPNIK);

        // Enable zoom and multi-touch controls
        mapView.setBuiltInZoomControls(true);
        mapView.setMultiTouchControls(true);

        // Initialize the location overlay to show the user's location
        myLocationOverlay = new MyLocationNewOverlay(new GpsMyLocationProvider(requireContext()), mapView);
        myLocationOverlay.enableMyLocation();
        mapView.getOverlays().add(myLocationOverlay);

        // Set initial zoom level
        mapView.getController().setZoom(15.0);

        // Check for location permission and center the map on the user's location
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            centerMapOnUserLocation();
        } else {
            // Request permission if not granted
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        }
    }

    private void centerMapOnUserLocation() {
        myLocationOverlay.runOnFirstFix(() -> {
            GeoPoint userLocation = myLocationOverlay.getMyLocation();
            if (userLocation != null) {
                // Ensure the animation runs on the main thread
                requireActivity().runOnUiThread(() -> {
                    mapView.getController().animateTo(userLocation);
                });
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1 && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            // Enable location overlay and center the map if permission is granted
            myLocationOverlay.enableMyLocation();
            centerMapOnUserLocation();
        } else {
            Toast.makeText(requireContext(), "Location permission denied", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        mapView.onResume(); // Required for osmdroid to handle lifecycle
    }

    @Override
    public void onPause() {
        super.onPause();
        mapView.onPause(); // Required for osmdroid to handle lifecycle
    }
}