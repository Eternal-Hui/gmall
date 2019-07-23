package com.atguigu.gmall.manage.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall.bean.PmsBaseSaleAttr;
import com.atguigu.gmall.bean.PmsProductImage;
import com.atguigu.gmall.bean.PmsProductInfo;
import com.atguigu.gmall.bean.PmsProductSaleAttr;
import com.atguigu.gmall.manage.Utils.MyUploads;
import com.atguigu.gmall.service.PmsProductService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@CrossOrigin
public class SPUController {

    @Reference
    PmsProductService pmsProductService;

    @RequestMapping("spuList")
    public List<PmsProductInfo> spuList(String catalog3Id){

        return pmsProductService.spuList(catalog3Id);

    }

    @RequestMapping("baseSaleAttrList")
    public List<PmsBaseSaleAttr> baseSaleAttrList(){

        return pmsProductService.baseSaleAttrList();

    }

    @RequestMapping("saveSpuInfo")
    public String saveSpuInfo(@RequestBody PmsProductInfo pmsProductInfo){

        pmsProductService.saveSpuInfo(pmsProductInfo);

        return "success";

    }

    @RequestMapping("fileUpload")
    public String fileUpload(@RequestParam("file") MultipartFile multipartFile){

        String imageUrl = MyUploads.uploadImage(multipartFile);

        System.out.println(imageUrl);
        // 返回文件的在服务器的访问URL
        return imageUrl;
    }

    @RequestMapping("spuSaleAttrList")
    public List<PmsProductSaleAttr> spuSaleAttrList(String spuId){

        return pmsProductService.spuSaleAttrList(spuId);
    }

    @RequestMapping("spuImageList")
    public List<PmsProductImage> spuImageList(String spuId){

        return pmsProductService.spuImageList(spuId);
    }




}
