package com.atguigu.gmall.manage.Utils;

import org.csource.common.MyException;
import org.csource.fastdfs.ClientGlobal;
import org.csource.fastdfs.StorageClient;
import org.csource.fastdfs.TrackerClient;
import org.csource.fastdfs.TrackerServer;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public class MyUploads {

    public static String uploadImage(MultipartFile multipartFile){

        String imgUrl = "Http://192.168.186.100";


        // 先连链接tracker
        String path = MyUploads.class.getClassLoader().getResource("tracker.conf").getPath();
        System.out.println(path);
        try {
            ClientGlobal.init(path);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (MyException e) {
            e.printStackTrace();
        }

        TrackerClient trackerClient = new TrackerClient();

        TrackerServer connection = null;
        try {
            connection = trackerClient.getConnection();
        } catch (IOException e) {
            e.printStackTrace();
        }

        // 通过tracker获得storage
        StorageClient storageClient = new StorageClient(connection,null);

        // 通过storage上传
        try {
            byte[] bytes = multipartFile.getBytes();
            String originalFilename = multipartFile.getOriginalFilename();
            int i = originalFilename.lastIndexOf(".");
            String substringExtName = originalFilename.substring(i + 1);
            String[] jpgs = new String[0];
            jpgs = storageClient.upload_file(bytes, substringExtName, null);
            for (String jpg : jpgs) {
                imgUrl += "/"+jpg;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return imgUrl;

    }





}
