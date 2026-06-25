package com.aisandbox.survey.repository;

import com.aisandbox.survey.model.Survey;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SurveyRepository extends JpaRepository<Survey, Long> {

	/** Active (not soft-deleted) surveys. */
	List<Survey> findByDeletedFalse();
}
