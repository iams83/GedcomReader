package iamd.gedcom.ui;

import java.awt.Color;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;

@SuppressWarnings("serial")
public class EditorPanel extends JPanel
{

    protected JLabel newJLabel(String string)
    {
        JLabel label = new JLabel(string);
        label.setBackground(new Color(0, 0, 0, 0));
        return label;
    }

    protected JTextField newReadonlyJTextField(String string)
    {
        JTextField textField = new JTextField(string);
        textField.setBackground(new Color(0, 0, 0, 0));
        textField.setBorder(new EtchedBorder(EtchedBorder.LOWERED));
        textField.setEditable(false);
        return textField;
    }

    protected JComponent createTopBorder(JComponent component)
    {
        component.setBorder(new EmptyBorder(6, 0, 0, 0));
        component.setBackground(new Color(0, 0, 0, 0));
        return component;
    }

}
