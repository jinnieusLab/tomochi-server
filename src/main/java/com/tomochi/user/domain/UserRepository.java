package com.tomochi.user.domain;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, Long> {

	boolean existsByInviteCode(String inviteCode);

	Optional<User> findByInviteCodeAndDeletedAtIsNull(String inviteCode);

	/**
	 * 두 사용자 행을 id 순서로 잠가요 (SELECT ... FOR UPDATE). 한 문장이 PK 인덱스를 오름차순으로 훑으며 잠가서,
	 * A→B와 B→A가 동시에 와도 늘 작은 id부터 잠그고, 서로 반대로 잡고 기다리는 데드락이 생기지 않아요.
	 */
	@Query(value = "SELECT id FROM users WHERE id IN (:a, :b) ORDER BY id FOR UPDATE", nativeQuery = true)
	List<Long> lockInIdOrder(@Param("a") Long a, @Param("b") Long b);
}
