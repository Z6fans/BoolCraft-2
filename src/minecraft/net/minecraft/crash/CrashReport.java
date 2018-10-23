package net.minecraft.crash;

import java.io.PrintWriter;
import java.io.StringWriter;
import org.apache.commons.io.IOUtils;

public class CrashReport
{
    /** Description of the crash report. */
    private final String description;

    /** The Throwable that is the "cause" for this crash and Crash Report. */
    private final Throwable cause;

    public CrashReport(String p_i1348_1_, Throwable p_i1348_2_)
    {
        this.description = p_i1348_1_;
        this.cause = p_i1348_2_;
    }

    /**
     * Returns the description of the Crash Report.
     */
    public String getDescription()
    {
        return this.description;
    }

    /**
     * Returns the Throwable object that is the cause for the crash and Crash Report.
     */
    public Throwable getCrashCause()
    {
        return this.cause;
    }

    /**
     * Gets the stack trace of the Throwable that caused this crash report, or if that fails, the cause .toString().
     */
    private String getCauseStackTraceOrString()
    {
        StringWriter var1 = null;
        PrintWriter var2 = null;
        Object var3 = this.cause;

        if (((Throwable)var3).getMessage() == null)
        {
            if (var3 instanceof NullPointerException)
            {
                var3 = new NullPointerException(this.description);
            }
            else if (var3 instanceof StackOverflowError)
            {
                var3 = new StackOverflowError(this.description);
            }
            else if (var3 instanceof OutOfMemoryError)
            {
                var3 = new OutOfMemoryError(this.description);
            }

            ((Throwable)var3).setStackTrace(this.cause.getStackTrace());
        }

        String var4 = ((Throwable)var3).toString();

        try
        {
            var1 = new StringWriter();
            var2 = new PrintWriter(var1);
            ((Throwable)var3).printStackTrace(var2);
            var4 = var1.toString();
        }
        finally
        {
            IOUtils.closeQuietly(var1);
            IOUtils.closeQuietly(var2);
        }

        return var4;
    }

    /**
     * Gets the complete report with headers, stack trace, and different sections as a string.
     */
    public String getCompleteReport()
    {
        StringBuilder var1 = new StringBuilder();
        var1.append("---- Minecraft Crash Report ----\n");
        var1.append("Description: ");
        var1.append(this.description);
        var1.append("\n\n");
        var1.append(this.getCauseStackTraceOrString());
        return var1.toString();
    }

    /**
     * Creates a crash report for the exception
     */
    public static CrashReport makeCrashReport(Throwable p_85055_0_, String p_85055_1_)
    {
        CrashReport var2;

        if (p_85055_0_ instanceof ReportedException)
        {
            var2 = ((ReportedException)p_85055_0_).getCrashReport();
        }
        else
        {
            var2 = new CrashReport(p_85055_1_, p_85055_0_);
        }

        return var2;
    }
}
