import com.sun.image.codec.jpeg.JPEGCodec;
import com.sun.image.codec.jpeg.JPEGImageEncoder;
import org.w3c.dom.*;

import javax.imageio.ImageIO;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Calendar;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class epaper {
    //private String IMAGE_PATH = "Z:\\epapercover\\";
    //private String XML_PATH = "D:\\xml\\southcn\\";
    private String IMAGE_PATH = "F:\\myjob\\xx\\";
    private String XML_PATH = "F:\\myjob\\xx\\yy\\";

    private String folderName = null;

    public epaper()
    {
        Date date = new Date();
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
        folderName = df.format(date);
    }


    /**
     * @description 获取DocumentBuilder对象
     * @return documentBuilder DocumentBuilder
     */
    private DocumentBuilder getDocumentBuilder()
    {
        // 获得DocumentBuilderFactory对象
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = null;
        try {
            db = factory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        }
        return db;
    }

    /**
     * @description 指定图片宽度和高度和压缩比例对图片进行压缩
     * @param imgsrc 源图片地址
     * @param imgdist 目标图片地址
     * @param widthdist 压缩后的图片宽度
     * @param heightdist 压缩后的图片高度
     * @param rate 压缩比例
     */
    private void reduceImg(String imgsrc, String imgdist, int widthdist, int heightdist, Float rate)
    {
        try {
            File srcfile = new File(imgsrc);
            // 检查图片文件是否存在
            if ( !srcfile.exists() ) {
                System.out.println("文件不存在");
            }
            // 如果比例不为空则说明是按比例压缩
            if (rate != null && rate > 0) {
                //获得源图片的宽高存入数组中
                int[] results = this.getImgWidthHeight(srcfile);
                if (results == null || results[0] == 0 || results[1] == 0) {
                    return;
                } else {
                    //按比例缩放或扩大图片大小，将浮点型转为整型
                    widthdist = (int) (results[0] * rate);
                    heightdist = (int) (results[1] * rate);
                }
            }
            // 开始读取文件并进行压缩
            Image src = ImageIO.read(srcfile);

            // 构造一个类型为预定义图像类型之一的 BufferedImage
            BufferedImage tag = new BufferedImage((int) widthdist, (int) heightdist, BufferedImage.TYPE_INT_RGB);

            //绘制图像  getScaledInstance表示创建此图像的缩放版本，返回一个新的缩放版本Image,按指定的width,height呈现图像
            //Image.SCALE_SMOOTH,选择图像平滑度比缩放速度具有更高优先级的图像缩放算法。
            tag.getGraphics().drawImage(src.getScaledInstance(widthdist, heightdist, Image.SCALE_SMOOTH), 0, 0, null);

            //创建文件输出流
            FileOutputStream out = new FileOutputStream(imgdist);
            //将图片按JPEG压缩，保存到out中
            JPEGImageEncoder encoder = JPEGCodec.createJPEGEncoder(out);
            encoder.encode(tag);
            //关闭文件输出流
            out.close();
        } catch (Exception ef) {
            ef.printStackTrace();
        }
    }

    /**
     * @description 获取图片宽度和高度
     * @param file File
     * @return 返回图片的宽度
     */
    private int[] getImgWidthHeight(File file) {
        InputStream is;
        BufferedImage src;
        int result[] = { 0, 0 };
        try {
            // 获得文件输入流
            is = new FileInputStream(file);
            // 从流里将图片写入缓冲图片区
            src = ImageIO.read(is);
            result[0] =src.getWidth(null); // 得到源图片宽
            result[1] =src.getHeight(null);// 得到源图片高
            is.close();  //关闭输入流
        } catch (Exception ef) {
            ef.printStackTrace();
        }
        return result;
    }

    /**
     * @description 根据url和编码方式获取网页源码
     * @param  tempurl
     * @param  encoding
     * @throws IOException
     * @return
     */
    public void getHTML(String tempurl, String encoding) throws IOException
    {
        URL url;
        BufferedReader breader = null ;
        InputStream is = null ;
        StringBuffer resultBuffer = new StringBuffer();
        try {
            url = new URL(tempurl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestProperty("User-agent","Mozilla/5.0");
            connection.setRequestMethod("GET");
            is = connection.getInputStream();
            if ( "".equals(encoding) )
            {
                breader = new BufferedReader(new InputStreamReader(is));
            } else {
                breader = new BufferedReader(new InputStreamReader(is, encoding));
            }
            String line;
            while ( (line = breader.readLine()) != null )
            {
                resultBuffer.append(line);
            }
        } catch(Exception e) {
            e.printStackTrace();
        } finally {
            breader.close();
            is.close();
        }
        String html = resultBuffer.toString();
        System.out.println("1. 图片本地化保存");
        Pattern pattern = Pattern.compile("<img src=([^\"]*?) border=0 USEMAP=#pagepicmap>", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(html);
        if ( matcher.find() ) {
            String imageUrl = matcher.group(1);
            imageUrl = "http://epaper.southcn.com/nfdaily/" + imageUrl.substring(9);
            String imageName = imageUrl.split("/")[imageUrl.split("/").length - 1];
            String imagePath = this.IMAGE_PATH + imageName;
            File image_file = new File(imagePath);
            if ( !image_file.exists() ) {
                System.out.println("正在下载图片: " + imageUrl);
                this.downloadPicture(imageUrl, imagePath);
            }

            String nimageName = "small_" + imageName;
            String nimagePath = this.IMAGE_PATH + nimageName;
            File nimage_file = new File(nimagePath);
            if ( !nimage_file.exists() ) {
                System.out.println("正在压缩图片: " + nimagePath);
                this.reduceImg(imagePath, nimagePath, 140, 230, null);
            }

            HashMap<String, String> hashMap = new HashMap<String, String>();
            hashMap.put("title", this.folderName + " 封面");
            hashMap.put("content", "<img border=\"0\" align=\"center\" title=\"封面图\" src=\"/data/attachement/epapercover/" + nimageName + "\" />");
            hashMap.put("channelid", "384367");
            hashMap.put("nsdate", this.folderName + " 08:00:00");
            hashMap.put("url", tempurl);

            String article_path = this.XML_PATH + "epaper-cover-" + this.folderName + ".xml";
            File article_file = new File(article_path);
            if ( !article_file.exists() ) {
                this.createRss(article_path, hashMap);
            } else {
                System.out.println("今天的封面已经入库");
            }
        }
    }

    /**
     * @description 创建rss
     * @param filename String
     * @param hashMap HashMap
     * @return void
     */
    public void createRss(String filename, HashMap<String, String> hashMap)
    {
        DocumentBuilder documentBuilder = this.getDocumentBuilder();
        Document document = documentBuilder.newDocument();
        document.setXmlStandalone(true);

        Element founderEnpML = document.createElement("FounderEnpML");
        Element version = document.createElement("Version");
        version.setTextContent("2.0.2");

        Element pack = document.createElement("Package");

        Element sourceSystem = document.createElement("SourceSystem");
        CDATASection SourceSystem = document.createCDATASection("");
        sourceSystem.appendChild(SourceSystem);

        Element targetLib = document.createElement("TargetLib");
        targetLib.setTextContent("2");

        Element articleType = document.createElement("ArticleType");
        articleType.setTextContent("1");

        Element targetNode = document.createElement("TargetNode");
        targetNode.setTextContent("");

        Element targetNodeId = document.createElement("TargetNodeId");
        CDATASection TargetNodeId = document.createCDATASection(hashMap.get("channelid"));
        targetNodeId.appendChild(TargetNodeId);

        Element publishState = document.createElement("PublishState");
        publishState.setTextContent("3");

        Element objectType = document.createElement("ObjectType");
        objectType.setTextContent("1");

        Element action = document.createElement("Action");
        action.setTextContent("1");

        // Element userName = document.createElement("UserName");
        // CDATASection UserName = document.createCDATASection("zhangp");
        // userName.appendChild(UserName);

        Element siteId = document.createElement("SiteId");
        siteId.setTextContent("4");


        pack.appendChild(sourceSystem);
        pack.appendChild(targetLib);
        pack.appendChild(articleType);
        pack.appendChild(targetNode);
        pack.appendChild(targetNodeId);
        pack.appendChild(publishState);
        pack.appendChild(objectType);
        pack.appendChild(action);
        // pack.appendChild(userName);
        pack.appendChild(siteId);


        Element article = document.createElement("Article");

        Element docId = document.createElement("DocId");
        CDATASection DocId = document.createCDATASection("0");
        docId.appendChild(DocId);

        Element jabbarMark = document.createElement("JabbarMark");
        CDATASection JabbarMark = document.createCDATASection("");
        jabbarMark.appendChild(JabbarMark);

        Element loginname = document.createElement("Loginname");
        CDATASection Loginname = document.createCDATASection("");
        loginname.appendChild(Loginname);

        Element introtitle = document.createElement("Introtitle");
        CDATASection Introtitle = document.createCDATASection("");
        introtitle.appendChild(Introtitle);

        Element subtitle = document.createElement("Subtitle");
        CDATASection Subtitle = document.createCDATASection("");
        subtitle.appendChild(Subtitle);

        Element abst = document.createElement("Abstract");
        CDATASection Abst = document.createCDATASection("");
        abst.appendChild(Abst);

        Element attr = document.createElement("Attr");
        CDATASection Attr = document.createCDATASection("");
        attr.appendChild(Attr);

        Element wordCount = document.createElement("wordCount");
        CDATASection WordCount = document.createCDATASection("0");
        wordCount.appendChild(WordCount);

        Element importance = document.createElement("Importance");
        CDATASection Importance = document.createCDATASection("0");
        importance.appendChild(Importance);

        Element keyword = document.createElement("keyword");
        CDATASection Keyword = document.createCDATASection("数字报 封面");
        keyword.appendChild(Keyword);

        Element piccount = document.createElement("piccount");
        CDATASection Piccount = document.createCDATASection("1");
        piccount.appendChild(Piccount);

        Element nsdate = document.createElement("Nsdate");
        CDATASection Nsdate = document.createCDATASection(hashMap.get("nsdate"));
        nsdate.appendChild(Nsdate);

        Element source = document.createElement("Source");
        CDATASection Source = document.createCDATASection("南方日报");
        source.appendChild(Source);

        Element sourceUrl = document.createElement("SourceUrl");
        CDATASection SourceUrl = document.createCDATASection("");
        sourceUrl.appendChild(SourceUrl);

        Element author = document.createElement("Author");
        CDATASection Author = document.createCDATASection("南方日报");
        author.appendChild(Author);

        Element title = document.createElement("Title");
        CDATASection Title = document.createCDATASection(hashMap.get("title"));
        title.appendChild(Title);

        Element multiattach = document.createElement("Multiattach");
        CDATASection Multiattach = document.createCDATASection("");
        multiattach.appendChild(Multiattach);

        Element nodePath = document.createElement("NodePath");
        CDATASection NodePath = document.createCDATASection("");
        nodePath.appendChild(NodePath);

        Element url = document.createElement("Url");
        CDATASection urll = document.createCDATASection(hashMap.get("url"));
        url.appendChild(urll);

        Element hasTitlePic = document.createElement("hasTitlePic");

        Element content = document.createElement("Content");
        CDATASection Content = document.createCDATASection(hashMap.get("content"));
        content.appendChild(Content);

        Element expirationTime = document.createElement("ExpirationTime");
        CDATASection ExpirationTime = document.createCDATASection("");
        expirationTime.appendChild(ExpirationTime);

        Element isTop = document.createElement("IsTop");
        CDATASection IsTop = document.createCDATASection("");
        isTop.appendChild(IsTop);
        Element editor = document.createElement("Editor");
        Element attachement = document.createElement("Attachement");

        article.appendChild(docId);
        article.appendChild(jabbarMark);
        article.appendChild(loginname);
        article.appendChild(introtitle);
        article.appendChild(subtitle);
        article.appendChild(abst);
        article.appendChild(attr);
        article.appendChild(wordCount);
        article.appendChild(importance);
        article.appendChild(keyword);
        article.appendChild(piccount);
        article.appendChild(nsdate);
        article.appendChild(source);
        article.appendChild(sourceUrl);
        article.appendChild(author);
        article.appendChild(title);
        article.appendChild(multiattach);
        article.appendChild(nodePath);
        article.appendChild(url);
        article.appendChild(hasTitlePic);
        article.appendChild(content);
        article.appendChild(expirationTime);
        article.appendChild(isTop);
        article.appendChild(editor);
        article.appendChild(attachement);

        founderEnpML.appendChild(version);
        founderEnpML.appendChild(pack);
        founderEnpML.appendChild(article);

        document.appendChild(founderEnpML);
        TransformerFactory transFactory = TransformerFactory.newInstance();
        try {
            System.out.println("2. 生成xml文件");
            Transformer transformer = transFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "Yes");
            transformer.transform(new DOMSource(document), new StreamResult(filename));
        } catch (TransformerConfigurationException e) {
            e.printStackTrace();
        } catch (TransformerException e) {
            e.printStackTrace();
        }
    }

    /**
     * @description      链接url下载图片
     * @param imageUrl  网络图片地址
     * @param  imagePath  图片地址
     */
    public void downloadPicture(String imageUrl, String imagePath) {
        try {
            URL url = new URL(imageUrl);
            DataInputStream dataInputStream = new DataInputStream(url.openStream());

            FileOutputStream fileOutputStream = new FileOutputStream(new File(imagePath));
            ByteArrayOutputStream output = new ByteArrayOutputStream();

            byte[] buffer = new byte[1024];
            int length;

            while ((length = dataInputStream.read(buffer)) > 0) {
                output.write(buffer, 0, length);
            }
            fileOutputStream.write(output.toByteArray());
            dataInputStream.close();
            fileOutputStream.close();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String args[]) throws IOException
    {
        epaper ep = new epaper();
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM/dd/");
        String today = df.format(new Date());

        Calendar cal = Calendar.getInstance();
        int dow = cal.get(Calendar.DAY_OF_WEEK);

        String node = "node_581.htm";

        if ( dow == 1 )
            node = "node_2.htm";

        ep.getHTML("http://epaper.southcn.com/nfdaily/html/" + today + node, "UTF-8");
        System.out.println("3. 结束了");
    }

}