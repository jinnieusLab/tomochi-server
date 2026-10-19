package com.tomochi.friend.domain;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FriendshipRepository extends JpaRepository<Friendship, Long> {

	Optional<Friendship> findByUserLowAndUserHigh(Long userLow, Long userHigh);

	default Optional<Friendship> findBetween(Long a, Long b) {
		return findByUserLowAndUserHigh(Math.min(a, b), Math.max(a, b));
	}

	@Query("select count(f) from Friendship f where f.userLow = :userId or f.userHigh = :userId")
	long countByUser(@Param("userId") Long userId);
}
