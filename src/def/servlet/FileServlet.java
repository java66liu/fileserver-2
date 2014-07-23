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
	final long MAX_SIZE = 10 * 1024 * 1024;// �����ϴ��ļ����Ϊ 10M
	// �����ϴ����ļ���ʽ���б�
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
			log.warn("û�������ϴ�·����������Ĭ��·��:" + uploadPath);
		}
		log.debug("�ļ��ϴ��󱣴�·��:" + uploadPath);
		// ʵ����һ��Ӳ���ļ�����,���������ϴ����ServletFileUpload
		dfif = new DiskFileItemFactory();
		dfif.setSizeThreshold(1024 * 5 * 1024);// �����ϴ��ļ�ʱ������ʱ����ļ����ڴ��С,������4K.���ڵĲ��ֽ���ʱ����Ӳ��
		dfif.setRepository(new File(uploadPath));// ���ô����ʱ�ļ���Ŀ¼
		
		// �����Ϲ���ʵ�����ϴ����
		sfu = new ServletFileUpload(dfif);
		// ��������ϴ��ߴ�
		sfu.setSizeMax(MAX_SIZE);
		log.info("��ʼ��FileServlet���");
	}

	public void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		doPost(request, response);
	}

	public void doPost(HttpServletRequest req, HttpServletResponse res)
			throws ServletException, IOException {
		log.debug("�յ��ϴ�����");
		if(!ServletFileUpload.isMultipartContent(req)) {
			log.debug("�����ļ��ϴ����󣬷���");
			return ;
		}
		
		res.setContentType("text/html");
		// �����ַ�����ΪUTF-8, ����֧�ֺ�����ʾ
		res.setCharacterEncoding("UTF-8");

		PrintWriter out = res.getWriter();
		// ��request�õ� ���� �ϴ�����б�
		List fileList = null;
		try {
			fileList = sfu.parseRequest(req);
		} catch (FileUploadException e) {// �����ļ��ߴ�����쳣
			if (e instanceof SizeLimitExceededException) {
				log.warn("�ļ��ߴ糬���涨��С:" + MAX_SIZE + "�ֽ�");
				return;
			}
			log.debug("�����ļ�ʱ����", e);
		}
		// û���ļ��ϴ�
		if (fileList == null || fileList.size() == 0) {
			log.debug("no files upload!");
			return;
		}
		// �õ������ϴ����ļ�
		Iterator fileItr = fileList.iterator();
		// ѭ�����������ļ�
		while (fileItr.hasNext()) {
			FileItem fileItem = null;
			String path = null;
			long size = 0;
			// �õ���ǰ�ļ�
			fileItem = (FileItem) fileItr.next();
			// ���Լ�form�ֶζ������ϴ�����ļ���(<input type="text" />��)
			if (fileItem == null || fileItem.isFormField()) {
				continue;
			}
			// �õ��ļ�������·��
			path = fileItem.getName();
			// �õ��ļ��Ĵ�С
			size = fileItem.getSize();
			if ("".equals(path) || size == 0) {
				return;
			}
			// �õ�ȥ��·�����ļ���
			String t_name = path.substring(path.lastIndexOf("\\") + 1);
			// �õ��ļ�����չ��(����չ��ʱ���õ�ȫ��)
			String t_ext = t_name.substring(t_name.lastIndexOf(".") + 1);
			// �ܾ����ܹ涨�ļ���ʽ֮����ļ�����
			int i = 0;
			int allowedExtCount = allowedExt.length;
			for (; i < allowedExtCount; i++) {
				if (allowedExt[i].equals(t_ext))
					break;
			}
			// ����ϵͳʱ�������ϴ��󱣴���ļ���
			String prefix = sdf.format(new Date());
			// ����������ļ�����·��,������web��Ŀ¼�µ�ImagesUploadedĿ¼��
			String u_name = t_name.substring(0, t_name.lastIndexOf(".")) + "_" + prefix + "." + t_ext;
			try {
				log.debug("�����ļ���:" + uploadPath + ", �ļ���:" + u_name);
				// �����ļ�
				fileItem.write(new File(uploadPath, u_name));
			} catch (Exception e) {
				log.warn("д���ļ�ʧ��:" + uploadPath + ", name:" + u_name, e);
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
				log.error("��ȡ" + CONFIG_FILE_PATH + "ʧ��", e);
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