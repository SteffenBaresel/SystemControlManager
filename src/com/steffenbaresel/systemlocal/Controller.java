/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.steffenbaresel.systemlocal;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.*;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.mail.MessagingException;
import javax.mail.NoSuchProviderException;
import javax.naming.NamingException;
import org.json.*;
import org.quartz.CronExpression;

/**
 *
 * @author sbaresel
 */
public class Controller {
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws InterruptedException, SQLException, ClassNotFoundException, FileNotFoundException, IOException {
        int size = 0;
        for (String value : args) { size++; }
        if (size > 0) {
            Properties props = Basics.getConfiguration(args[0]);
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
                log.write("[" + ctime1 + "] Dienst: SystemLocal gestartet\n");
                log.write("[" + ctime1 + "] Author: Steffen Baresel 2015\n");
                log.write("[" + ctime1 + "] Konfiguration: Geladen\n");
                log.write("[" + ctime1 + "] Konfiguration: debug.file = " + props.getProperty("debug.logfile") + "\n");
                log.write("[" + ctime1 + "] Konfiguration: url.repository = " + props.getProperty("url.repository") + "\n");
                log.write("[" + ctime1 + "] Konfiguration: url.monitoring = " + props.getProperty("url.monitoring") + "\n");
                log.write("[" + ctime1 + "] Konfiguration: url.gateway = " + props.getProperty("url.gateway") + "\n");
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
            
            
        if ( Basics.isValidEmailAddress(props.getProperty("mail.from")) && Basics.isValidEmailAddress(props.getProperty("mail.admin")) ) {
                
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
                        psR.setString(1,Basics.encodeString( uuid ));
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
                            if (Basics.isValidEmailAddress(rsMR.getString( 1 ))) {
                                to+= rsMR.getString( 1 ) + ",";
                            } else {
                                try {
                                    Basics.send(hostM,portM,userM,passM,props.getProperty("mail.admin"),"",from,"++ SystemLocal ++","<html><head></head><body>Die angegebene E-Mail Adresse ist nicht valide.<br>E-Mail:" + rsMR.getString( 1 ) + "</body></html>");
                                } catch (NoSuchProviderException ex) {
                                    Logger.getLogger(Controller.class.getName()).log(Level.SEVERE, null, ex);
                                } catch (MessagingException ex) {
                                    Logger.getLogger(Controller.class.getName()).log(Level.SEVERE, null, ex);
                                } catch (NamingException ex) {
                                    Logger.getLogger(Controller.class.getName()).log(Level.SEVERE, null, ex);
                                }
                                if ("on".equals(props.getProperty("debug"))) {
                                    log.write("[" + ctime2 + "] ++ SystemLocal ++ Die angegebene E-Mail Adresse ist nicht valide: " + rsMR.getString( 1 ) + "\n");
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
                            mailtext = Basics.encodeHtml("<html><head></head><body><font face='Arial' size=2>" + text + "<br><br><hr><b>Instanz:</b> " + inst + "<b><br>Host:</b> " + host + "<br><b>Service:</b> " + serv + "<br><b>Bearbeiter:</b> " + user + "<br><b>Beginn:</b> " + Basics.ConvertUtime(t1) + "<br><b>Ende:</b> " + Basics.ConvertUtime(t2) + "<hr><br>" + comment + "<br><br>" + footM + "</font></body></html>");
                        } else if (mtypid == 1) {
                            mailtext = Basics.encodeHtml("<html><head></head><body><font face='Arial' size=2>" + text + "<br><br><hr><b>Instanz:</b> " + inst + "<b><br>Bearbeiter:</b> " + user + "<hr><br>" + comment + "<br><br>" + footM + "</font></body></html>");
                        } else {
                            mailtext = Basics.encodeHtml("<html><head></head><body><font face='Arial' size=2>" + text + "<br><br><hr><b>Instanz:</b> " + inst + "<b><br>Host:</b> " + host + "<br><b>Service:</b> " + serv + "<br><b>Bearbeiter:</b> " + user + "<hr><br>" + comment + "<br><br>" + footM + "</font></body></html>");
                        }
                        
                        try {
                            /* send mail */
                            String exec = Basics.encodeString("" + mailsubject + "-" + mailtext + "-" + to + "");
                            if( !temp.equals(exec) ) { Basics.send(hostM,portM,userM,passM,to,"",from,Basics.encodeSubject(mailsubject),mailtext); }
                            temp = exec;
                        } catch (NoSuchProviderException ex) {
                            Logger.getLogger(Controller.class.getName()).log(Level.SEVERE, null, ex);
                        } catch (MessagingException ex) {
                            Logger.getLogger(Controller.class.getName()).log(Level.SEVERE, null, ex);
                        } catch (NamingException ex) {
                            Logger.getLogger(Controller.class.getName()).log(Level.SEVERE, null, ex);
                        }
                        /* debug */
                        if ("on".equals(props.getProperty("debug"))) {
                            log.write("[" + ctime2 + "] Loop: \"MAILFROM\":\"" + from + "\",\"MAILTO\":\"" + to + "\",\"MAILSUBJECT\":\"" + mailsubject + ",\"MAILTEXT\":\"" + Basics.encodeHtml( mailtext ) + "\"\n");
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
                    String rsubject = Basics.encodeSubject( rsPM.getString( 5 ) );
                    String rtext = Basics.encodeHtml( "<html><head></head><body><font face=Arial size=2>" + rsPM.getString( 6 ) + "</font></body></html>");
                    try {
                        /* send mail */
                        Basics.send(hostM,portM,userM,passM,rto,rcc,rfrom,rsubject,rtext);                      
                    } catch (NoSuchProviderException ex) {
                        Logger.getLogger(Controller.class.getName()).log(Level.SEVERE, null, ex);
                    } catch (MessagingException ex) {
                        Logger.getLogger(Controller.class.getName()).log(Level.SEVERE, null, ex);
                    } catch (NamingException ex) {
                        Logger.getLogger(Controller.class.getName()).log(Level.SEVERE, null, ex);
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
                    String msmsub = Basics.encodeSubject( rsMSM.getString( 3 ).replace(": _M_","") );
                    String msmtxt = Basics.encodeHtml( "<html><head></head><body><font face=Arial size=2>" + rsMSM.getString( 4 ) + "<br><br>" + rsMSM.getString( 2 ) + "</font></body></html>");
                    Integer msmmailid = rsMSM.getInt( 6 );
                    try {
                        /* send mail */
                        Basics.send(hostM,portM,userM,passM,props.getProperty("mail.admin"),"",from,msmsub,msmtxt);                      
                    } catch (NoSuchProviderException ex) {
                        Logger.getLogger(Controller.class.getName()).log(Level.SEVERE, null, ex);
                    } catch (MessagingException ex) {
                        Logger.getLogger(Controller.class.getName()).log(Level.SEVERE, null, ex);
                    } catch (NamingException ex) {
                        Logger.getLogger(Controller.class.getName()).log(Level.SEVERE, null, ex);
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
                /* Job Scheduler */
                PreparedStatement psJS = cnR.prepareStatement("SELECT cronid,datestart,function,status FROM cron_reporting");
                ResultSet rsJS = psJS.executeQuery();
                while ( rsJS.next() ) {
                    
                    Integer cronid = rsJS.getInt(1);
                    Long datestart = rsJS.getLong(2);
                    String function = rsJS.getString(3);
                    String expression = rsJS.getString(4);
                    
                    /* compare dates */
                    
                    Integer c=1;
                    if (ctime2 > datestart) {
                        try {
                            if ("on".equals(props.getProperty("debug"))) {
                                log.write("[" + ctime2 + "] Job Scheduler: Execute " + function + " \n");
                                log.flush();
                            }

                            Basics http = new Basics();
                            String URL = props.getProperty("url.gateway").replace("<FUNCTION>", function).replace("\\", "/");
                            
                            if ("on".equals(props.getProperty("debug"))) {
                                log.write("[" + ctime2 + "] Gathering remote data ... " + URL + "\n");
                                log.flush();
                            }
                            
                            /* Update cron table */
                            
                            CronExpression cronExpression = new CronExpression(expression);
                            String nvd = cronExpression.getNextValidTimeAfter(new java.util.Date()).toString();
                            Long nvdms = Basics.CronConvertDate(nvd);
                            
                            PreparedStatement psNVD = cnR.prepareStatement("UPDATE cron_reporting SET DATESTART=?,INTERVALL=? WHERE cronid=?");
                            psNVD.setLong(1,nvdms);
                            psNVD.setString(2,nvd);
                            psNVD.setInt(3, cronid);
                            psNVD.executeUpdate();
                            
                            /* gather Remote data and execute Report */ 
                            
                            JSONObject object = new JSONObject(http.sendGet(URL));
                            String json = object.getString("MESSAGE");
                            
                            if ("on".equals(props.getProperty("debug"))) {
                                log.write("[" + ctime2 + "] JSON Get 'MESSAGE': " + json + "\n");
                                log.flush();
                            }
                            
                            /* Send Mail with Report */
                            String mail = object.getString("MAIL");
                            String cunm = object.getString("CUNM");
                            String file = Basics.decodeString(object.getString("FILE"));
                            /* Define filename */
                            if ("on".equals(props.getProperty("debug"))) {
                                log.write("[" + ctime2 + "] File " + file + " \n");
                                log.flush();
                            }
                            
                            String filename = file.substring(file.lastIndexOf("/")+1);
                            
                            if ("on".equals(props.getProperty("debug"))) {
                                log.write("[" + ctime2 + "] Filename " + filename + " \n");
                                log.flush();
                            }
                            
                            String sfrom = object.getString("FROM");
                            String sto = object.getString("TO");
                            if ( Basics.isValidEmailAddress(mail) ) {
                                PreparedStatement psGMC = cnR.prepareStatement("SELECT decode(val,'base64') FROM config_gateway WHERE mod=encode('MAILAPI','base64') AND key=encode('HEADER','base64')");
                                ResultSet rsGMC = psGMC.executeQuery();
                                String mmhead="";
                                if ( rsGMC.next() ) { mmhead = rsGMC.getString(1); }
                                
                                PreparedStatement psGMC2 = cnR.prepareStatement("SELECT decode(val,'base64') FROM config_gateway WHERE mod=encode('MAILAPI','base64') AND key=encode('FOOTER','base64')");
                                ResultSet rsGMC2 = psGMC2.executeQuery();
                                String mmfoot="";
                                if ( rsGMC2.next() ) { mmfoot = rsGMC2.getString(1); }
                                
                                String mmsub = Basics.encodeSubject( "SIV.AG - Service Report vom " + sfrom + " bis " + sto);
                                String mmtxt = Basics.encodeHtml( "<html><head></head><body><font face=Arial size=2>" + mmhead + "<br>nachfolgend erhalten Sie den aktuellen Service Report.<br><br>" + mmfoot + "</font></body></html>");
                                try {
                                    /* send mail */
                                    Basics.sendFile(hostM,portM,userM,passM,mail,"",from,mmsub,mmtxt,file,filename);                      
                                } catch (NoSuchProviderException ex) {
                                    Logger.getLogger(Controller.class.getName()).log(Level.SEVERE, null, ex);
                                } catch (MessagingException ex) {
                                    Logger.getLogger(Controller.class.getName()).log(Level.SEVERE, null, ex);
                                } catch (NamingException ex) {
                                    Logger.getLogger(Controller.class.getName()).log(Level.SEVERE, null, ex);
                                }
                            }
                            
                            /* Send Notification to Admin */
                            if ( Basics.isValidEmailAddress(props.getProperty("mail.admin") ) ) {
                                String mmsub = Basics.encodeSubject( "Ein Report wurde erstellt für " + Basics.decodeString(cunm) + " " + sfrom + " - " + sto + "");
                                String file2 = "";
                                if (file.matches("^[a-zA-Z]{1}:{1}.*")) {
                                    file2 = file.replace("/", "\\");
                                } else {
                                    file2 = file;
                                }
                                String mmtxt = Basics.encodeHtml( "<html><head></head><body><font face=Arial size=2>Die Datei liegt auf dem AS unter '" + file2 + "'.</font></body></html>");
                                
                                try {
                                    /* send mail */
                                    Basics.send(hostM,portM,userM,passM,props.getProperty("mail.admin"),"",from,mmsub,mmtxt);                      
                                } catch (NoSuchProviderException ex) {
                                    Logger.getLogger(Controller.class.getName()).log(Level.SEVERE, null, ex);
                                } catch (MessagingException ex) {
                                    Logger.getLogger(Controller.class.getName()).log(Level.SEVERE, null, ex);
                                } catch (NamingException ex) {
                                    Logger.getLogger(Controller.class.getName()).log(Level.SEVERE, null, ex);
                                }
                            }
                            
                            /* debug */
                            if ("on".equals(props.getProperty("debug"))) {
                                log.write("[" + ctime2 + "] Job Scheduler: Executed " + function + " \n");
                                log.flush();
                            }
                            c=0;
                        } catch (Exception ex) {
                            Logger.getLogger(Controller.class.getName()).log(Level.SEVERE, null, ex);
                        }
                        
                    } else {
                        c=0;
                    }
                    
                    if (c == 1) {
                        if ("on".equals(props.getProperty("debug"))) {
                            log.write("[" + ctime2 + "] Job Scheduler: Failed.\n");
                            log.flush();
                        }
                    }
                                      
                }
                /* Close Connections */
                cnR.close();
                cnM.close();
                Thread.sleep(5000);
                i++;
            }
        
        } else {
            if ("on".equals(props.getProperty("debug"))) {
                log.write("[" + ctime1 + "] ++ SystemLocal ++ Die angegebenen E-Mail Adressen mail.from oder mail.admin sind nicht valide.\n");
                log.flush();
            }
        }
        
        /* End of Main If */
        
        } else {
            System.err.println("Keine Konfigurationsdatei angegeben.");
            System.exit(2);
        }
        
    }
}
