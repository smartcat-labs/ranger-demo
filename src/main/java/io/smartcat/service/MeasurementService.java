package io.smartcat.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import io.smartcat.domain.Measurement;
import io.smartcat.repository.MeasurementRepository;

@Service
public class MeasurementService {
	
	@Autowired
	private MeasurementRepository measurementRepository;

	public List<Measurement> getNewestMeasurementsForUser(String username, int limit) {
		PageRequest request = new PageRequest(0, limit, new Sort(Sort.Direction.DESC, "created"));
		// db.measurements.find( { owner : "passed_username" }).sort( { created : -1 }).limit(50);
		return measurementRepository.findByOwner(username, request);
	}
}
