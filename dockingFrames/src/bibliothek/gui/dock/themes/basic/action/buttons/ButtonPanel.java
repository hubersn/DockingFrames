package bibliothek.gui.dock.themes.basic.action.buttons;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JPanel;

import bibliothek.gui.DockController;
import bibliothek.gui.DockUI;
import bibliothek.gui.Dockable;
import bibliothek.gui.dock.action.DefaultDockActionSource;
import bibliothek.gui.dock.action.DockAction;
import bibliothek.gui.dock.action.DockActionSource;
import bibliothek.gui.dock.action.actions.SimpleMenuAction;
import bibliothek.gui.dock.action.view.ActionViewConverter;
import bibliothek.gui.dock.action.view.ViewTarget;
import bibliothek.gui.dock.event.DockActionSourceListener;
import bibliothek.gui.dock.event.DockHierarchyEvent;
import bibliothek.gui.dock.event.DockHierarchyListener;
import bibliothek.gui.dock.event.IconManagerListener;
import bibliothek.gui.dock.themes.basic.action.BasicTitleViewItem;
import bibliothek.gui.dock.title.DockTitle.Orientation;

/**
 * A panel showing some {@link bibliothek.gui.dock.action.DockAction}s. The
 * panel can show a menu for actions which to do not have enough space. Clients
 * using this panel should call {@link #getPreferredSizes()} to get a list of 
 * possible sizes of this panel, and then {@link #setVisibleActions(int)}
 * in order to use one size. This panel will {@link #revalidate()} itself 
 * whenever the number of actions changes.  
 * @author Benjamin Sigg
 */
public class ButtonPanel extends JPanel{
	/** how to layout the panel */
	private Orientation orientation = Orientation.FREE_HORIZONTAL;
	
	/** the Dockable for which the actions are shown */
	private Dockable dockable;
	/** the list of actions to show */
	private DockActionSource source;
	
	/** a listener to {@link #source} and the {@link bibliothek.gui.dock.util.IconManager} */
	private Listener listener = new Listener();
	
	/** The list of actions which are currently known */
	private List<ActionItem> actions = new ArrayList<ActionItem>();
    
    /** the list of actions shown in the menu */
    private DefaultDockActionSource menuSource;
    /** a button used to open the menu */
    private BasicTitleViewItem<JComponent> menuItem;
    /** an action representing the menu */
    private SimpleMenuAction menuAction;
    
    /** the number of actions visible as button, might be -1 to indicate the the value is unknown */
    private int visibleActions = -1;
    
	/**
	 * Creates a new panel
	 * @param menu whether a menu should be used or not
	 */
	public ButtonPanel( boolean menu ){
		setLayout( null );
		setOpaque( false );
		
		if( menu ){
			menuSource = new DefaultDockActionSource();
			menuAction = new SimpleMenuAction( menuSource );
		}
		
		setFocusable( false );
		setFocusTraversalPolicyProvider( true );
		setFocusTraversalPolicy( new ContainerOrderFocusTraversalPolicy() );
	}
	
    /**
     * Creates a new item for <code>action</code> which will be shown on this panel. The default
     * implementation will use the {@link ActionViewConverter} of <code>dockable</code> to create the
     * view. The {@link ViewTarget} is set to {@link ViewTarget#TITLE}. Subclasses may overridde this 
     * method to create custom views or use other settings.
     * @param action The action which will be triggered by the button
     * @param dockable The {@link Dockable} which will be affected by the action
     * @return the new graphical representation of the action 
     */
    protected BasicTitleViewItem<JComponent> createItemFor( DockAction action, Dockable dockable ){
    	return dockable.getController().getActionViewConverter().createView( 
    			action, ViewTarget.TITLE, dockable );
    }
	
    /**
     * Gets the number of items which are shown on this panel.
     * @return the number of items
     */
    public int getItemCount(){
    	int count = 0;
    	for( ActionItem entry : actions ){
    		if( entry.item != null )
    			count++;
    	}
    	return count;
    }
    
    /**
     * Changes the orientation of this panel.
     * @param orientation the new orientation
     */
    public void setOrientation( Orientation orientation ){
    	if( this.orientation != orientation ){
    		this.orientation = orientation;
    		
    		if( menuItem != null )
    			menuItem.setOrientation( orientation );
    		
    		for( ActionItem item : actions )
    			if( item.item != null )
    				item.item.setOrientation( orientation );
    		
    		revalidate();
    	}
	}
    
    /**
     * Gets the orientation of this panel. The orientation tells whether the
     * buttons have to be layed out horizontally or vertically.
     * @return the orientation
     */
    public Orientation getOrientation(){
		return orientation;
	}
    
    /**
     * Changes the content which is shown.
     * @param dockable the Dockable for which the actions are shown, can be <code>null</code>
     */
    public void set( Dockable dockable ){
    	if( dockable == null )
    		set( null, null );
    	else
    		set( dockable, dockable.getGlobalActionOffers());
    }
    
    /**
     * Changes the content which is shown.
     * @param dockable the Dockable for which the actions are shown, can be <code>null</code>
     * @param source the list of actions, can be <code>null</code>
     * @throws IllegalArgumentException if <code>dockable</code> or <code>source</code>
     * is <code>null</code> while the other is not <code>null</code>.
     */
    public void set( Dockable dockable, DockActionSource source ){
    	set( dockable, source, false );
    }
    
    /**
     * Changes the content which is shown, ensures that the current properties
     * are used.
     */
    private void set(){
    	set( dockable, source, true );
    }

    /**
     * Changes the content which is shown.
     * @param dockable the Dockable for which the actions are shown, can be <code>null</code>
     * @param source the list of actions, can be <code>null</code>
     * @param force if <code>true</code>, then no optimations are allowed
     * @throws IllegalArgumentException if <code>dockable</code> or <code>source</code>
     * is <code>null</code> while the other is not <code>null</code>.
     */
    public void set( Dockable dockable, DockActionSource source, boolean force ){
    	if( (source == null) != (dockable == null) )
    		throw new IllegalArgumentException( "Either both arguments are null, or none" );
    	
    	if( force || this.source != source || this.dockable != dockable ){
    		if( this.source != null ){
    			// remove old items
    			removeAll();
    			
    			for( ActionItem item : actions ){
    				if( item.item != null ){
    					item.item.unbind();
    					item.action.unbind( this.dockable );
    				}
    			}
    			
    			if( menuAction != null ){
	    			if( menuItem != null ){
	    				menuItem.unbind();
	    				menuItem = null;
	    			}
	    			
	    			menuAction.unbind( this.dockable );
	    			menuSource.removeAll();
    			}
    			
    			actions.clear();
    			this.source.removeDockActionSourceListener( listener );
    		}
    		
    		this.source = source;
    		this.dockable = dockable;
    		
    		listener.setDockable( dockable );
    		
    		if( source != null ){
    			if( menuAction != null ){
	    			menuAction.bind( dockable );
	    			
	    			if( dockable.getController() != null ){
		    			menuItem = createItemFor( menuAction, dockable );
		    			if( menuItem != null ){
			    			menuItem.setOrientation( orientation );
			    			menuItem.setForeground( getForeground() );
			    			menuItem.setBackground( getBackground() );
			    			menuItem.bind();
		    			}
	    			}
    			}
    			
    			source.addDockActionSourceListener( listener );
    			for( DockAction action : source ){
    				ActionItem actionItem = new ActionItem();
    				actionItem.action = action;
    				actions.add( actionItem );
    				if( dockable.getController() != null ){
	    				BasicTitleViewItem<JComponent> item = createItemFor( action, dockable );
	    				if( item != null ){
	    					action.bind( dockable );
	    					item.bind();
	    					actionItem.item = item;
	    					
	    					item.setOrientation( orientation );
	    					item.setForeground( getForeground() );
	    					item.setBackground( getBackground() );
	    				}
    				}
    			}
    		}
        	
    		
    		visibleActions = actions.size();
    		resetVisibleActions();
    		
        	revalidate();
    	}
	}
    
    
    @Override
    public void setForeground( Color fg ) {
        super.setForeground( fg );
        
        if( menuItem != null )
            menuItem.setForeground( fg );        
        if( actions != null ){
	        for( ActionItem item : actions ){
	        	if( item.item != null )
	        		item.item.setForeground( fg );
	        }
        }
    }
    
    @Override
    public void setBackground( Color bg ) {
        super.setBackground( bg );
        
        if( menuItem != null )
        	menuItem.setBackground( bg );
        
        if( actions != null ){
	        for( ActionItem item : actions ){
	        	if( item.item != null )
	        		item.item.setBackground( bg );
	        }
        }
    }
    
    @Override
    public void updateUI() {
        super.updateUI();
        
        // the children of this panel must have the same colors as
        // this panel, so wait until the updateUI reached them as well,
        // and then reset the colors to be sure
        EventQueue.invokeLater( new Runnable(){
            public void run() {
                setForeground( getForeground() );
                setBackground( getBackground() );
            }
        });
    }
    
	@Override
	public Dimension getMinimumSize(){
		if( actions.isEmpty() )
			return new Dimension( 0, 0 );
		
		if( menuItem == null )
			return getPreferredSize();
		
		return menuItem.getItem().getMinimumSize();
	}
	
	/**
	 * Computes the preferred sizes of this panel. Dimension <code>result[n]</code> 
	 * would be the size required if <code>n</code> actions are shown. The number
	 * <code>n</code> should be used for calling {@link #setVisibleActions(int)}.
	 * @return An array of the size of the number of available actions +1.
	 * @see #setVisibleActions(int)
	 */
	public Dimension[] getPreferredSizes(){
		if( actions.isEmpty() )
			return new Dimension[]{ new Dimension( 0, 0 ) };
		
		if( menuAction == null )
			return new Dimension[]{ getPreferredSize() };
		
		Dimension current = new Dimension( 0, 0 );
		Dimension menuPreferred = menuItem == null ?
				new Dimension( 0, 0 ) : menuItem.getItem().getPreferredSize();

		Dimension[] results = new Dimension[ actions.size()+1 ];
		results[0] = new Dimension( menuPreferred );
				
		for( int i = 0, n = actions.size(); i<n; i++ ){
			BasicTitleViewItem<JComponent> item = actions.get( i ).item;
			if( item != null ){
				Dimension preferred = item.getItem().getPreferredSize();
				if( orientation.isHorizontal() ){
					current.width += preferred.width;
					current.height = Math.max( current.height, preferred.height );
				}
				else{
					current.width = Math.max( current.width, preferred.width );
					current.height += preferred.height;
				}
			}
			
			Dimension result = new Dimension( current );
			
			if( i+1 < n ){
				if( orientation.isHorizontal() ){
					result.width += menuPreferred.width;
					result.height = Math.max( result.height, menuPreferred.height );
				}
				else{
					result.width = Math.max( result.width, menuPreferred.width );
					result.height += menuPreferred.height;
				}
			}
			
			results[i+1] = result; 
		}
		
		Insets insets = getInsets();
		for( Dimension dimension : results ){
			dimension.width += insets.left + insets.right;
			dimension.height += insets.top + insets.bottom;
		}
		
		return results;
	}
	
	/**
	 * Gets the preferred size of this panel assuming that not all actions
	 * are shown.
	 * @return the preferred size
	 */
	public Dimension getCurrentPreferredSize(){
		int width = 0;
		int height = 0;
		
		if( orientation.isHorizontal() ){
			for( int i = 0, n = getComponentCount(); i<n; i++ ){
				Dimension preferred = getComponent( i ).getPreferredSize();
				width += preferred.width;
				height = Math.max( height, preferred.height );	
			}
		}
		else{
			for( int i = 0, n = getComponentCount(); i<n; i++ ){
				Dimension preferred = getComponent( i ).getPreferredSize();
				width = Math.max( width, preferred.width );
				height += preferred.height;
			}
		}
		
		Insets insets = getInsets();
		return new Dimension( width + insets.left + insets.right,
				height + insets.top + insets.bottom );
	}
	
	@Override
	public Dimension getPreferredSize(){
		int width = 0;
		int height = 0;
		
		if( orientation.isHorizontal() ){
			for( ActionItem entry : actions ){
				if( entry.item != null ){
					Dimension preferred = entry.item.getItem().getPreferredSize();
					width += preferred.width;
					height = Math.max( height, preferred.height );	
				}
			}
		}
		else{
			for( ActionItem entry : actions ){
				if( entry.item != null ){
					Dimension preferred = entry.item.getItem().getPreferredSize();
					width = Math.max( width, preferred.width );
					height += preferred.height;
				}
			}
		}
		
		Insets insets = getInsets();
		return new Dimension( width + insets.left + insets.right,
				height + insets.top + insets.bottom );
	}
	

	/**
	 * Sets the number of actions which should be shown directly on this panel.
	 * If not all available actions are shown, the remaining actions are put
	 * together in a menu.
	 * @param count the number of actions
	 * @see #getPreferredSizes()
	 */
	public void setVisibleActions( int count ){
		if( visibleActions != count ){
		    visibleActions = count;
		    resetVisibleActions();
		}
	}
	
	/**
	 * Update the components shown on this panel.
	 */
	private void resetVisibleActions(){
	    removeAll();
	    if( menuItem == null ){
	        for( ActionItem entry : actions ){
	            if( entry.item != null ){
	                add( entry.item.getItem() );
	            }
	        }
	    }
	    else{
	        menuSource.removeAll();

	        int set = 0;
	        int index = 0;
	        int max = actions.size();
	        int length = actions.size();

	        while( set < visibleActions && index < length ){
	            BasicTitleViewItem<JComponent> item = actions.get( index++ ).item;
	            if( item == null ){
	                max--;
	            }
	            else{
	                set++;
	                add( item.getItem() );
	            }
	        }

	        if( set < max ){
	            for( int i = set, n = actions.size(); i<n; i++ ){
	                menuSource.add( actions.get( i ).action );
	            }
	            add( menuItem.getItem() );
	        }
	    }
	}
	
	@Override
	public void doLayout(){
		Dimension current = getCurrentPreferredSize();
		Insets insets = getInsets();
		
		current.width -= insets.left + insets.right;
		current.height -= insets.top + insets.bottom;
		
		if( orientation.isHorizontal() ){
			if( current.width <= 0 )
				return;
			
			int x = insets.left;
			int y = insets.top;
			int height = getHeight() - insets.top - insets.bottom;
			int width = getWidth() - insets.left - insets.right;
			
			if( width > current.width ){
				x += width - current.width;
				width = current.width;
			}
			
			for( int i = 0, n = getComponentCount(); i<n; i++ ){
				Component c = getComponent( i );
				Dimension preferred = c.getPreferredSize();
				if( current.width <= 0 ){
					if( width <= 0 )
						width = 1;
					
					current.width = width;
				}
				
				if( width == current.width ){
					c.setBounds( x, y, preferred.width, height );
				}
				else{
					double factor = width / (double)current.width;
					c.setBounds( x, y, (int)(factor * preferred.width), height );
				}
				
				current.width -= preferred.width;
				width -= c.getWidth();
				x += c.getWidth();
				
			}
		}
		else{
			if( current.width <= 0 )
				return;
			
			int x = insets.left;
			int y = insets.top;
			int height = getHeight() - insets.top - insets.bottom;
			int width = getWidth() - insets.left - insets.bottom;
			
			if( height > current.height ){
				y += height - current.height;
				height = current.height;
			}
			
			for( int i = 0, n = getComponentCount(); i<n; i++ ){
				Component c = getComponent( i );
				Dimension preferred = c.getPreferredSize();
				if( current.height <= 0 ){
					if( height <= 0 )
						height = 1;
					current.height = height;
				}
				
				if( height == current.height ){
					c.setBounds( x, y, width, preferred.height );
				}
				else{
					double factor = height / (double)current.height;
					c.setBounds( x, y, width, (int)(factor * preferred.height) );
				}
				
				current.height -= preferred.height;
				height -= c.getHeight();
				y += c.getHeight();
			}
		}
	}
	
	/**
	 * One item on this panel.
	 * @author Benjamin Sigg
	 */
	private static class ActionItem{
		public DockAction action;
		public BasicTitleViewItem<JComponent> item;
	}
	
	private class Listener implements DockActionSourceListener, IconManagerListener, DockHierarchyListener{
		private Dockable dockable;
		private DockController controller;
		
		public void setDockable( Dockable dockable ){
			if( this.dockable != null ){
				this.dockable.removeDockHierarchyListener( this );
				this.dockable = null;
			}
			
			if( controller != null ){
				if( menuAction != null )
					controller.getIcons().remove( DockUI.OVERFLOW_MENU_ICON, this );
				controller = null;
			}
			
			this.dockable = dockable;
			
			if( dockable != null ){
				dockable.addDockHierarchyListener( this );
				controller = dockable.getController();
			}
			
			if( menuAction != null ){
				if( controller != null ){
					controller.getIcons().add( DockUI.OVERFLOW_MENU_ICON, this );
					iconChanged( DockUI.OVERFLOW_MENU_ICON, controller.getIcons().getIcon( DockUI.OVERFLOW_MENU_ICON ) );
				}
				else
					iconChanged( DockUI.OVERFLOW_MENU_ICON, null );
			}
		}
		
		public void iconChanged( String key, Icon icon ){
			if( menuAction != null )
				menuAction.setIcon( icon );
		}
		
		public void controllerChanged( DockHierarchyEvent event ){
		    if( controller != null ){
				if( menuAction != null )
					controller.getIcons().remove( DockUI.OVERFLOW_MENU_ICON, this );
				controller = null;
			}
			
			if( dockable != null ){
				controller = dockable.getController();
			}
			
			if( menuAction != null ){
				if( controller != null ){
					controller.getIcons().add( DockUI.OVERFLOW_MENU_ICON, this );
					iconChanged( DockUI.OVERFLOW_MENU_ICON, controller.getIcons().getIcon( DockUI.OVERFLOW_MENU_ICON ) );
				}
				else
					iconChanged( DockUI.OVERFLOW_MENU_ICON, null );
			}
			
			set();
		}
		
		public void hierarchyChanged( DockHierarchyEvent event ){
			// ignore
		}
		
        public void actionsAdded( DockActionSource source, int firstIndex, int lastIndex ) {
            for( int i = firstIndex; i <= lastIndex; i++ ){
                DockAction action = source.getDockAction( i );
                ActionItem entry = new ActionItem();
                entry.action = action;
                if( dockable.getController() != null ){
	                BasicTitleViewItem<JComponent> item = createItemFor( action, dockable );
	                entry.item = item;
	                
	                if( item != null ){
	                	action.bind( dockable );
	                    item.bind();
	                    item.setOrientation( orientation );
	                    item.setForeground( getForeground() );
		                item.setBackground( getBackground() );
	                }
                }
            
                actions.add( i, entry );
            }
            
            visibleActions = actions.size();
            resetVisibleActions();
            revalidate();
        }
        public void actionsRemoved( DockActionSource source, int firstIndex, int lastIndex ) {
            for( int i = lastIndex; i >= firstIndex; i-- ){
            	ActionItem entry = actions.remove( i );
                
                if( entry.item != null ){
                	entry.item.unbind();
                	entry.action.unbind( dockable );
                }
            }
            
            visibleActions = actions.size();
            resetVisibleActions();
            revalidate();
        }
	}
}
