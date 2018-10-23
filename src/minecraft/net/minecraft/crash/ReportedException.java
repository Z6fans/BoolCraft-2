package net.minecraft.crash;

public class ReportedException extends RuntimeException
{
	private static final long serialVersionUID = 6623792971934986491L;
	
	/** Instance of CrashReport. */
    private final CrashReport theReportedExceptionCrashReport;

    public ReportedException(CrashReport cr)
    {
        this.theReportedExceptionCrashReport = cr;
    }

    /**
     * Gets the CrashReport wrapped by this exception.
     */
    public CrashReport getCrashReport()
    {
        return this.theReportedExceptionCrashReport;
    }

    public Throwable getCause()
    {
        return this.theReportedExceptionCrashReport.getCrashCause();
    }

    public String getMessage()
    {
        return this.theReportedExceptionCrashReport.getDescription();
    }
}
