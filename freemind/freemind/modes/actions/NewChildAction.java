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
 * Created on 05.05.2004
 */
/*$Id: NewChildAction.java,v 1.1.2.4 2004-07-30 20:49:48 christianfoltin Exp $*/

package freemind.modes.actions;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.AbstractAction;
import javax.swing.ImageIcon;
import javax.xml.bind.JAXBException;

import freemind.controller.actions.ActionPair;
import freemind.controller.actions.ActorXml;
import freemind.controller.actions.generated.instance.DeleteNodeAction;
import freemind.controller.actions.generated.instance.NewNodeAction;
import freemind.controller.actions.generated.instance.XmlAction;
import freemind.modes.ControllerAdapter;
import freemind.modes.MindMapNode;
import freemind.modes.NodeAdapter;
import freemind.modes.MindMapLinkRegistry.ID_Registered;
import freemind.view.mindmapview.NodeView;


public class NewChildAction extends AbstractAction implements ActorXml {
    private final ControllerAdapter c;
    public NewChildAction(ControllerAdapter modeController) {
        super(modeController.getText("new_child"), new ImageIcon(modeController.getResource("images/newNode.png")));
        this.c = modeController;
		this.c.getActionFactory().registerActor(this, getDoActionClass());
    }

    public void actionPerformed(ActionEvent e) {
       this.c.addNew(c.getSelected(), ControllerAdapter.NEW_CHILD, null);
    }
    /* (non-Javadoc)
     * @see freemind.controller.actions.ActorXml#act(freemind.controller.actions.generated.instance.XmlAction)
     */
    public void act(XmlAction action) {
		System.out.println("NewNodeAction");
		NewNodeAction addNodeAction = (NewNodeAction) action;
		NodeAdapter parent = this.c.getNodeFromID(addNodeAction.getNode());
		int index = addNodeAction.getIndex();
		MindMapNode newNode = c.newNode();
		String newId = addNodeAction.getNewId();
		ID_Registered reg = c.getModel().getLinkRegistry().registerLinkTarget(newNode,newId);
		if(!reg.getID().equals(newId)) {
			throw new IllegalArgumentException("Designated id '"+newId+"' was not given to the node. It received '"+reg.getID()+"'.");
		}
		if(addNodeAction.getPosition()!= null) {
			newNode.setLeft(addNodeAction.getPosition().equals("left"));
		}
		c.getModel().insertNodeInto(newNode, parent, index);
		c.getFrame().repaint(); 
    }
    /* (non-Javadoc)
     * @see freemind.controller.actions.ActorXml#getDoActionClass()
     */
    public Class getDoActionClass() {
        return NewNodeAction.class;
    }
    
    
	public void addNew(final MindMapNode target, final int newNodeMode, final KeyEvent e) {
	   //closeEdit();
       
//		 MindMapNode newNode = newNode();
	   final MindMapNode targetNode = target;

	   switch (newNodeMode) {
		 case ControllerAdapter.NEW_SIBLING_BEFORE:
		 case ControllerAdapter.NEW_SIBLING_BEHIND:
		   if (targetNode.isRoot()) {
			c.getController().errorMessage(
			c.getText("new_node_as_sibling_not_possible_for_the_root"));
			c.setBlocked(false);
			 return; 
		   }
		   MindMapNode parent = targetNode.getParentNode();
		   int childPosition = parent.getChildPosition(targetNode);
		   if (newNodeMode == ControllerAdapter.NEW_SIBLING_BEHIND) {
			  childPosition++;
		   }
//			 if(targetNode.isLeft()!= null) {
//				 newNode.setLeft(targetNode.isLeft().getValue());
//			 }
		   //getModel().insertNodeInto(newNode, parent, childPosition);
		   MindMapNode newNode = addNewNode(parent, childPosition);	
		c.select(newNode.getViewer());
		c.getFrame().repaint(); //  getLayeredPane().repaint();
		c.edit.edit(newNode.getViewer(), target.getViewer(), e, true, false, false);
		   break;
  
		 case ControllerAdapter.NEW_CHILD:
		 case ControllerAdapter.NEW_CHILD_WITHOUT_FOCUS:
		   final boolean parentFolded = targetNode.isFolded();
		   if (parentFolded) {
			c.getModel().setFolded(targetNode,false);
		   }
		   int position = c.getFrame().getProperty("placenewbranches").equals("last") ?
			  targetNode.getChildCount() : 0;
		   // Here the NodeView is created for the node. 
//			 getModel().insertNodeInto(newNode, targetNode, position);
//			 getFrame().repaint(); //  getLayeredPane().repaint();
			MindMapNode  newChildNode = addNewNode(targetNode, position);	
			   if (newNodeMode == ControllerAdapter.NEW_CHILD) {
				c.select(newChildNode.getViewer());
			   }
		c.edit.edit(newChildNode.getViewer(), target.getViewer(), e, true, parentFolded, false);
		   break;
	   }
	}

	public MindMapNode addNewNode(MindMapNode parent, int index){
		try {
			String newId = c.getModel().getLinkRegistry().generateUniqueID("_");
			System.out.println("Uniq:"+newId);
			c.getActionFactory().startTransaction(c.getText("new_child"));
            NewNodeAction newNodeAction =
                getAddNodeAction(parent, index, newId);
			// Undo-action
			DeleteNodeAction deleteAction = c.deleteChild.getDeleteNodeAction(newId);
			c.getActionFactory().executeAction(new ActionPair(newNodeAction, deleteAction));
			c.getActionFactory().endTransaction(c.getText("new_child"));
			return (MindMapNode) parent.getChildAt(index);
		} catch (JAXBException e) {
			e.printStackTrace();
		}
		return null;
	}


    public NewNodeAction getAddNodeAction(
        MindMapNode parent,
        int index,
        String newId)
        throws JAXBException {
        String pos = null;
        if(parent.isLeft() != null) 
        	pos = parent.isLeft().getValue()?"left":"right";
        NewNodeAction newNodeAction = c.getActionXmlFactory().createNewNodeAction();
        newNodeAction.setNode(c.getNodeID(parent));
        newNodeAction.setPosition(pos);
        newNodeAction.setIndex(index);
        newNodeAction.setNewId(newId);
        return newNodeAction;
    }


}