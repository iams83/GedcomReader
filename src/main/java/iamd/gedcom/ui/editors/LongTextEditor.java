package iamd.gedcom.ui.editors;

import java.lang.reflect.Field;

import iamd.gedcom.datamodel.Document;
import iamd.gedcom.datamodel.LongText;
import iamd.ui.AttributeEditorListener;
import iamd.ui.TextAreaEditor;

@SuppressWarnings("serial")
public class LongTextEditor extends TextAreaEditor
{
    private Object editingObject;
    private Field editingField;
    private Document model;
    
    public LongTextEditor(int numRows)
    {
        super(numRows);
        
        this.addAttributeEditionListener(new AttributeEditorListener()
        {
            @Override
            public void attributeModified(Object editingObject, Field editingField, Object value)
            {
                if (LongTextEditor.this.model == null ||
                    LongTextEditor.this.editingField == null ||
                    LongTextEditor.this.editingObject == null)
                {
                    return;
                }
                
                try
                {
                    LongText longText = null;
                    
                    String text = (String) value;
                    
                    if (text != null && !text.isEmpty())
                        longText = new LongText(LongTextEditor.this.editingField.getName(), model, text);
                    
                    LongTextEditor.this.editingField.set(LongTextEditor.this.editingObject, longText);
                }
                catch (IllegalArgumentException | IllegalAccessException e)
                {
                    throw new AssertionError(e);
                }
            }
        });
    }
    
    @Override
    public void bindValue(Object editingObject, String attribute)
    {
        this.editingObject = editingObject;
        
        try
        {
            this.editingField = this.editingObject.getClass().getDeclaredField(attribute);
            
            if (LongTextEditor.this.model == null ||
                LongTextEditor.this.editingField == null ||
                LongTextEditor.this.editingObject == null)
            {
                return;
            }

            if (this.editingField.getType() != LongText.class)
                throw new AssertionError("Given field should be a LongText.");
            
            LongText longText = (LongText) this.editingField.get(this.editingObject);
            
            if (longText != null)
                super.initializeValue(longText.getText());
            else
                super.initializeValue(null);
        }
        catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e)
        {
            throw new AssertionError(e);
        }
    }

    public void setDocument(Document model)
    {
        this.model = model;
    }
}
