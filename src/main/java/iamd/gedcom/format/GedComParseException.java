package iamd.gedcom.format;

@SuppressWarnings("serial")
public class GedComParseException extends Exception
{
    public GedComParseException(String message)
    {
        super(message);
    }

    public GedComParseException(Throwable e)
    {
        super(e);
    }
}
