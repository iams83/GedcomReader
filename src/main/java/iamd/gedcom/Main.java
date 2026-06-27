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
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        }
        catch (ClassNotFoundException | InstantiationException
                | IllegalAccessException | UnsupportedLookAndFeelException e)
        {
            e.printStackTrace(System.err);
        }
        
        new MainWindow();
    }
}
