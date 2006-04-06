/*FreeMind - A Program for creating and viewing Mindmaps
 *Copyright (C) 2000-2004  Joerg Mueller, Daniel Polansky, Christian Foltin and others.
 *
 *See COPYING for Details
 *
 *This program is free software; you can redistribute it and/or
 *modify it under the terms of the GNU General Public License
 *as published by the Free Software Foundation; either version 2
 *of the License, or (at your option) any later version.
 *
 *This program is distributed in the hope that it will be useful,
 *but WITHOUT ANY WARRANTY; without even the implied warranty of
 *MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *GNU General Public License for more details.
 *
 *You should have received a copy of the GNU General Public License
 *along with this program; if not, write to the Free Software
 *Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 *
 * Created on 08.08.2004
 */
/*$Id: MapModuleManager.java,v 1.1.4.4.2.1 2006-04-05 21:26:24 dpolivaev Exp $*/

package freemind.controller;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import freemind.modes.MindMap;
import freemind.modes.Mode;
import freemind.modes.ModeController;
import freemind.view.MapModule;
import freemind.view.mindmapview.MapView;


/**
     * Manages the list of MapModules.
     * As this task is very complex, I exported it
     * from Controller to this class to keep Controller
     * simple.
     * 
     * The information exchange between controller and this class is managed by oberser pattern (the controller observes
     * changes to the map modules here).
     */
    public class MapModuleManager {
    	
    		public static interface MapModuleChangeOberser {
    			/** The params may be null to indicate the there was no previous map,
    			 * or that the last map is closed now.
    			 * @param oldMapModule
    			 * @param newMapModule
    			 * @return
    			 */
    			boolean isMapModuleChangeAllowed(MapModule oldMapModule, Mode oldMode, MapModule newMapModule, Mode newMode);
    			void beforeMapModuleChange(MapModule oldMapModule, Mode oldMode, MapModule newMapModule, Mode newMode);
    			void afterMapModuleChange(MapModule oldMapModule, Mode oldMode, MapModule newMapModule, Mode newMode);
    			/** To enable/disable the previous/next map actions.
    			 * @param number
    			 */
    			void numberOfOpenMapInformation(int number);
    		}
    	
    		public static class MapModuleChangeOberserCompound implements MapModuleChangeOberser {
    			private HashSet listeners = new HashSet();
    			public void addListener(MapModuleChangeOberser listener) {
    				listeners.add(listener);
    			}
    			public void removeListener(MapModuleChangeOberser listener) {
    				listeners.remove(listener);
    			}
				public boolean isMapModuleChangeAllowed(MapModule oldMapModule, Mode oldMode, MapModule newMapModule, Mode newMode) {
					boolean returnValue = true;
					for (Iterator iter = listeners.iterator(); iter.hasNext();) {
						MapModuleChangeOberser observer = (MapModuleChangeOberser) iter.next();
						returnValue = observer.isMapModuleChangeAllowed(oldMapModule, oldMode, newMapModule, newMode);
						if(!returnValue){
							break;
						}
					}
					return returnValue;
				}
				public void beforeMapModuleChange(MapModule oldMapModule, Mode oldMode, MapModule newMapModule, Mode newMode) {
					for (Iterator iter = listeners.iterator(); iter.hasNext();) {
						MapModuleChangeOberser observer = (MapModuleChangeOberser) iter.next();
						observer.beforeMapModuleChange(oldMapModule, oldMode, newMapModule, newMode);
					}
				}
				public void afterMapModuleChange(MapModule oldMapModule, Mode oldMode, MapModule newMapModule, Mode newMode) {
					for (Iterator iter = listeners.iterator(); iter.hasNext();) {
						MapModuleChangeOberser observer = (MapModuleChangeOberser) iter.next();
						observer.afterMapModuleChange(oldMapModule, oldMode, newMapModule, newMode);
					}
				}
				public void numberOfOpenMapInformation(int number) {
					for (Iterator iter = listeners.iterator(); iter.hasNext();) {
						MapModuleChangeOberser observer = (MapModuleChangeOberser) iter.next();
						observer.numberOfOpenMapInformation(number);
					}
				}
    		}
    		
    		MapModuleChangeOberserCompound listener = new MapModuleChangeOberserCompound();
    		
			public void addListener(MapModuleChangeOberser pListener) {
				listener.addListener(pListener);
			}
			public void removeListener(MapModuleChangeOberser pListener) {
				listener.removeListener(pListener);
			}

        /** Contains pairs String (key+extension) => MapModule instances.
         * The instances of mode, ie. the Model/View pairs. Normally, the
        * order should be the order of insertion, but such a Map is not
        * available. */
        private Map mapModules = new HashMap();

        /** reference to the current mapmodule; null is allowed, too. */
        private MapModule mapModule;
        /**
         * Reference to the current mode as the mapModule may be null. 
         */
        private Mode mCurrentMode = null;
        
        private Controller mController;


        MapModuleManager(Controller c) {
           this.mController=c;
        }

        public Map getMapModules() {
           return mapModules; }
        
        public MapModule getMapModule() {
           return mapModule; }

        public void newMapModule(MindMap map, ModeController modeController) {
            MapModule mapModule = new MapModule(map, new MapView(map, mController),
				modeController.getMode(), modeController);
            addToMapModules(mapModule.toString(), mapModule);
            setMapModule(mapModule, modeController.getMode());
        }

        public void updateMapModuleName() {
            getMapModules().remove(getMapModule().toString());
            //removeFromViews() doesn't work because MapModuleChanged()
            //must not be called at this state
            getMapModule().rename();
            addToMapModules(getMapModule().toString(),getMapModule());
            setMapModule(getMapModule(), getMapModule().getMode());
        }

        void nextMapModule() {
            List keys = new LinkedList(getMapModules().keySet());
            int index = (getMapModule()!=null)?keys.indexOf(getMapModule().toString()):keys.size()-1;
            ListIterator i = keys.listIterator(index+1);
            if (i.hasNext()) {
               changeToMapModule((String)i.next()); }
            else if (keys.iterator().hasNext()) {
               // Change to the first in the list
               changeToMapModule((String)keys.iterator().next()); }}

        void previousMapModule() {
            List keys = new LinkedList(getMapModules().keySet());
            int index = (getMapModule()!=null)?keys.indexOf(getMapModule().toString()):0;
            ListIterator i = keys.listIterator(index);
            if (i.hasPrevious()) {
               changeToMapModule((String)i.previous()); }
            else {
               Iterator last = keys.listIterator(keys.size()-1);
               if (last.hasNext()) {
                  changeToMapModule((String)last.next()); }}}

        //Change MapModules
		/** This is the question whether the map is already opened. If this is the case,
		 * the map is automatically opened + returns true. Otherwise does nothing + returns false.*/
        public boolean tryToChangeToMapModule(String mapModule) {
            if (mapModule != null && getMapModules().containsKey(mapModule)) {
                changeToMapModule(mapModule);
                return true; }
            else {
               return false; }}

        void changeToMapModule(String mapModule) {
            MapModule map = (MapModule)(getMapModules().get(mapModule));
            setMapModule(map, map.getMode()); 
        }


    public void changeToMapOfMode(Mode mode) {
		for (Iterator i = getMapModules().keySet().iterator(); i.hasNext();) {
			String keyString = (String) i.next();
			if (((MapModule) getMapModules().get(keyString)).getMode() == mode) {
				changeToMapModule(keyString);
				return;
			}
		}
        // there is no map with the given mode open. We have to create an empty one?
        setMapModule(null, mode);
		//FIXME: Is getting here an error? fc, 25.11.2005.
	}

    /**
	 * @param newMapModule
	 *            is null if the old mode should be closed.
	 * @return true if the set command was sucessful.
	 */
	boolean setMapModule(MapModule newMapModule, Mode newMode) {
		// allowed?
		MapModule oldMapModule = this.mapModule;
		Mode oldMode = mCurrentMode;
		if(!listener.isMapModuleChangeAllowed(oldMapModule, oldMode, newMapModule, newMode)){
			return false;
		}

		listener.beforeMapModuleChange(oldMapModule, oldMode, newMapModule, newMode);
		this.mapModule = newMapModule;
		this.mCurrentMode = newMode;
		listener.afterMapModuleChange(oldMapModule, oldMode, newMapModule, newMode);
		listener.numberOfOpenMapInformation(getMapModules().keySet().size());
		return true;
	}


        //private

        private void addToMapModules(String key, MapModule newMapModule) {
            // begin bug fix, 20.12.2003, fc.
            // check, if already present:
            String extension = "";
            int count = 1;
            while (mapModules.containsKey(key+extension)) {
                extension = "<"+(++count)+">";
            }
            // rename map:
            newMapModule.setName(key+extension);
            mapModules.put(key+extension,newMapModule);
            // end bug fix, 20.12.2003, fc.
       }

        /**
        *  Close the currently active map, return false if closing cancelled.
         * @param force forces the closing without any save actions.
        */
       public boolean close(boolean force) {
       	    // (DP) The mode controller does not close the map
    	   		MapModule module = getMapModule();
    	   		// FIXME: This is not correct, as this class should not ask somebody. 
    	   		// This class is only a list!
            boolean closingNotCancelled = module.getModeController().close(force, this);
            if (!closingNotCancelled) {
               return false; }	
            
            String toBeClosed = getMapModule().toString();
            mapModules.remove(toBeClosed);
            if (mapModules.isEmpty()) {
			/*Keep the current running mode*/
			setMapModule(null, module.getMode());
		} else {
			changeToMapModule((String) mapModules.keySet().iterator().next());
		}
            return true; }

       // }}

    }