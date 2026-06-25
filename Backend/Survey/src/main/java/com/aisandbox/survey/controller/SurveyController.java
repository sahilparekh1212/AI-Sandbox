package com.aisandbox.survey.controller;

import com.aisandbox.survey.exception.ResourceNotFoundException;
import com.aisandbox.survey.model.Survey;
import com.aisandbox.survey.ratelimit.TransactionalRequestExecutor;
import com.aisandbox.survey.repository.SurveyRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/surveys")
@Tag(name = "Surveys", description = "Survey CRUD operations")
public class SurveyController {

	private static final Logger log = LoggerFactory.getLogger(SurveyController.class);

	private final SurveyRepository surveyRepository;
	// Wraps mutations so a superseding request rolls the work back transactionally.
	private final TransactionalRequestExecutor txExecutor;

	public SurveyController(SurveyRepository surveyRepository, TransactionalRequestExecutor txExecutor) {
		this.surveyRepository = surveyRepository;
		this.txExecutor = txExecutor;
	}

	@GetMapping
	@Operation(summary = "List surveys (excludes soft-deleted unless includeDeleted=true)")
	public List<Survey> findAll(@RequestParam(defaultValue = "false") boolean includeDeleted) {
		log.info("Fetching surveys includeDeleted={}", includeDeleted);
		return includeDeleted ? surveyRepository.findAll() : surveyRepository.findByDeletedFalse();
	}

	@GetMapping("/{id}")
	@Operation(summary = "Get survey by ID")
	public Survey findById(@PathVariable Long id) {
		log.info("Fetching survey id={}", id);
		return surveyRepository.findById(id)
			.orElseThrow(() -> new ResourceNotFoundException("Survey not found: " + id));
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@Operation(summary = "Create a new survey")
	public Survey create(@RequestBody Survey survey) {
		Survey saved = txExecutor.run(() -> surveyRepository.save(survey));
		log.info("Created survey id={}", saved.getId());
		return saved;
	}

	@PutMapping("/{id}")
	@Operation(summary = "Update an existing survey")
	public Survey update(@PathVariable Long id, @RequestBody Survey payload) {
		Survey saved = txExecutor.run(() -> {
			Survey survey = surveyRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Survey not found: " + id));
			survey.setTitle(payload.getTitle());
			survey.setDescription(payload.getDescription());
			survey.setStatus(payload.getStatus());
			return surveyRepository.save(survey);
		});
		log.info("Updated survey id={}", id);
		return saved;
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@Operation(summary = "Delete a survey (soft delete)")
	public void delete(@PathVariable Long id) {
		txExecutor.run(() -> {
			Survey survey = surveyRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Survey not found: " + id));
			survey.setDeleted(true);
			surveyRepository.save(survey);
			return null;
		});
		log.info("Soft-deleted survey id={}", id);
	}

	@GetMapping("/health")
	@Operation(summary = "Health check")
	public ResponseEntity<String> health() {
		return ResponseEntity.ok("survey-service OK");
	}

}
