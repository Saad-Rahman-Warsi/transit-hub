import { useEffect } from 'react';
import { MapContainer, TileLayer, Marker, Popup, CircleMarker, useMap } from 'react-leaflet';
import L from 'leaflet';
import type { Stop, VehiclePosition } from '../types';
import { useVehicles } from '../hooks/useTransit';
import 'leaflet/dist/leaflet.css';

// Fix Leaflet default icon path broken by Vite bundling
delete (L.Icon.Default.prototype as any)._getIconUrl;
L.Icon.Default.mergeOptions({
  iconRetinaUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.9.4/images/marker-icon-2x.png',
  iconUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.9.4/images/marker-icon.png',
  shadowUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.9.4/images/marker-shadow.png',
});

const BUS_ICON = L.divIcon({
  className: '',
  html: `<div style="
    width:28px;height:28px;border-radius:50%;
    background:#1D9E75;border:2px solid #085041;
    display:flex;align-items:center;justify-content:center;
    color:#fff;font-size:11px;font-weight:500;
    box-shadow:0 1px 4px rgba(0,0,0,.25)">🚌</div>`,
  iconSize: [28, 28],
  iconAnchor: [14, 14],
  popupAnchor: [0, -16],
});

const SELECTED_STOP_ICON = L.divIcon({
  className: '',
  html: `<div style="
    width:18px;height:18px;border-radius:50%;
    background:#E24B4A;border:3px solid #fff;
    box-shadow:0 1px 4px rgba(0,0,0,.35)"></div>`,
  iconSize: [18, 18],
  iconAnchor: [9, 9],
  popupAnchor: [0, -12],
});

function FlyTo({ lat, lon }: { lat: number; lon: number }) {
  const map = useMap();
  useEffect(() => {
    map.flyTo([lat, lon], 16, { duration: 1.2 });
  }, [lat, lon, map]);
  return null;
}

interface Props {
  selectedStop: Stop | null;
  nearbyStops: Stop[];
  onStopClick: (stop: Stop) => void;
}

export default function MapView({ selectedStop, nearbyStops, onStopClick }: Props) {
  const routeFilter = undefined; // show all vehicles; pass routeId to filter
  const { data: vehicles } = useVehicles(routeFilter);

  const centre: [number, number] = selectedStop
    ? [selectedStop.lat, selectedStop.lon]
    : [45.4215, -75.6972]; // Ottawa city centre default

  return (
    <div style={{ flex: 1, position: 'relative' }}>
      <MapContainer
        center={centre}
        zoom={15}
        style={{ width: '100%', height: '100%', minHeight: 400 }}
        zoomControl={true}
      >
        <TileLayer
          attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>'
          url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
        />

        {/* Fly to selected stop */}
        {selectedStop && <FlyTo lat={selectedStop.lat} lon={selectedStop.lon} />}

        {/* Nearby stops */}
        {nearbyStops.map((stop) => {
          const isSelected = selectedStop?.stopCode === stop.stopCode;
          return (
            <CircleMarker
              key={stop.stopId}
              center={[stop.lat, stop.lon]}
              radius={isSelected ? 9 : 6}
              pathOptions={{
                fillColor: isSelected ? '#E24B4A' : '#378ADD',
                fillOpacity: 1,
                color: '#fff',
                weight: 2,
              }}
              eventHandlers={{ click: () => onStopClick(stop) }}
            >
              <Popup>
                <strong>{stop.stopName}</strong>
                <br />
                Stop #{stop.stopCode}
                <br />
                <button
                  onClick={() => onStopClick(stop)}
                  style={{
                    marginTop: 6,
                    padding: '4px 10px',
                    background: '#1D9E75',
                    color: '#fff',
                    border: 'none',
                    borderRadius: 6,
                    cursor: 'pointer',
                    fontSize: 12,
                  }}
                >
                  View arrivals
                </button>
              </Popup>
            </CircleMarker>
          );
        })}

        {/* Live vehicles */}
        {(vehicles ?? [])
          .filter((v) => v.latitude !== 0 && v.longitude !== 0)
          .map((v) => (
            <Marker
              key={v.vehicleId}
              position={[v.latitude, v.longitude]}
              icon={BUS_ICON}
            >
              <Popup>
                <strong>Route {v.routeId}</strong>
                <br />
                Vehicle #{v.vehicleId}
                {v.speed != null && (
                  <>
                    <br />
                    Speed: {Math.round(v.speed * 3.6)} km/h
                  </>
                )}
                {v.occupancyStatus && v.occupancyStatus !== 'UNKNOWN' && (
                  <>
                    <br />
                    Occupancy: {v.occupancyStatus.replace(/_/g, ' ').toLowerCase()}
                  </>
                )}
              </Popup>
            </Marker>
          ))}
      </MapContainer>

      {/* Live vehicles count badge */}
      {vehicles && vehicles.length > 0 && (
        <div
          style={{
            position: 'absolute',
            top: 12,
            right: 12,
            zIndex: 1000,
            background: 'var(--color-background-primary)',
            border: '0.5px solid var(--color-border-secondary)',
            borderRadius: 8,
            padding: '6px 12px',
            fontSize: 12,
            color: 'var(--color-text-secondary)',
            display: 'flex',
            alignItems: 'center',
            gap: 6,
            boxShadow: '0 1px 4px rgba(0,0,0,.1)',
          }}
        >
          <span
            style={{
              width: 7,
              height: 7,
              borderRadius: '50%',
              background: '#1D9E75',
            }}
          />
          {vehicles.length} buses live
        </div>
      )}
    </div>
  );
}
