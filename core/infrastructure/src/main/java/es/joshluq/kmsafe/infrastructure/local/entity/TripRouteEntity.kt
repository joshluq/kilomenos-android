package es.joshluq.kmsafe.infrastructure.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import es.joshluq.kmsafe.domain.model.TripRoute

/**
 * Room Entity representing a trip route linked to an odometer record.
 */
@Entity(
    tableName = "trip_route",
    foreignKeys = [
        ForeignKey(
            entity = OdometerRecordEntity::class,
            parentColumns = ["id"],
            childColumns = ["recordId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class TripRouteEntity(
    @PrimaryKey val recordId: String,
    val encodedPolyline: String,
    val pointCount: Int
)

/**
 * Maps [TripRouteEntity] to domain [TripRoute].
 */
fun TripRouteEntity.toDomain(): TripRoute = TripRoute(
    recordId = recordId,
    encodedPolyline = encodedPolyline,
    pointCount = pointCount
)

/**
 * Maps domain [TripRoute] to [TripRouteEntity].
 */
fun TripRoute.toEntity(): TripRouteEntity = TripRouteEntity(
    recordId = recordId,
    encodedPolyline = encodedPolyline,
    pointCount = pointCount
)
