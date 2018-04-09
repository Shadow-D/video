package toolsOfHFUT;

import java.awt.Color;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.border.LineBorder;

import org.apache.http.NameValuePair;
import org.apache.http.NoHttpResponseException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;


public class VideoLessons extends JFrame implements ActionListener {
    private static final long serialVersionUID = 1L;
	public static CloseableHttpResponse response;
	public static String baseUrl="http://hfutspk.hfut.edu.cn/";
    public static ArrayList<NameValuePair> postData;// 存儲post參數
    public static Map<String, String> headers=new HashMap<>();// 存儲header參數
    public static CloseableHttpClient request;
    public static JPanel mainPanel;
	public static JLabel userLable, passwordLable;// 提示標籤
	public static JTextField userText;// 賬號輸入框
	public static JPasswordField passwordText;//密碼輸入框
	public static JButton okButton;// 確定按鈕
	public static JLabel showLable;
	public static JLabel[] showLables;
    static {
    	headers.put("User-Agent", "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/65.0.3325.181 Safari/53");
	    headers.put("Content-Type", "application/x-www-form-urlencoded");
	    headers.put("X-Requested-With", "XMLHttpRequest");
	    headers.put("Connection", "keep-alive");
	    headers.put("Accept", "*/*");
	    request=HttpClients.custom().setRetryHandler(new HttpRequestRetryHandler() {
			@Override
			public boolean retryRequest(IOException arg0, int arg1, HttpContext arg2) {
				// TODO Auto-generated method stub
				if (arg1 > 5) {
					return false;
				} 
				if (arg0 instanceof ConnectTimeoutException||arg0 instanceof NoHttpResponseException) { 
					return true; 
				} 
				return false;
			}
		}).build();
    }
    
	public static ArrayList<String> login(String user, String password) throws ClientProtocolException, IOException, InterruptedException {
		Pattern lessonPattern=Pattern.compile("student/teachingTask/coursehomepage\\.do\\?[0-9]{13}&courseId=[0-9]+");// 課程詳情頁
		Pattern playPattern=Pattern.compile("student/video/manageVideo\\.do\\?[0-9]{13}&method=index&courseId=[0-9]+");// 課程播放頁
		ArrayList<String> playUrl=new ArrayList<>();
		// 獲取驗證碼
		response=request.execute(new HttpGet(baseUrl+"getRandomImage.do?"+new Date().toString().replaceAll(" ", "%20")));
		String captcha=Captcha.FindString(response.getEntity().getContent());
		// 登錄
		postData=new ArrayList<>();
		postData.add(new BasicNameValuePair("logname", user));
		postData.add(new BasicNameValuePair("password", password));
		postData.add(new BasicNameValuePair("randomCode", captcha));
		HttpPost loginPost=new HttpPost(baseUrl+"login.do?");
		loginPost.setEntity(new UrlEncodedFormEntity(postData));
		response=request.execute(loginPost);
		// 登錄成功返回302
		if (response.getStatusLine().getStatusCode()==302) {
			// 打開主頁
			response=request.execute(new HttpGet(response.getFirstHeader("location").getValue()));
			Matcher lessonMatcher=lessonPattern.matcher(EntityUtils.toString(response.getEntity()));
			// 存儲所有課程地址
			while (lessonMatcher.find()) {
				response=request.execute(new HttpGet(baseUrl+lessonMatcher.group()));
				Matcher playMatcher=playPattern.matcher(EntityUtils.toString(response.getEntity()));
				if (playMatcher.find()) {
					boolean flag=false;
					for (String url:playUrl) {
						if (url.split("&")[2].equals(playMatcher.group(0).split("&")[2])) {
							flag=true;
							break;
						}
					}
					if (flag==false) {
						playUrl.add(playMatcher.group(0));
					}
				}
			}
		} 
		return playUrl;
	}
	
	public static void autoChat(String playUrl, int turn) throws ClientProtocolException, IOException {
		Pattern chatPattern=Pattern.compile("student/bbs/index\\.do\\?[0-9]{13}&teachingTaskId=[0-9]+");// 討論頁面
		Pattern titlePattern=Pattern.compile("student/bbs/manageDiscuss\\.do\\?[0-9]{13}&method=view?&teachingTaskId=[0-9]+&discussId=[0-9]+&isModerator=[a-z]+&isClick=[a-z]+&forumId=[0-9]+");// 話題頁面url
		Pattern examplePattern=Pattern.compile("<td\\swidth=\"100%\">[\\s\\S]+?</td>");// 其他人的回覆
		Pattern replyPattern=Pattern.compile("student/bbs/manageDiscuss\\.do\\?[0-9]+&method=reply");// 回覆操作
		showLables[turn].removeAll();;
		showLables[turn].setText("*autoChat start...");;
		showLables[turn].updateUI();
		// 打開視頻播放頁面
		response=request.execute(new HttpGet(baseUrl+playUrl));
		String result=EntityUtils.toString(response.getEntity());
		// 找到討論頁面網址
		Matcher chatMatcher=chatPattern.matcher(result);
		String chatUrl=null;
		if (chatMatcher.find()) {
			chatUrl=chatMatcher.group(0);
		} 
		// 打開討論頁面
		response=request.execute(new HttpGet(baseUrl+chatUrl));
		result=EntityUtils.toString(response.getEntity());
		// 獲取話題頁面網址
		Matcher titleMatcher=titlePattern.matcher(result);
		ArrayList<String> topicUrls=new ArrayList<>();
		while (titleMatcher.find()) {
			topicUrls.add(titleMatcher.group());
		}
		// 依次選擇兩個有評論的回覆
		int count=0;
		for (String topicUrl:topicUrls) {
			// 打開話題頁面
			response=request.execute(new HttpGet(baseUrl+topicUrl));
			result=EntityUtils.toString(response.getEntity());
			// 判斷是否有回覆
			Matcher exampleMatcher=examplePattern.matcher(result);
			Matcher replyMatcher=replyPattern.matcher(result);
			if (exampleMatcher.find()&&replyMatcher.find()) {
				// 複製第一條評論
				String comment=exampleMatcher.group(0).replaceAll("\\s", "").replaceAll("<[a-z0-9/=\"%]+?>", "");
				String[] parameter=topicUrl.split("&");
				// 回覆參數
				postData=new ArrayList<>();
				postData.add(new BasicNameValuePair("discussId", parameter[3].split("=")[1]));
				postData.add(new BasicNameValuePair("forumId", parameter[6].split("=")[1]));
				postData.add(new BasicNameValuePair("type", "1"));
				postData.add(new BasicNameValuePair("isModerator", parameter[4].split("=")[1]));
				postData.add(new BasicNameValuePair("teachingTaskId", parameter[2].split("=")[1]));
				postData.add(new BasicNameValuePair("content", comment));
				// 有中文,注意編碼問題
				StringEntity postEntity=new UrlEncodedFormEntity(postData, "UTF-8");
				// 參與討論
				HttpPost replyPost=new HttpPost(baseUrl+replyMatcher.group());
				replyPost.addHeader("User-Agent", headers.get("User-Agent"));
				replyPost.addHeader("Content-Type", headers.get("Content-Type"));
				replyPost.addHeader("Accept", headers.get("Accept"));
				replyPost.setEntity(postEntity);
				response=request.execute(replyPost);
				// 回覆成功返回302
				if (response.getStatusLine().getStatusCode()==302) {
					System.out.println(comment);
					showLables[turn].removeAll();
					showLables[turn].setText("<html><body>--------------------<br/>*status: success<br/><body/><html/>");
					showLables[turn].updateUI();
					// 討論次數
					count++;
				}
			}
			// 只討論兩次
			if (count==2) {
				return;
			}
		}
	}
	
	public static void saveTime(String playUrl, int left, int turn) throws ClientProtocolException, IOException, InterruptedException {
		Pattern savePattern=Pattern.compile("student/savePlayTime\\.do\\?[0-9]{13}&method=savePlayTime");// 保存進度url
		Pattern parameterPattern=Pattern.compile("teachingTaskId=[0-9]+&videoVisitId=[0-9]+&videoLength=[0-9]+&playTime=");// 保存進度post參數
	    Pattern playPattern=Pattern.compile("currentTime\\s<\\s[0-9]+");// 視頻總秒數
	    Pattern lastPattern=Pattern.compile("currentTime\\s=\\s[0-9]+");// 當前播放秒數
	    Pattern countPattern=Pattern.compile("href='\\s+javascript:jwplayer\\(\"player-container\"\\)\\.pause\\(\\);alert\\(\"请按顺序学习\"\\);\\s+'>");// 用於計數未播放視頻
	    showLables[turn].removeAll();
		showLables[turn].setText("<html><body>*video "+left+" start...<body/><html/>");
		showLables[turn].updateUI();
	    // 打開視頻播放頁面
	    HttpGet playGet=new HttpGet(baseUrl+playUrl);
		response=request.execute(playGet);
		String result=EntityUtils.toString(response.getEntity());
		Matcher countMatcher=countPattern.matcher(result);
		// 剩餘視頻個數,默認爲一
		int count=1;
		while (countMatcher.find()) {
			count++;
		}
		// 依次播放視頻
		for (int i=0;i<count;i++) {
			// 重新打開播放頁面
			response=request.execute(playGet);
			result=EntityUtils.toString(response.getEntity());
			// 保存請求參數,視頻時長,當前播放時間,剩餘視頻數
			Matcher parameterMatcher=parameterPattern.matcher(result);
			Matcher playMatcher=playPattern.matcher(result);
			Matcher lastMatcher=lastPattern.matcher(result);
			// 每次更換視頻時改變
			String playTime=null, lastTime=null;
			String[] playData=null;
			if (parameterMatcher.find()&&playMatcher.find()&&lastMatcher.find()) {
				playData=parameterMatcher.group(0).split("&");
				playTime=playMatcher.group(0).split("<")[1].trim();
				lastTime=lastMatcher.group(0).split("=")[1].trim();
			} 
			// 狀態值
			String status="";
			// 剩餘時長
			String leftTime=String.valueOf(Integer.parseInt(playTime)-Integer.parseInt(lastTime));
			// 開始時間
			long startDate=new Date().getTime();
			// 當返回值爲complete時表示播放完成
			while (!status.equals("complete")) {
				String saveUrl=null;
				// 重新打開播放頁面
				response=request.execute(playGet);
				result=EntityUtils.toString(response.getEntity());
				// 新的保存地址
				Matcher saveMatcher=savePattern.matcher(result);
				if (saveMatcher.find()) {
					saveUrl=saveMatcher.group(0);
				} 
				HttpPost savePost=new HttpPost(baseUrl+saveUrl);
				savePost.addHeader("User-Agent", headers.get("User-Agent"));
				savePost.addHeader("Content-Type", headers.get("Content-Type"));
				savePost.addHeader("X-Requested-With", headers.get("X-Requested-With"));
				savePost.addHeader("Connection", headers.get("Connection"));
				savePost.addHeader("Accept", headers.get("Accept"));
				postData=new ArrayList<>();
				postData.add(new BasicNameValuePair("teachingTaskId", playData[0].split("=")[1]));
				postData.add(new BasicNameValuePair("videoVisitId", playData[1].split("=")[1]));
				postData.add(new BasicNameValuePair("videoLength", playData[2].split("=")[1]));
				showLables[turn].removeAll();
				String seconds;
				// 間隔大於240s,直接加
				if (Integer.parseInt(lastTime)<Integer.parseInt(playTime)-240) {
					// 保存請求
					lastTime=String.valueOf(Integer.parseInt(lastTime)+240);
					postData.add(new BasicNameValuePair("playTime", lastTime));
					savePost.setEntity(new UrlEncodedFormEntity(postData));
					response=request.execute(savePost);
					status=EntityUtils.toString(response.getEntity());
					seconds=String.valueOf(Long.parseLong(leftTime)-(new Date().getTime()-startDate)/(1000));
					showLables[turn].setText("<html><body>--------------------<br/>*course: "+left+"<br/>*videos: "+(count-i)+"<br/>*seconds: "+seconds+"s<br/>*status: "+status+"<br/><body/><html/>");
				} else {
					lastTime=String.valueOf(Integer.parseInt(playTime)-1);
					postData.add(new BasicNameValuePair("playTime", lastTime));
					savePost.setEntity(new UrlEncodedFormEntity(postData));
					response=request.execute(savePost);
					status=EntityUtils.toString(response.getEntity());
					seconds=String.valueOf(Long.parseLong(leftTime)-(new Date().getTime()-startDate)/(1000));
					showLables[turn].setText("<html><body>--------------------<br/>*course: "+left+"<br/>*videos: "+(count-i)+"<br/>*seconds: "+seconds+"s<br/>*status: "+status+"<br/><body/><html/>");
					// 每次返回invalid時當前視頻播放進度都會清零,故需從0開始
					lastTime="0";
				}
				showLables[turn].updateUI();
				Thread.sleep(3000);
			}
			showLables[turn].removeAll();
			showLables[turn].setText("<html><body>*****************<br/>*complete,next video...<br/>*****************<br/><body/><html/>");
			showLables[turn].updateUI();
		}
	}
	
	public VideoLessons() {
		// TODO Auto-generated constructor stub
		this.setTitle("登录");
		this.setBounds(500, 300, 240, 180);
		this.setLayout(null);
		this.setResizable(false);
		this.setDefaultCloseOperation(EXIT_ON_CLOSE);
		
		mainPanel=new JPanel(null);
		mainPanel.setBounds(0, 0, 240, 180);
		
		userLable=new JLabel("账号:");
		userLable.setFont(new Font("黑体", 1, 20));
		userLable.setBounds(10, 10, 60, 40);
		mainPanel.add(userLable);
		
		userText=new JTextField("", 10);
		userText.setFont(new Font("黑体", 1, 20));
		userText.setBounds(90, 15, 140, 30);
		mainPanel.add(userText);
		
		passwordLable=new JLabel("密码:");
		passwordLable.setFont(new Font("黑体", 1, 20));
		passwordLable.setBounds(10, 50, 60, 40);
		mainPanel.add(passwordLable);
		
		passwordText=new JPasswordField("", 15);
		passwordText.setFont(new Font("黑体", 1, 20));
		passwordText.setBounds(90, 55, 140, 30);
		mainPanel.add(passwordText);
		
		okButton=new JButton("登录");
		okButton.setFont(new Font("黑体", 1, 20));
		okButton.setBounds(80, 100, 80, 30);
		okButton.addActionListener(this);
		mainPanel.add(okButton);
		
		this.add(mainPanel);
	}
	@Override
	public void actionPerformed(ActionEvent e) {
		// TODO Auto-generated method stub
		if (e.getSource()==okButton) {
			String user=userText.getText();
			String password=String.valueOf(passwordText.getPassword());
			boolean isNotEmpty=user!=null&&password!=null&&user.length()>0&&password.length()>0;// 判斷是否爲空
			if (isNotEmpty) {
				try {
					// 判斷是否能登陸
					ArrayList<String> playUrl=login(user, password);
					if (playUrl!=null&&playUrl.size()>0) {
						// 判斷開幾個小塊
						int cols=1, rows=1, count=playUrl.size();
						if (count>1) {
							cols=2;
						}
						if (count%2==0) {
							rows=count/2;
						} else {
							rows=count/2+1;
						}
						this.dispose();
						this.setTitle("課程進度");
						this.setBounds(500, 300, 270*cols, 30+150*rows);
						mainPanel.removeAll();
						mainPanel.setBounds(0, 0, 270*cols, 30+150*rows);
						this.setVisible(true);
						showLables=new JLabel[playUrl.size()];
						for (int i=0;i<playUrl.size();i++) {
						    showLable=new JLabel("");
						    showLable.setHorizontalAlignment(SwingConstants.LEFT);
							showLable.setBounds(270*(i%2), 150*(i/2), 270, 150);
							showLable.setFont(new Font("黑体", 1, 20));
							showLable.setBorder(new LineBorder(Color.black));
							mainPanel.add(showLable);
							showLables[i]=showLable;
							new playVideo(playUrl.get(i), i+1, i).start();
						}
					} else {
						new JOptionPane();
						JOptionPane.showMessageDialog(null, "登錄失敗或公選課爲空", "提示", JOptionPane.ERROR_MESSAGE);
					}
				} catch (Exception e2) {
					// TODO: handle exception
					e2.printStackTrace();
				}
			} else {// 提示錯誤
				new JOptionPane();
				JOptionPane.showMessageDialog(null, "用户名或密码为空", "提示", JOptionPane.ERROR_MESSAGE);	
			}
		}
	}
	public static class playVideo extends Thread {
		String playUrl;
		int left, turn;
		public playVideo(String playUrl, int left, int turn) {
			this.playUrl=playUrl;
			this.left=left;
			this.turn=turn;
		}
		@Override
		public void run() {
			// TODO Auto-generated method stub
			try {
				saveTime(playUrl, left, turn);
				autoChat(playUrl, turn);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} 
		}
	}
	public static void main(String[] args) {
		// TODO Auto-generated method stub
			VideoLessons vl=new VideoLessons();
			vl.setVisible(true);
	}
}
