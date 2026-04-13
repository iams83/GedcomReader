package iamd.gedcom.ui;

import java.awt.Color;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

@SuppressWarnings("serial")
public class EditorPanel extends JPanel
{

    protected JLabel newJLabel(String string)
    {
        JLabel label = new JLabel(string);
        label.setBackground(new Color(0, 0, 0, 0));
        return label;
    }

    protected JComponent createTopBorder(JComponent component)
    {
        component.setBorder(new EmptyBorder(6, 0, 0, 0));
        component.setBackground(new Color(0, 0, 0, 0));
        return component;
    }

}
