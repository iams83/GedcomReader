package iamd.gedcom.ui.editors;

import iamd.gedcom.datamodel.Individual.Sex;
import iamd.gedcom.ui.Messages;
import iamd.ui.ComboBoxEditor;

@SuppressWarnings("serial")
public class SexEditor extends ComboBoxEditor<Sex>
{
    public SexEditor()
    {
        super((new Sex[] { null, Sex.F, Sex.M }));
    }

    @Override
    protected String valueTypeToString(Sex s)
    {
        if (s == null)
            return " ?"; //$NON-NLS-1$
        
        return s.symbol + (s == Sex.F ? Messages.getString("IndividualEditorPanel.female") : Messages.getString("IndividualEditorPanel.male")); //$NON-NLS-1$ //$NON-NLS-2$
    }
}