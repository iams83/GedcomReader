package iamd.gedcom;

import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import iamd.gedcom.ui.MainWindow;

public class Main
{
    static public void main(String[] args)
    {
        try
        {
            UIManager.setLookAndFeel("com.sun.java.swing.plaf.windows.WindowsLookAndFeel");
        }
        catch (ClassNotFoundException e)
        {
            e.printStackTrace(System.err);
        }
        catch (InstantiationException e)
        {
            e.printStackTrace(System.err);
        }
        catch (IllegalAccessException e)
        {
            e.printStackTrace(System.err);
        }
        catch (UnsupportedLookAndFeelException e)
        {
            e.printStackTrace(System.err);
        }
        
        new MainWindow();
    }
}
