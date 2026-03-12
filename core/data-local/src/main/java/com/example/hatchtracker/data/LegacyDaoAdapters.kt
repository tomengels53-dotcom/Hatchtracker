package com.example.hatchtracker.data

suspend fun FlockEntityDao.getFlockByCloudId(cloudId: String) = getFlockEntityByCloudId(cloudId)
suspend fun BirdEntityDao.getBirdByCloudId(cloudId: String) = getBirdEntityByCloudId(cloudId)
suspend fun IncubationEntityDao.getIncubationByCloudId(cloudId: String) = getIncubationEntityByCloudId(cloudId)

suspend fun FlockEntityDao.getAllFlocksSync() = getAllFlockEntitysSync()
suspend fun IncubationEntityDao.getAllIncubations() = getAllIncubationEntitys()
