package com.easyfamily.feedback.repository;

import com.easyfamily.feedback.entity.Feedback;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FeedbackRepository extends JpaRepository<Feedback, Long> {

    Page<Feedback> findByStatusOrderByCreatedAtDesc(String status, Pageable pageable);

    Page<Feedback> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
