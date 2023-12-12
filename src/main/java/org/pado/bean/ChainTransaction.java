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
 * @Date 2023/12/12 14:48
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "transaction")
public class ChainTransaction {
    @Id
    private Long id;
    private String hash;
    private BigInteger nonce;
    private BigInteger blockNumber;
    private String from;
    private String to;
    private BigInteger gasPrice;
    private BigInteger gas;
    private BigInteger value;
    private String input;
    private String method;
    /**
     * 0x1 or 0x0
     */
    private String status;

}
