# CameraSample
Android Camera for Camera 2 API


# Crawler 
>可爬微博, 人人網, QQ空間, 網易博客和各大新聞媒體 

### INI file format
>設定檔的範例
>[DEFAULT]裡的設定值放一般爬蟲會用到ＩＰ和　ＤＢＮＡＭＥ
>[各網站名稱]裡設定值放collection，collection . input和output等，另外還有各個爬蟲所需的設定

example:		
	[DEFAULT]
	ip = 192.168.1.3
	dbname = Test

	[WEIBO]
	collection = Weibo
	collectioncomment = WeiboComment 
	type = 1 
	input = /path/file
	output = /path/file

	[...]
	key = value

### key and value in INI file
>說明每個key和每個value

	type = 1 -- 利用關鍵字去搜尋結果
	type = 2 -- 直接用url去爬

	input -- 主要是放關鍵字或url，每個file 只能放一種格式(url or keyword)
	output -- 只能放JSON格式的檔案

### Input file
>一個input file裡面每一行只放一個關鍵字或url

example
##### 1. TYPE = 1:
	康師傅
	方便麵
	統一
	綠茶
##### 2. TYPE = 2:
	http://www.renren.com
	http://i.qq.com/
	http://login.sina.com.cn/

### Output file
>一個output file 裡面每一行只放一個JSON，暫時不放JSON ARRAY
>評論放另外一個output file

example:
	{
		publisher : "Test", 
		accout : "http://192.168.1.2/test/", 
		url : "http://192.168.1.2/test/12345678", 
		publishDate : 2016-08-15 10:49:00(Date format), 
		content : "testetstetstetstesttstettststsrtdrtsftsdftrstfdrs",
		likeCount : 123,
		commentCount : 123
		.
		.
		.
		.
	}

### Read config and Write config
Usage:
>python

```python
from com.hgdata.crawler.utility import config

if __name__ == '__main__':
	test_dist = {'DBName':'Test', 'IP': '192.168.1.3'}
	test_weibo = {'Collection': 'Weibo', 'type': 1, 'input':'/path/inputfile', 'output':'/path/outputfile' }
	
	crawler_config.write_section('DEFAULT', test_dist)
	crawler_config.write_section('WEIBO', test_weibo)
	print(crawler_config.read_section('WEIBO'))
	for key in crawler_config.read_section('WEIBO'):
		print(key, crawler_config.read_section('WEIBO')[key] )
```

### Read input file of config and Write output file of config
>utiliy.common 的 read_config_input and write_config_output 用法

>read function 會回傳一個list，list裡的item是一行的的內容

>write funciton 要放list進去

Usaga:

```python
from com.hgdata.crawler.utility import common

if __name__ == '__main__':
	content = common.read_config_input('config.ini')
	print(content)

	content = ['A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J']
	common.write_config_output('JsonData', content)
```

read_config_input　output:
	['[DEFAULT]', 'dbname = Test', 'ip = 192.168.1.3', '', '[WEIBO]', 'collection = Weibo', 'type = 1', 'input = /path/inputfile', 'output = /path/outputfile', '']

### Schedule
暫時用別人寫的python套件跑schedule，因為linux的crontab 無法跑爬蟲，會持續找替代方案
這個排程可以設定分，時，天和星期，無法做月的排程，另外只有當Crawler跑完，才會執行schedule的設定

Usage:

```python
import schedule
import time

def Crawler():
	date_str = time.strftime('%Y-%m-%d %H:%M:%S')
	print("I'm working...", date_str)


if __name__ == '__main__':
	print('start test_schedule')
	#schedule.every(10).minutes.do(Crawler)
	schedule.every().hour.do(Crawler)
	#schedule.every().day.at("10:30").do(Crawler)
	#schedule.every().monday.do(job)
	#schedule.every().wednesday.at("13:15").do(Crawler)

	while True:
		schedule.run_pending()
		time.sleep(60)
	
	print('finish test_schedule')
```