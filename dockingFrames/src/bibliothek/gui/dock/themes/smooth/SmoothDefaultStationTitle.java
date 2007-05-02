/**
 * Bibliothek - DockingFrames
 * Library built on Java/Swing, allows the user to "drag and drop"
 * panels containing any Swing-Component the developer likes to add.
 * 
 * Copyright (C) 2007 Benjamin Sigg
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * Benjamin Sigg
 * benjamin_sigg@gmx.ch
 * 
 * Wunderklingerstr. 59
 * 8215 Hallau
 * CH - Switzerland
 */


package bibliothek.gui.dock.themes.smooth;

import bibliothek.gui.dock.Dockable;
import bibliothek.gui.dock.title.DefaultStationTitle;
import bibliothek.gui.dock.title.DockTitleVersion;
import bibliothek.util.Colors;

/**
 * A station-title which smoothly changes its color from active to passive.
 * @author Benjamin Sigg
 *
 */
public class SmoothDefaultStationTitle extends DefaultStationTitle{
    /** the counter, tells where transition between active and passive stands. */
    private int current = 0;
    
    /**
     * Source for pulses for this title.
     */
    private SmoothChanger changer = new SmoothChanger(){
        @Override
        protected boolean isActive() {
            return SmoothDefaultStationTitle.this.isActive();
        }
        
        @Override
        protected void repaint( int current ) {
            SmoothDefaultStationTitle.this.current = current;
            SmoothDefaultStationTitle.this.updateColors();
        }
    };
    
    @Override
    public void setActive( boolean active ) {
        super.setActive(active);
        
        if( changer != null )
            changer.trigger();
    }
    
    /**
     * Constructs a new station title
     * @param dockable the owner of this title
     * @param origin the version which was used to create this title
     */
    public SmoothDefaultStationTitle( Dockable dockable, DockTitleVersion origin ) {
        super(dockable, origin);
    }
    
    /**
     * Gets the duration of one transition from active to passive
     * @return the duration
     */
    public int getDuration(){
        return changer.getDuration();
    }
    
    /**
     * Sets the duration of one transition from active to passive, or
     * in the other direction.
     * @param duration the duration
     */
    public void setDuration( int duration ){
        changer.setDuration( duration );
    }
        
    @Override
    protected void updateColors() {
        super.updateColors();
        
        if( changer != null ){
            int duration = getDuration();
            
            if( (isActive() && current != duration) || 
                (!isActive() && current != 0 )){
                
                double ratio = current / (double)duration;
                
                setForeground( Colors.between( getInactiveTextColor(), getActiveTextColor(), ratio ));
                setBackground( Colors.between( getInactiveColor(), getActiveColor(), ratio ) );
            }
        }
    }
}
