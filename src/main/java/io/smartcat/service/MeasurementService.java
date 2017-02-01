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
//		db.measurements.find( { owner : "batman" }).sort( { created : -1 }).limit(50);
//		select * from measurements where owner = 'batman' sort by created desc limit 50;
		return measurementRepository.findByOwner(username, request);
	}
	
	public List<Measurement> getMeasurementsByUserAndSensor(String owner, String sensor) {
//		db.measurements.find({owner : "batman", sensor : "proximity"}, {_id : 0, _class : 0})
		return measurementRepository.findByOwnerAndSensor(owner, sensor);
	}
}
