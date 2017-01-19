package io.smartcat.repository;

import java.io.Serializable;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.CrudRepository;

import io.smartcat.domain.Measurement;

public interface MeasurementRepository  extends CrudRepository<Measurement, Serializable>{
	
	List<Measurement> findByOwner(String owner, Pageable pageable);
	List<Measurement> findByOwnerAndSensor(String owner, String sensor);

}
