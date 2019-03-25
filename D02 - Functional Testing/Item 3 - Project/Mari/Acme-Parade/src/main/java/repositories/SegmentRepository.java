
package repositories;

import java.util.Collection;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import domain.Parade;
import domain.Segment;

@Repository
public interface SegmentRepository extends JpaRepository<Segment, Integer> {

	@Query("select s.parade from Segment s where s.id = ?1")
	Parade findParadeBySegment(int id);

	@Query("select s from Segment s where s.parade.id = ?1")
	Collection<Segment> findAllSegmentsByParadeId(int id);
}
