/*
 * 실서버 JetworkServer-0.0.10-SNAPSHOT.jar (2022-04-18 빌드) 에서 역컴파일한 소스 (CFR 0.152).
 */
package com.jetwork.controller;

import com.jetwork.bin.SearchVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

public class PostAPIController {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @RequestMapping(method={RequestMethod.POST}, path={"/postRequest"})
    public SearchVO postRequest(@RequestBody SearchVO searchVo) {
        this.logger.info(searchVo.getEmail());
        this.logger.info(searchVo.getId());
        this.logger.info(String.valueOf(searchVo.getPage()));
        searchVo.setEmail("ysson@gmail.com");
        searchVo.setId("fasa21");
        searchVo.setPage(1);
        return searchVo;
    }

    @PostMapping(value={"/postMapping"})
    public SearchVO postMapping(@RequestBody SearchVO searchVo) {
        this.logger.info("postMapping");
        return searchVo;
    }
}
