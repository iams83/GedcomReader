package iamd.gedcom.ui.editors;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridLayout;
import java.lang.reflect.Field;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;

import iamd.gedcom.datamodel.Bool;
import iamd.gedcom.datamodel.DateTime;
import iamd.gedcom.datamodel.Document;
import iamd.gedcom.datamodel.Event;
import iamd.gedcom.ui.Messages;
import iamd.gedcom.format.GedComParseException;
import iamd.gedcom.ui.EditorPanel;
import iamd.ui.AttributeBinder;
import iamd.ui.AttributeEditorListener;
import iamd.ui.BorderListPanelGenerator;
import iamd.ui.ComboBoxEditor;
import iamd.ui.TextLineEditor;

@SuppressWarnings("serial")
public class EventEditor extends JPanel
{
    final private AttributeBinder<Event> attributesBinder = new AttributeBinder<Event>()
    {
        @Override
        protected void initializeValue(Event value)
        {
            EventEditor.this.happenedEditor.initializeValue(EventEditor.this.happened = null);
            EventEditor.this.dateEditor    .initializeValue(EventEditor.this.date = ""); //$NON-NLS-1$
            EventEditor.this.timeEditor    .initializeValue(EventEditor.this.time = ""); //$NON-NLS-1$
            EventEditor.this.placeEditor   .initializeValue(EventEditor.this.place = ""); //$NON-NLS-1$
            
            if (value != null)
            {
                EventEditor.this.happenedEditor.initializeValue(
                        EventEditor.this.happened = 
                                (value.DATE != null || value.PLAC != null ? Bool.Y : value.happened));
                
                if (value.DATE != null)
                {
                    EventEditor.this.dateEditor.initializeValue(EventEditor.this.date = value.DATE.dateToString(true));
                    EventEditor.this.timeEditor.initializeValue(EventEditor.this.time = value.DATE.timeToString());
                }
                
                EventEditor.this.placeEditor.initializeValue(EventEditor.this.place = value.PLAC);
            }
        }
    };
    
    final private TextLineEditor dateEditor = new TextLineEditor()
    {
        @Override
        protected boolean validateCurrentValue(String dateAsString)
        {
            if (dateAsString == null || dateAsString.trim().isEmpty())
                return true;
            
            try
            {
                new DateTime.DateParser(dateAsString, true);
                
                return true;
            }
            catch(GedComParseException e)
            {
                return false;
            }
        }

        @Override
        protected String valueNotValidErrorMessage()
        {
            return Messages.getString("EventEditor.baddate");
        }
    };
    
    final private TextLineEditor timeEditor = new TextLineEditor()
    {
        @Override
        protected boolean validateCurrentValue(String timeAsString)
        {
            if (timeAsString == null || timeAsString.isEmpty())
                return true;

            try
            {
                new DateTime.TimeParser(timeAsString);
                
                return true;
            }
            catch(GedComParseException e)
            {
                return false;
            }
        }

        @Override
        protected String valueNotValidErrorMessage()
        {
            return Messages.getString("EventEditor.badtime");
        }
    };

    final private JPanel parent;
    final private String label;
    final private boolean showHappened;

    final private TextLineEditor placeEditor = new TextLineEditor();
    
    final private ComboBoxEditor<Bool> happenedEditor = new ComboBoxEditor<Bool>(new Bool[] { null, Bool.Y, Bool.N })
    {
        @Override
        protected String valueTypeToString(Bool s)
        {
            return s == null ? "" : (s == Bool.Y ? Messages.getString("EventEditor.yes") : Messages.getString("EventEditor.no")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        }
    };
    
    private boolean isUpdating = false;
    private Document document;
    private Bool happened;
    private String date, time, place;
    
    public EventEditor(String label, String gedCode, boolean showHappened, JPanel parent)
    {
        this.parent = parent;
        this.label = label;
        this.showHappened = showHappened;
        
        this.dateEditor.getComponent().setColumns(10);
        this.timeEditor.getComponent().setColumns(10);
        
        this.dateEditor.alignValueRight();
        this.timeEditor.alignValueRight();
        
        AttributeEditorListener<Object> attributesEditorListener = new AttributeEditorListener<Object>()
        {
            @Override
            public void attributeModified(Object editingObject, Field editingField, Object value)
            {
                if (EventEditor.this.isUpdating)
                    return;
                EventEditor.this.isUpdating = true;
                
                try
                {
                    DateTime date = null;
                    
                    if (EventEditor.this.date != null && !EventEditor.this.date.isEmpty())
                    {
                        try
                        {
                            date = new DateTime(EventEditor.this.document, EventEditor.this.date);
                            
                            if (EventEditor.this.time != null && !EventEditor.this.time.isEmpty())
                            {
                                date.setTime(EventEditor.this.time);
                            }
                        }
                        catch(GedComParseException e)
                        {
                            throw new AssertionError(e);
                        }
                    }
                    
                    String place = null;
                    
                    if (EventEditor.this.place != null && !EventEditor.this.place.isEmpty())
                        place = EventEditor.this.place;
                    
                    Event event = null;
                    
                    if (EventEditor.this.happened != null || date != null || place != null)
                    {
                        event = new Event(gedCode, EventEditor.this.document, 
                                EventEditor.this.happened == null ? null : EventEditor.this.happened.name());
                        
                        if (EventEditor.this.happened == Bool.Y)
                        {
                            event.DATE = date;
                            event.PLAC = place;
                            
                            if (event.DATE != null)
                            {
                                String dateAsString = event.DATE.dateToString(true);
                                String timeAsString = event.DATE.timeToString();
                                
                                if (dateAsString != null)
                                    EventEditor.this.dateEditor.initializeValue(dateAsString);
                                
                                if (timeAsString != null)
                                    EventEditor.this.timeEditor.initializeValue(timeAsString);
                            }
                        }
                    }
                    
                    EventEditor.this.attributesBinder.setBindedValue(event);
                }
                finally
                {
                    EventEditor.this.isUpdating = false;
                }
            }
        };

        AttributeEditorListener<Bool> happenedValueEditorListener = new AttributeEditorListener<Bool>()
        {
            @Override
            public void attributeModified(Object editingObject, Field editingField, Bool value)
            {
                if (EventEditor.this.happened != Bool.Y)
                {
                    EventEditor.this.dateEditor.initializeValue(
                            EventEditor.this.date = null);
                    
                    EventEditor.this.timeEditor.initializeValue(
                            EventEditor.this.time = null);
                    
                    EventEditor.this.placeEditor.initializeValue(
                            EventEditor.this.place = null);
                }
                
                attributesEditorListener.attributeModified(editingObject, editingField, value);
                
                EventEditor.this.reloadComponents();
            }
        };

        AttributeEditorListener<String> eventDetailValueEditorListener = new AttributeEditorListener<String>()
        {
            @Override
            public void attributeModified(Object editingObject, Field editingField, String value)
            {
                if ((EventEditor.this.date != null && !EventEditor.this.date.isEmpty()) ||
                    (EventEditor.this.time != null && !EventEditor.this.time.isEmpty()) ||
                    (EventEditor.this.place != null && !EventEditor.this.place.isEmpty()))
                {
                    EventEditor.this.happenedEditor.initializeValue(
                            EventEditor.this.happened = Bool.Y);
                }
                
                attributesEditorListener.attributeModified(editingObject, editingField, value);
            }
        };
        
        this.dateEditor.bindValue(this, "date"); //$NON-NLS-1$
        this.timeEditor.bindValue(this, "time"); //$NON-NLS-1$
        this.placeEditor.bindValue(this, "place"); //$NON-NLS-1$
        this.happenedEditor.bindValue(this, "happened"); //$NON-NLS-1$
        
        this.dateEditor .addAttributeEditionListener(eventDetailValueEditorListener);
        this.timeEditor .addAttributeEditionListener(eventDetailValueEditorListener);
        this.placeEditor.addAttributeEditionListener(eventDetailValueEditorListener);
        this.happenedEditor.addAttributeEditionListener(happenedValueEditorListener);
        
        this.reloadComponents();
    }
    
    private void reloadComponents()
    {
        this.removeAll();
        
        boolean showExpanded = !this.showHappened || this.happened == Bool.Y;
        
        this.setLayout(new GridLayout(showExpanded ? 4 : 1, 2));
        
        BorderListPanelGenerator happenedPanel = new BorderListPanelGenerator(BorderLayout.EAST);
        happenedPanel.add(this.happenedEditor);
        
        BorderListPanelGenerator datePanel = new BorderListPanelGenerator(BorderLayout.EAST);
        datePanel.add(this.dateEditor);
        
        BorderListPanelGenerator timePanel = new BorderListPanelGenerator(BorderLayout.EAST);
        timePanel.add(this.timeEditor);
        
        BorderListPanelGenerator placePanel = new BorderListPanelGenerator(BorderLayout.WEST);
        placePanel.add(EditorPanel.newContainerBackgroundLabel(Messages.getString("EventEditor.place"))); //$NON-NLS-1$

        if (this.showHappened)
            this.add(happenedPanel.extractPanel(EditorPanel.newContainerBackgroundLabel(this.label)));
        else
            this.add(EditorPanel.newContainerBackgroundLabel(this.label));

        if (showExpanded)
        {
            this.add(indentLeftPanel(datePanel.extractPanel(EditorPanel.newContainerBackgroundLabel(Messages.getString("EventEditor.date"))))); //$NON-NLS-1$
            this.add(indentLeftPanel(timePanel.extractPanel(EditorPanel.newContainerBackgroundLabel(Messages.getString("EventEditor.time"))))); //$NON-NLS-1$
            this.add(indentLeftPanel(placePanel.extractPanel(this.placeEditor)));
        }
        
        this.invalidate();
        this.updateUI();

        this.parent.invalidate();
        this.parent.updateUI();
    }

    static private Component indentLeftPanel(JPanel panel)
    {
        panel.setBorder(new EmptyBorder(0, 20, 0, 0));
        return panel;
    }

    public void setDocument(Document document)
    {
        this.document = document;
    }
    
    public void addAttributeEditionListener(AttributeEditorListener<Event> attributeEditorListener)
    {
        this.attributesBinder.addAttributeEditionListener(attributeEditorListener);
    }

    public void bindValue(Object editingObject, String editingField)
    {
        this.attributesBinder.bindValue(editingObject, editingField);
        
        SwingUtilities.invokeLater(new Runnable()
        {
            @Override
            public void run()
            {
                EventEditor.this.reloadComponents();
            }
        });
    }

}
