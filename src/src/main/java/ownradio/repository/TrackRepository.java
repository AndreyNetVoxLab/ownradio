package ownradio.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import ownradio.domain.Track;

import java.util.UUID;

/**
 * Интерфейс репозитория, для хранения треков
 *
 * @author Alpenov Tanat
 */
public interface TrackRepository extends JpaRepository<Track, UUID> {
	@Query(value = "select * from getnexttrackid_string(?1)", nativeQuery = true)
	UUID getNextTrackId(UUID deviceId);
}
