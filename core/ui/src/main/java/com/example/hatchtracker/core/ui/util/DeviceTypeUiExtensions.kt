package com.example.hatchtracker.core.ui.util

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.hatchtracker.core.ui.R
import com.example.hatchtracker.model.DeviceCategory
import com.example.hatchtracker.model.DeviceType

/**
 * Centralized UI resolver for Equipment Taxonomy.
 * Prevents UI drift and ensures consistent terminology.
 */

fun DeviceType.icon(): ImageVector = when (this) {
    // --- INCUBATION ---
    DeviceType.SETTER -> Icons.Rounded.Egg
    DeviceType.HATCHER -> Icons.Rounded.ChildCare
    DeviceType.TURNER -> Icons.Rounded.Refresh
    DeviceType.HUMIDITY_CONTROLLER -> Icons.Rounded.WaterDrop
    DeviceType.THERMOSTAT -> Icons.Rounded.Thermostat
    
    // --- BROODING ---
    DeviceType.BROOD_PLATE -> Icons.Rounded.Home
    DeviceType.HEAT_LAMP -> Icons.Rounded.WbSunny
    DeviceType.HEAT_PANEL -> Icons.Rounded.Square
    
    // --- HOUSING ---
    DeviceType.COOP -> Icons.Rounded.House
    DeviceType.NEST_BOX -> Icons.Rounded.GridOn
    DeviceType.RUN -> Icons.Rounded.Fence
    DeviceType.COOP_AUTO -> Icons.Rounded.Settings
    
    // --- CARE ---
    DeviceType.FEEDER -> Icons.Rounded.Restaurant
    DeviceType.WATERER -> Icons.Rounded.Water
    DeviceType.WASHER -> Icons.Rounded.CleanHands
    DeviceType.SANITIZER -> Icons.Rounded.Sanitizer
    
    // --- MONITORING ---
    DeviceType.CANDLER -> Icons.Rounded.Highlight
    DeviceType.SCALE -> Icons.Rounded.Scale
    DeviceType.THERMOMETER -> Icons.Rounded.DeviceThermostat
    DeviceType.HYGROMETER -> Icons.Rounded.Opacity
    DeviceType.CAMERA -> Icons.Rounded.Videocam
    
    // --- LEGACY ---
    DeviceType.INCUBATOR -> Icons.Rounded.Egg
    DeviceType.BROODER -> Icons.Rounded.Home
}

fun DeviceType.labelRes(): Int = when (this) {
    DeviceType.SETTER -> R.string.device_type_setter
    DeviceType.HATCHER -> R.string.device_type_hatcher
    DeviceType.TURNER -> R.string.device_type_turner
    DeviceType.HUMIDITY_CONTROLLER -> R.string.device_type_humidity_controller
    DeviceType.THERMOSTAT -> R.string.device_type_thermostat
    DeviceType.BROOD_PLATE -> R.string.device_type_brood_plate
    DeviceType.HEAT_LAMP -> R.string.device_type_heat_lamp
    DeviceType.HEAT_PANEL -> R.string.device_type_heat_panel
    DeviceType.COOP -> R.string.device_type_coop
    DeviceType.NEST_BOX -> R.string.device_type_nest_box
    DeviceType.RUN -> R.string.device_type_run
    DeviceType.COOP_AUTO -> R.string.device_type_coop_auto
    DeviceType.FEEDER -> R.string.device_type_feeder
    DeviceType.WATERER -> R.string.device_type_waterer
    DeviceType.WASHER -> R.string.device_type_washer
    DeviceType.SANITIZER -> R.string.device_type_sanitizer
    DeviceType.CANDLER -> R.string.device_type_candler
    DeviceType.SCALE -> R.string.device_type_scale
    DeviceType.THERMOMETER -> R.string.device_type_thermometer
    DeviceType.HYGROMETER -> R.string.device_type_hygrometer
    DeviceType.CAMERA -> R.string.device_type_camera
    DeviceType.INCUBATOR -> R.string.device_type_setter
    DeviceType.BROODER -> R.string.device_type_brood_plate
}

fun DeviceCategory.labelRes(): Int = when (this) {
    DeviceCategory.INCUBATION -> R.string.device_category_incubation
    DeviceCategory.BROODING -> R.string.device_category_brooding
    DeviceCategory.HOUSING -> R.string.device_category_housing
    DeviceCategory.CARE -> R.string.device_category_care
    DeviceCategory.MONITORING -> R.string.device_category_monitoring
}
