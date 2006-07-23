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
 * Created on 29.09.2004
 */
/* $Id: RemoveAllIconsAction.java,v 1.1.2.2.2.1 2006-04-05 21:26:28 dpolivaev Exp $ */

package freemind.modes.mindmapmode.actions;

import java.util.Iterator;

import freemind.controller.actions.generated.instance.CompoundAction;
import freemind.controller.actions.generated.instance.RemoveAllIconsXmlAction;
import freemind.controller.actions.generated.instance.XmlAction;
import freemind.modes.MapAdapter;
import freemind.modes.MindIcon;
import freemind.modes.MindMapNode;
import freemind.modes.mindmapmode.MindMapController;
import freemind.modes.mindmapmode.actions.xml.ActionPair;

/**
 * @author foltin
 *
 */
public class RemoveAllIconsAction extends NodeGeneralAction implements NodeActorXml {

    private final IconAction addIconAction;

    /**
     * @param modeController
     * @param textID
     * @param iconPath
     * @param singleNodeOperation
     */
    public RemoveAllIconsAction(MindMapController modeController, IconAction addIconAction) {
        super(modeController, "remove_all_icons", "images/edittrash.png");
        this.addIconAction = addIconAction;
        addActor(this);
    }

    public ActionPair apply(MapAdapter model, MindMapNode selected)  {
        CompoundAction undoAction = new CompoundAction();
        for (Iterator i = selected.getIcons().iterator(); i.hasNext();) {
            MindIcon icon = (MindIcon) i.next();
            undoAction.addChoice(addIconAction.createAddIconAction(selected, icon));
        }
        return new ActionPair(createRemoveAllIconsXmlAction(selected), undoAction);
    }

    public RemoveAllIconsXmlAction createRemoveAllIconsXmlAction(MindMapNode node)  {
        RemoveAllIconsXmlAction action = new RemoveAllIconsXmlAction();
        action.setNode(node.getObjectId(modeController));
        return action;
    }

    public void act(XmlAction action) {
        if (action instanceof RemoveAllIconsXmlAction) {
            RemoveAllIconsXmlAction removeAction = (RemoveAllIconsXmlAction) action;
            MindMapNode node = modeController.getNodeFromID(removeAction.getNode());
            while(node.getIcons().size()>0) {
                node.removeLastIcon();
            }
            modeController.nodeChanged(node);
        }
    }

    public void removeAllIcons(MindMapNode node) {
        modeController.getActionFactory().startTransaction(
                (String) getValue(NAME));
        modeController.getActionFactory().executeAction(
                apply(modeController.getMap(), node));
        modeController.getActionFactory().endTransaction(
                (String) getValue(NAME));
    }

    public Class getDoActionClass() {
        return RemoveAllIconsXmlAction.class;
    }

}