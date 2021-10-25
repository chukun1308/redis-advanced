package com.chukun.redis.controller;

import com.chukun.redis.entities.Product;
import com.chukun.redis.service.ProductService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @author chukun
 */
@RestController
@Slf4j
@Api(description = "聚划算商品列表接口")
public class DoubleProductController
{
    @Autowired
    private ProductService productService;

    /**
     * 分页查询：在高并发的情况下，只能走redis查询，走db的话必定会把db打垮
     * http://localhost:5555/swagger-ui.html#/jhs-product-controller/findUsingGET
     */
    @RequestMapping(value = "/pruduct/double/find",method = RequestMethod.GET)
    @ApiOperation("按照分页和每页显示容量，点击查看")
    public List<Product> find(int page, int size) {
        return productService.queryProductsFromDoubleCache(page, size);
    }

}