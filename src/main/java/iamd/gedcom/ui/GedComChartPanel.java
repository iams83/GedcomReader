package iamd.gedcom.ui;

import iamd.ui.ChartPanel;
import iamd.ui.ChartPanelListener;

import java.awt.Color;
import java.awt.Font;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collection;

import iamd.gedcom.datamodel.Family;
import iamd.gedcom.datamodel.Individual;
import iamd.gedcom.datamodel.Individual.FamilyChildRelationship;
import iamd.gedcom.datamodel.Individual.Sex;

@SuppressWarnings("serial")
public class GedComChartPanel extends ChartPanel<GedComChartArc>
{
    enum ChartType
    {
        DescendantChart,
        ParentChart,
        TwoWayChart
    }

    private static final int MAX_DEPTH = 10;
    private static final int DESCENDANT_MAX_DEPTH = 6;
    
    final private Color[] colors = new Color[] 
    {
        new Color(186,225,255),
        new Color(255,179,186),
        new Color(255,223,186),
        new Color(255,255,186),
        new Color(186,255,201),
    };
    
    final private Color[] partnerColors = colors; 
    
    private ChartType type = ChartType.TwoWayChart;
    
    private Individual selectedIndividual;
    
    public GedComChartPanel()
    {
        this.setBackground(Color.white);
        
        this.addChartPanelListener(new ChartPanelListener<GedComChartArc>()
        {
            @Override
            public void mouseClicked(MouseEvent e, GedComChartArc arc)
            {
            }
            
            @Override
            public void mouseMoved(MouseEvent e, GedComChartArc arc)
            {
                GedComChartPanel.this.repaint();
            }
            
            @Override
            public void mouseExited(MouseEvent e, GedComChartArc arc)
            {
                arc.setFillingColor(arc.getColor());
                
                GedComChartPanel.this.repaint();
            }
            
            @Override
            public void mouseEntered(MouseEvent e, GedComChartArc arc)
            {
                arc.setFillingColor(Color.red);
                
                GedComChartPanel.this.repaint();
            }
        });
    }
    
    public void setModel(Family family)
    {
        if (family == null)
            this.setModel((Individual) null);
        else
            this.setModel(family.getMember());
    }
    
    public void setModel(Individual individual)
    {
        this.selectedIndividual = individual;
        
        this.reloadModel();
    }
    
    public void reloadModel()
    {
        this.clearArcs();

        if (this.selectedIndividual == null)
            return;
        
        GedComChartArc individualArc = new GedComChartArc(50, 120, - Math.PI / 2, 2 * Math.PI, this.selectedIndividual);
        
        individualArc.setFont(this.getFont().deriveFont(Font.BOLD));
        individualArc.setColor(this.getPartnerColor(0));
        
        this.addArc(individualArc);

        if (this.type == ChartType.TwoWayChart)
        {
            this.createParentChartArc(this.selectedIndividual, 0, Math.PI, 2 * Math.PI, 50, false, MAX_DEPTH);

            this.createDescendantChartArcs(this.selectedIndividual, 0, 0.0, Math.PI, DESCENDANT_MAX_DEPTH);
        }
        else if (this.type == ChartType.ParentChart)
        {
            this.createParentChartArc(this.selectedIndividual, 0, Math.PI / 2, 5 * Math.PI / 2, 50, true, MAX_DEPTH);

            ArrayList<Individual> children = new ArrayList<>();
            
            for (Family family : this.selectedIndividual.getFamilies())
                children.addAll(family.getChildren());
            
            if (children != null)
            {
                int numChilds = children.size();
                
                int currentChild = 0;
                
                for (Individual child : children)
                {
                    GedComChartArc childArc = new GedComChartArc(20, 50, 
                            currentChild * 2 * Math.PI / numChilds, 
                            2 * Math.PI / numChilds, child);
                    
                    childArc.setFont(this.getFont());
                    childArc.setText1(Sex.toCharSymbol(child.SEX));
                    childArc.setText2(null);
                    childArc.setColor(colors[4]);
                    
                    this.addArc(childArc);
                    
                    currentChild ++;
                }
            }
        }
        else if (this.type == ChartType.DescendantChart)
        {
            this.createDescendantChartArcs(this.selectedIndividual, 0, - Math.PI / 2, 3 * Math.PI / 2, DESCENDANT_MAX_DEPTH);

            double minExtent = - Math.PI / 2;
            double maxExtent = 2 * Math.PI + minExtent;
            
            Collection<FamilyChildRelationship> familyChilds = this.selectedIndividual.getParentFamilies();
            
            int nParentFamily = 0;
            
            for (FamilyChildRelationship familyChild : familyChilds)
            {
                Family parentFamily = familyChild.family;
                
                double minExtent1 = minExtent +  nParentFamily      * (maxExtent - minExtent) / familyChilds.size();
                double maxExtent1 = minExtent + (nParentFamily + 1) * (maxExtent - minExtent) / familyChilds.size();

                double minExtent0 = familyChilds.size() == 1 ? minExtent1 : minExtent1 + (maxExtent1 - minExtent1) * 0.015;
                double maxExtent0 = familyChilds.size() == 1 ? maxExtent1 : maxExtent1 - (maxExtent1 - minExtent1) * 0.015;
                
                Individual parent1 = parentFamily.getSpouse1();
                Individual parent2 = parentFamily.getSpouse2();
                
                if (parent1 != null)
                {
                    GedComChartArc parent1Arc = new GedComChartArc(20, 45, 
                            minExtent0 + (maxExtent0 - minExtent0) / 4, 
                            (maxExtent0 - minExtent0) / 2, parent1);
                    
                    parent1Arc.setFont(this.getFont());
                    parent1Arc.setText1(Sex.toCharSymbol(parent1.SEX));
                    parent1Arc.setText2(null);
                    parent1Arc.setColor(colors[4]);
                    
                    this.addArc(parent1Arc);
                }

                if (parent2 != null)
                {
                    GedComChartArc parent2Arc = new GedComChartArc(20, 45, 
                            maxExtent0 - (maxExtent0 - minExtent0) / 4, 
                            (maxExtent0 - minExtent0) / 2, parent2);
                    
                    parent2Arc.setFont(this.getFont());
                    parent2Arc.setText1(Sex.toCharSymbol(parent2.SEX));
                    parent2Arc.setText2(null);
                    parent2Arc.setColor(colors[4]);
                    
                    this.addArc(parent2Arc);
                }
                
                nParentFamily ++;
            }
        }
        
        this.initializeBoundingBox();
        
        this.repaint();
    }

    private Color getPartnerColor(int level)
    {
        while (level < 0)
            level += this.colors.length;
        
        return this.partnerColors[level % this.partnerColors.length];
    }


    private Color getColor(int level)
    {
        while (level < 0)
            level += this.colors.length;
        
        return this.colors[level % this.colors.length];
    }

    private void createParentChartArc(Individual individual, int level, double minExtent, double maxExtent, double radiusOffset, boolean circleChart, int maxDepth)
    {
        if (maxDepth <= 0)
            return;
        
        if (level != 0)
        {
            GedComChartArc individualArc = new GedComChartArc(
                    radiusOffset + level * 80, 
                    radiusOffset + level * 80 + 70, 
                    (minExtent + maxExtent) / 2, 
                    (maxExtent - minExtent), individual);

            individualArc.setFont(this.getFont());
            individualArc.setColor(getColor(- level));
            
            addArc(individualArc);
        }
        
        Collection<FamilyChildRelationship> familyChilds = individual.getParentFamilies();
        
        int nParentFamily = 0;
        
        for (FamilyChildRelationship familyChild : familyChilds)
        {
            Family parentFamily = familyChild.family;
            
            double minExtent1 = minExtent +  nParentFamily      * (maxExtent - minExtent) / familyChilds.size();
            double maxExtent1 = minExtent + (nParentFamily + 1) * (maxExtent - minExtent) / familyChilds.size();
            
            double minExtent0 = circleChart && familyChilds.size() == 1 ? minExtent1 : minExtent1 + (maxExtent1 - minExtent1) * 0.01;
            double maxExtent0 = circleChart && familyChilds.size() == 1 ? maxExtent1 : maxExtent1 - (maxExtent1 - minExtent1) * 0.01;
            
            Individual spouse1 = parentFamily.getSpouse1();
            Individual spouse2 = parentFamily.getSpouse2();
            
            if (spouse1 != null)
                createParentChartArc(spouse1, level + 1, minExtent0, (minExtent0 + maxExtent0) / 2, radiusOffset, circleChart, maxDepth - 1);

            if (spouse2 != null)
                createParentChartArc(spouse2, level + 1, (minExtent0 + maxExtent0) / 2, maxExtent0, radiusOffset, circleChart, maxDepth - 1);
            
            nParentFamily ++;
        }
    }

    private void createDescendantChartArcs(Individual individual, int level, double minExtent, double maxExtent, int maxDepth)
    {
        if (maxDepth <= 0)
            return;
        
        if (level != 0)
        {
            GedComChartArc individualArc = new GedComChartArc(
                     50 + level * 150, 
                    120 + level * 150, 
                    (minExtent + maxExtent) / 2, 
                    (maxExtent - minExtent), individual);
            
            individualArc.setFont(this.getFont());
            individualArc.setColor(getColor(level));
            
            this.addArc(individualArc);
        }
        
        Collection<Family> families = individual.getFamilies();
        
        int nFamily = 0;
        
        for (Family family : families)
        {
            double minExtent0 = minExtent +  nFamily      * (maxExtent - minExtent) / families.size();
            double maxExtent0 = minExtent + (nFamily + 1) * (maxExtent - minExtent) / families.size();

            double minExtent1 = families.size() == 1 ? minExtent0 : minExtent0 + (maxExtent0 - minExtent0) * 0.015;
            double maxExtent1 = families.size() == 1 ? maxExtent0 : maxExtent0 - (maxExtent0 - minExtent0) * 0.015;
            
            Individual partner = family.getSpouse(individual);
            
            if (partner != null)
            {
                GedComChartArc partnerArc = new GedComChartArc(
                        120 + level * 150, 
                        190 + level * 150, 
                        (minExtent1 + maxExtent1) / 2, 
                        (maxExtent1 - minExtent1), partner);
                
                partnerArc.setFont(this.getFont());
                partnerArc.setColor(this.getPartnerColor(level));
                
                this.addArc(partnerArc);
            }
            
            Collection<Individual> children = family.getChildren();
            
            int currentChild = 0;
            
            for (Individual child : children)
            {
                createDescendantChartArcs(child, level + 1, 
                        minExtent1 +  currentChild      * (maxExtent1 - minExtent1) / children.size(), 
                        minExtent1 + (currentChild + 1) * (maxExtent1 - minExtent1) / children.size(), maxDepth - 1);
                
                currentChild ++;
            }
            
            nFamily ++;
        }
    }


    public void setChartType(ChartType type)
    {
        this.type = type;
        
        this.reloadModel();
    }

    public ChartType getType()
    {
        return this.type;
    }

    public void setFont(Font font)
    {
        super.setFont(font);
        
        this.reloadModel();
    }

}
