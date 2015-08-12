package File;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.UUID;

import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.fileupload.*;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.catalina.connector.OutputBuffer;

import DB.Results.results;

public class xlxfile {
	public static String savefile(HttpServletRequest request) throws IOException{
	    String uploadPath = "image/"; // 上传文件的目录  
	    String tempPath = "imagetmp/"; // 临时文件目录  
	    String serverPath = null;  
	    String pathname = request.getSession().getServletContext().getRealPath("/"); 
	    int sizeMax = 3;//图片最大上限  
	    String[] fileType = new String[]{".jpg",".gif",".bmp",".png",".jpeg",".ico"};  
	    results myResults=new results();
       // serverPath = getServletContext().getRealPath("/").replace("\\", "/");  
	    serverPath=pathname;
        //Servlet初始化时执行,如果上传文件目录不存在则自动创建  
        if(!new File(serverPath+uploadPath).isDirectory()){  
            new File(serverPath+uploadPath).mkdirs();  
        }  
        if(!new File(serverPath+tempPath).isDirectory()){  
            new File(serverPath+tempPath).mkdirs();  
        }  
  
        DiskFileItemFactory factory = new DiskFileItemFactory(); 
        factory.setSizeThreshold(5*1024); //最大缓存  
        factory.setRepository(new File(serverPath+tempPath));//临时文件目录  
          
        ServletFileUpload upload = new ServletFileUpload(factory);
        upload.setSizeMax(sizeMax*1024*1024);//文件最大上限  
          
        String filePath = null;  
        try {  
            List<FileItem> items = upload.parseRequest(request);//获取所有文件列表  
            for (FileItem item : items) {  
                //获得文件名，这个文件名包括路径  
            	if(!item.isFormField()){
                    //文件名  
                    String fileName = item.getName().toLowerCase();  
                      
                    if(fileName.endsWith(fileType[0])||fileName.endsWith(fileType[1])||fileName.endsWith(fileType[2])||fileName.endsWith(fileType[3])||fileName.endsWith(fileType[4])||fileName.endsWith(fileType[5])){  
                        String uuid = UUID.randomUUID().toString();
                        filePath = uploadPath+uuid+fileName.substring(fileName.lastIndexOf("."));  
                        item.write(new File(serverPath+filePath));
                        myResults.message=filePath;
                        myResults.success=true;
                        return myResults.toString();
                    }else{
                        myResults.message="请确认上传的文件存在并且类型是图片!";
                        myResults.success=true;
                        return myResults.toString();
                    }
                }  
            }  
        } catch (Exception e) {  
            e.printStackTrace();  
            results tempResults=new results();
            tempResults.message="上传失败,请确认上传的文件大小不能超过"+sizeMax+"M";
            tempResults.success=false;
            return tempResults.toString();
        }
        myResults.message="上传图片错误";
        myResults.success=false;
		return myResults.toString();  
	}
	
	public static class uploadfile{
		public String filename;
		public FileInputStream xlxStream;
		public String savepathString;
		public uploadfile(){
			
		}
		public boolean save(){
			File myfile=new File(savepathString);
			if(!myfile.exists())
				try {
					myfile.createNewFile();
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			
			try {
				FileOutputStream output=new FileOutputStream(myfile);
				byte[] buffer=new byte[1024];
				int count;
				count=0;
				while(true)
				{
					count=xlxStream.read(buffer);
					if(count<=0)
						break;
					else {
						output.write(buffer);
					}
				}
				output.flush();
				output.close();
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}catch (IOException e){
				e.printStackTrace();
			}
			return false;
		}
	}
}