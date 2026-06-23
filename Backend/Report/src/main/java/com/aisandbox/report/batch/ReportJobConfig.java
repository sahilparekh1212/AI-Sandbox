package com.aisandbox.report.batch;

import com.aisandbox.report.model.Report;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
public class ReportJobConfig {

	@Bean
	public Job reportJob(JobRepository jobRepository, Step reportStep) {
		return new JobBuilder("reportJob", jobRepository)
			.start(reportStep)
			.build();
	}

	@Bean
	public Step reportStep(
		JobRepository jobRepository,
		PlatformTransactionManager transactionManager,
		ReportItemReader reader,
		ReportItemProcessor processor,
		ReportItemWriter writer
	) {
		return new StepBuilder("reportStep", jobRepository)
			.<Report, Report>chunk(10, transactionManager)
			.reader(reader)
			.processor(processor)
			.writer(writer)
			.build();
	}

}
