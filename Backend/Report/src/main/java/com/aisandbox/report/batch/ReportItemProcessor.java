package com.aisandbox.report.batch;

import com.aisandbox.report.model.Report;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

@Component
public class ReportItemProcessor implements ItemProcessor<Report, Report> {

	@Override
	public Report process(Report report) {
		// Placeholder: apply business logic / transformations here
		report.setStatus("PROCESSED");
		return report;
	}

}
