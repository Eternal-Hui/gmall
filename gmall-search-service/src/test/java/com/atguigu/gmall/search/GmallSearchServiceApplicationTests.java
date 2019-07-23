package com.atguigu.gmall.search;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall.bean.PmsSearchSkuInfo;
import com.atguigu.gmall.bean.PmsSkuInfo;
import com.atguigu.gmall.service.PmsSkuService;
import io.searchbox.client.JestClient;
import io.searchbox.client.JestResult;
import io.searchbox.core.Index;
import io.searchbox.core.Search;
import io.searchbox.core.SearchResult;
import org.elasticsearch.index.query.*;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@RunWith(SpringRunner.class)
@SpringBootTest
public class GmallSearchServiceApplicationTests {

    @Autowired
    JestClient jestClient;

    @Reference
    PmsSkuService pmsSkuService;

    @Test
    public void testQuery() throws IOException {

        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();

        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();

        MatchQueryBuilder matchQueryBuilder = new MatchQueryBuilder("skuName", "拯救者");
        boolQueryBuilder.must(matchQueryBuilder);

        TermQueryBuilder termQueryBuilder = new TermQueryBuilder("skuAttrValueList.valueId", "104");
        boolQueryBuilder.filter(termQueryBuilder);

        SearchSourceBuilder query = searchSourceBuilder.query(boolQueryBuilder);

        Search search = new Search.Builder(query.toString())
                .addIndex("gmall")
                .addType("PmsSearchSkuInfo")
                .build();
        SearchResult execute = jestClient.execute(search);

        List<SearchResult.Hit<PmsSearchSkuInfo, Void>> hits = execute.getHits(PmsSearchSkuInfo.class);

        for (SearchResult.Hit<PmsSearchSkuInfo, Void> hit : hits) {
            PmsSearchSkuInfo pmsSearchSkuInfo = hit.source;
            System.out.println(pmsSearchSkuInfo.toString());
        }

    }

    @Test
    public void contextLoads() throws IOException {

//        Search search = new Search.Builder("{\"query\": {\"match\": {\"actorList.name\": \"zhang\"}}}")
//                .addIndex("movie_index").addType("movie").build();

        List<PmsSearchSkuInfo> pmsSearchSkuInfos = new ArrayList<>();

        List<PmsSkuInfo> skuInfos = pmsSkuService.getAllSku();

        for (PmsSkuInfo skuInfo : skuInfos) {
            PmsSearchSkuInfo pmsSearchSkuInfo = new PmsSearchSkuInfo();
            BeanUtils.copyProperties(skuInfo,pmsSearchSkuInfo);
            String id = skuInfo.getId();
            pmsSearchSkuInfo.setId(Long.parseLong(id));
            pmsSearchSkuInfos.add(pmsSearchSkuInfo);
        }

        for (PmsSearchSkuInfo pmsSearchSkuInfo : pmsSearchSkuInfos) {

            Index index = new Index.Builder(pmsSearchSkuInfo).index("gmall")
                                                            .type("PmsSearchSkuInfo")
                                                            .id(pmsSearchSkuInfo.getId()+"")
                                                            .build();

            JestResult execute = jestClient.execute(index);
        }

        //System.out.println(execute);

    }

}
