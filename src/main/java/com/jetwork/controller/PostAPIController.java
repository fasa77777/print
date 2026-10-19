package com.jetwork.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import com.jetwork.bin.SearchVO;
 

public class PostAPIController {
	private final Logger logger = LoggerFactory.getLogger(this.getClass());

    // http통신할때 post는 body에다 data를 집어넣어서 받아오겠다.
    // @RequestBody에 SearchVO에 있는 값을 매칭해서 보내줘라.
    
    @RequestMapping(method = RequestMethod.POST, path = "/postRequest")
    public SearchVO postRequest(@RequestBody SearchVO searchVo){
    	logger.info(searchVo.getEmail());
    	logger.info(searchVo.getId());
    	logger.info(searchVo.getPage()+"");
    	
    	searchVo.setEmail("ysson@gmail.com");
    	searchVo.setId("fasa21");
    	searchVo.setPage(1);

        return searchVo;
    }
    
    @PostMapping(value = "/postMapping")
    public SearchVO postMapping(@RequestBody SearchVO searchVo){
    	logger.info("postMapping");

    	return searchVo;
    }
 
}
