package iamd.gedcom.datamodel;

import java.io.PrintStream;
import java.util.GregorianCalendar;

import iamd.gedcom.format.GedComNode;
import iamd.gedcom.format.GedComParseException;
import iamd.gedcom.format.GedComNode.GEDNodeIgnore;

@GEDNodeIgnore(propertyToIgnore = "NOTE")
public class DateTime extends GedComNode
{
    static public enum Uncertainty
    {
        CER, 
        AFT,
        ABT,
        BEF,
    }
    
    static public enum Month
    {
        JAN, FEB, MAR, APR, MAY, JUN, JUL, AUG, SEP, OCT, NOV, DEC;
        
        public String humanName()
        {
            return Messages.getString("DateTime." + this.name().toLowerCase()); //$NON-NLS-1$
        }
    }
    
    private Uncertainty uncertainty;

    public int day = -1;
    public Month month = null;
    public int year = -1;
    public int rangeMaxYear = -1;
    
    public int hour = -1, min = -1, sec = -1;
    
    public DateTime(String gedCode, Document document, String data) throws GedComParseException
    {
        super(gedCode, document);
        
        DateParser dateTokens = new DateParser(data, false);
        
        this.uncertainty  = dateTokens.uncertainty;
        this.day          = dateTokens.day;
        this.month        = dateTokens.month;
        this.year         = dateTokens.year;
        this.rangeMaxYear = dateTokens.rangeMaxYear;
    }

    public DateTime(Document document, String date) throws GedComParseException
    {
        super("DATE", document);
        
        DateParser dateTokens = new DateParser(date, true);
        
        this.uncertainty  = dateTokens.uncertainty;
        this.day          = dateTokens.day;
        this.month        = dateTokens.month;
        this.year         = dateTokens.year;
        this.rangeMaxYear = dateTokens.rangeMaxYear;
    }

    private DateTime(Document document)
    {
        super("DATE", document); //$NON-NLS-1$
    }

    @Override
    public GedComNode setGEDNode(String gedCode, String data) throws GedComParseException
    {
        if ("TIME".equals(gedCode)) //$NON-NLS-1$
        {
            this.setTime(data);
            
            return null;
        }
        
        return super.setGEDNode(gedCode, data);
    }

    public void setTime(String timeAsString) throws GedComParseException
    {
        TimeParser timeTokens = new TimeParser(timeAsString);
        
        this.hour = timeTokens.hour;
        this.min  = timeTokens.min;
        this.sec  = timeTokens.sec;
    }

    static public class TimeParser
    {
        final int hour, min, sec;
    
        public TimeParser(String timeAsString) throws GedComParseException
        {
            if (timeAsString == null)
                throw new GedComParseException("Could not parse time: " + timeAsString); //$NON-NLS-1$
            
            try
            {
                String[] timeTokens = timeAsString.split(":"); //$NON-NLS-1$
                
                if (timeTokens.length != 2 && timeTokens.length != 3)
                    throw new GedComParseException("Could not parse time: " + timeAsString); //$NON-NLS-1$
                
                if (timeTokens[0].length() > 2)
                    throw new GedComParseException("Could not parse time: " + timeAsString); //$NON-NLS-1$
                
                int hour = Integer.parseInt(timeTokens[0]);
        
                if (hour < 0 || hour >= 24)
                    throw new GedComParseException("Could not parse time: " + timeAsString); //$NON-NLS-1$
        
                if (timeTokens[1].length() != 2)
                    throw new GedComParseException("Could not parse time: " + timeAsString); //$NON-NLS-1$
                
                int min = Integer.parseInt(timeTokens[1]);
        
                if (min < 0 || min >= 60)
                    throw new GedComParseException("Could not parse time: " + timeAsString); //$NON-NLS-1$
        
                int sec = -1;
                
                if (timeTokens.length == 3)
                {
                    sec = Integer.parseInt(timeTokens[2]);
        
                    if (timeTokens[2].length() != 2)
                        throw new GedComParseException("Could not parse time: " + timeAsString); //$NON-NLS-1$
                    
                    if (sec < 0 || sec >= 60)
                        throw new GedComParseException("Could not parse time: " + timeAsString); //$NON-NLS-1$
                }
                
                this.hour = hour;
                this.min  = min;
                this.sec  = sec;
            }
            catch(NumberFormatException e)
            {
                throw new GedComParseException("Could not parse time: " + timeAsString); //$NON-NLS-1$
            }
        }
    }

    static public class DateParser
    {
        final public Uncertainty uncertainty;
        final public int day;
        final public Month month;
        final public int year, rangeMaxYear;

        public DateParser(String dateAsString, boolean humanMonths) throws GedComParseException
        {
            if (dateAsString == null)
                throw new GedComParseException("Could not parse date: " + dateAsString); //$NON-NLS-1$
            
            try
            {
                dateAsString = dateAsString.replaceAll("\\s+", " ").trim();
    
                Uncertainty uncertainty = null;
                
                for (Uncertainty u : Uncertainty.values())
                {
                    if (dateAsString.toUpperCase().startsWith(u.name().toUpperCase() + " "))
                    {
                        uncertainty = u;
                        
                        dateAsString = dateAsString.substring(4).trim();
                    }
                }
                
                if (dateAsString.endsWith("?"))
                {
                    dateAsString = dateAsString.substring(0, dateAsString.length() - 1).trim();
                    
                    uncertainty = Uncertainty.ABT;
                }
                
                this.uncertainty = uncertainty;
                
                int rangeMaxYear = -1;
                
                if (dateAsString.startsWith("BET "))
                {
                    int andIndex = dateAsString.indexOf("AND");
                    
                    if (andIndex == -1)
                        throw new GedComParseException("Could not parse date: " + dateAsString); //$NON-NLS-1$
                    
                    String prevDate = dateAsString.substring(4, andIndex).trim();
                    String nextDate = dateAsString.substring(andIndex + 4).trim();
                    
                    dateAsString = prevDate + "/" + nextDate;
                }

                if (dateAsString.matches(".*/\\d\\d\\d\\d"))
                {
                    rangeMaxYear = Integer.parseInt(dateAsString.substring(dateAsString.length() - 4)); 
                    
                    dateAsString = dateAsString.substring(0, dateAsString.length() - 5);
                }
                else if (dateAsString.matches(".*/\\d+"))
                {
                    int indexOfSlash = dateAsString.lastIndexOf("/");
                    
                    rangeMaxYear = Integer.parseInt(dateAsString.substring(indexOfSlash + 1)); 
                    
                    dateAsString = dateAsString.substring(0, indexOfSlash);
                }
                
                int year = -1;
                Month month = null;
                int day = -1;
    
                for (Month m : Month.values())
                {
                    String monthName = humanMonths ? m.humanName() : m.name();
                    
                    dateAsString = 
                        dateAsString.replace("/" + (m.ordinal() + 1) + "/", " " + monthName + " ") //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
                                    .replace("/0" + (m.ordinal() + 1) + "/", " " + monthName + " ") //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
                                    .trim();
                }
                
                String[] dateTokens = dateAsString.split(" "); //$NON-NLS-1$
                
                if (dateTokens.length < 1 || dateTokens.length > 3)
                    throw new GedComParseException("Could not parse date: " + dateAsString); //$NON-NLS-1$
                
                if (dateTokens.length > 0)
                    year = Integer.parseInt(dateTokens[dateTokens.length - 1]);
        
                if (dateTokens.length > 1)
                {
                    try
                    {
                        month = null;
                        
                        String monthToken = dateTokens[dateTokens.length - 2];
    
                        if (humanMonths)
                        {
                            for (Month m : Month.values())
                            {
                                if (m.humanName().toUpperCase().equals(monthToken.toUpperCase()))
                                {
                                    month = m;
                                    break;
                                }
                            }
                        }
                        else
                        {
                            month = Month.valueOf(monthToken.toUpperCase());
                        }
                        
                        if (month == null)
                            throw new GedComParseException("Could not parse date: " + dateAsString); //$NON-NLS-1$
                    }
                    catch(IllegalArgumentException e)
                    {
                        throw new GedComParseException("Could not parse date: " + dateAsString); //$NON-NLS-1$
                    }
                }
        
                if (dateTokens.length == 3)
                    day = Integer.parseInt(dateTokens[dateTokens.length - 3]);
                
                if (year > 2100 || ((day < 0 || day > 31) && day != -1))
                    throw new GedComParseException("Could not parse date: " + dateAsString); //$NON-NLS-1$
                
                this.day   = day;
                this.month = month;
                this.year  = year;

                if (this.year > 99 && rangeMaxYear >= 10 && rangeMaxYear < 100)
                    this.rangeMaxYear = (year / 100) * 100 + rangeMaxYear; 
                else if (this.year > 9 && rangeMaxYear >= 0 && rangeMaxYear < 10)
                    this.rangeMaxYear = (year / 10) * 10 + rangeMaxYear; 
                else
                    this.rangeMaxYear = rangeMaxYear;
            }
            catch(NumberFormatException e)
            {
                throw new GedComParseException("Could not parse date: " + dateAsString); //$NON-NLS-1$
            }
        }
    }
    
    @Override
    public String getData()
    {
        return dateToString(false);
    }
    
    @Override
    public void print(PrintStream out, int depth)
    {
        super.print(out, depth);
        
        if (this.hour != -1)
        {
            out.println((depth + 1) + " TIME " + timeToString()); //$NON-NLS-1$
        }
    }

    public String dateToString(boolean humanMonths)
    {
        return (this.uncertainty != null ? this.uncertainty.name() + " " : "") +                    //$NON-NLS-1$ //$NON-NLS-2$ 
                (this.month == null ? "" : (this.day == -1 ? "" : this.day + " ") +                 //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                    (humanMonths ? this.month.humanName() : this.month.name()) + " ") + this.year + //$NON-NLS-1$
                    (this.rangeMaxYear != -1 ? "/" + this.rangeMaxYear : "");                       //$NON-NLS-1$ //$NON-NLS-2$ 
    }

    public String timeToString()
    {
        if (this.hour == -1)
            return ""; //$NON-NLS-1$
        
        return this.hour + ":" +  //$NON-NLS-1$
                (this.min < 10 ? "0" : "") + this.min +   //$NON-NLS-1$ //$NON-NLS-2$
                (this.sec != -1 ? ":" + (this.sec < 10 ? "0" : "") + this.sec : ""); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
    }

    static public DateTime now(Document document)
    {
        DateTime thiz = new DateTime(document);
        
        GregorianCalendar date = new GregorianCalendar();
        
        thiz.day   = date.get(GregorianCalendar.DAY_OF_MONTH);
        thiz.month = Month.values()[date.get(GregorianCalendar.MONTH)];
        thiz.year  = date.get(GregorianCalendar.YEAR);

        thiz.hour = date.get(GregorianCalendar.HOUR_OF_DAY);
        thiz.min  = date.get(GregorianCalendar.MINUTE);
        thiz.sec  = date.get(GregorianCalendar.SECOND);
        
        return thiz;
    }

    public boolean isEmpty()
    {
        return this.uncertainty == null &&
                this.day == -1 &&
                this.month == null &&
                this.year == -1 &&
                this.hour == -1 &&
                this.min == -1 &&
                this.sec == -1;
    }
}
