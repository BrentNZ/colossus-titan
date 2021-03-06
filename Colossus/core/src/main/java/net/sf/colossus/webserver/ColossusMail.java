package net.sf.colossus.webserver;


import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.logging.Logger;

import net.sf.colossus.webcommon.IColossusMail;


/**
 *  Encapsulates the way how the web server sends mail in some situations,
 *  so far only for registration procedure.
 *
 *  @author Clemens Katzer
 */
public class ColossusMail implements IColossusMail
{
    private static final Logger LOGGER = Logger.getLogger(ColossusMail.class
        .getName());

    // For sending the registration mail:
    private final String mailServer;
    private final String fromAddress;
    private final String fromName;
    private final String thisServer;
    private final String contactMail;
    private final String contactWWW;
    /**
     *  Whether or not to really send a mail. During development on PC I set
     *  this in the cf file to false, because I do not really have a mail
     *  server process running.
     */
    private final boolean reallyMail;
    private final String mailToFileName;
    private final File mailToFileFile;
    private final boolean mailToFileFlag;

    public ColossusMail(WebServerOptions options)
    {
        mailServer = options.getStringOption(WebServerConstants.optMailServer);
        fromAddress = options
            .getStringOption(WebServerConstants.optMailFromAddress);
        fromName = options.getStringOption(WebServerConstants.optMailFromName);
        thisServer = options
            .getStringOption(WebServerConstants.optMailThisServer);
        contactMail = options
            .getStringOption(WebServerConstants.optMailContactEmail);
        contactWWW = options
            .getStringOption(WebServerConstants.optMailContactWWW);
        reallyMail = options.getOption(WebServerConstants.optMailReallyMail);
        mailToFileName = options
            .getStringOption(WebServerConstants.optMailToFile);

        boolean success = false;

        File testFile = null;

        if (mailToFileName != null && !mailToFileName.equals(""))
        {
            try
            {
                testFile = new File(mailToFileName);
                File directory = testFile.getParentFile();
                if (!directory.exists() || !directory.canWrite())
                {
                    LOGGER.warning("Invalid mailFile '" + mailToFileName
                        + "':\n  '" + directory.getAbsolutePath()
                        + "'\ndoes not exist or is not a writable directory! "
                        + "Check the configuration.");
                }
                else
                {
                    PrintWriter mailToFileWriter = new PrintWriter(
                        new FileOutputStream(testFile, true));
                    mailToFileWriter.println("");
                    mailToFileWriter.println("WebServer started.");
                    mailToFileWriter.println("");
                    mailToFileWriter.close();
                    success = true;
                }
            }
            catch (IOException e)
            {
                LOGGER.warning("Exception while trying to write "
                    + "initial message to mail file: " + e);
            }

        }
        mailToFileFlag = success;
        mailToFileFile = testFile;
    }

    public String sendConfirmationMail(String username, String email,
        String confCode)
    {
        try
        {
            SmtpSimple smtp = new SmtpSimple();

            String subject = "Confirmation code for registration at "
                + thisServer;
            String message = "Hello " + username + ",\n\n"
                + "please use the following confirmation code\n\n    "
                + confCode + "\n\n" + "to complete your registration at the "
                + thisServer + "." + "\n\n\nWith Regards,\n\n"
                + "Clemens Katzer (administrator of this server)\n\n\n"
                + "\n-------------\n\n" + "NOTE:\n"
                + "If you didn't do anything related to a registration "
                + "at this server,\n"
                + "probably someone else used your email address\n"
                + "(accidentally or intentionally).\n\n"
                + "If you wish, you may report this to " + contactMail + ",\n"
                + "or go to " + contactWWW + " to contact us.\n\n\n--\n"
                + "PS: do not reply to this email - noone will read it...\n";

            if (reallyMail)
            {
                LOGGER.fine("ok, sending mail to " + username + " <" + email
                    + ">");

                smtp.sendEmail(mailServer, fromAddress, fromName, email,
                    username, subject, message);
            }

            if (mailToFileFlag)
            {
                PrintWriter mailOut = null;
                try
                {
                    mailOut = new PrintWriter(new FileOutputStream(
                        mailToFileFile, true));

                    mailOut
                        .println("\nI WOULD NOW SEND THE FOLLOWING MAIL:\n\n"
                            + "From: " + fromName + " <" + fromAddress + ">\n"
                            + "To: " + username + " <" + email + ">\n"
                            + "Subject: " + subject + "\n\n" + message
                            + "\nEND OF MAIL\n\n");
                }
                catch (IOException e)
                {
                    LOGGER.warning("Exception while) trying to write "
                        + "a mail for user '" + username + "' to mail file: "
                        + e);
                }
                finally
                {
                    if (mailOut != null)
                    {
                        mailOut.close();
                    }
                }
            }
        }
        catch (Exception ex)
        {
            LOGGER.severe("Exception during mail sending: " + ex);
            return "Sending mail failed - see log file!";
        }

        return null;
    }
}
