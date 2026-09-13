package com.community.waste.repo;

import com.community.waste.model.EventParticipant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EventParticipantRepo extends JpaRepository<EventParticipant, Long> {
    List<EventParticipant> findByEventId(Long eventId);
}
