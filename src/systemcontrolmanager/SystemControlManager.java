/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package systemcontrolmanager;

import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.Properties;
import java.util.TimeZone;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.mail.Address;
import javax.mail.MessagingException;
import javax.mail.NoSuchProviderException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.naming.NamingException;

/**
 *
 * @author sbaresel
 */
public class SystemControlManager {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws InterruptedException, SQLException, ClassNotFoundException, FileNotFoundException, IOException {
        if (args[0] == null) {
            System.err.println("Keine Konfigurationsdatei angegeben.");
            System.exit(2);
        } else {
            Properties props = getConfiguration(args[0]);
            String from = props.getProperty("mail.from");
            Class.forName("org.postgresql.Driver");
            String temp="LKHduguge";
            /* Allocate Variables */
            String hostM = null; int portM = 0; String userM = null; String passM = null; String footM = null;
            /* Open JDBC Connections */
            Connection cn = DriverManager.getConnection(props.getProperty("url.repository"));
            /* Get Mail Configuration */
            PreparedStatement ps = cn.prepareStatement("SELECT decode(key,'base64'),decode(val,'base64') FROM config_gateway WHERE MOD like encode('MAILAPI','base64') ORDER BY 1 ASC");
            ResultSet rs = ps.executeQuery();
            while ( rs.next() ) { 
                if ("HOST".equals(rs.getString( 1 ))) {
                    hostM = rs.getString( 2 );
                } else if ("PORT".equals(rs.getString( 1 ))) {
                    portM = Integer.parseInt(rs.getString( 2 ));
                } else if ("USER".equals(rs.getString( 1 ))) {
                    userM = rs.getString( 2 );
                } else if ("PASS".equals(rs.getString( 1 ))) {
                    passM = rs.getString( 2 );
                } else if ("FOOTER".equals(rs.getString( 1 ))) {
                    footM = rs.getString( 2 );
                }
            } 
            cn.close();
            
            Integer i = 0;
            Long ctime1 = System.currentTimeMillis()/1000;
        
            FileWriter fstream = new FileWriter(props.getProperty("debug.logfile"),true);
            BufferedWriter log = new BufferedWriter(fstream);
            
            if ("on".equals(props.getProperty("debug"))) {
                log.write("[" + ctime1 + "] Dienst: System Control Manager gestartet\n");
                log.write("[" + ctime1 + "] Author: Steffen Baresel 2014\n");
                log.write("[" + ctime1 + "] Konfiguration: Geladen\n");
                log.write("[" + ctime1 + "] Konfiguration: debug.file = " + props.getProperty("debug.logfile") + "\n");
                log.write("[" + ctime1 + "] Konfiguration: url.repository = " + props.getProperty("url.repository") + "\n");
                log.write("[" + ctime1 + "] Konfiguration: url.monitoring = " + props.getProperty("url.monitoring") + "\n");
                log.write("[" + ctime1 + "] Konfiguration: mail.host = " + hostM + "\n");
                log.write("[" + ctime1 + "] Konfiguration: mail.port = " + portM + "\n");
                log.write("[" + ctime1 + "] Konfiguration: mail.user = " + userM + "\n");
                log.write("[" + ctime1 + "] Konfiguration: mail.pass = " + passM + "\n");
                log.write("[" + ctime1 + "] Konfiguration: mail.from = " + props.getProperty("mail.from") + "\n");
                log.write("[" + ctime1 + "] Konfiguration: mail.admin = " + props.getProperty("mail.admin") + "\n");
                log.write("[" + ctime1 + "] Dienst: Loop gestartet\n");
                log.flush();
            } else {
                log.close();
            }
            
            
        if ( isValidEmailAddress(props.getProperty("mail.from")) && isValidEmailAddress(props.getProperty("mail.admin")) ) {
                
            while (true) {
                /* Open JDBC Connections */
                Connection cnM = DriverManager.getConnection(props.getProperty("url.monitoring"));
                Connection cnR = DriverManager.getConnection(props.getProperty("url.repository"));
                /* Select Monitoring Mails */
                Long ctime2 = System.currentTimeMillis()/1000;
                PreparedStatement psLt = cnM.prepareStatement("select b.hstln,d.srvna,c.instna,e.rlid,decode(a.usr,'base64'),decode(a.comment,'base64'),f.mtyph,f.mtypt,a.created,a.mailid,g.current_state,a.t1,a.t2,f.mtypid from monitoring_mailing a, monitoring_info_host b, monitoring_info_instance c, monitoring_info_service d, monitoring_host_role_mapping e,class_mailtypes f, monitoring_status g where a.hstid=b.hstid and b.instid=c.instid and a.srvid=d.srvid and a.hstid=e.hstid and a.mtypid=f.mtypid and a.srvid=g.srvid and a.done=false");
                ResultSet rsLt = psLt.executeQuery();
                while ( rsLt.next() ) {
                    /* initial variables */
                    String host = rsLt.getString( 1 ); String serv = rsLt.getString( 2 ); String inst = rsLt.getString( 3 ); Integer role = rsLt.getInt( 4 ); String uuid = rsLt.getString( 5 ); String comment = rsLt.getString( 6 ); String header = rsLt.getString( 7 ); String text = rsLt.getString( 8 ); long ts = rsLt.getLong( 9 ); String sts = rsLt.getString( 9 ); String user = null; String to = ""; Integer mailid = rsLt.getInt( 10 ); Integer cstate = rsLt.getInt( 11 ); Long t1 = rsLt.getLong( 12 ); Long t2 = rsLt.getLong( 13 ); Integer mtypid = rsLt.getInt( 14 );
                    /* get full username */
                    if (!"system".equals(uuid)) {
                        PreparedStatement psR = cnR.prepareStatement("select decode(usdc,'base64') from profiles_user where usnm=?");
                        psR.setString(1,encodeString( uuid ));
                        ResultSet rsR = psR.executeQuery();
                        if (rsR.next()) { user = rsR.getString( 1 ); }
                    } else {
                        user = "System";
                    }
                    /* get mail recipients */
                    PreparedStatement psMR = cnR.prepareStatement("select decode(a.umai,'base64') from profiles_user a, profiles_user_group_mapping b, profiles_group_role_mapping c where a.uuid=b.uuid and b.grid=c.grid and rlid=?");
                    psMR.setInt(1, role);
                    ResultSet rsMR = psMR.executeQuery();
                    while (rsMR.next()) {
                        if (!"-".equals(rsMR.getString( 1 ))) {
                            if (isValidEmailAddress(rsMR.getString( 1 ))) {
                                to+= rsMR.getString( 1 ) + ",";
                            } else {
                                try {
                                    send(hostM,portM,userM,passM,props.getProperty("mail.admin"),"",from,"++ SystemControlManager ++","<html><head></head><body>Die angegebene E-Mail Adresse ist nicht valide.<br>E-Mail:" + rsMR.getString( 1 ) + "</body></html>");
                                } catch (NoSuchProviderException ex) {
                                    Logger.getLogger(SystemControlManager.class.getName()).log(Level.SEVERE, null, ex);
                                } catch (MessagingException ex) {
                                    Logger.getLogger(SystemControlManager.class.getName()).log(Level.SEVERE, null, ex);
                                } catch (NamingException ex) {
                                    Logger.getLogger(SystemControlManager.class.getName()).log(Level.SEVERE, null, ex);
                                }
                                if ("on".equals(props.getProperty("debug"))) {
                                    log.write("[" + ctime2 + "] ++ SystemControlManager ++ Die angegebene E-Mail Adresse ist nicht valide: " + rsMR.getString( 1 ) + "\n");
                                    log.flush();
                                }
                            }
                        } 
                    }
                    if (to.length() > 0) {
                        to = to.substring(0, to.length()-1);
                        /* build mail components */
                        String status="";
                        if(cstate == 1) { status="WARNUNG"; } else if(cstate == 2) { status="KRITISCH"; } else if(cstate == 3) { status="UNBEKANNT"; } else { status="OK"; }
                        String mailsubject = header.replace("_S_", serv).replace("_H_", host).replace("_U_", user).replace("_A_", status);
                        String mailtext;
                        if (mtypid == 2 || mtypid == 6) { 
                            mailtext = encodeHtml("<html><head></head><body><font face='Arial' size=2>" + text + "<br><br><hr><b>Instanz:</b> " + inst + "<b><br>Host:</b> " + host + "<br><b>Service:</b> " + serv + "<br><b>Bearbeiter:</b> " + user + "<br><b>Beginn:</b> " + ConvertUtime(t1) + "<br><b>Ende:</b> " + ConvertUtime(t2) + "<hr><br>" + comment + "<br><br>" + footM + "</font></body></html>");
                        } else if (mtypid == 1) {
                            mailtext = encodeHtml("<html><head></head><body><font face='Arial' size=2>" + text + "<br><br><hr><b>Instanz:</b> " + inst + "<b><br>Bearbeiter:</b> " + user + "<hr><br>" + comment + "<br><br>" + footM + "</font></body></html>");
                        } else {
                            mailtext = encodeHtml("<html><head></head><body><font face='Arial' size=2>" + text + "<br><br><hr><b>Instanz:</b> " + inst + "<b><br>Host:</b> " + host + "<br><b>Service:</b> " + serv + "<br><b>Bearbeiter:</b> " + user + "<hr><br>" + comment + "<br><br>" + footM + "</font></body></html>");
                        }
                        
                        try {
                            /* send mail */
                            String exec = encodeString("" + mailsubject + "-" + mailtext + "-" + to + "");
                            if( !temp.equals(exec) ) { send(hostM,portM,userM,passM,to,"",from,encodeSubject(mailsubject),mailtext); }
                            temp = exec;
                        } catch (NoSuchProviderException ex) {
                            Logger.getLogger(SystemControlManager.class.getName()).log(Level.SEVERE, null, ex);
                        } catch (MessagingException ex) {
                            Logger.getLogger(SystemControlManager.class.getName()).log(Level.SEVERE, null, ex);
                        } catch (NamingException ex) {
                            Logger.getLogger(SystemControlManager.class.getName()).log(Level.SEVERE, null, ex);
                        }
                        /* debug */
                        if ("on".equals(props.getProperty("debug"))) {
                            log.write("[" + ctime2 + "] Loop: \"MAILFROM\":\"" + from + "\",\"MAILTO\":\"" + to + "\",\"MAILSUBJECT\":\"" + mailsubject + ",\"MAILTEXT\":\"" + encodeHtml( mailtext ) + "\"\n");
                            log.flush();
                        }
                    }
                    /* set false to true */
                    PreparedStatement psD = cnM.prepareStatement("UPDATE monitoring_mailing SET DONE=true WHERE mailid=?");
                    psD.setInt(1, mailid);
                    psD.executeUpdate();
                }
                /* Profiles Mailing */
                PreparedStatement psPM = cnR.prepareStatement("select a.mailid,decode(a.mto,'base64'),decode(a.mcc,'base64'),decode(b.umai,'base64'),decode(a.msubject,'base64'),decode(a.mbody,'base64') from profiles_mailing a, profiles_user b where a.uuid=b.uuid and a.done=false");
                ResultSet rsPM = psPM.executeQuery();
                while ( rsPM.next() ) {
                    /* initial variables */
                    Integer rmailid = rsPM.getInt( 1 );
                    String rto = rsPM.getString( 2 );
                    String rcc = rsPM.getString( 3 );
                    String rfrom = rsPM.getString( 4 );
                    String rsubject = encodeSubject( rsPM.getString( 5 ) );
                    String rtext = encodeHtml( "<html><head></head><body><font face=Arial size=2>" + rsPM.getString( 6 ) + "</font></body></html>");
                    try {
                        /* send mail */
                        send(hostM,portM,userM,passM,rto,rcc,rfrom,rsubject,rtext);                      
                    } catch (NoSuchProviderException ex) {
                        Logger.getLogger(SystemControlManager.class.getName()).log(Level.SEVERE, null, ex);
                    } catch (MessagingException ex) {
                        Logger.getLogger(SystemControlManager.class.getName()).log(Level.SEVERE, null, ex);
                    } catch (NamingException ex) {
                        Logger.getLogger(SystemControlManager.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    /* debug */
                    if ("on".equals(props.getProperty("debug"))) {
                        log.write("[" + ctime2 + "] Loop: \"MAILFROM\":\"" + rfrom + "\",\"MAILTO\":\"" + rto + "\",\"MAILSUBJECT\":\"" + rsubject + ",\"MAILTEXT\":\"" + rtext + "\"\n");
                        log.flush();
                    }
                    /* set false to true */
                    PreparedStatement psPMD = cnR.prepareStatement("UPDATE profiles_mailing SET DONE=true WHERE mailid=?");
                    psPMD.setInt(1, rmailid);
                    psPMD.executeUpdate();
                }
                /* Close Connections */
                /* Monitoring System Mailing */
                PreparedStatement psMSM = cnM.prepareStatement("select decode(a.usr,'base64'),decode(a.comment,'base64'),b.mtyph,b.mtypt,a.created,a.mailid from monitoring_mailing a, class_mailtypes b where a.hstid=0 and a.srvid=0 and a.mtypid=7 and a.mtypid=b.mtypid and a.done=false");
                ResultSet rsMSM = psMSM.executeQuery();
                while ( rsMSM.next() ) {
                    /* initial variables */
                    String msmsub = encodeSubject( rsMSM.getString( 3 ).replace(": _M_","") );
                    String msmtxt = encodeHtml( "<html><head></head><body><font face=Arial size=2>" + rsMSM.getString( 4 ) + "<br><br>" + rsMSM.getString( 2 ) + "</font></body></html>");
                    Integer msmmailid = rsMSM.getInt( 6 );
                    try {
                        /* send mail */
                        send(hostM,portM,userM,passM,props.getProperty("mail.admin"),"",from,msmsub,msmtxt);                      
                    } catch (NoSuchProviderException ex) {
                        Logger.getLogger(SystemControlManager.class.getName()).log(Level.SEVERE, null, ex);
                    } catch (MessagingException ex) {
                        Logger.getLogger(SystemControlManager.class.getName()).log(Level.SEVERE, null, ex);
                    } catch (NamingException ex) {
                        Logger.getLogger(SystemControlManager.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    /* debug */
                    if ("on".equals(props.getProperty("debug"))) {
                        log.write("[" + ctime2 + "] Loop: \"MAILFROM\":\"" + from + "\",\"MAILTO\":\"" + props.getProperty("mail.admin") + "\",\"MAILSUBJECT\":\"" + msmsub + ",\"MAILTEXT\":\"" + msmtxt + "\"\n");
                        log.flush();
                    }
                    /* set false to true */
                    PreparedStatement psPMD = cnM.prepareStatement("UPDATE monitoring_mailing SET DONE=true WHERE mailid=?");
                    psPMD.setInt(1, msmmailid);
                    psPMD.executeUpdate();
                }
                /* Close Connections */
                cnR.close();
                cnM.close();
                Thread.sleep(2000);
                i++;
            }
        
        } else {
            if ("on".equals(props.getProperty("debug"))) {
                log.write("[" + ctime1 + "] ++ SystemControlManager ++ Die angegebenen E-Mail Adressen mail.from oder mail.admin sind nicht valide.\n");
                log.flush();
            }
        }
        
        }
    }
    
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
}
