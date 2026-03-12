package com.example.hatchtracker.data.remote.mappers

import com.example.hatchtracker.data.models.ChatMessage
import com.example.hatchtracker.data.remote.models.ChatMessageDocument
import com.example.hatchtracker.data.remote.models.DeviceDocument
import com.example.hatchtracker.data.remote.models.MaintenanceLogDocument
import com.example.hatchtracker.model.Device
import com.example.hatchtracker.model.EquipmentMaintenanceLog
import com.example.hatchtracker.model.MaintenanceLogType
import com.google.firebase.Timestamp
import java.util.Date

fun DeviceDocument.toModel(): com.example.hatchtracker.model.Device = com.example.hatchtracker.model.Device(
    id = id,
    userId = userId,
    type = type,
    modelId = modelId,
    displayName = displayName,
    capacityEggs = capacityEggs,
    features = features,
    createdAt = createdAt,
    isActive = isActive,
    purchaseDate = purchaseDate,
    purchasePrice = purchasePrice,
    residualValue = residualValue,
    lifecycleStatus = com.example.hatchtracker.model.DeviceLifecycleStatus.valueOf(lifecycleStatus ?: com.example.hatchtracker.model.DeviceLifecycleStatus.ACTIVE.name),
    disposedAt = disposedAt
)

fun com.example.hatchtracker.model.Device.toDocument(): DeviceDocument = DeviceDocument(
    id = id,
    userId = userId,
    type = type,
    modelId = modelId,
    displayName = displayName,
    capacityEggs = capacityEggs,
    features = features,
    createdAt = createdAt,
    isActive = isActive,
    purchaseDate = purchaseDate,
    purchasePrice = purchasePrice,
    residualValue = residualValue,
    lifecycleStatus = lifecycleStatus.name,
    disposedAt = disposedAt
)

fun ChatMessageDocument.toModel(idOverride: String? = null): ChatMessage = ChatMessage(
    id = if (!idOverride.isNullOrBlank()) idOverride else id,
    ticketId = ticketId,
    senderId = senderId,
    senderName = senderName,
    senderRole = senderRole,
    content = content,
    isInternal = isInternal,
    attachments = attachments,
    createdAt = createdAt?.toDate()?.time,
    readBy = readBy
)

fun ChatMessage.toDocument(): ChatMessageDocument = ChatMessageDocument(
    id = id,
    ticketId = ticketId,
    senderId = senderId,
    senderName = senderName,
    senderRole = senderRole,
    content = content,
    isInternal = isInternal,
    attachments = attachments,
    createdAt = createdAt?.let { Timestamp(Date(it)) },
    readBy = readBy
)

fun MaintenanceLogDocument.toModel(): EquipmentMaintenanceLog = EquipmentMaintenanceLog(
    id = id,
    equipmentId = equipmentId,
    date = date,
    type = MaintenanceLogType.valueOf(type.ifEmpty { MaintenanceLogType.OTHER.name }),
    description = description,
    cost = cost
)

fun EquipmentMaintenanceLog.toDocument(): MaintenanceLogDocument = MaintenanceLogDocument(
    id = id,
    equipmentId = equipmentId,
    date = date,
    type = type.name,
    description = description,
    cost = cost
)
