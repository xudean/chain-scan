package org.pado.bean;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigInteger;
import java.time.LocalDateTime;

/**
 * @Author xuda
 * @Date 2023/12/12 14:43
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "block")
public class ChainBlock {
    @Id
    private Long id;
    private BigInteger blockNumber;
    private String blockHash;
    private LocalDateTime timestamp;
}
