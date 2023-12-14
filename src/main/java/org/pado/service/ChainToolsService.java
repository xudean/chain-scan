package org.pado.service;

import cn.hutool.core.util.StrUtil;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.pado.bean.ChainBlock;
import org.pado.bean.ChainTransaction;
import org.pado.pojo.ActivityStaticVO;
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

import java.io.IOException;
import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.*;
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

    private static final Map<String, String[]> contractMap = new ConcurrentHashMap<>() {{
        put("PADO", new String[] {"0x9b2846a6", "0xc4b7dcba12866f6f8181b949ca443232c4e94334"});
        put("zkpass", new String[] {"0x4b2bc20e", "0x3b30d7c4e5aa3d7da11431af23e8d1f7d25bb0b8"});
        put("clique", new String[] {"0x07432196", "0x065e959ffd4c76ae2e0d31cfcf91c0c9834472ec"});
        put("GitCoin", new String[] {"0x48a15e6f", "0xc94abf0292ac04aac18c251d9c8169a8dd2bbbdc"});
        put("openid3", new String[] {"0x07432196", "0xce048492076b0130821866f6d05a0b621b1715c8"});
        put("nomis", new String[] {"0x2205306d", "0xe2627bab7af74e333f3d89e4bd78c919d8b930e8"});
        put("0xscore", new String[] {"0xbc2a4c29", "0xbdec68492d69a7ff1fb4c2abf5c28ade535dc88a"});
        put("Trusta", new String[] {"0x07432196", "0xb86b3e16b6b960fd822849fd4b4861d73805879b"});
    }};

    @PostConstruct
    public void init() {
        int cpuCores = Runtime.getRuntime().availableProcessors();
        int maxThreads = cpuCores * 2;
        ThreadFactory threadFactory = Executors.defaultThreadFactory();
        executorPool =
            new ThreadPoolExecutor(20, 40, 20, TimeUnit.SECONDS, new ArrayBlockingQueue<Runnable>(1024),
                threadFactory);
    }

    public void syncChainBlock(Long startBlock) {
        if (startBlock == null) {
            startBlock = 1092628L;
        }
        //        ChainBlock topByBlockNumber = chainBlockRepository.findTopByOrderByBlockNumberDesc();
        //        if(topByBlockNumber!=null){
        //            startBlock = topByBlockNumber.getBlockNumber().longValue();
        //        }
        Long endBlock = Long.MAX_VALUE;
        String method = "0x9b2846a6";
        Web3j web3j = Web3j.build(new HttpService("https://rpc.linea.build"));
        Long size = 0L;
        for (Long blockIndex = startBlock; blockIndex <= endBlock; blockIndex++) {
            try {
                log.info("blockNumber:{}", blockIndex);
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
                chainBlock.setBlockNumber(number.longValue());
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
                    log.info("-----transactionHash:{}", hash);
                    Optional<TransactionReceipt> transactionReceipt =
                        web3j.ethGetTransactionReceipt(hash).send().getTransactionReceipt();
                    TransactionReceipt transactionReceipt1 = transactionReceipt.get();
                    //0x1: success,
                    String status = transactionReceipt1.getStatus();
                    String input = transaction1.getInput();
                    String methodStr = StrUtil.sub(input, 0, 10);
                    ChainTransaction chainTransaction = new ChainTransaction();
                    chainTransaction.setId(IdUtil.nextId());
                    chainTransaction.setBlockNumber(transaction1.getBlockNumber().longValue());
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
                        log.info("-----transaction :{} has sync", hash);
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

    public void asyncChainBlock(Long startBlock) throws IOException {
        if (startBlock == null) {
            startBlock = 1092628L;
        }
        //        ChainBlock topByBlockNumber = chainBlockRepository.findTopByOrderByBlockNumberDesc();
        //        if(topByBlockNumber!=null){
        //            startBlock = topByBlockNumber.getBlockNumber().longValue();
        //        }
        Long endBlock = Long.MAX_VALUE;
        String method = "0x9b2846a6";
        Web3j web3j = Web3j.build(new HttpService("https://rpc.linea.build"));
        Long size = 0L;
        for (Long blockIndex = startBlock; blockIndex <= endBlock; blockIndex++) {

            Long finalBlockIndex = blockIndex;
            executorPool.submit(new Runnable() {
                @SneakyThrows
                @Override
                public void run() {
                    log.info("blockNumber:{}", finalBlockIndex);
                    //check blockNumber has sync
                    Query query = new Query(Criteria.where("blockNumber").is(finalBlockIndex));
                    long count = mongoTemplate.count(query, ChainBlock.class);
                    if (count > 0) {
                        log.info("blockNumber:{} has sync!", finalBlockIndex);
                        return;
                    }
                    EthBlock.Block block = null;
                    while (block == null) {
                        block = getBlock(web3j, finalBlockIndex);
                    }
                    ChainBlock chainBlock = new ChainBlock();
                    chainBlock.setBlockHash(block.getHash());
                    BigInteger number = block.getNumber();
                    chainBlock.setBlockNumber(number.longValue());
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
                        log.info("-----transactionHash:{}", hash);
                        Optional<TransactionReceipt> transactionReceipt =
                            web3j.ethGetTransactionReceipt(hash).send().getTransactionReceipt();
                        TransactionReceipt transactionReceipt1 = transactionReceipt.get();
                        //0x1: success,
                        String status = transactionReceipt1.getStatus();
                        String input = transaction1.getInput();
                        String methodStr = StrUtil.sub(input, 0, 10);
                        ChainTransaction chainTransaction = new ChainTransaction();
                        chainTransaction.setId(IdUtil.nextId());
                        chainTransaction.setBlockNumber(transaction1.getBlockNumber().longValue());
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
                            log.info("-----transaction :{} has sync", hash);
                            continue;
                        }
                        mongoTemplate.save(chainTransaction);
                    }
                    mongoTemplate.save(chainBlock);
                }
            });

        }
    }

    public EthBlock.Block getBlock(Web3j web3j, Long blockIndex) throws IOException {
        Request<?, EthBlock> ethBlockRequest =
            web3j.ethGetBlockByNumber(new DefaultBlockParameterNumber(blockIndex), true);
        EthBlock send = ethBlockRequest.send();
        EthBlock.Block block = send.getBlock();
        if (block == null) {
            log.info("block :{} is empty！", blockIndex);
            //sleep for 3 seconds
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                log.error(e.getMessage());
            }
        }
        return block;
    }

    public void check() {
        Long start = 1092628L;
        //        Long start = 1L;
        Long first = start;
        for (Long i = start; i < 1189374L; i++) {

            Query query = new Query(Criteria.where("blockNumber").is(i));
            long count = mongoTemplate.count(query, ChainBlock.class);
            if (count == 0) {
                if (i.equals(first)) {
                    first++;
                    continue;
                }
                log.info("{} ~ {}", first, i - 1);
                first = i + 1;
            }
        }
    }

    public List<ActivityStaticVO> activityStaticVOList(Long startBlock, Long endBlock) {
        if (startBlock == null) {
            startBlock = 1092628L;
        }
        Set<String> strings = contractMap.keySet();
        List<ActivityStaticVO> list = new ArrayList<>();
        for (String string : strings) {
            String[] strings1 = contractMap.get(string);
            String method = strings1[0];
            String contractAddress = strings1[1];
            Query query = new Query(Criteria.where("blockNumber").gte(startBlock));
            if (endBlock != null) {
                query.addCriteria(Criteria.where("blockNumber").lte(endBlock));
            }
            query.addCriteria(Criteria.where("method").is(method));
            query.addCriteria(Criteria.where("contractAddress").is(contractAddress));
            query.addCriteria(Criteria.where("status").is("0x1"));
            Long longs = mongoTemplate.count(query, ChainTransaction.class);
            ActivityStaticVO activityStaticVO = new ActivityStaticVO();
            activityStaticVO.setCompany(string);
            activityStaticVO.setSize(longs);
            list.add(activityStaticVO);
        }
        return list;

    }
}
