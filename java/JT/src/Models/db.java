package Models;

import DB.Results.results;
import javafx.scene.layout.Region;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.SAXException;

import sun.management.counter.Variability;

import com.mysql.jdbc.Field;

import Models.config;

import java.io.IOException;
import java.lang.reflect.Modifier;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;


public class db {
	private String dburls="";
	private String dbuser="";
	private String dbpwd="";
	
	public db(){
		///读取config.xml文件中的数据库配置
		try {
			SAXParserFactory parserFactory=SAXParserFactory.newInstance();
			SAXParser parser;
			parser = parserFactory.newSAXParser();
			config dbconfig=new config();
			parser.parse("H:/javaproject/JTBackstage/WebContent/WEB-INF/config.xml",dbconfig);
			dburls=dbconfig.getDburls();
			dbpwd=dbconfig.getDbpwd();
			dbuser=dbconfig.getDbuser();
			
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (IOException e){
			e.printStackTrace();
		}
	}
	
	///新增操作
	public results add(Object myobj) throws Exception {
			Connection conn=null;
			Class.forName("com.mysql.jdbc.Driver");
			conn=DriverManager.getConnection(dburls,dbuser,dbpwd);
			String cmd="";
			///
			Class temp=myobj.getClass();
			java.lang.reflect.Field[] fields=temp.getDeclaredFields();
			///CMD拼装
			cmd=cmd+"insert into "+temp.getName()+" values(";
			for(int i=0;i<fields.length;i++)
			{
				cmd=cmd+fields[i].toString();
				if(i==fields.length-1)
					cmd=cmd+")";
				else cmd=cmd+",";
			}
			System.out.println(cmd);
			
			PreparedStatement ps = conn.prepareStatement(cmd);
			results myResults=new results();
			myResults.success=ps.execute();
			if(myResults.success)
				myResults.message="新增成功";
			else myResults.message="新增失败";
			return myResults;
	}
	
	///修改操作
	public results modify(Object myobj) throws Exception{
		Connection conn=null;
		Class.forName("com.mysql.jdbc.Driver");
		conn=DriverManager.getConnection(dburls,dbuser,dbpwd);
		String cmd="";
		String priString="";
		String [] prikey=((modelsbasic)myobj).GetPrimaryKey().split(",");
		///反射获取对象属性
		Class temp=myobj.getClass();
		java.lang.reflect.Field[] fields=temp.getDeclaredFields();
		
		
		///CMD命令拼装
		cmd=cmd+"update "+temp.getName()+" set ";
		for(int i=0;i<fields.length;i++)
		{
			cmd=cmd+fields[i].getName()+"='"+fields[i].toString()+"' ";
			if(i==fields.length-1)
				cmd=cmd+")";
			else cmd=cmd+",";
			for (String tt:prikey) {
				if(tt.equals(fields[i].getName())){
					if(priString.equals(""))
						priString=tt+"="+fields[i].toString();
					else priString=" and "+tt+"="+fields[i].toString();
				}
			}
		}
		cmd=cmd+" where "+priString;
		
		System.out.println(cmd);
		
		PreparedStatement ps = conn.prepareStatement(cmd);
		results myResults=new results();
		myResults.success=ps.execute();
		if(myResults.success)
			myResults.message="修改操作成功";
		else myResults.message="修改操作失败";
		return myResults;
		
	}
	
	///查询操作
	public results query(String cmd){
		try{
			Connection conn=null;
			Class.forName("com.mysql.jdbc.Driver");
			conn=DriverManager.getConnection(dburls,dbuser,dbpwd);
			PreparedStatement ps = conn.prepareStatement(cmd);
			results myresult=new results();
			myresult.success=ps.execute();
			if(myresult.success)
				myresult.message="查询成功";
			else myresult.message="查询失败";
			return myresult;
		}
		catch(Exception e){
			System.out.println(e.getMessage());
			return new results(false,e.getMessage());
		}
	}
	///获取所有数据
	public results getall(Object target,String cmd){
		try{
			Connection conn=null;
			ResultSet rs=null;
			Class.forName("com.mysql.jdbc.Driver");
			conn=DriverManager.getConnection(dburls,dbuser,dbpwd);
			PreparedStatement ps = conn.prepareStatement(cmd);
			rs=ps.executeQuery();
			results myresult=new results();
			myresult.success=true;
			myresult.message="[";
			///反射获取对象的属性
			Class temp=target.getClass();
			java.lang.reflect.Field[] fields=temp.getDeclaredFields();
			while(rs.next())
			{
				///json数据拼装
				myresult.message=myresult.message+"{";
				for(int i=0;i<fields.length;i++)
				{
					myresult.message=myresult.message+"'"+fields[i].getName()+"'";
					myresult.message=myresult.message+":'"+rs.getString(fields[i].getName())+"'";
				}
				myresult.message=myresult.message+"}";
				if(!rs.isLast())
					myresult.message=myresult.message+",";
			}
			myresult.message=myresult.message+"]";
			return myresult;
		}
		catch(Exception e){
			System.out.println(e.getMessage());
			return new results(false,e.getMessage());
		}
	}
}