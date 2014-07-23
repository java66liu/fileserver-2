package def.servlet;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadBase.SizeLimitExceededException;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.log4j.Logger;

public class FileServlet extends HttpServlet {

	private Logger log = Logger.getLogger(this.getClass());
	
	private String uploadPath = null;
	private String CONFIG_FILE_PATH="/fileupload.properties";
	final long MAX_SIZE = 10 * 1024 * 1024;// 设置上传文件最大为 10M
	// 允许上传的文件格式的列表
	final String[] allowedExt = new String[] { "jpg", "jpeg", "gif", "txt", "doc", "docx", "mp3", "wma", "m4a" };
	private DiskFileItemFactory dfif;
	private ServletFileUpload sfu;
	private SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HH_mm_ss_SSS");
	
	public FileServlet() {
		super();
	}
	
	public void init() throws ServletException {
		Properties p = getProperties();
		uploadPath = (String) p.get("file.upload.path");
		if(uploadPath == null) {
			uploadPath = System.getProperty("user.home");
			log.warn("没有配置上传路径，将采用默认路径:" + uploadPath);
		}
		log.debug("文件上传后保存路径:" + uploadPath);
		// 实例化一个硬盘文件工厂,用来配置上传组件ServletFileUpload
		dfif = new DiskFileItemFactory();
		dfif.setSizeThreshold(1024 * 5 * 1024);// 设置上传文件时用于临时存放文件的内存大小,这里是4K.多于的部分将临时存在硬盘
		dfif.setRepository(new File(uploadPath));// 设置存放临时文件的目录
		
		// 用以上工厂实例化上传组件
		sfu = new ServletFileUpload(dfif);
		// 设置最大上传尺寸
		sfu.setSizeMax(MAX_SIZE);
		log.info("初始化FileServlet完毕");
	}

	public void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		doPost(request, response);
	}

	public void doPost(HttpServletRequest req, HttpServletResponse res)
			throws ServletException, IOException {
		log.debug("收到上传请求！");
		if(!ServletFileUpload.isMultipartContent(req)) {
			log.debug("不是文件上传请求，返回");
			return ;
		}
		
		res.setContentType("text/html");
		// 设置字符编码为UTF-8, 这样支持汉字显示
		res.setCharacterEncoding("UTF-8");

		PrintWriter out = res.getWriter();
		// 从request得到 所有 上传域的列表
		List fileList = null;
		try {
			fileList = sfu.parseRequest(req);
		} catch (FileUploadException e) {// 处理文件尺寸过大异常
			if (e instanceof SizeLimitExceededException) {
				log.warn("文件尺寸超过规定大小:" + MAX_SIZE + "字节");
				return;
			}
			log.debug("解析文件时出错", e);
		}
		// 没有文件上传
		if (fileList == null || fileList.size() == 0) {
			log.debug("no files upload!");
			return;
		}
		// 得到所有上传的文件
		Iterator fileItr = fileList.iterator();
		// 循环处理所有文件
		while (fileItr.hasNext()) {
			FileItem fileItem = null;
			String path = null;
			long size = 0;
			// 得到当前文件
			fileItem = (FileItem) fileItr.next();
			// 忽略简单form字段而不是上传域的文件域(<input type="text" />等)
			if (fileItem == null || fileItem.isFormField()) {
				continue;
			}
			// 得到文件的完整路径
			path = fileItem.getName();
			// 得到文件的大小
			size = fileItem.getSize();
			if ("".equals(path) || size == 0) {
				return;
			}
			// 得到去除路径的文件名
			String t_name = path.substring(path.lastIndexOf("\\") + 1);
			// 得到文件的扩展名(无扩展名时将得到全名)
			String t_ext = t_name.substring(t_name.lastIndexOf(".") + 1);
			// 拒绝接受规定文件格式之外的文件类型
			int i = 0;
			int allowedExtCount = allowedExt.length;
			for (; i < allowedExtCount; i++) {
				if (allowedExt[i].equals(t_ext))
					break;
			}
			// 根据系统时间生成上传后保存的文件名
			String prefix = sdf.format(new Date());
			// 保存的最终文件完整路径,保存在web根目录下的ImagesUploaded目录下
			String u_name = t_name.substring(0, t_name.lastIndexOf(".")) + "_" + prefix + "." + t_ext;
			try {
				log.debug("保存文件到:" + uploadPath + ", 文件名:" + u_name);
				// 保存文件
				fileItem.write(new File(uploadPath, u_name));
			} catch (Exception e) {
				log.warn("写入文件失败:" + uploadPath + ", name:" + u_name, e);
			}
		}
	}
	

	public void destroy() {
		super.destroy(); 
	}
	
	private Properties getProperties() {
		InputStream in = getClass().getResourceAsStream(CONFIG_FILE_PATH);
		Properties p = new Properties();
		if(in != null) {
			try {
				p.load(in);
			} catch (IOException e) {
				log.error("读取" + CONFIG_FILE_PATH + "失败", e);
			} finally {
				if(in != null) {
					try {
						in.close();
					} catch (IOException e) {
					}
				}
			}
		}
		return p;
	}

}