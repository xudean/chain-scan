package org.pado.controller;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.pado.service.ChainToolsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * @Author xuda
 * @Date 2023/12/12 16:06
 */
@RestController
@Slf4j
@RequestMapping("/chain")
public class ChainToolsController {

    @Resource
    private ChainToolsService chainToolsService;

    @GetMapping("/block")
    public void syncChainBlock(@RequestParam Long start) {
        chainToolsService.syncChainBlock(start);
    }
}
