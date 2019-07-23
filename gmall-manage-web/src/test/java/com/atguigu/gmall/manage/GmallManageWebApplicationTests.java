package com.atguigu.gmall.manage;

import org.csource.common.MyException;
import org.csource.fastdfs.ClientGlobal;
import org.csource.fastdfs.StorageClient;
import org.csource.fastdfs.TrackerClient;
import org.csource.fastdfs.TrackerServer;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.io.InputStream;

@RunWith(SpringRunner.class)
@SpringBootTest
public class GmallManageWebApplicationTests {

    @Test
    public void testFileUpload() throws Exception {

        String path = GmallManageWebApplicationTests.class.getClassLoader().getResource("tracker.conf").getPath();

        ClientGlobal.init(path);
        // 获取stacker连接
        TrackerClient trackerClient = new TrackerClient();

        TrackerServer connection = trackerClient.getConnection();

        // 根据stacker连接获取storage
        StorageClient storageClient = new StorageClient(connection, null);

        String imgUrl = "http://192.168.186.100";

        // 通过storage上传文件
        String[] jpgs = storageClient.upload_file("C:\\Users\\MrGuo\\Pictures\\4.jpg", "jpg", null);

        for (String jpg : jpgs) {
            imgUrl += "/" + jpg;
        }
        System.out.println(imgUrl);
    }

}
