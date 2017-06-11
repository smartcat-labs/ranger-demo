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
//		db.measurements.find( { owner : "username" }).sort( { created : -1 }).limit(50);
//		select * from measurements where owner = 'username' sort by created desc limit 50;
		return measurementRepository.findByOwner(username, request);
	}
	
	public List<Measurement> getNewestMeasurementsForUserAndSensor(String username, String sensor, int limit) {
		PageRequest request = new PageRequest(0, limit, new Sort(Sort.Direction.DESC, "created"));
//		db.measurements.find( { owner : "username", sensor : "sensor" }).sort( { created : -1 }).limit(50);
//		select * from measurements where owner = 'username' and sensor = 'sensor' sort by created desc limit 50;
		return measurementRepository.findByOwnerAndSensor(username, sensor, request);
	}
	
	public List<Measurement> getMeasurementsByUserAndSensor(String owner, String sensor) {
//		db.measurements.find({owner : "owner", sensor : "heart-rate-monitor"}, {_id : 0, _class : 0})
		return measurementRepository.findByOwnerAndSensor(owner, sensor);
	}
}
