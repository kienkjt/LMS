package com.kjt.lms.repository;

import com.kjt.lms.model.entity.OtpRedis;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OtpRedisRepository extends CrudRepository<OtpRedis, String> {

}

