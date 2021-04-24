package net.minecraft.crash;

import java.io.PrintWriter;
import java.io.StringWriter;
import org.apache.commons.io.IOUtils;

public class CrashReport extends RuntimeException
{
    /**
	 * 
	 */
	private static final long serialVersionUID = 7671683602603126604L;

	/** Description of the crash report. */
    private final String description;

    /** The Throwable that is the "cause" for this crash and Crash Report. */
    private final Throwable cause;

    private CrashReport(String p_i1348_1_, Throwable p_i1348_2_)
    {
        this.description = p_i1348_1_;
        this.cause = p_i1348_2_;
    }

    /**
     * Returns the description of the Crash Report.
     */
    public String getMessage()
    {
        return this.description;
    }

    /**
     * Returns the Throwable object that is the cause for the crash and Crash Report.
     */
    public Throwable getCause()
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
        Throwable var3 = this.cause;

        if (var3.getMessage() == null)
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

            var3.setStackTrace(this.cause.getStackTrace());
        }

        String var4 = var3.toString();

        try
        {
            var1 = new StringWriter();
            var2 = new PrintWriter(var1);
            var3.printStackTrace(var2);
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
        var1.append("---- Boolcraft Crash Report ----\n");
        var1.append("Description: ");
        var1.append(this.description);
        var1.append("\n\n");
        var1.append(this.getCauseStackTraceOrString());
        return var1.toString();
    }

    /**
     * Creates a crash report for the exception
     */
    public static CrashReport makeCrashReport(Throwable cause, String desc)
    {
        if (cause instanceof CrashReport)
        {
            return (CrashReport)cause;
        }
        else
        {
            return new CrashReport(desc, cause);
        }
    }
}
