package org.pado.service;

import cn.hutool.core.util.StrUtil;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.pado.bean.ChainBlock;
import org.pado.bean.ChainTransaction;
import org.pado.repository.ChainBlockRepository;
import org.pado.utils.DateUtils;
import org.pado.utils.IdUtil;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.GroupOperation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterNumber;
import org.web3j.protocol.core.Request;
import org.web3j.protocol.core.methods.response.EthBlock;
import org.web3j.protocol.core.methods.response.Transaction;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.protocol.http.HttpService;

import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.*;

/**
 * @Author xuda
 * @Date 2023/12/12 14:53
 */
@Service
@Slf4j
public class ChainToolsService {

    private ThreadPoolExecutor executorPool;

    @Resource
    private MongoTemplate mongoTemplate;

    @Resource
    private ChainBlockRepository chainBlockRepository;

    @PostConstruct
    public void init() {
        int cpuCores = Runtime.getRuntime().availableProcessors();
        int maxThreads = cpuCores * 2;
        ThreadFactory threadFactory = Executors.defaultThreadFactory();
        executorPool =
            new ThreadPoolExecutor(cpuCores, maxThreads, 10, TimeUnit.SECONDS, new ArrayBlockingQueue<Runnable>(1024),
                threadFactory);
    }

    public void syncChainBlock() {
        Long startBlock = 1092628L;
        ChainBlock topByBlockNumber = chainBlockRepository.findTopByOrderByBlockNumberDesc();
        if(topByBlockNumber!=null){
            startBlock = topByBlockNumber.getBlockNumber().longValue();
        }
        Long endBlock = Long.MAX_VALUE;
        String method = "0x9b2846a6";
        Web3j web3j = Web3j.build(new HttpService("https://rpc.linea.build"));
        Long size = 0L;
        for (Long blockIndex = startBlock; blockIndex <= endBlock; blockIndex++) {
            try {
                log.info("blockNumber:{}",blockIndex);
                //check blockNumber has sync
                Query query = new Query(Criteria.where("blockNumber").is(blockIndex));
                long count = mongoTemplate.count(query, ChainBlock.class);
                if (count > 0) {
                    log.info("blockNumber:{} has sync!", blockIndex);
                    continue;
                }
                Request<?, EthBlock> ethBlockRequest =
                    web3j.ethGetBlockByNumber(new DefaultBlockParameterNumber(blockIndex), true);
                EthBlock send = ethBlockRequest.send();
                EthBlock.Block block = send.getBlock();
                if (block == null) {
                    log.info("block :{} is empty！", blockIndex);
                    blockIndex--;
                    //sleep for 3 seconds
                    try {
                        Thread.sleep(3000);
                    } catch (InterruptedException e) {
                        log.error(e.getMessage());
                    }
                    continue;
                }
                ChainBlock chainBlock = new ChainBlock();
                chainBlock.setBlockHash(block.getHash());
                BigInteger number = block.getNumber();
                chainBlock.setBlockNumber(number);
                BigInteger timestamp = block.getTimestamp().multiply(new BigInteger("1000"));
                LocalDateTime localDateTime = DateUtils.convertToLocalDateTime(timestamp.longValue());
                chainBlock.setTimestamp(localDateTime);
                chainBlock.setId(IdUtil.nextId());
                //save chainBlock
                List<EthBlock.TransactionResult> transactions = block.getTransactions();
                for (EthBlock.TransactionResult transaction : transactions) {
                    EthBlock.TransactionObject o = (EthBlock.TransactionObject)transaction.get();
                    Transaction transaction1 = o.get();
                    String toAddress = transaction1.getTo();
                    String hash = transaction1.getHash();
                    log.info("-----transactionHash:{}",hash);
                    Optional<TransactionReceipt> transactionReceipt =
                        web3j.ethGetTransactionReceipt(hash).send().getTransactionReceipt();
                    TransactionReceipt transactionReceipt1 = transactionReceipt.get();
                    //0x1: success,
                    String status = transactionReceipt1.getStatus();
                    String input = transaction1.getInput();
                    String methodStr = StrUtil.sub(input, 0, 10);
                    ChainTransaction chainTransaction = new ChainTransaction();
                    chainTransaction.setId(IdUtil.nextId());
                    chainTransaction.setBlockNumber(transaction1.getBlockNumber());
                    chainTransaction.setTo(transaction1.getTo());
                    chainTransaction.setFrom(transaction1.getFrom());
                    chainTransaction.setGas(transaction1.getGas());
                    chainTransaction.setGasPrice(transaction1.getGasPrice());
                    chainTransaction.setHash(transaction1.getHash());
                    chainTransaction.setInput(transaction1.getInput());
                    chainTransaction.setNonce(transaction1.getNonce());
                    chainTransaction.setMethod(methodStr);
                    chainTransaction.setStatus(status);
                    chainTransaction.setValue(transaction1.getValue());
                    //check transaction  exists
                    if (mongoTemplate.exists(new Query(Criteria.where("hash").is(hash)), Transaction.class)) {
                        log.info("-----transaction :{} has sync",hash);
                        continue;
                    }
                    mongoTemplate.save(chainTransaction);
                }
                mongoTemplate.save(chainBlock);
            } catch (Exception e) {
                log.error("error occurred:{}", e.getMessage());
                blockIndex--;
            }
        }
    }
}
