package com.ssairen.backend.domain.frozenaccount.repository;

import com.ssairen.backend.domain.frozenaccount.entity.FrozenAccount;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FrozenAccountRepository extends JpaRepository<FrozenAccount, Long> {

    List<FrozenAccount> findAllByOrderByRequestedAtDesc();
}
