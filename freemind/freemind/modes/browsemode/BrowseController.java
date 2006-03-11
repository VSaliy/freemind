/*FreeMind - A Program for creating and viewing Mindmaps
 *Copyright (C) 2000-2001  Joerg Mueller <joergmueller@bigfoot.com>
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
 */
/*$Id: BrowseController.java,v 1.13.18.7 2006-02-21 20:49:42 christianfoltin Exp $*/

package freemind.modes.browsemode;

import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Vector;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JPopupMenu;
import javax.swing.JToolBar;

import freemind.controller.MenuBar;
import freemind.controller.StructuredMenuHolder;
import freemind.extensions.HookFactory;
import freemind.main.Tools;
import freemind.modes.MapAdapter;
import freemind.modes.MindMapNode;
import freemind.modes.Mode;
import freemind.modes.ModeController;
import freemind.modes.common.GotoLinkNodeAction;
import freemind.modes.mindmapmode.hooks.MindMapHookFactory;
import freemind.modes.viewmodes.ViewControllerAdapter;
import freemind.view.mindmapview.NodeView;

public class BrowseController extends ViewControllerAdapter {

	private JPopupMenu popupmenu;

	private JToolBar toolbar;

	Action followLink;

	Action nodeUp;

	Action nodeDown;

	private HookFactory mBrowseHookFactory;

	public BrowseController(Mode mode) {
		super(mode);
		mBrowseHookFactory = new BrowseHookFactory();
		// Daniel: Actions are initialized here and not above because of
		// some error it would produce. Not studied in more detail.
		followLink = new FollowLinkAction();

		popupmenu = new BrowsePopupMenu(this);
		toolbar = new BrowseToolBar(this);
		setAllActions(false);
	}

	public MapAdapter newModel(ModeController modeController) {
		return new BrowseMapModel(getFrame(), modeController);
	}

	public void plainClick(MouseEvent e) {
        /* perform action only if one selected node.*/
        if(getSelecteds().size() != 1)
            return;
        MindMapNode node = ((NodeView)(e.getComponent())).getModel();
        if (getView().getSelected().isInFollowLinkRegion(e.getX())) {
            loadURL(); }
        else {
            if (!node.hasChildren()) {
                // the emulate the plain click. 
                doubleClick(e);
                return;
            }
            toggleFolded.toggleFolded(getSelecteds().listIterator());
        }

	}
	public void doubleClick() {
		/* If the link exists, follow the link; toggle folded otherwise */
		if (getSelected().getLink() == null) {
			toggleFolded.toggleFolded();
		} else {
			loadURL();
		}
	}

//	public void anotherNodeSelected(MindMapNode n) {
//		super.anotherNodeSelected(n);
//		if(n.isRoot()){
//			return;
//		}
//		//Presentation:
//		setFolded(n, false);
//		foldOthers(n);
//	}
//	
//	
//	private void foldOthers(MindMapNode n) {
//		if(n.isRoot()){
//			return;
//		}
//		MindMapNode parent = n.getParentNode();
//		for (Iterator iter = parent.childrenUnfolded(); iter.hasNext();) {
//			MindMapNode element = (MindMapNode) iter.next();
//			if(element != n){
//				setFolded(element, true);
//			}
//		}
//		foldOthers(parent);
//	}

	public MindMapNode newNode(Object userObject) {
		return new BrowseNodeModel(userObject, getFrame());
	}

	public JPopupMenu getPopupMenu() {
		return popupmenu;
	}

	/**
	 * Link implementation: If this is a link, we want to make a popup with at
	 * least removelink available.
	 */
	public JPopupMenu getPopupForModel(java.lang.Object obj) {
		if (obj instanceof BrowseArrowLinkModel) {
			// yes, this is a link.
			BrowseArrowLinkModel link = (BrowseArrowLinkModel) obj;
			JPopupMenu arrowLinkPopup = new JPopupMenu();

			arrowLinkPopup.add(getGotoLinkNodeAction(link.getSource()));
			arrowLinkPopup.add(getGotoLinkNodeAction(link.getTarget()));

			arrowLinkPopup.addSeparator();
			// add all links from target and from source:
			HashSet NodeAlreadyVisited = new HashSet();
			NodeAlreadyVisited.add(link.getSource());
			NodeAlreadyVisited.add(link.getTarget());
			Vector links = getModel().getLinkRegistry().getAllLinks(
					link.getSource());
			links.addAll(getModel().getLinkRegistry().getAllLinks(
					link.getTarget()));
			for (int i = 0; i < links.size(); ++i) {
				BrowseArrowLinkModel foreign_link = (BrowseArrowLinkModel) links
						.get(i);
				if (NodeAlreadyVisited.add(foreign_link.getTarget())) {
					arrowLinkPopup.add(getGotoLinkNodeAction(foreign_link
							.getTarget()));
				}
				if (NodeAlreadyVisited.add(foreign_link.getSource())) {
					arrowLinkPopup.add(getGotoLinkNodeAction(foreign_link
							.getSource()));
				}
			}
			return arrowLinkPopup;
		}
		return null;
	}

	/**
	 * @param destination
	 * @return
	 */
	private GotoLinkNodeAction getGotoLinkNodeAction(MindMapNode destination) {
		return new GotoLinkNodeAction(this, destination);
	}

	public JToolBar getModeToolBar() {
		return getToolBar();
	}
	BrowseToolBar getToolBar() {
		return (BrowseToolBar) toolbar;
	}

	public void loadURL(String relative) {
		// copy from mind map controller:
        if (relative.startsWith("#")) {
			// inner map link, fc, 12.10.2004
			String target = relative.substring(1);
			try {
				MindMapNode node = getNodeFromID(target);
				centerNode(node);
				return;
			} catch (IllegalArgumentException e) {
				// bad luck.
				getFrame().out(Tools.expandPlaceholders(getText("link_not_found"), target));
				return;
			}
		}  

		URL absolute = null;
		try {
			BrowseMapModel map = (BrowseMapModel) getMap();
			// TODO: fc, 1.2.06: How should this be zero??
			if (map != null) {
				absolute = new URL(map.getURL(), relative);
			} else {
				absolute = new URL(relative);
			}
			// absolute = new URL(relative);
			getFrame().out(absolute.toString());
		} catch (MalformedURLException ex) {
			getController().errorMessage(
					getText("url_error") + " " + ex.getMessage());
			// getFrame().err(getText("url_error"));
			return;
		}

		String type = Tools.getExtension(absolute.getFile());
		try {
			if (type.equals("mm")) {
				getFrame().setWaitingCursor(true);
				load(absolute);
			} else {
				getFrame().openDocument(absolute);
			}
		} catch (Exception ex) {
			getController().errorMessage(getText("url_load_error") + absolute);
			ex.printStackTrace();
			// for some reason, this exception is thrown anytime...
		} finally {
			getFrame().setWaitingCursor(false);
		}

	}

	public void loadURL() {
		String link = getSelected().getLink();
		if (link != null) {
			loadURL(link);
		}
	}

	public ModeController load(URL url) throws IOException {
        ModeController newModeController = super.load(url);
        // decorator pattern.
        ((BrowseToolBar) newModeController.getModeToolBar()).setURLField(url.toString());
        return newModeController;
	}

	/**
	 * Enabled/Disabled all actions that are dependent on whether there is a map
	 * open or not.
	 */
	protected void setAllActions(boolean enabled) {
		super.setAllActions(enabled);
		toggleFolded.setEnabled(enabled);
		toggleChildrenFolded.setEnabled(enabled);
		followLink.setEnabled(enabled);
	}

	// ////////
	// Actions
	// ///////

	private class FollowLinkAction extends AbstractAction {
		FollowLinkAction() {
			super(getText("follow_link"));
		}

		public void actionPerformed(ActionEvent e) {
			loadURL();
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see freemind.modes.ModeController#updateMenus(freemind.controller.StructuredMenuHolder)
	 */
	public void updateMenus(StructuredMenuHolder holder) {
		add(holder, MenuBar.EDIT_MENU + "/find/find", find, "keystroke_find");
		add(holder, MenuBar.EDIT_MENU + "/find/findNext", findNext,
				"keystroke_find_next");
		add(holder, MenuBar.EDIT_MENU + "/find/followLink", followLink,
				"keystroke_follow_link");
		holder.addSeparator(MenuBar.EDIT_MENU + "/find");
		add(holder, MenuBar.EDIT_MENU + "/find/toggleFolded", toggleFolded,
				"keystroke_toggle_folded");
		add(holder, MenuBar.EDIT_MENU + "/find/toggleChildrenFolded",
				toggleChildrenFolded, "keystroke_toggle_children_folded");
	}

	public HookFactory getHookFactory() {
		return mBrowseHookFactory;
	}
}