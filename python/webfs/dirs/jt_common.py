#!/usr/bin/env python
# --*-- coding:utf-8 --*--
import os
import jt_log
import httplib,urllib
import socket
import jt_buffer
import jt_machine_list
import random
import jt_global as GLOBAL
import traceback
import time

try:
	import cPickle as pickle
except ImportError:
	import pickle

#重名名/home/asura/python + aws = /home/asura/aws
def rename(path,name):
	dirs=path.split(os.sep)
	if name!="" and len(dirs)>1:
		temp_path=dirs[0:-1]
		new_name=os.sep.join(temp_path)+os.sep+name
	else:
		new_name=name
	print new_name

#
def dictToString(mdict):
	res=""
	for k in mdict:
		v=mdict[k]
		if isinstance(v,str) or isinstance(v,int) or isinstance(v,float):
			res=res+str(k)+":"+str(v)+";"
		else:
			res=res+str(k)+":unknow;"
	return res

#名字解析
def pathFilter(path):
	dirs=path.split(os.sep)
	res={"path":"","file":"","filename":"","type":""}
	if len(dirs)>0:
		if len(dirs)==1:
			files=dirs[0].split('.')
			if len(files)==2:
				res['file']=dirs[0]
				res['filename']=files[0]
				res['type']=files[1]
		else:
			res['path']=os.sep.join(dirs[0:-1])
			res['file']=dirs[-1]
			files=dirs[-1].split('.')
			if len(files)==2:
				res['filename']=files[0]
				res['type']=files[1]
	return res

#将名字切分
def pathSplit(path):
	names=path.split(os.sep)
	name=""
	name_left=""
	index=0
	for item in names:
		index=index+1
		if item!="":
			name=item
			break
	name_left=os.sep.join(names[index:])
	return {"name":name,"name_left":name_left}

#解析命令
def cmds(path):
	res={}
	alls=path.split("?")
	if len(alls)>=1:
		res['path']=alls[0]
	if len(alls)>1:
		left_params=alls[1].split("&")
		for item in left_params:
			temp=item.split("=")
			if len(temp)==2:
				if temp[0] in res:
					jt_log.write("error","repeate key in url request")
					continue
				else:
					res[temp[0]]=temp[1]
	return res

#获取随机的名字
def getRandomName(count=8):
	origin_str="abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ"
	length=len(origin_str)
	res=""
	i=0
	while i < count:
		index=random.randint(0,length-1)
		res=res+origin_str[index]
		i=i+1
	return res

#处理文件名
def processFilename(name):
	names=name.split(os.sep)
	name_array=[]
	for item in names:
		if item!="":
			name_array.append(item)	
	name=os.sep.join(name_array)
	name=name.replace(os.sep,"_")
	return name

#存储文件
def storeFile(name,files):
	try:
		name=processFilename(name)
		f=open(GLOBAL.file_location+os.sep+name,"wb")
		f.write(files)
		f.close()
		return True
	except Exception,e:
		traceback.print_exc()
		jt_log.log.write(GLOBAL.error_log_path,"存储失败:"+e.message)
		return False

#获取文件
def getFile(name):
	try:
		name=processFilename(name)
		f=open(GLOBAL.file_location+os.sep+name,"rb")
		res=f.read()
		f.close()
		return res
	except Exception,e:
		traceback.print_exc()
		jt_log.log.write(GLOBAL.error_log_path,"读取文件失败:"+e.message)
		return False	

#获取文件存储的最佳位置		
def getFileBestMC(mac=[]):
	return GLOBAL.MacList.getBestMC()

#获取目录存储的最佳位置
def getDirBestMC(mac=[]):
	return GLOBAL.MacList.getBestMC()	

#调用GET方法
def get(machine,dirs,params):
	try:
		#解析命令参数
		cmds=urllib.urlencode(params)
		conn=httplib.HTTPConnection(machine.getAddress(),machine.getPort(),GLOBAL.time_out)
		conn.request("GET",dirs+"?"+cmds)
		res=conn.getresponse()
		if res.status==200 and res.reason=="OK":
			result=res.read()
			conn.close()
			return result
		else:
			conn.close()
			jt_log.log.write("log/data/error.log","jt_common,get"+res.read())
			return False
	except Exception,e:
		conn.close()
		traceback.print_exc()
		jt_log.log.write(GLOBAL.error_log_path,"get error"+e.message)

#调用POST方法
def post(machine_list,dirs,params,index=0):
	machine_list=removeSame(machine_list)
	try:
		#已经尝试了所有的地址都发送失败了
		if index>=len(machine_list):
			return False
		machine=machine_list[index]
	except Exception,e:
		traceback.print_exc()
		jt_log.log.write(GLOBAL.error_log_path,"post error 所有地址尝试发送均失败")
		return False

	try:
		#params=urllib.urlencode(params)
		if not checkMacExist(machine):
			return post(machine_list,dirs,params,index+1)	
		#print "post address:",machine.getAddress(),"port:",machine.getPort()
		address=str(machine.getAddress())+str(machine.getPort())
		headers={"Content-type":"application/x-www-form-urlencoded","Accept":"text/plain"}
		conn=httplib.HTTPConnection(machine.getAddress(),machine.getPort(),GLOBAL.time_out)
		temp_params=pickle.dumps(params)
		#直接取缓存数据
		buffers=jt_buffer.bufferPost(address,temp_params)
		if buffers:
			return buffers

		conn.request("POST",dirs,temp_params,headers)
		res=conn.getresponse()
		max_age=res.getheader("Cache-control",['max-age'])
		max_age=max_age.split("=")[1]
		if res.status==200 and res.reason=='OK':
			result=pickle.loads(res.read())
			if int(max_age)>0:
				jt_buffer.StoreBufferPost(address,temp_params,result,max_age)
			conn.close()
			return result
		else:
			conn.close()
			jt_log.log.write("log/data/error.log","jt_common,post"+res.read())
			return False
	except Exception,e:
		traceback.print_exc()
		#conn.close()
		removeRemoteMac(machine)
		jt_log.log.write(GLOBAL.error_log_path,"post 发送失败"+e.message)
		return post(machine_list,dirs,params,index+1)

def removeSame(machine_list):
	res=[]
	for item in machine_list:
		signal=0
		for p in res:
			if p.getPort()==item.getPort() and p.getAddress()==item.getAddress():
				signal=1
		if signal==0:
			res.append(item)
	return res	
		
#发送给所有的机器信息
def sendToAll(dirs,params):
	all_mac=GLOBAL.remote_mac
	for item in all_mac:
		temp_mac=jt_machine_list.machine("",item["addr"],item["port"],"")
		post([temp_mac],dirs,params)

#删除无效的地址
def removeRemoteMac(mac):
	for index in range(0,len(GLOBAL.remote_mac)):
		if str(GLOBAL.remote_mac[index]['addr'])==str(mac.getAddress()) and str(GLOBAL.remote_mac[index]['port'])==str(mac.getPort()):
			GLOBAL.remote_mac.pop(index)
			break	

#判断机器是否存在
def checkMacExist(mac):
	for item in GLOBAL.remote_mac:
		if str(item['addr'])==str(mac.getAddress()) and str(item['port'])==str(mac.getPort()):
			return True
	return False

#保存镜像文件
def saveGhost():
	try:
		f=open("ghost","wb")
		data={"data":GLOBAL.LocalData,"mac":GLOBAL.MacList}
		pickle.dump(data,f)
		f.close()
	except Exception,e:
		jt_log.log.write(GLOBAL.error_log_path,"镜像文件保存失败")

#加载镜像文件
def loadGhost():
	try:
		f=open("ghost","rb")
		data=pickle.load(f)
		GLOBAL.LocalData=data['data']
		GLOBAL.MacList=data['mac']
		f.close()
	except Exception,e:
		jt_log.log.write(GLOBAL.error_log_path,"镜像文件加载失败")

if __name__=="__main__":
	rename("a.txt","b.txt")
	a=cmds("/home/asura/xielixiang/a.txt?cmd=rmdir&a=b&c=d&e=f")
	#print a
	b=pathFilter("txt")
	#print b
	mac=jt_machine_list.machine("test","127.0.0.1",8802,{'a':'b'})
	#print get(mac,'/index.html',{"cmd":"ls"})
	#print post(mac,"/index.html",{"cmd":"ls"},{"a":"b","p":3,"d":'ebe'})
	print getRandomName()
	print getRandomName()
	print pathSplit("/home/asura/xielixiang")
	print pathSplit("/home")
	print pathSplit("/home/")
	print pathSplit("home")
	print pathSplit("/home/asura")
	print pathSplit("/home/asura/")
