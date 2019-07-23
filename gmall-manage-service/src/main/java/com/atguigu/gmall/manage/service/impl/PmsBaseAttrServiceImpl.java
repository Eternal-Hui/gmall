package com.atguigu.gmall.manage.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.atguigu.gmall.bean.PmsBaseAttrInfo;
import com.atguigu.gmall.bean.PmsBaseAttrValue;
import com.atguigu.gmall.manage.mapper.PmsBaseAttrInfoMapper;
import com.atguigu.gmall.manage.mapper.PmsBaseAttrValueMapper;
import com.atguigu.gmall.service.PmsBaseAttrService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashSet;
import java.util.List;

@Service
public class PmsBaseAttrServiceImpl implements PmsBaseAttrService {

    @Autowired
    PmsBaseAttrInfoMapper pmsBaseAttrInfoMapper;

    @Autowired
    PmsBaseAttrValueMapper pmsBaseAttrValueMapper;

    @Override
    public List<PmsBaseAttrInfo> attrInfoList(String catalog3Id) {

        PmsBaseAttrInfo pmsBaseAttrInfo = new PmsBaseAttrInfo();

        pmsBaseAttrInfo.setCatalog3Id(catalog3Id);

        List<PmsBaseAttrInfo> pmsBaseAttrInfos = pmsBaseAttrInfoMapper.select(pmsBaseAttrInfo);

        // 查询平台属性值
        for (PmsBaseAttrInfo baseAttrInfo : pmsBaseAttrInfos) {

            PmsBaseAttrValue pmsBaseAttrValue = new PmsBaseAttrValue();
            pmsBaseAttrValue.setAttrId(baseAttrInfo.getId());

            List<PmsBaseAttrValue> pmsBaseAttrValues = pmsBaseAttrValueMapper.select(pmsBaseAttrValue);

            baseAttrInfo.setAttrValueList(pmsBaseAttrValues);
        }

        return pmsBaseAttrInfos;
    }

    @Override
    public void saveAttrInfo(PmsBaseAttrInfo pmsBaseAttrInfo) {

        String id  = pmsBaseAttrInfo.getId();

        // 获取属性值的集合
        List<PmsBaseAttrValue> attrValueList = pmsBaseAttrInfo.getAttrValueList();

        // 如果属性id为空,则说明是新增
        if (StringUtils.isBlank(id)){

            pmsBaseAttrInfoMapper.insertSelective(pmsBaseAttrInfo);
            // 获取插入成功返回的主键id
            String attrId = pmsBaseAttrInfo.getId();

            // 遍历attrValueList
            for (PmsBaseAttrValue pmsBaseAttrValue : attrValueList) {

                // 设置属性的id
                pmsBaseAttrValue.setAttrId(attrId);

                // 保存到数据库中
                pmsBaseAttrValueMapper.insertSelective(pmsBaseAttrValue);

            }

        }else{// 不为空,说明是修改属性信息
            // 更新pmsBaseAttrInfo
            pmsBaseAttrInfoMapper.updateByPrimaryKey(pmsBaseAttrInfo);

            // 先把原来的属性值清空
            PmsBaseAttrValue pmsBaseAttrValue = new PmsBaseAttrValue();
            pmsBaseAttrValue.setAttrId(id);
            pmsBaseAttrValueMapper.delete(pmsBaseAttrValue);

            // 重新保存属性信息
            // 遍历attrValueList
            for (PmsBaseAttrValue newPmsBaseAttrValue : attrValueList) {

                // 设置属性的id
                newPmsBaseAttrValue.setAttrId(id);

                // 保存到数据库中
                pmsBaseAttrValueMapper.insertSelective(newPmsBaseAttrValue);

            }

        }



    }

    @Override
    public List<PmsBaseAttrValue> getAttrValueList(String attrId) {

        PmsBaseAttrValue pmsBaseAttrValue = new PmsBaseAttrValue();

        pmsBaseAttrValue.setAttrId(attrId);

        return pmsBaseAttrValueMapper.select(pmsBaseAttrValue);

    }

    @Override
    public List<PmsBaseAttrInfo> getAttrValueListByValueIds(HashSet<String> attrValueIdsSet) {

        String attrValueId = StringUtils.join(attrValueIdsSet,",");

        List<PmsBaseAttrInfo> pmsBaseAttrInfos = pmsBaseAttrInfoMapper.selectAttrValueListByValueIds(attrValueId);

        return pmsBaseAttrInfos;
    }
}
