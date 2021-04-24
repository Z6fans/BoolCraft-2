package net.minecraft.world;

public class SessionLockException extends Exception
{
	private static final long serialVersionUID = 7816402061998464647L;

	public SessionLockException(String message)
    {
        super(message);
    }
}
