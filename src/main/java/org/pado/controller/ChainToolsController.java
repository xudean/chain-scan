package org.pado.controller;

import cn.hutool.core.util.StrUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.pado.pojo.ActivityStaticVO;
import org.pado.service.ChainToolsService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @Author xuda
 * @Date 2023/12/12 16:06
 */
@RestController
@Slf4j
@RequestMapping("/chain")
@CrossOrigin
public class ChainToolsController {

    @Resource
    private ChainToolsService chainToolsService;

    @GetMapping("/block")
    public void syncChainBlock(@RequestParam Long start) {
        chainToolsService.syncChainBlock(start);
    }

    @GetMapping("/block/async")
    public void asyncChainBlock(@RequestParam Long start) {
        chainToolsService.syncChainBlock(start);
    }

    @GetMapping("/check")
    public void check() {
        chainToolsService.check();
    }

    @GetMapping("/linea")
    public List<ActivityStaticVO> list(@RequestParam(required = false) String start, @RequestParam(required = false) String end){
        Long startBlock = null;
        Long endBlock = null;
        if(StrUtil.isNotEmpty(start)){
            startBlock = Long.valueOf(start);
        }
        if(StrUtil.isNotEmpty(end)){
            endBlock = Long.valueOf(end);
        }
        return chainToolsService.activityStaticVOList(startBlock,endBlock);
    }
}
