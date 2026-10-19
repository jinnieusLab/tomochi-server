package com.tomochi.auth.domain;

import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

	Optional<RefreshToken> findByTokenHash(String tokenHash);

	/**
	 * 아직 살아 있을 때만 폐기해요. 같은 토큰으로 두 요청이 동시에 와도 InnoDB가 이 행을 잠그고 차례로 실행해서,
	 * 영향 받은 행이 1인 요청은 하나뿐이에요. 락을 따로 잡지 않고 UPDATE 하나로 승자를 정해요.
	 * 끝나면 영속성 컨텍스트를 비워서, 뒤에서 다시 읽을 때 옛 값을 쓰지 않게 해요.
	 *
	 * @return 1이면 이 요청이 회전에 성공, 0이면 이미 누군가 폐기함
	 */
	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("update RefreshToken t set t.revokedAt = :now, t.lastUsedAt = :now where t.id = :id and t.revokedAt is null")
	int revokeIfActive(@Param("id") Long id, @Param("now") Instant now);

	/** 탈취로 본 family의 남은 토큰을 모두 폐기해요. */
	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("update RefreshToken t set t.revokedAt = :now where t.familyId = :familyId and t.revokedAt is null")
	int revokeFamily(@Param("familyId") String familyId, @Param("now") Instant now);

	/**
	 * 잠금 읽기(SELECT ... FOR UPDATE)라 REPEATABLE READ의 처음 스냅샷이 아니라 지금 커밋된 값을 읽어요.
	 * 회전에 진 요청이 "누가 언제 폐기했는지"를 정확히 봐야 해서 써요.
	 */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select t from RefreshToken t where t.familyId = :familyId")
	List<RefreshToken> findFamilyForUpdate(@Param("familyId") String familyId);
}
