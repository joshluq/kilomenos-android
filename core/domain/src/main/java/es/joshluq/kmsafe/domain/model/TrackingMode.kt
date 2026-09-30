package es.joshluq.kmsafe.domain.model

/**
 * Defines the origin and nature of an active trip tracking session.
 */
enum class TrackingMode {
    /**
     * User-initiated session (e.g. 1-tap "Iniciar Viaje" in Assisted Copilot or manual start).
     * Inviolable: Must NEVER be terminated by automated telemetry triggers (AR EXIT, Walking, BT disconnect, BT Heartbeat).
     */
    MANUAL,

    /**
     * Sensor-initiated session (e.g. SMART Copilot Bluetooth Fast-Path or Activity Recognition fallback).
     * Subject to automated trip finalization heuristics (vehicle disconnection, pedestrian transitions, etc.).
     */
    AUTOMATIC
}
