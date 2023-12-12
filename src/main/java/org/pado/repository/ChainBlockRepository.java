package org.pado.repository;

import org.pado.bean.ChainBlock;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Service;

/**
 * @Author xuda
 * @Date 2023/12/12 16:12
 */
@Service
public interface ChainBlockRepository extends MongoRepository<ChainBlock, Long> {
    ChainBlock findTopByOrderByBlockNumberDesc();
}
