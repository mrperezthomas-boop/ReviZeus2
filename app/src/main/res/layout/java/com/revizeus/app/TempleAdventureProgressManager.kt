package com.revizeus.app

import android.content.Context
import com.revizeus.app.models.AppDatabase
import com.revizeus.app.models.TempleAdventureMapEntity
import com.revizeus.app.models.TempleAdventureNodeProgressEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object TempleAdventureProgressManager {

    data class MapProgressState(
        val map: TempleAdventureMapEntity,
        val nodes: List<TempleAdventureNodeProgressEntity>,
        val completionPercent: Int,
        val isCompleted: Boolean
    )

    suspend fun loadOrCreateMapProgress(
        context: Context,
        config: TempleMapConfig
    ): MapProgressState = withContext(Dispatchers.IO) {
        val dao = AppDatabase.getDatabase(context).templeAdventureDao()
        val now = System.currentTimeMillis()

        val mapState = dao.getMapState(config.subject, config.godId, config.templeLevel, config.mapIndex)
            ?: TempleAdventureMapEntity(
                subject = config.subject,
                godId = config.godId,
                templeLevel = config.templeLevel,
                mapIndex = config.mapIndex,
                isUnlocked = true,
                isCompleted = false,
                completionPercent = 0,
                lastPlayedAt = now,
                visualStateJson = "",
                metadataJson = ""
            ).also { dao.upsertMapState(it) }

        val existingNodes = dao.getNodeStates(config.subject, config.godId, config.templeLevel, config.mapIndex)
        if (existingNodes.isEmpty()) {
            val initialUnlockedNodeIds = config.nodeList
                .map { it.nodeId }
                .filter { nodeId -> config.edgeList.none { it.toNodeId == nodeId } }
                .toSet()

            val seedNodes = config.nodeList.map { node ->
                TempleAdventureNodeProgressEntity(
                    subject = config.subject,
                    godId = config.godId,
                    templeLevel = config.templeLevel,
                    mapIndex = config.mapIndex,
                    nodeId = node.nodeId,
                    nodeType = node.nodeType.name,
                    isUnlocked = initialUnlockedNodeIds.contains(node.nodeId),
                    isCompleted = false,
                    completionCount = 0,
                    lastPlayedAt = 0L,
                    bestResult = 0,
                    rewardClaimedStateJson = "",
                    rareStateLocked = false,
                    metadataJson = ""
                )
            }
            dao.upsertNodeStates(seedNodes)
        }

        val refreshedNodes = dao.getNodeStates(config.subject, config.godId, config.templeLevel, config.mapIndex)
        val completionPercent = computeCompletionPercent(refreshedNodes)
        val isCompleted = refreshedNodes.isNotEmpty() && refreshedNodes.all { it.isCompleted }

        val latestMap = mapState.copy(
            isCompleted = isCompleted,
            completionPercent = completionPercent,
            lastPlayedAt = now
        ).also { dao.upsertMapState(it) }

        val finalMap = dao.getMapState(config.subject, config.godId, config.templeLevel, config.mapIndex) ?: latestMap
        val finalNodes = dao.getNodeStates(config.subject, config.godId, config.templeLevel, config.mapIndex)

        MapProgressState(
            map = finalMap,
            nodes = finalNodes,
            completionPercent = finalMap.completionPercent,
            isCompleted = finalMap.isCompleted
        )
    }

    suspend fun getMapState(
        context: Context,
        subject: String,
        godId: String,
        templeLevel: Int,
        mapIndex: Int
    ): TempleAdventureMapEntity? = withContext(Dispatchers.IO) {
        AppDatabase.getDatabase(context).templeAdventureDao()
            .getMapState(subject, godId, templeLevel, mapIndex)
    }

    suspend fun getNodeStates(
        context: Context,
        subject: String,
        godId: String,
        templeLevel: Int,
        mapIndex: Int
    ): List<TempleAdventureNodeProgressEntity> = withContext(Dispatchers.IO) {
        AppDatabase.getDatabase(context).templeAdventureDao()
            .getNodeStates(subject, godId, templeLevel, mapIndex)
    }

    suspend fun markNodeCompletedAndLoad(
        context: Context,
        config: TempleMapConfig,
        nodeId: String,
        result: Int = 100
    ): MapProgressState = withContext(Dispatchers.IO) {
        val dao = AppDatabase.getDatabase(context).templeAdventureDao()
        val now = System.currentTimeMillis()

        dao.markNodeCompleted(
            subject = config.subject,
            godId = config.godId,
            templeLevel = config.templeLevel,
            mapIndex = config.mapIndex,
            nodeId = nodeId,
            completedAt = now,
            result = result.coerceIn(0, 100)
        )

        val updatedNodes = dao.getNodeStates(config.subject, config.godId, config.templeLevel, config.mapIndex)
        unlockEligibleChildNodes(dao, config, updatedNodes, now)

        val refreshedNodes = dao.getNodeStates(config.subject, config.godId, config.templeLevel, config.mapIndex)
        val completionPercent = computeCompletionPercent(refreshedNodes)
        val allCompleted = refreshedNodes.isNotEmpty() && refreshedNodes.all { it.isCompleted }

        if (allCompleted) {
            dao.markMapCompleted(
                subject = config.subject,
                godId = config.godId,
                templeLevel = config.templeLevel,
                mapIndex = config.mapIndex,
                completedAt = now
            )
        } else {
            val currentMap = dao.getMapState(config.subject, config.godId, config.templeLevel, config.mapIndex)
            if (currentMap != null) {
                dao.upsertMapState(
                    currentMap.copy(
                        isCompleted = false,
                        completionPercent = completionPercent,
                        lastPlayedAt = now
                    )
                )
            }
        }

        val finalMap = dao.getMapState(config.subject, config.godId, config.templeLevel, config.mapIndex)
            ?: TempleAdventureMapEntity(
                subject = config.subject,
                godId = config.godId,
                templeLevel = config.templeLevel,
                mapIndex = config.mapIndex,
                isUnlocked = true,
                isCompleted = allCompleted,
                completionPercent = completionPercent,
                lastPlayedAt = now,
                visualStateJson = "",
                metadataJson = ""
            ).also { dao.upsertMapState(it) }
        val finalNodes = dao.getNodeStates(config.subject, config.godId, config.templeLevel, config.mapIndex)

        MapProgressState(
            map = finalMap,
            nodes = finalNodes,
            completionPercent = finalMap.completionPercent,
            isCompleted = finalMap.isCompleted
        )
    }

    private fun computeCompletionPercent(nodes: List<TempleAdventureNodeProgressEntity>): Int {
        if (nodes.isEmpty()) return 0
        val completedCount = nodes.count { it.isCompleted }
        return (completedCount * 100) / nodes.size
    }

    private suspend fun unlockEligibleChildNodes(
        dao: com.revizeus.app.models.TempleAdventureDao,
        config: TempleMapConfig,
        currentNodes: List<TempleAdventureNodeProgressEntity>,
        now: Long
    ) {
        if (currentNodes.isEmpty()) return
        val stateByNodeId = currentNodes.associateBy { it.nodeId }
        val allNodeIds = currentNodes.map { it.nodeId }.toSet()
        val parentIdsByChild = config.edgeList
            .groupBy({ it.toNodeId }, { it.fromNodeId })
            .mapValues { (_, parents) -> parents.filter { allNodeIds.contains(it) }.distinct() }

        currentNodes.forEach { nodeState ->
            if (nodeState.isUnlocked) return@forEach
            val parents = parentIdsByChild[nodeState.nodeId].orEmpty()
            if (parents.isEmpty()) return@forEach
            val allParentsCompleted = parents.all { parentId -> stateByNodeId[parentId]?.isCompleted == true }
            if (allParentsCompleted) {
                dao.upsertNodeState(nodeState.copy(isUnlocked = true, lastPlayedAt = now))
            }
        }
    }
}
