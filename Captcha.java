package toolsOfHFUT;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import javax.imageio.ImageIO;

public class Captcha {
	// 轉爲黑白圖
	public static void changeImage(BufferedImage img) throws FileNotFoundException, IOException {
		int width=img.getWidth();
		int height=img.getHeight();
		long count=0;
		// 總像素RGB值
		for (int i=0;i<width;i++) {
			for (int j=0;j<height;j++) {
				int rgb=img.getRGB(i, j);
				Color color=new Color(rgb);
				count+=color.getBlue()+color.getGreen()+color.getRed();
			}
		}
		// 平均值
		count=(long)(1.1*(count/(width*height)));
		// 大於爲白,小於爲黑
		for (int i=0;i<width;i++) {
			for (int j=0;j<height;j++) {
				if ((isWhite(img.getRGB(i, j),count)==1)) {
					img.setRGB(i, j, Color.white.getRGB());
				} else {
					img.setRGB(i, j, Color.black.getRGB());
				}
			}
		}
	}
	// 粗略切割
	public static BufferedImage[] cutImages(BufferedImage bfi) throws FileNotFoundException, IOException {
		BufferedImage[] img=new BufferedImage[4];
		img[0]=bfi.getSubimage(0, 4, 20, 15);
		img[1]=bfi.getSubimage(15, 2, 20, 15);
		img[2]=bfi.getSubimage(30, 5, 20, 15);
		img[3]=bfi.getSubimage(45, 3, 20, 15);
	    return img;
	}
	// 二次切割
	public static BufferedImage reCutImage(BufferedImage img) throws FileNotFoundException ,IOException {
		int width=img.getWidth();
		int height =img.getHeight();
		int[] index=new int[width];
		int indexRight=2;
		// 一列
		for (int i=0;i<width;i++) {
			int count=0;
			// 有幾個黑色像素
			for (int j=0;j<height;j++) {
				if (isWhite(img.getRGB(i, j))==-1) {
					count++;
			    }
		    }
			// 大於二則視爲字符邊界
			if (count>=2) {
				index[i]=1;
			} else {
				index[i]=0;
			}
		}
		// 切割
		for (int i=1;i<10;i++) {
			if (index[i]==1&&index[i+1]==1&&index[i-1]==0) {
				indexRight=i;
				break;
			}
		}
		return img.getSubimage(indexRight-1, 0, 12, 15);
	}
	// 去噪點
	public static void removeSpot(BufferedImage img) throws FileNotFoundException, IOException {
		// 左邊界
		for (int i=1;i<img.getHeight()-1;i++) {
			// 找到黑點
			if (img.getRGB(0, i)!=-1) {
				// 判斷周圍有幾個黑點
				int count=isWhite(img.getRGB(0, i-1))+isWhite(img.getRGB(0, i+1))+isWhite(img.getRGB(1, i));
				// 小於兩個則刪除該點
				if(count>1) {
					img.setRGB(0, i, Color.white.getRGB());
				}
			}
		}
		// 除邊界外的地方
		for (int i=1;i<img.getHeight()-1;i++) {
			for (int j=1;j<img.getWidth()-1;j++) {
				if (img.getRGB(j, i)!=-1) {
					int count=isWhite(img.getRGB(j+1, i))+isWhite(img.getRGB(j-1, i))+isWhite(img.getRGB(j, i+1))+isWhite(img.getRGB(j, i-1));
				    if (count>=2) {
				    	img.setRGB(j, i, Color.white.getRGB());
				    }
				}
			}
		}
	}
	// 根据平均值判断颜色
	public static int isWhite(int colorInt,long count) {
		Color color=new Color(colorInt);
		if ((color.getRed()+color.getBlue()+color.getGreen())>count) {
			return 1;
		} else {
			return -1;
		}
	}
	// 默认值，用于预处理后的黑白图片
	public static int isWhite(int colorInt) {
		Color color=new Color(colorInt);
		if ((color.getRed()+color.getBlue()+color.getGreen())>300) {
			return 1;
		} else {
			return -1;
		}
	}
	// 将图片和模板对比，获得字符
	public static String compareImage(BufferedImage image) throws FileNotFoundException, IOException {
		File patternFile=new File(Captcha.class.getResource("/pattern").getPath());// 模板图片目录
		
		int width=image.getWidth();
		int height=image.getHeight();
		String str="";
		int count=0;
		for (File f2:patternFile.listFiles()) {// 遍历模板文件夹，与所有图片对比
			if (f2.isDirectory()) {
				for (File f3:f2.listFiles()) {
					BufferedImage bfi=ImageIO.read(new FileInputStream(f3));
					int flag=0;// 相似度
					for (int i=0;i<width;i++) {
				    	for (int j=0;j<height;j++) {
				    		// 只比较黑色像素
				    		boolean flag1=isWhite(image.getRGB(i, j))==-1||isWhite(bfi.getRGB(i, j))==-1;
				    		// 颜色是否相同
				    		boolean flag2=isWhite(bfi.getRGB(i, j))==isWhite(image.getRGB(i, j));
				    		if (flag1&&flag2) {// 相同加分，不同减分
				    			flag+=2;
				    		} else if (flag1&&!flag2) {
				    			flag+=-1;
				    		}
				    	}
				    }
					if (flag>count) {// 存储当前相似度最高的图片的信息
						count=flag;
						str=f3.getParent().substring(f3.getParent().length()-1);
					}
				}
			}
		}
		return str;
	}
	// 综合处理
	public static BufferedImage[] unionCut(BufferedImage image) throws FileNotFoundException, IOException {
		BufferedImage[] img;
		changeImage(image);
		removeSpot(image);
		removeSpot(image);
		img=cutImages(image);
		for (int i=0;i<4;i++) {
			img[i]=reCutImage(img[i]);
		}
		return img;
	}
	// 识别字符串
	public static String FindString(InputStream input) throws FileNotFoundException, IOException {
		BufferedImage img=ImageIO.read(input);
		StringBuilder string=new StringBuilder();
		BufferedImage[] img2=unionCut(img);
		for (int i=0;i<4;i++) {
			string.append(compareImage(img2[i]));
		}
		return string.toString();// 返回识别结果
	}
}
