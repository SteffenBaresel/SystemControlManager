/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.steffenbaresel.systemlocal;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.Date;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.Properties;
import java.util.TimeZone;
import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.Address;
import javax.mail.BodyPart;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.NoSuchProviderException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.naming.NamingException;
import javax.net.ssl.HttpsURLConnection;

/**
 *
 * @author sbaresel
 */
public class Basics {
    
    private final String USER_AGENT = "Mozilla/5.0";
    
    static public Properties getConfiguration(String file) throws FileNotFoundException, IOException {
        Properties props;
        props = new Properties();
        BufferedInputStream in = new BufferedInputStream(new FileInputStream(file));
        props.load(in);
        in.close(); 
        return props;
    }
    
    static public String encodeHtml(String desc) {
        String replace = desc.replace("\\256", "&reg;").replace("\\303\\234", "&Uuml;").replace("\\303\\274", "&uuml;").replace("\\304", "&Auml;").replace("\\303\\204", "&Auml;").replace("\\344", "&auml;").replace("\\303\\244", "&auml;").replace("\\326", "&Ouml;").replace("\\303\\226", "&Ouml;").replace("\\366", "&ouml;").replace("\\303\\266", "&ouml;").replace("\\334", "&Uuml;").replace("\\374", "&uuml;").replace("\\337", "&szlig;").replace("\\303\\237", "&szlig;").replace("\\012", "<br>").replace("\\302\\264", "&acute;").replace("\\011", "").replace("\\342\\200\\223", "-").replace("\"", "&quot;");
        return replace;
    }
    
    static public String encodeSubject(String desc) {
        String replace = desc.replace("\\303\\234", "Ü").replace("\\303\\274", "ü").replace("\\304", "Ä").replace("\\303\\204", "Ä").replace("\\344", "ä").replace("\\303\\244", "ä").replace("\\326", "Ö").replace("\\303\\226", "Ö").replace("\\366", "ö").replace("\\303\\266", "ö").replace("\\334", "Ü").replace("\\374", "ü").replace("\\337", "ß").replace("\\303\\237", "ß").replace("\\011", "").replace("\\342\\200\\223", "-");
        return replace;
    }
    
    static public String ConvertUtime(Long utime) throws FileNotFoundException, IOException
    {
        Date date = new Date(utime*1000L); // *1000 is to convert minutes to milliseconds
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"); // the format of your date
        String formattedDate = sdf.format(date);
        sdf.setTimeZone(TimeZone.getTimeZone("GMT-1"));
        return formattedDate;
    }
    
    public static boolean isValidInternetEmailAddress(String email) {
        boolean result = true;
        try {
            InternetAddress emailAddr = new InternetAddress(email);
            emailAddr.validate();
        } catch (AddressException ex) {
            result = false;
        }
        return result;
    }
    
    public static boolean isValidEmailAddress(String email) {
       java.util.regex.Pattern p = java.util.regex.Pattern.compile(".+@.+\\.[a-z]+");
       java.util.regex.Matcher m = p.matcher(email);
       return m.matches();
    }
    
    /*
     * Mailing
     */
    
    static public void send(String host, Integer port, String user, String pass, String to, String cc, String from, String subject, String text) throws FileNotFoundException, IOException, NoSuchProviderException, MessagingException, NamingException, SQLException {
        Properties mail=new Properties();
        mail.put("mail.smtp.auth", "true");
        mail.put("mail.smtp.starttls.enable", "true");
        mail.put("mail.from", from);
        
        Session session=Session.getInstance(mail);
        Transport transport=session.getTransport("smtp");
        transport.connect(host, port, user, pass);
        
        Address[] addresses=InternetAddress.parse(to);
        Address[] ccaddresses=InternetAddress.parse(cc);
        
        MimeMessage message = new MimeMessage(session);
        message.setFrom();
        message.setRecipients(MimeMessage.RecipientType.TO, addresses);
        message.addRecipients(MimeMessage.RecipientType.CC, ccaddresses);
        message.setSubject(subject);
        
        message.setText(text, "utf-8", "html");
        
        transport.sendMessage(message, message.getAllRecipients());
        
        transport.close();
    }
    
    static public void sendFile(String host, Integer port, String user, String pass, String to, String cc, String from, String subject, String text, String File, String Filename) throws FileNotFoundException, IOException, NoSuchProviderException, MessagingException, NamingException, SQLException {
        Properties mail=new Properties();
        mail.put("mail.smtp.auth", "true");
        mail.put("mail.smtp.starttls.enable", "true");
        mail.put("mail.from", from);
        
        Session session=Session.getInstance(mail);
        Transport transport=session.getTransport("smtp");
        transport.connect(host, port, user, pass);
        
        Address[] addresses=InternetAddress.parse(to);
        Address[] ccaddresses=InternetAddress.parse(cc);
        
        MimeMessage message = new MimeMessage(session);
        message.setFrom();
        message.setRecipients(MimeMessage.RecipientType.TO, addresses);
        message.addRecipients(MimeMessage.RecipientType.CC, ccaddresses);
        message.setSubject(subject);
        
        BodyPart messageBodyPart = new MimeBodyPart();
        
        //messageBodyPart.setText(text, "utf-8", "html");
        messageBodyPart.setContent(text, "text/html");
        
        Multipart multipart = new MimeMultipart();
        multipart.addBodyPart(messageBodyPart);
        messageBodyPart = new MimeBodyPart();
        String file = File;
        String filename = Filename;
        DataSource source = new FileDataSource(file);
        messageBodyPart.setDataHandler(new DataHandler(source));
        messageBodyPart.setFileName(filename);
        multipart.addBodyPart(messageBodyPart);
        
        message.setContent(multipart);
        
        transport.sendMessage(message, message.getAllRecipients());
        
        transport.close();
    }
    
    /*
     * Base64
     */
    
    private static final String systemLineSeparator = System.getProperty("line.separator");
    
    private static final char[] map1 = new char[64];
        static {
            int i=0;
            for (char c='A'; c<='Z'; c++) {
                map1[i++] = c;
            }
            for (char c='a'; c<='z'; c++) {
                map1[i++] = c;
            }
            for (char c='0'; c<='9'; c++) {
                map1[i++] = c;
            }
            map1[i++] = '+'; map1[i++] = '/';
        }

    private static final byte[] map2 = new byte[128];
        static {
            for (int i=0; i<map2.length; i++) {
                map2[i] = -1;
            }
            for (int i=0; i<64; i++) {
                map2[map1[i]] = (byte)i;
            }
        }

    public static String encodeString (String s) {
        return new String(encode(s.getBytes()));
    }

    public static String encodeLines (byte[] in) {
        return encodeLines(in, 0, in.length, 76, systemLineSeparator);
    }

    public static String encodeLines (byte[] in, int iOff, int iLen, int lineLen, String lineSeparator) {
        int blockLen = (lineLen*3) / 4;
        if (blockLen <= 0) {
            throw new IllegalArgumentException();
        }
        int lines = (iLen+blockLen-1) / blockLen;
        int bufLen = ((iLen+2)/3)*4 + lines*lineSeparator.length();
        StringBuilder buf = new StringBuilder(bufLen);
        int ip = 0;
        while (ip < iLen) {
            int l = Math.min(iLen-ip, blockLen);
            buf.append (encode(in, iOff+ip, l));
            buf.append (lineSeparator);
            ip += l;
        }
        return buf.toString();
    }

    public static char[] encode (byte[] in) {
        return encode(in, 0, in.length);
    }

    public static char[] encode (byte[] in, int iLen) {
        return encode(in, 0, iLen);
    }

    public static char[] encode (byte[] in, int iOff, int iLen) {
        int oDataLen = (iLen*4+2)/3;       // output length without padding
        int oLen = ((iLen+2)/3)*4;         // output length including padding
        char[] out = new char[oLen];
        int ip = iOff;
        int iEnd = iOff + iLen;
        int op = 0;
        while (ip < iEnd) {
            int i0 = in[ip++] & 0xff;
            int i1 = ip < iEnd ? in[ip++] & 0xff : 0;
            int i2 = ip < iEnd ? in[ip++] & 0xff : 0;
            int o0 = i0 >>> 2;
            int o1 = ((i0 &   3) << 4) | (i1 >>> 4);
            int o2 = ((i1 & 0xf) << 2) | (i2 >>> 6);
            int o3 = i2 & 0x3F;
            out[op++] = map1[o0];
            out[op++] = map1[o1];
            out[op] = op < oDataLen ? map1[o2] : '='; op++;
            out[op] = op < oDataLen ? map1[o3] : '='; op++;
        }
        return out;
    }
        
    public static String decodeString (String s) {
        return new String(decode(s));
    }

    public static byte[] decodeLines (String s) {
        char[] buf = new char[s.length()];
        int p = 0;
        for (int ip = 0; ip < s.length(); ip++) {
            char c = s.charAt(ip);
            if (c != ' ' && c != '\r' && c != '\n' && c != '\t') {
                buf[p++] = c;
            }
        }
        return decode(buf, 0, p);
    }

    public static byte[] decode (String s) {
        return decode(s.toCharArray());
    }

    public static byte[] decode (char[] in) {
        return decode(in, 0, in.length);
    }

    public static byte[] decode (char[] in, int iOff, int iLen) {
        if (iLen%4 != 0) {
            throw new IllegalArgumentException ("Length of Base64 encoded input string is not a multiple of 4.");
        }
        while (iLen > 0 && in[iOff+iLen-1] == '=') {
            iLen--;
        }
        int oLen = (iLen*3) / 4;
        byte[] out = new byte[oLen];
        int ip = iOff;
        int iEnd = iOff + iLen;
        int op = 0;
        while (ip < iEnd) {
            int i0 = in[ip++];
            int i1 = in[ip++];
            int i2 = ip < iEnd ? in[ip++] : 'A';
            int i3 = ip < iEnd ? in[ip++] : 'A';
            if (i0 > 127 || i1 > 127 || i2 > 127 || i3 > 127) {
                throw new IllegalArgumentException ("Illegal character in Base64 encoded data.");
            }
            int b0 = map2[i0];
            int b1 = map2[i1];
            int b2 = map2[i2];
            int b3 = map2[i3];
            if (b0 < 0 || b1 < 0 || b2 < 0 || b3 < 0) {
                throw new IllegalArgumentException ("Illegal character in Base64 encoded data.");
            }
            int o0 = ( b0       <<2) | (b1>>>4);
            int o1 = ((b1 & 0xf)<<4) | (b2>>>2);
            int o2 = ((b2 &   3)<<6) |  b3;
            out[op++] = (byte)o0;
            if (op<oLen) {
                out[op++] = (byte)o1;
            }
            if (op<oLen) {
                out[op++] = (byte)o2;
            } }
        return out;
    }
    
    /* HTTP */
    // HTTP GET request
    public String sendGet(String url) throws Exception {
 
        URL obj = new URL(url);
	HttpURLConnection con = (HttpURLConnection) obj.openConnection();
 
	// optional default is GET
	con.setRequestMethod("GET");
 
	//add request header
	con.setRequestProperty("User-Agent", USER_AGENT);
 
	int responseCode = con.getResponseCode();
	//System.out.println("\nSending 'GET' request to URL : " + url);
	//System.out.println("Response Code : " + responseCode);
 
	BufferedReader in = new BufferedReader(
	new InputStreamReader(con.getInputStream()));
	String inputLine;
	StringBuilder response = new StringBuilder();
 
	while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
	}
	in.close();
 
	//print result
	//System.out.println(response.toString());
        return response.toString();
 
    }
 
    // HTTP POST request
    public String sendPost(String url) throws Exception {
 
        URL obj = new URL(url);
        HttpsURLConnection con = (HttpsURLConnection) obj.openConnection();
 
        //add reuqest header
        con.setRequestMethod("POST");
        con.setRequestProperty("User-Agent", USER_AGENT);
        con.setRequestProperty("Accept-Language", "en-US,en;q=0.5");
 
        String urlParameters = "sn=C02G8416DRJM&cn=&locale=&caller=&num=12345";
 
        // Send post request
        con.setDoOutput(true);
        DataOutputStream wr = new DataOutputStream(con.getOutputStream());
        wr.writeBytes(urlParameters);
        wr.flush();
        wr.close();
 
	int responseCode = con.getResponseCode();
	System.out.println("\nSending 'POST' request to URL : " + url);
	System.out.println("Post parameters : " + urlParameters);
	System.out.println("Response Code : " + responseCode);
 
	BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
        String inputLine;
        StringBuilder response = new StringBuilder();
 
        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();
 
        //print result
        //System.out.println(response.toString());
        return response.toString();
    }
    
    static public Long CronConvertDate(String date) throws FileNotFoundException, IOException, ParseException
    {
        DateFormat sdf = new SimpleDateFormat("EEE MMM dd kk:mm:ss ZZZ yyyy", Locale.ENGLISH); // the format of your date
        Long utime = sdf.parse(date).getTime();
        utime = utime/1000;
        return utime;
    }
}
